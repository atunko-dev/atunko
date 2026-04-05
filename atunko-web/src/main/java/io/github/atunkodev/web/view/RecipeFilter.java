package io.github.atunkodev.web.view;

import io.github.atunkodev.core.recipe.RecipeInfo;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/** Pure Java filter logic for the recipe browser — no Vaadin dependencies. */
public final class RecipeFilter {

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private RecipeFilter() {}

    public static List<RecipeInfo> filter(List<RecipeInfo> recipes, String query, Set<String> tags) {
        return recipes.stream()
                .filter(r -> matchesText(r, query))
                .filter(r -> matchesTags(r, tags))
                .toList();
    }

    @SuppressWarnings("StringSplitter")
    static boolean matchesText(RecipeInfo recipe, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        String haystack = (recipe.name() + " " + recipe.displayName() + " " + recipe.description() + " "
                        + String.join(" ", recipe.tags()))
                .toLowerCase(Locale.ROOT);
        for (String word : WHITESPACE.split(query.toLowerCase(Locale.ROOT).trim())) {
            if (!haystack.contains(word)) {
                return false;
            }
        }
        return true;
    }

    static boolean matchesTags(RecipeInfo recipe, Set<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return true;
        }
        return recipe.tags().stream().anyMatch(tags::contains);
    }
}
