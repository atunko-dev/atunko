package io.github.atunkodev.core.recipe;

import io.github.reqstool.annotations.Requirements;

/**
 * Filter over the favorite state of a recipe, shared by every user interface so that favorites filtering is
 * implemented exactly once. {@link #next()} defines the cycling order used by the TUI toggle: All → Favorites → All.
 */
public enum FavoritesFilter {
    ALL,
    FAVORITES;

    /** Whether a recipe with the given favorite state passes this filter. */
    @Requirements({"atunko:CORE_0021.1"})
    public boolean matches(boolean favorite) {
        return switch (this) {
            case ALL -> true;
            case FAVORITES -> favorite;
        };
    }

    /** The next filter in the cycling order All → Favorites → All. */
    @Requirements({"atunko:CORE_0021.1"})
    public FavoritesFilter next() {
        FavoritesFilter[] filters = values();
        return filters[(ordinal() + 1) % filters.length];
    }
}
