package io.github.atunkodev.web.view;

import io.github.atunkodev.core.recipe.RecipeInfo;
import io.github.reqstool.annotations.Requirements;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Pure utility for coverage and reverse-index computation on recipe trees. No Vaadin dependencies.
 */
public final class RecipeCoverageUtils {

    private RecipeCoverageUtils() {}

    /**
     * For each selected composite, recursively collect all descendants via {@code recipeList()}.
     * The composite itself is NOT marked as covered — only its transitive children are.
     */
    @Requirements({"atunko:WEB_0001.15"})
    public static Set<RecipeInfo> computeCovered(Set<RecipeInfo> selected) {
        Set<RecipeInfo> covered = new HashSet<>();
        for (RecipeInfo recipe : selected) {
            if (recipe.isComposite()) {
                collectDescendants(recipe, covered, new HashSet<>());
            }
        }
        return covered;
    }

    private static void collectDescendants(RecipeInfo recipe, Set<RecipeInfo> result, Set<RecipeInfo> visited) {
        for (RecipeInfo child : recipe.recipeList()) {
            if (visited.add(child)) {
                result.add(child);
                if (child.isComposite()) {
                    collectDescendants(child, result, visited);
                }
            }
        }
    }

    /**
     * Build a reverse index: for each recipe that appears in any composite's {@code recipeList()},
     * map it to the list of composites that directly contain it.
     */
    @Requirements({"atunko:WEB_0001.16"})
    public static Map<RecipeInfo, List<RecipeInfo>> buildReverseIndex(List<RecipeInfo> allRecipes) {
        Map<RecipeInfo, List<RecipeInfo>> result = new HashMap<>();
        for (RecipeInfo recipe : allRecipes) {
            if (recipe.isComposite()) {
                collectParentage(recipe, result, new HashSet<>());
            }
        }
        return result;
    }

    private static void collectParentage(
            RecipeInfo parent, Map<RecipeInfo, List<RecipeInfo>> result, Set<RecipeInfo> visited) {
        if (!visited.add(parent)) {
            return;
        }
        for (RecipeInfo child : parent.recipeList()) {
            result.computeIfAbsent(child, k -> new ArrayList<>()).add(parent);
            if (child.isComposite()) {
                collectParentage(child, result, visited);
            }
        }
    }
}
