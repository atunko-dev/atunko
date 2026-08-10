package io.github.atunkodev.cli;

import io.github.atunkodev.core.RecipeToolchain;
import io.github.atunkodev.core.engine.ChangeApplier;
import io.github.atunkodev.core.engine.FileChange;
import io.github.atunkodev.core.engine.ProjectExecutionResult;
import io.github.atunkodev.core.engine.RecipeExecutionEngine;
import io.github.atunkodev.core.engine.WorkspaceExecutionEngine;
import io.github.atunkodev.core.engine.WorkspaceExecutionResult;
import io.github.atunkodev.core.git.GitCheckpointService;
import io.github.atunkodev.core.project.JavaSourceParser;
import io.github.atunkodev.core.project.ParsedSourcesCache;
import io.github.atunkodev.core.project.ProjectSourceParser;
import io.github.atunkodev.core.project.Workspace;
import io.github.atunkodev.core.project.WorkspaceScanner;
import io.github.atunkodev.daemon.AtunkoVersion;
import io.github.atunkodev.daemon.DaemonClient;
import io.github.atunkodev.daemon.protocol.DaemonMessage;
import io.github.reqstool.annotations.Requirements;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.openrewrite.SourceFile;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

@Command(
        name = "run",
        description = "Execute an OpenRewrite recipe against a project or workspace",
        mixinStandardHelpOptions = true)
public class RunCommand implements Runnable {

    @Option(
            names = {"-r", "--recipe"},
            required = true,
            description = "Fully qualified recipe name")
    private String recipe;

    @Option(names = "--project-dir", description = "Path to a single project directory")
    private Path projectDir;

    @Option(names = "--workspace", description = "Path to a workspace root — scans for all projects underneath")
    private Path workspaceDir;

    @Option(
            names = "--recipe-jar",
            description = "User recipe jar added to the execution environment (repeatable), "
                    + "so recipes discovered via `list --recipe-jar` can also be run")
    private List<Path> recipeJars = List.of();

    @Option(
            names = "--recipes-file",
            description = "User recipe YAML file added to the execution environment (repeatable), "
                    + "so recipes discovered via `list --recipes-file` can also be run")
    private List<Path> recipeFiles = List.of();

    @Option(
            names = "--no-daemon",
            description = "Execute in this process instead of through a daemon, starting and contacting none")
    private boolean noDaemon;

    @Option(
            names = "--git-checkpoint",
            description = "Create a git stash checkpoint per project before applying changes"
                    + " (restore with the printed `git restore --source=<sha> -- .` command)")
    private boolean gitCheckpoint;

    @Spec
    private CommandSpec spec;

    private final RecipeExecutionEngine engine;
    private final JavaSourceParser sourceParser;
    private final ChangeApplier changeApplier;
    private final GitCheckpointService checkpointService;
    private final DaemonClient daemonClient;

    public RunCommand() {
        this(new RecipeExecutionEngine(), new JavaSourceParser(), new ChangeApplier());
    }

    public RunCommand(RecipeExecutionEngine engine, JavaSourceParser sourceParser, ChangeApplier changeApplier) {
        this(engine, sourceParser, changeApplier, new GitCheckpointService());
    }

    public RunCommand(
            RecipeExecutionEngine engine,
            JavaSourceParser sourceParser,
            ChangeApplier changeApplier,
            GitCheckpointService checkpointService) {
        this(engine, sourceParser, changeApplier, checkpointService, new DaemonClient(AtunkoVersion.current()));
    }

    public RunCommand(
            RecipeExecutionEngine engine,
            JavaSourceParser sourceParser,
            ChangeApplier changeApplier,
            GitCheckpointService checkpointService,
            DaemonClient daemonClient) {
        this.engine = engine;
        this.sourceParser = sourceParser;
        this.changeApplier = changeApplier;
        this.checkpointService = checkpointService;
        this.daemonClient = daemonClient;
    }

