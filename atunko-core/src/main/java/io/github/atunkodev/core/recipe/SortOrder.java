package io.github.atunkodev.core.recipe;

import io.github.reqstool.annotations.Requirements;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

public enum SortOrder {
    NAME,
    TAGS;

    @Requirements({"atunko:CLI_0002.3", "atunko:CLI_0002.4", "atunko:CLI_0004.3", "atunko:CLI_0004.4"})
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
