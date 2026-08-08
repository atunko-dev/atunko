package io.github.atunkodev.core.recipe;

import io.github.reqstool.annotations.Requirements;

/**
 * Filter over {@link RecipeSource}, shared by every user interface so that source filtering is implemented exactly
 * once. {@link #next()} defines the cycling order used by the TUI toggle: All → Bundled → User → All.
 */
public enum RecipeSourceFilter {
    ALL,
    BUNDLED,
    USER;

    /** Whether a recipe with the given source passes this filter. */
    @Requirements({"atunko:CORE_0019.2"})
    public boolean matches(RecipeSource source) {
        return switch (this) {
            case ALL -> true;
            case BUNDLED -> source == RecipeSource.BUNDLED;
            case USER -> source == RecipeSource.USER;
        };
    }

    /** The next filter in the cycling order All → Bundled → User → All. */
    @Requirements({"atunko:CORE_0019.2"})
    public RecipeSourceFilter next() {
        RecipeSourceFilter[] filters = values();
        return filters[(ordinal() + 1) % filters.length];
    }
}
