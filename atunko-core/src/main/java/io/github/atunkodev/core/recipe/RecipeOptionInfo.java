package io.github.atunkodev.core.recipe;

import java.util.List;
import org.jspecify.annotations.Nullable;

public record RecipeOptionInfo(
        String name,
        String type,
        String displayName,
        String description,
        @Nullable String example,
        @Nullable List<String> valid,
        boolean required,
        @Nullable Object defaultValue) {}
