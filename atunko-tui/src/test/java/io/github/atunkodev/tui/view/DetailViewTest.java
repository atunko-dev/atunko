package io.github.atunkodev.tui.view;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.atunkodev.core.recipe.RecipeInfo;
import io.github.reqstool.annotations.SVCs;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

@SVCs({"atunko:SVC_TUI_0001.4"})
class DetailViewTest {

    private static final RecipeInfo LEAF =
            new RecipeInfo("org.test.Leaf", "Leaf Recipe", "A leaf recipe", Set.of("java"));
    private static final RecipeInfo SUB_1 = new RecipeInfo("org.test.Sub1", "Sub 1", "First sub", Set.of());
    private static final RecipeInfo SUB_2 = new RecipeInfo("org.test.Sub2", "Sub 2", "Second sub", Set.of());
    private static final RecipeInfo SUB_3 = new RecipeInfo("org.test.Sub3", "Sub 3", "Third sub", Set.of());
    private static final RecipeInfo COMPOSITE = new RecipeInfo(
            "org.test.Composite", "Composite Recipe", "A composite", Set.of(), List.of(SUB_1, SUB_2, SUB_3));

    @Test
    @SVCs({"atunko:SVC_TUI_0001.4"})
    void metadataLineCountLeafNoParents() {
        // 4 base + 1 buffer = 5
        assertThat(DetailView.metadataLineCount(LEAF, 0)).isEqualTo(5);
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.4"})
    void metadataLineCountLeafWithParents() {
        // 4 base + 2 (blank + included-in row) + 1 buffer = 7
        assertThat(DetailView.metadataLineCount(LEAF, 2)).isEqualTo(7);
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.4"})
    void metadataLineCountCompositeNoParents() {
        // 4 base + 2 (blank + "Recipe List:" label) + 3 sub-recipes + 1 buffer = 10
        assertThat(DetailView.metadataLineCount(COMPOSITE, 0)).isEqualTo(10);
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.4"})
    void metadataLineCountCompositeWithParents() {
        // 4 base + 2 + 3 subs + 2 (blank + included-in) + 1 buffer = 12
        assertThat(DetailView.metadataLineCount(COMPOSITE, 1)).isEqualTo(12);
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.4"})
    void metadataLineCountBufferIsAlwaysPresent() {
        int withoutParents = DetailView.metadataLineCount(LEAF, 0);
        int withParents = DetailView.metadataLineCount(LEAF, 1);

        // buffer means result is always > the raw row count
        assertThat(withoutParents).isGreaterThan(4);
        assertThat(withParents).isGreaterThan(6);
    }
}
