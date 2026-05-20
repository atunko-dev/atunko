package io.github.atunkodev.core.recipe;

import java.util.List;
import java.util.Set;

public record RecipeInfo(
        String name,
        String displayName,
        String description,
        Set<String> tags,
        List<RecipeInfo> recipeList,
        List<RecipeOptionInfo> options) {

    public RecipeInfo(String name, String displayName, String description, Set<String> tags) {
        this(name, displayName, description, tags, List.of(), List.of());
    }

    public RecipeInfo(
            String name, String displayName, String description, Set<String> tags, List<RecipeInfo> recipeList) {
        this(name, displayName, description, tags, recipeList, List.of());
    }

    public boolean isComposite() {
        return !recipeList.isEmpty();
    }
}
