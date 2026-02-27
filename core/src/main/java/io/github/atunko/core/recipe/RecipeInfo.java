package io.github.atunko.core.recipe;

import java.util.Set;

public record RecipeInfo(
    String name,
    String displayName,
    String description,
    Set<String> tags
) {}
