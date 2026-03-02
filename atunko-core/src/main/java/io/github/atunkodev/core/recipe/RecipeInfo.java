package io.github.atunkodev.core.recipe;

import java.util.List;
import java.util.Set;

public record RecipeInfo(
        String name, String displayName, String description, Set<String> tags, List<RecipeInfo> recipeList) {

    public RecipeInfo(String name, String displayName, String description, Set<String> tags) {
        this(name, displayName, description, tags, List.of());
    }

    public boolean isComposite() {
        return !recipeList.isEmpty();
    }
}
