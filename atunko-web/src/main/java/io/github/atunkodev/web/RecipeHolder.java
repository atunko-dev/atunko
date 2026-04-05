package io.github.atunkodev.web;

import io.github.atunkodev.core.recipe.RecipeInfo;
import java.util.List;

/** Static singleton bridging the CLI command into the Vaadin UI. Set once before server start. */
public final class RecipeHolder {

    private static volatile List<RecipeInfo> recipes = List.of();

    private RecipeHolder() {}

    public static void init(List<RecipeInfo> discovered) {
        recipes = List.copyOf(discovered);
    }

    public static List<RecipeInfo> getRecipes() {
        return recipes;
    }
}
