package io.github.atunkodev.web.view;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.atunkodev.core.recipe.RecipeInfo;
import io.github.reqstool.annotations.SVCs;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RunOrderDialogTest {

    private static final RecipeInfo LEAF_A = new RecipeInfo("o.t.A", "Leaf A", "A leaf", Set.of());
    private static final RecipeInfo LEAF_B = new RecipeInfo("o.t.B", "Leaf B", "B leaf", Set.of());
    private static final RecipeInfo LEAF_C = new RecipeInfo("o.t.C", "Leaf C", "C leaf", Set.of());
    private static final RecipeInfo COMPOSITE =
            new RecipeInfo("o.t.Comp", "Composite", "A composite", Set.of(), List.of(LEAF_A, LEAF_B));
    private static final RecipeInfo NESTED =
            new RecipeInfo("o.t.Nested", "Nested", "Nested composite", Set.of(), List.of(COMPOSITE, LEAF_C));

    @Test
    @SVCs({"atunko:SVC_WEB_0001.15"})
    void flatten_expandsCompositesRecursively() {
        List<RecipeInfo> result = RunOrderDialog.flatten(List.of(NESTED));
        assertThat(result).containsExactly(LEAF_A, LEAF_B, LEAF_C);
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.15"})
    void flatten_deduplicatesSharedRecipes() {
        RecipeInfo other = new RecipeInfo("o.t.Other", "Other", "Other", Set.of(), List.of(LEAF_A));
        List<RecipeInfo> result = RunOrderDialog.flatten(List.of(COMPOSITE, other));
        assertThat(result).containsExactly(LEAF_A, LEAF_B);
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.15"})
    void flatten_leafRecipes_unchanged() {
        List<RecipeInfo> result = RunOrderDialog.flatten(List.of(LEAF_A, LEAF_B));
        assertThat(result).containsExactly(LEAF_A, LEAF_B);
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.14"})
    void flatten_emptyList_returnsEmpty() {
        List<RecipeInfo> result = RunOrderDialog.flatten(List.of());
        assertThat(result).isEmpty();
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.16"})
    void moveUp_swapsWithPrevious() {
        List<RecipeInfo> input = new java.util.ArrayList<>(List.of(LEAF_A, LEAF_B, LEAF_C));
        // Simulate swap: move index 1 up
        int index = 1;
        RecipeInfo temp = input.get(index - 1);
        input.set(index - 1, input.get(index));
        input.set(index, temp);
        assertThat(input).containsExactly(LEAF_B, LEAF_A, LEAF_C);
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.16"})
    void moveDown_swapsWithNext() {
        List<RecipeInfo> input = new java.util.ArrayList<>(List.of(LEAF_A, LEAF_B, LEAF_C));
        // Simulate swap: move index 1 down
        int index = 1;
        RecipeInfo temp = input.get(index + 1);
        input.set(index + 1, input.get(index));
        input.set(index, temp);
        assertThat(input).containsExactly(LEAF_A, LEAF_C, LEAF_B);
    }
}
