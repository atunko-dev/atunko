package io.github.atunkodev.cli;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.atunkodev.core.recipe.RecipeInfo;
import io.github.reqstool.annotations.Requirements;
import java.io.PrintWriter;
import java.util.List;

public enum OutputFormat {
    TEXT,
    JSON;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public void render(PrintWriter out, List<RecipeInfo> recipes) {
        if (recipes.isEmpty()) {
            out.println("No recipes found.");
            return;
        }
        switch (this) {
            case TEXT -> renderText(out, recipes);
            case JSON -> renderJson(out, recipes);
            default -> throw new IllegalStateException("Unsupported format: " + this);
        }
    }

    @Requirements({"atunko:CLI_0002.1", "atunko:CLI_0004.1"})
    private void renderText(PrintWriter out, List<RecipeInfo> recipes) {
        for (RecipeInfo recipe : recipes) {
            out.println(recipe.name() + " - " + recipe.description());
        }
        out.println("\n" + recipes.size() + " recipe(s) found.");
    }

    @Requirements({"atunko:CLI_0002.2", "atunko:CLI_0004.2"})
    private void renderJson(PrintWriter out, List<RecipeInfo> recipes) {
        try {
            out.println(MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(recipes));
        } catch (JsonProcessingException e) {
            out.println("Error writing JSON: " + e.getMessage());
        }
    }
}
