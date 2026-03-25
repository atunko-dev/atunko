package io.github.atunkodev.web.view;

import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.github.mvysny.kaributesting.v10.Routes;
import com.vaadin.flow.component.treegrid.TreeGrid;
import io.github.atunkodev.core.recipe.RecipeInfo;
import io.github.atunkodev.web.RecipeHolder;
import io.github.reqstool.annotations.SVCs;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class RecipeBrowserViewTest {

    private static final RecipeInfo ALPHA =
            RecipeInfo.of("org.test.Alpha", "Alpha Recipe", "First recipe about spring", Set.of("java", "spring"));
    private static final RecipeInfo BETA =
            RecipeInfo.of("org.test.Beta", "Beta Recipe", "Second recipe", Set.of("spring"));
    private static final RecipeInfo GAMMA =
            RecipeInfo.of("org.test.Gamma", "Gamma Recipe", "Third recipe", Set.of("java"));

    private static final RecipeInfo CHILD_1 =
            RecipeInfo.of("org.test.Child1", "Child One", "First child", Set.of("java"));
    private static final RecipeInfo CHILD_2 =
            RecipeInfo.of("org.test.Child2", "Child Two", "Second child", Set.of("java"));
    private static final RecipeInfo COMPOSITE = RecipeInfo.of(
            "org.test.Composite", "Composite Recipe", "A composite recipe", Set.of("java"), List.of(CHILD_1, CHILD_2));

    private static final Routes ROUTES = new Routes().autoDiscoverViews("io.github.atunkodev.web");

    private RecipeBrowserView setupView(List<RecipeInfo> recipes) {
        RecipeHolder.init(recipes);
        MockVaadin.setup(ROUTES);
        return _get(RecipeBrowserView.class);
    }

    @AfterEach
    void tearDown() {
        MockVaadin.tearDown();
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.1"})
    void noFilter_displaysAllRecipes() {
        RecipeBrowserView view = setupView(List.of(ALPHA, BETA, GAMMA));
        assertThat(view.getVisibleRecipes()).containsExactlyInAnyOrder(ALPHA, BETA, GAMMA);
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.2"})
    void textSearch_filtersByName() {
        RecipeBrowserView view = setupView(List.of(ALPHA, BETA, GAMMA));
        view.applyTextFilter("Alpha");
        assertThat(view.getVisibleRecipes()).containsExactly(ALPHA);
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.2"})
    void textSearch_filtersByDescription() {
        RecipeBrowserView view = setupView(List.of(ALPHA, BETA, GAMMA));
        view.applyTextFilter("about spring");
        assertThat(view.getVisibleRecipes()).containsExactly(ALPHA);
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.2"})
    void tagFilter_filtersWithOrLogic() {
        RecipeBrowserView view = setupView(List.of(ALPHA, BETA, GAMMA));
        view.applyTagFilter(Set.of("spring"));
        assertThat(view.getVisibleRecipes()).containsExactlyInAnyOrder(ALPHA, BETA);
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.2"})
    void textAndTagFilter_composeWithAnd() {
        RecipeBrowserView view = setupView(List.of(ALPHA, BETA, GAMMA));
        view.applyTextFilter("Alpha");
        view.applyTagFilter(Set.of("spring"));
        assertThat(view.getVisibleRecipes()).containsExactly(ALPHA);
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.2"})
    void tagFilter_emptySelection_showsAllRecipes() {
        RecipeBrowserView view = setupView(List.of(ALPHA, BETA, GAMMA));
        view.applyTagFilter(Set.of());
        assertThat(view.getVisibleRecipes()).containsExactlyInAnyOrder(ALPHA, BETA, GAMMA);
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.4"})
    void compositeRecipe_isExpandable() {
        RecipeBrowserView view = setupView(List.of(COMPOSITE, GAMMA));
        TreeGrid<TreeNode> grid = view.getTreeGrid();
        TreeNode compositeNode = new TreeNode(COMPOSITE, COMPOSITE.name());
        assertThat(grid.isExpanded(compositeNode)).isFalse();
        grid.expand(compositeNode);
        assertThat(grid.isExpanded(compositeNode)).isTrue();
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.5"})
    void cascadeSelection_checkParent_checksAllChildren() {
        RecipeBrowserView view = setupView(List.of(COMPOSITE));
        view.getCascadeHandler().selectItem(COMPOSITE);
        assertThat(view.getSelectedRecipes()).contains(COMPOSITE, CHILD_1, CHILD_2);
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.5"})
    void cascadeSelection_checkAllChildren_checksParent() {
        RecipeBrowserView view = setupView(List.of(COMPOSITE));
        view.getCascadeHandler().selectItem(CHILD_1);
        view.getCascadeHandler().selectItem(CHILD_2);
        assertThat(view.getSelectedRecipes()).contains(COMPOSITE);
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.5"})
    void cascadeSelection_uncheckOneChild_parentBecomesIndeterminate() {
        RecipeBrowserView view = setupView(List.of(COMPOSITE));
        view.getCascadeHandler().selectItem(COMPOSITE);
        view.getCascadeHandler().deselectItem(CHILD_1);
        assertThat(view.getSelectedRecipes()).doesNotContain(COMPOSITE);
        assertThat(view.getCascadeHandler().isIndeterminate(COMPOSITE)).isTrue();
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.6"})
    void clickRecipe_updatesDetailPanel() {
        RecipeBrowserView view = setupView(List.of(ALPHA, BETA));
        view.selectForDetail(ALPHA);
        assertThat(view.getDetailPanelRecipe()).isEqualTo(ALPHA);
    }
}
