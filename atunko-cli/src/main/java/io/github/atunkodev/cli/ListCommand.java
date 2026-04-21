package io.github.atunkodev.cli;

import io.github.atunkodev.core.project.WorkspaceScanner;
import io.github.atunkodev.core.recipe.RecipeDiscoveryService;
import io.github.atunkodev.core.recipe.RecipeInfo;
import io.github.atunkodev.core.recipe.SortOrder;
import io.github.reqstool.annotations.Requirements;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.List;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

@Command(name = "list", description = "List all available OpenRewrite recipes", mixinStandardHelpOptions = true)
public class ListCommand implements Runnable {

    @Option(
            names = "--format",
            description = "Output format: ${COMPLETION-CANDIDATES} (default: ${DEFAULT-VALUE})",
            defaultValue = "TEXT")
    private OutputFormat format;

    @Option(
            names = "--sort",
            description = "Sort order: ${COMPLETION-CANDIDATES} (default: ${DEFAULT-VALUE})",
            defaultValue = "NAME")
    private SortOrder sort;

    @Option(
            names = "--workspace",
            description = "Path to a workspace root — lists discovered project paths instead of recipes")
    private Path workspaceDir;

    @Spec
    private CommandSpec spec;

    private final RecipeDiscoveryService service;

    public ListCommand() {
        this(new RecipeDiscoveryService());
    }

    public ListCommand(RecipeDiscoveryService service) {
        this.service = service;
    }

    @Override
    @Requirements({"atunko:CLI_0002", "atunko:CLI_0005"})
    public void run() {
        if (workspaceDir != null) {
            listWorkspaceProjects();
        } else {
            listRecipes();
        }
    }

    @Requirements({"atunko:CLI_0002"})
    private void listRecipes() {
        PrintWriter out = spec.commandLine().getOut();
        List<RecipeInfo> recipes =
                service.discoverAll().stream().sorted(sort.comparator()).toList();
        format.render(out, recipes);
        out.flush();
    }

    @Requirements({"atunko:CLI_0005"})
    private void listWorkspaceProjects() {
        PrintWriter out = spec.commandLine().getOut();
        List<Path> candidates = WorkspaceScanner.discoverProjectDirs(
                workspaceDir.toAbsolutePath().normalize());
        if (candidates.isEmpty()) {
            out.println("No projects found in workspace: " + workspaceDir);
        } else {
            out.println("Projects in workspace " + workspaceDir + ":");
            candidates.forEach(p -> out.println("  " + p));
        }
        out.flush();
    }
}
