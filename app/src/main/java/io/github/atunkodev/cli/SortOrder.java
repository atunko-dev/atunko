package io.github.atunkodev.cli;

import io.github.atunkodev.core.recipe.RecipeInfo;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

public enum SortOrder {
    NAME,
    TAGS;

    public Comparator<RecipeInfo> comparator() {
        return switch (this) {
            case NAME -> Comparator.comparing(r -> r.name().toLowerCase(Locale.ROOT));
            case TAGS ->
                Comparator.comparing((RecipeInfo r) -> r.tags().isEmpty()
                                ? ""
                                : Collections.min(r.tags()).toLowerCase(Locale.ROOT))
                        .thenComparing(r -> r.name().toLowerCase(Locale.ROOT));
        };
    }
}
