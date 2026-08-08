package io.github.atunkodev.core.recipe;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.reqstool.annotations.SVCs;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RecipeCoverageUtilsTest {

    private static RecipeInfo leaf(String name) {
        return new RecipeInfo(name, name, "", Set.of());
    }

    private static RecipeInfo composite(String name, RecipeInfo... children) {
        return new RecipeInfo(name, name, "", Set.of(), List.of(children));
    }

    // ── computeCovered ────────────────────────────────────────────────────────

    @Test
    @SVCs({"atunko:SVC_CORE_0013"})
    void computeCoveredReturnsDirectChildrenOfSelectedComposite() {
        RecipeInfo child1 = leaf("child1");
        RecipeInfo child2 = leaf("child2");
        RecipeInfo parent = composite("parent", child1, child2);

        Set<RecipeInfo> covered = RecipeCoverageUtils.computeCovered(Set.of(parent));

        assertThat(covered).containsExactlyInAnyOrder(child1, child2);
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0013"})
    void computeCoveredDoesNotIncludeCompositeItself() {
        RecipeInfo child = leaf("child");
        RecipeInfo parent = composite("parent", child);

        Set<RecipeInfo> covered = RecipeCoverageUtils.computeCovered(Set.of(parent));

        assertThat(covered).doesNotContain(parent);
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0013"})
    void computeCoveredIncludesAllTransitiveDescendants() {
        RecipeInfo grandchild = leaf("grandchild");
        RecipeInfo middle = composite("middle", grandchild);
        RecipeInfo top = composite("top", middle);

        Set<RecipeInfo> covered = RecipeCoverageUtils.computeCovered(Set.of(top));

        assertThat(covered).containsExactlyInAnyOrder(middle, grandchild);
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0013"})
    void computeCoveredReturnsEmptyForNonCompositeSelection() {
        RecipeInfo simple = leaf("simple");

        Set<RecipeInfo> covered = RecipeCoverageUtils.computeCovered(Set.of(simple));

        assertThat(covered).isEmpty();
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0013"})
    void computeCoveredReturnsEmptyForEmptySelection() {
        assertThat(RecipeCoverageUtils.computeCovered(Set.of())).isEmpty();
    }

    // ── buildReverseIndex ─────────────────────────────────────────────────────

    @Test
    @SVCs({"atunko:SVC_CORE_0013.1"})
    void buildReverseIndexMapsChildToDirectParent() {
        RecipeInfo child = leaf("child");
        RecipeInfo parent = composite("parent", child);

        Map<RecipeInfo, List<RecipeInfo>> index = RecipeCoverageUtils.buildReverseIndex(List.of(parent, child));

        assertThat(index).containsKey(child);
        assertThat(index.get(child)).containsExactly(parent);
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0013.1"})
    void buildReverseIndexDoesNotIncludeStandaloneLeaf() {
        RecipeInfo standalone = leaf("standalone");

        Map<RecipeInfo, List<RecipeInfo>> index = RecipeCoverageUtils.buildReverseIndex(List.of(standalone));

        assertThat(index).doesNotContainKey(standalone);
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0013.1"})
    void buildReverseIndexMapsSharedChildToAllParents() {
        RecipeInfo shared = leaf("shared");
        RecipeInfo parentA = composite("parentA", shared);
        RecipeInfo parentB = composite("parentB", shared);

        Map<RecipeInfo, List<RecipeInfo>> index =
                RecipeCoverageUtils.buildReverseIndex(List.of(parentA, parentB, shared));

        assertThat(index.get(shared)).containsExactlyInAnyOrder(parentA, parentB);
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0013.1"})
    void buildReverseIndexReturnsEmptyMapForEmptyInput() {
        assertThat(RecipeCoverageUtils.buildReverseIndex(List.of())).isEmpty();
    }
}