    @Override
    @Requirements({"atunko:CLI_0003", "atunko:CLI_0005", "atunko:CLI_0005.1", "atunko:CLI_0005.2"})
    public void run() {
        if (workspaceDir != null) {
            runWorkspace();
        } else if (projectDir != null) {
            runSingleProject();
        } else {
            spec.commandLine().getErr().println("Error: one of --project-dir or --workspace is required");
            spec.commandLine().usage(spec.commandLine().getErr());
            throw new picocli.CommandLine.ParameterException(
                    spec.commandLine(), "one of --project-dir or --workspace is required");
        }
    }

    /**
     * Creates an optional git stash checkpoint immediately before changes are applied to {@code dir}, so the
     * snapshot reflects the files the recipe output is about to overwrite.
     *
     * <p>Degrades gracefully: a missing git binary, a directory outside any git repository, unmodified tracked
     * files, or a git failure each print a clear message and the run continues without a checkpoint — but a
     * failure is reported as a failure, never as a clean tree.
     */
    @Requirements({"atunko:CORE_0006.3"})
    private void maybeCreateGitCheckpoint(Path dir) {
        if (!gitCheckpoint) {
            return;
        }
        PrintWriter out = spec.commandLine().getOut();
        GitCheckpointService.Outcome outcome = checkpointService.checkpoint(dir);
        switch (outcome.status()) {
            case NO_GIT -> out.println("git executable not found - continuing without checkpoint");
            case NOT_A_REPOSITORY -> out.println("Not a git repository: " + dir + " - continuing without checkpoint");
            case NOTHING_TO_STASH ->
                out.println(
                        outcome.untrackedPresent()
                                ? "No tracked changes to checkpoint - untracked files are NOT covered by the"
                                        + " checkpoint"
                                : "Working tree clean - no git checkpoint needed");
            case FAILED ->
                out.println("Git checkpoint FAILED (" + outcome.detail() + ") - continuing without checkpoint");
            case CREATED -> {
                out.println("Git checkpoint created: " + outcome.checkpoint().stashSha());
                out.println("Restore with: " + outcome.checkpoint().restoreCommand());
                if (outcome.untrackedPresent()) {
                    out.println("Note: untracked files are NOT covered by the checkpoint");
                }
            }
            default -> throw new IllegalStateException("Unexpected checkpoint status: " + outcome.status());
        }
    }

    /** The injected engine, or a user-source-aware one when user recipe jars or files are in play. */
    @Requirements({"atunko:CLI_0008.1"})
    private RecipeExecutionEngine effectiveEngine() {
        return RecipeToolchain.resolve(null, engine, recipeJars, recipeFiles).engine();
    }

    private void runSingleProject() {
        PrintWriter out = spec.commandLine().getOut();

        List<FileChange> changes = daemonChanges();
        if (changes == null) {
            List<SourceFile> sources = sourceParser.parse(projectDir);
            if (sources.isEmpty()) {
                out.println("No Java source files found in " + projectDir);
                out.flush();
                return;
            }
            changes = effectiveEngine().execute(recipe, sources).changes();
        }

        reportAndApply(out, changes);
    }

    private void reportAndApply(PrintWriter out, List<FileChange> changes) {
        if (changes.isEmpty()) {
            out.println("No changes produced by recipe: " + recipe);
        } else {
            maybeCreateGitCheckpoint(projectDir);
            changeApplier.apply(projectDir, changes);
            for (FileChange change : changes) {
                out.println("Changed: " + change.path());
            }
            out.println("\n" + changes.size() + " file(s) changed.");
        }
        out.flush();
    }

