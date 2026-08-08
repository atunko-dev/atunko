package io.github.atunkodev.cli;

import io.github.atunkodev.core.engine.ChangeApplier;
import io.github.atunkodev.core.engine.ExecutionResult;
import io.github.atunkodev.core.engine.FileChange;
import io.github.atunkodev.core.engine.ProjectExecutionResult;
import io.github.atunkodev.core.engine.RecipeExecutionEngine;
import io.github.atunkodev.core.engine.WorkspaceExecutionEngine;
import io.github.atunkodev.core.engine.WorkspaceExecutionResult;
import io.github.atunkodev.core.git.GitCheckpoint;
import io.github.atunkodev.core.git.GitService;
import io.github.atunkodev.core.project.JavaSourceParser;
import io.github.atunkodev.core.project.ParsedSourcesCache;
import io.github.atunkodev.core.project.ProjectSourceParser;
import io.github.atunkodev.core.project.Workspace;
import io.github.atunkodev.core.project.WorkspaceScanner;
import io.github.reqstool.annotations.Requirements;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
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
            description = "Create a git stash checkpoint before applying changes (undo with `git stash apply <sha>`)")
    private boolean gitCheckpoint;

    @Spec
    private CommandSpec spec;

    private final RecipeExecutionEngine engine;
    private final JavaSourceParser sourceParser;
    private final ChangeApplier changeApplier;
    private final GitService gitService = new GitService();

    public RunCommand() {
        this(new RecipeExecutionEngine(), new JavaSourceParser(), new ChangeApplier());
    }

    public RunCommand(RecipeExecutionEngine engine, JavaSourceParser sourceParser, ChangeApplier changeApplier) {
        this.engine = engine;
        this.sourceParser = sourceParser;
        this.changeApplier = changeApplier;
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
     * Creates an optional git stash checkpoint before changes are applied.
     *
     * <p>Degrades gracefully: a missing git binary, a directory outside any git repository, or a clean working tree
     * each print a clear message and the run continues without a checkpoint.
     */
    @Requirements({"atunko:CORE_0006.3"})
    private void maybeCreateGitCheckpoint(Path dir) {
        if (!gitCheckpoint) {
            return;
        }
        PrintWriter out = spec.commandLine().getOut();
        if (!gitService.isGitAvailable()) {
            out.println("git executable not found - continuing without checkpoint");
            return;
        }
        if (!gitService.isGitRepository(dir)) {
            out.println("Not a git repository: " + dir + " - continuing without checkpoint");
            return;
        }
        String message = "atunko: pre-recipe " + Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Optional<GitCheckpoint> checkpoint = gitService.createCheckpoint(dir, message);
        if (checkpoint.isPresent()) {
            out.println("Git checkpoint created: " + checkpoint.orElseThrow().stashSha());
            out.println("Restore with: " + checkpoint.orElseThrow().restoreCommand());
        } else {
            out.println("Working tree clean - no git checkpoint needed");
        }
    }

    private void runSingleProject() {
        PrintWriter out = spec.commandLine().getOut();
        maybeCreateGitCheckpoint(projectDir);
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
        maybeCreateGitCheckpoint(workspaceDir);
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

        printSummaryTable(out, result);
        out.flush();

        if (result.hasFailures()) {
            throw new picocli.CommandLine.ExecutionException(
                    spec.commandLine(), result.failureCount() + " project(s) failed");
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
                if (changes > 0) {
                    changeApplier.apply(pr.entry().projectDir(), pr.result().changes());
                }
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
