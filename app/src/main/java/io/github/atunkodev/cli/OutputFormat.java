package io.github.atunkodev.cli;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.atunkodev.core.recipe.RecipeInfo;
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
            case TEXT -> {
                for (RecipeInfo recipe : recipes) {
                    out.println(recipe.name() + " - " + recipe.description());
                }
                out.println("\n" + recipes.size() + " recipe(s) found.");
            }
            case JSON -> {
                try {
                    out.println(MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(recipes));
                } catch (JsonProcessingException e) {
                    out.println("Error writing JSON: " + e.getMessage());
                }
            }
            default -> throw new IllegalStateException("Unsupported format: " + this);
        }
    }
}
