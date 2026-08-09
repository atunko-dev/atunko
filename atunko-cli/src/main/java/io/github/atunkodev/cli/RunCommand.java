package io.github.atunkodev.cli;

import io.github.atunkodev.core.engine.ChangeApplier;
import io.github.atunkodev.core.engine.ExecutionResult;
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
import io.github.reqstool.annotations.Requirements;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.List;
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
        this.engine = engine;
        this.sourceParser = sourceParser;
        this.changeApplier = changeApplier;
        this.checkpointService = checkpointService;
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

    private void runSingleProject() {
        PrintWriter out = spec.commandLine().getOut();
        List<SourceFile> sources = sourceParser.parse(projectDir);

        if (sources.isEmpty()) {
            out.println("No Java source files found in " + projectDir);
            out.flush();
            return;
        }

        ExecutionResult result = engine.execute(recipe, sources);

        if (result.changes().isEmpty()) {
            out.println("No changes produced by recipe: " + recipe);
        } else {
            maybeCreateGitCheckpoint(projectDir);
            changeApplier.apply(projectDir, result.changes());
            for (FileChange change : result.changes()) {
                out.println("Changed: " + change.path());
            }
            out.println("\n" + result.changes().size() + " file(s) changed.");
        }
        out.flush();
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
        WorkspaceExecutionEngine workspaceEngine =
                new WorkspaceExecutionEngine(engine, new ParsedSourcesCache(new ProjectSourceParser(), false));
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
