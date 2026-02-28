package io.github.atunkodev.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.atunkodev.core.recipe.RecipeDiscoveryService;
import io.github.atunkodev.core.recipe.RecipeInfo;
import io.github.reqstool.annotations.Requirements;
import java.io.PrintWriter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
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
        List<RecipeInfo> recipes = sorted(service.discoverAll());
        render(out, recipes);
        out.flush();
    }

    private List<RecipeInfo> sorted(List<RecipeInfo> recipes) {
        Comparator<RecipeInfo> comparator =
                switch (sort) {
                    case TAGS ->
                        Comparator.comparing((RecipeInfo r) -> r.tags().isEmpty()
                                        ? ""
                                        : r.tags().iterator().next().toLowerCase(Locale.ROOT))
                                .thenComparing(r -> r.name().toLowerCase(Locale.ROOT));
                    case NAME -> Comparator.comparing(r -> r.name().toLowerCase(Locale.ROOT));
                };
        return recipes.stream().sorted(comparator).toList();
    }

    private void render(PrintWriter out, List<RecipeInfo> recipes) {
        if (recipes.isEmpty()) {
            out.println("No recipes found.");
            return;
        }
        switch (format) {
            case TEXT -> {
                for (RecipeInfo recipe : recipes) {
                    out.println(recipe.name() + " - " + recipe.description());
                }
                out.println("\n" + recipes.size() + " recipe(s) found.");
            }
            case JSON -> {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(recipes));
                } catch (Exception e) {
                    out.println("Error writing JSON: " + e.getMessage());
                }
            }
            default -> throw new IllegalStateException("Unsupported format: " + format);
        }
    }
}
