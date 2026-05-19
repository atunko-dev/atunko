package io.github.atunkodev.web.view;

import io.github.atunkodev.core.recipe.RecipeInfo;
import io.github.reqstool.annotations.Requirements;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Web-UI façade for coverage utilities; delegates to {@code core.recipe.RecipeCoverageUtils}. */
public final class RecipeCoverageUtils {

    private RecipeCoverageUtils() {}

    @Requirements({"atunko:WEB_0001.15"})
    public static Set<RecipeInfo> computeCovered(Set<RecipeInfo> selected) {
        return io.github.atunkodev.core.recipe.RecipeCoverageUtils.computeCovered(selected);
    }

    @Requirements({"atunko:WEB_0001.16"})
    public static Map<RecipeInfo, List<RecipeInfo>> buildReverseIndex(List<RecipeInfo> allRecipes) {
        return io.github.atunkodev.core.recipe.RecipeCoverageUtils.buildReverseIndex(allRecipes);
    }
}
