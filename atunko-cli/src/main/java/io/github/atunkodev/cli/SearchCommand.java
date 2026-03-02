package io.github.atunkodev.cli;

import io.github.atunkodev.core.recipe.RecipeDiscoveryService;
import io.github.atunkodev.core.recipe.RecipeField;
import io.github.atunkodev.core.recipe.RecipeInfo;
import io.github.atunkodev.core.recipe.SortOrder;
import io.github.reqstool.annotations.Requirements;
import java.io.PrintWriter;
import java.util.List;
import java.util.Set;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

@Command(
        name = "search",
        description = "Search available OpenRewrite recipes by keyword",
        mixinStandardHelpOptions = true)
public class SearchCommand implements Runnable {

    private static final Set<RecipeField> ALL_FIELDS = Set.of(RecipeField.values());

    @Parameters(index = "0", description = "Search query")
    private String query;

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

    @Option(names = "--field", description = "Fields to search: ${COMPLETION-CANDIDATES} (default: all)", split = ",")
    private Set<RecipeField> fields;

    @Spec
    private CommandSpec spec;

    private final RecipeDiscoveryService service;

    public SearchCommand() {
        this(new RecipeDiscoveryService());
    }

    public SearchCommand(RecipeDiscoveryService service) {
        this.service = service;
    }

    @Override
    @Requirements({"atunko:CLI_0004"})
    public void run() {
        PrintWriter out = spec.commandLine().getOut();
        Set<RecipeField> searchFields = (fields != null && !fields.isEmpty()) ? fields : ALL_FIELDS;
        List<RecipeInfo> recipes = service.search(query, searchFields).stream()
                .sorted(sort.comparator())
                .toList();
        format.render(out, recipes);
        out.flush();
    }
}