    /**
     * The daemon's answer, or {@code null} when this run must execute in-process.
     *
     * <p>A daemon problem is never fatal: the reason goes to stderr as a warning and the caller re-runs the recipe
     * here, producing the same result the user would have got without a daemon at all.
     */
    @Requirements({"atunko:CLI_0009", "atunko:CLI_0009.1", "atunko:CLI_0009.2"})
    private List<FileChange> daemonChanges() {
        if (!daemonEligible()) {
            return null;
        }
        DaemonClient.Attempt attempt =
                daemonClient.execute(projectDir, new DaemonMessage.Execute(List.of(recipe), Map.of(), false));

        if (attempt.result().isEmpty()) {
            spec.commandLine()
                    .getErr()
                    .println("Daemon unavailable (" + attempt.fallbackReason() + ") - running in this process");
            return null;
        }
        if (attempt.startedDaemon()) {
            spec.commandLine()
                    .getErr()
                    .println("Started an atunko daemon for " + projectDir
                            + " (stop it with `atunko daemon stop`, or use --no-daemon)");
        }
        return attempt.result().get().changedFiles().stream()
                .map(changed -> new FileChange(Path.of(changed.path()), changed.before(), changed.after()))
                .toList();
    }

    /**
     * User recipe jars and files are deliberately excluded: the daemon builds its environment from its own
     * classpath, so a run carrying extra recipe sources would silently not see them.
     */
    @Requirements({"atunko:CLI_0009.2"})
    private boolean daemonEligible() {
        return !noDaemon && !DaemonClient.disabledByConfiguration() && recipeJars.isEmpty() && recipeFiles.isEmpty();
    }

    @Requirements({"atunko:CLI_0005", "atunko:CLI_0005.1", "atunko:CLI_0005.2"})
    private void runWorkspace() {
        PrintWriter out = spec.commandLine().getOut();
        Workspace workspace = WorkspaceScanner.scan(workspaceDir);

        if (workspace.projects().isEmpty()) {
            out.println("No projects found in workspace: " + workspaceDir);
            out.flush();
            return;
        }

        // One-shot run: every project is visited exactly once, so a live cache could never hit and would
        // only pin every project's LSTs in memory until the process exits.
        WorkspaceExecutionEngine workspaceEngine = new WorkspaceExecutionEngine(
                effectiveEngine(), new ParsedSourcesCache(new ProjectSourceParser(), false));
        WorkspaceExecutionResult result = workspaceEngine.execute(recipe, workspace);

        applyWorkspaceChanges(result);
        printSummaryTable(out, result);
        out.flush();

        if (result.hasFailures()) {
            throw new picocli.CommandLine.ExecutionException(
                    spec.commandLine(), result.failureCount() + " project(s) failed");
        }
    }

    /**
     * Applies each successful project's changes, checkpointing per project directory: workspace projects are
     * frequently independent git repositories, so one checkpoint at the workspace root could cover none of them.
     */
    private void applyWorkspaceChanges(WorkspaceExecutionResult result) {
        for (ProjectExecutionResult pr : result.results()) {
            if (pr.succeeded() && !pr.result().changes().isEmpty()) {
                maybeCreateGitCheckpoint(pr.entry().projectDir());
                changeApplier.apply(pr.entry().projectDir(), pr.result().changes());
            }
        }
    }

    private void printSummaryTable(PrintWriter out, WorkspaceExecutionResult result) {
        out.println();
        out.printf("%-50s %8s  %s%n", "Project", "Changes", "Status");
        out.println("-".repeat(70));
        for (ProjectExecutionResult pr : result.results()) {
            String name = pr.entry().projectDir().getFileName().toString();
            if (pr.succeeded()) {
                int changes = pr.result().changes().size();
                out.printf("%-50s %8d  PASS%n", name, changes);
            } else {
                out.printf(
                        "%-50s %8s  FAIL (%s)%n",
                        name,
                        "-",
                        pr.failure().getMessage() != null
                                ? pr.failure().getMessage()
                                : pr.failure().getClass().getSimpleName());
            }
        }
        out.println("-".repeat(70));
        out.printf(
                "Total: %d project(s), %d change(s), %d failure(s)%n",
                result.results().size(), result.totalChanges(), result.failureCount());
    }
}
