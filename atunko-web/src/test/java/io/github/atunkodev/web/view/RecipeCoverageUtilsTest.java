package io.github.atunkodev.web.view;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.atunkodev.core.recipe.RecipeInfo;
import io.github.reqstool.annotations.SVCs;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RecipeCoverageUtilsTest {

    private static final RecipeInfo LEAF_A = new RecipeInfo("o.t.A", "Leaf A", "A leaf", Set.of());
    private static final RecipeInfo LEAF_B = new RecipeInfo("o.t.B", "Leaf B", "B leaf", Set.of());
    private static final RecipeInfo LEAF_C = new RecipeInfo("o.t.C", "Leaf C", "C leaf", Set.of());
    private static final RecipeInfo COMPOSITE_1 =
            new RecipeInfo("o.t.C1", "Composite1", "First composite", Set.of(), List.of(LEAF_A, LEAF_B));
    private static final RecipeInfo NESTED_COMPOSITE =
            new RecipeInfo("o.t.NC", "NestedComposite", "Nested", Set.of(), List.of(COMPOSITE_1, LEAF_C));

    // --- computeCovered ---

    @Test
    @SVCs({"atunko:SVC_WEB_0001.25"})
    void computeCoveredSingleCompositeReturnsChildren() {
        Set<RecipeInfo> covered = RecipeCoverageUtils.computeCovered(Set.of(COMPOSITE_1));
        assertThat(covered).containsExactlyInAnyOrder(LEAF_A, LEAF_B);
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.25"})
    void computeCoveredNestedCompositeReturnsAllDescendants() {
        Set<RecipeInfo> covered = RecipeCoverageUtils.computeCovered(Set.of(NESTED_COMPOSITE));
        assertThat(covered).containsExactlyInAnyOrder(COMPOSITE_1, LEAF_A, LEAF_B, LEAF_C);
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.25"})
    void computeCoveredNoCompositeSelectedReturnsEmpty() {
        Set<RecipeInfo> covered = RecipeCoverageUtils.computeCovered(Set.of(LEAF_A));
        assertThat(covered).isEmpty();
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.25"})
    void computeCoveredCompositeItselfNotMarkedAsCovered() {
        Set<RecipeInfo> covered = RecipeCoverageUtils.computeCovered(Set.of(COMPOSITE_1));
        assertThat(covered).doesNotContain(COMPOSITE_1);
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.25"})
    void computeCoveredEmptySelectionReturnsEmpty() {
        Set<RecipeInfo> covered = RecipeCoverageUtils.computeCovered(Set.of());
        assertThat(covered).isEmpty();
    }

    // --- buildReverseIndex ---

    @Test
    @SVCs({"atunko:SVC_WEB_0001.27"})
    void buildReverseIndexChildInOneCompositeMapsToParent() {
        Map<RecipeInfo, List<RecipeInfo>> index = RecipeCoverageUtils.buildReverseIndex(List.of(COMPOSITE_1));
        assertThat(index.get(LEAF_A)).containsExactly(COMPOSITE_1);
        assertThat(index.get(LEAF_B)).containsExactly(COMPOSITE_1);
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.27"})
    void buildReverseIndexChildInMultipleCompositesMapsToAll() {
        RecipeInfo other = new RecipeInfo("o.t.Other", "Other", "Other composite", Set.of(), List.of(LEAF_A));
        Map<RecipeInfo, List<RecipeInfo>> index = RecipeCoverageUtils.buildReverseIndex(List.of(COMPOSITE_1, other));
        assertThat(index.get(LEAF_A)).containsExactlyInAnyOrder(COMPOSITE_1, other);
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.27"})
    void buildReverseIndexNoCompositesEmptyMap() {
        Map<RecipeInfo, List<RecipeInfo>> index = RecipeCoverageUtils.buildReverseIndex(List.of(LEAF_A, LEAF_B));
        assertThat(index).isEmpty();
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.27"})
    void buildReverseIndexNestedCompositeMapsAllLevels() {
        Map<RecipeInfo, List<RecipeInfo>> index = RecipeCoverageUtils.buildReverseIndex(List.of(NESTED_COMPOSITE));
        assertThat(index.get(COMPOSITE_1)).containsExactly(NESTED_COMPOSITE);
        assertThat(index.get(LEAF_C)).containsExactly(NESTED_COMPOSITE);
        assertThat(index.get(LEAF_A)).containsExactly(COMPOSITE_1);
        assertThat(index.get(LEAF_B)).containsExactly(COMPOSITE_1);
    }
}
