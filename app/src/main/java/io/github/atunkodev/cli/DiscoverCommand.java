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

@Command(
        name = "discover",
        description = "List and search available OpenRewrite recipes",
        mixinStandardHelpOptions = true)
public class DiscoverCommand implements Runnable {

    @Option(names = "--search", description = "Filter recipes by keyword")
    private String search;

    @Spec
    private CommandSpec spec;

    @Override
    @Requirements({"CLI_0002"})
    public void run() {
        PrintWriter out = spec.commandLine().getOut();
        RecipeDiscoveryService service = new RecipeDiscoveryService();

        List<RecipeInfo> recipes;
        if (search != null && !search.isBlank()) {
            recipes = service.search(search);
        } else {
            recipes = service.discoverAll();
        }

        if (recipes.isEmpty()) {
            out.println("No recipes found.");
        } else {
            for (RecipeInfo recipe : recipes) {
                out.println(recipe.name() + " - " + recipe.description());
            }
            out.println("\n" + recipes.size() + " recipe(s) found.");
        }
        out.flush();
    }
}
