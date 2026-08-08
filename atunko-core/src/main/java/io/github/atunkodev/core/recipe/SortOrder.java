package io.github.atunkodev.core.recipe;

import io.github.reqstool.annotations.Requirements;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public enum SortOrder {
    NAME,
    TAGS,
    RECENT;

    @Requirements({"atunko:CLI_0002.3", "atunko:CLI_0002.4", "atunko:CLI_0004.3", "atunko:CLI_0004.4"})
    public Comparator<RecipeInfo> comparator() {
        return switch (this) {
            case NAME -> Comparator.comparing(r -> r.name().toLowerCase(Locale.ROOT));
            case TAGS ->
                Comparator.comparing((RecipeInfo r) -> r.tags().isEmpty()
                                ? ""
                                : Collections.min(r.tags()).toLowerCase(Locale.ROOT))
                        .thenComparing(r -> r.name().toLowerCase(Locale.ROOT));
            // Without recent context RECENT degrades honestly to name order.
            case RECENT -> NAME.comparator();
        };
    }

    /**
     * A comparator aware of recently used recipes: for {@link #RECENT}, recipes named in {@code recentFirst} sort
     * first in that list's order (newest first), everything else follows in name order. Other sort orders ignore
     * {@code recentFirst}.
     */
    @Requirements({"atunko:CORE_0022.1"})
    public Comparator<RecipeInfo> comparator(List<String> recentFirst) {
        if (this != RECENT || recentFirst.isEmpty()) {
            return comparator();
        }
        Map<String, Integer> rank = new HashMap<>();
        for (int i = 0; i < recentFirst.size(); i++) {
            rank.putIfAbsent(recentFirst.get(i), i);
        }
        return Comparator.comparingInt((RecipeInfo r) -> rank.getOrDefault(r.name(), Integer.MAX_VALUE))
                .thenComparing(NAME.comparator());
    }
}
