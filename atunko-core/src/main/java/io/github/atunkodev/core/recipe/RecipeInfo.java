package io.github.atunkodev.core.recipe;

import java.util.List;
import java.util.Set;

public record RecipeInfo(
        String name,
        String displayName,
        String description,
        Set<String> tags,
        List<RecipeInfo> recipeList,
        List<RecipeOptionInfo> options,
        RecipeSource source) {

    public RecipeInfo(String name, String displayName, String description, Set<String> tags) {
        this(name, displayName, description, tags, List.of(), List.of());
    }

    public RecipeInfo(
            String name, String displayName, String description, Set<String> tags, List<RecipeInfo> recipeList) {
        this(name, displayName, description, tags, recipeList, List.of());
    }

    public RecipeInfo(
            String name,
            String displayName,
            String description,
            Set<String> tags,
            List<RecipeInfo> recipeList,
            List<RecipeOptionInfo> options) {
        this(name, displayName, description, tags, recipeList, options, RecipeSource.BUNDLED);
    }

    public boolean isComposite() {
        return !recipeList.isEmpty();
    }
}
