package io.github.atunkodev.cli;

import io.github.atunkodev.core.recipe.RecipeDiscoveryService;
import io.github.atunkodev.core.recipe.RecipeInfo;
import io.github.reqstool.annotations.Requirements;
import java.io.PrintWriter;
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
    @Requirements({"CLI_0002"})
    public void run() {
        PrintWriter out = spec.commandLine().getOut();
        List<RecipeInfo> recipes =
                service.discoverAll().stream().sorted(sort.comparator()).toList();
        format.render(out, recipes);
        out.flush();
    }
}
