package io.github.atunkodev.web.view;

import static com.github.mvysny.kaributesting.v10.LocatorJ._find;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.github.mvysny.kaributesting.v10.Routes;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.treegrid.TreeGrid;
import io.github.atunkodev.core.recipe.RecipeInfo;
import io.github.atunkodev.web.RecipeHolder;
import io.github.reqstool.annotations.SVCs;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class RecipeBrowserViewTest {

    private static final RecipeInfo ALPHA =
            new RecipeInfo("org.test.Alpha", "Alpha Recipe", "First recipe about spring", Set.of("java", "spring"));
    private static final RecipeInfo BETA =
            new RecipeInfo("org.test.Beta", "Beta Recipe", "Second recipe", Set.of("spring"));
    private static final RecipeInfo GAMMA =
            new RecipeInfo("org.test.Gamma", "Gamma Recipe", "Third recipe", Set.of("java"));

    private static final RecipeInfo CHILD_1 =
            new RecipeInfo("org.test.Child1", "Child One", "First child", Set.of("java"));
    private static final RecipeInfo CHILD_2 =
            new RecipeInfo("org.test.Child2", "Child Two", "Second child", Set.of("java"));
    private static final RecipeInfo COMPOSITE = new RecipeInfo(
            "org.test.Composite", "Composite Recipe", "A composite recipe", Set.of("java"), List.of(CHILD_1, CHILD_2));

    // Deep 3-level hierarchy for cascade edge cases
    private static final RecipeInfo LEVEL_3 = new RecipeInfo("o.t.L3", "Level3", "Leaf node", Set.of());
    private static final RecipeInfo LEVEL_2 =
            new RecipeInfo("o.t.L2", "Level2", "Middle node", Set.of(), List.of(LEVEL_3));
    private static final RecipeInfo LEVEL_1 =
            new RecipeInfo("o.t.L1", "Level1", "Root node", Set.of(), List.of(LEVEL_2));

    // Same recipe referenced under two different parents
    private static final RecipeInfo SHARED = new RecipeInfo("o.t.Shared", "Shared", "Shared recipe", Set.of());
    private static final RecipeInfo PARENT_A =
            new RecipeInfo("o.t.PA", "ParentA", "First parent", Set.of(), List.of(SHARED));
    private static final RecipeInfo PARENT_B =
            new RecipeInfo("o.t.PB", "ParentB", "Second parent", Set.of(), List.of(SHARED));

    private static final Routes ROUTES = new Routes().autoDiscoverViews("io.github.atunkodev.web");

    private RecipeBrowserView setupView(List<RecipeInfo> recipes) {
        RecipeHolder.init(recipes, Path.of("."));
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

    // --- Status bar (SVC_WEB_0001.7) ---

    @Test
    @SVCs({"atunko:SVC_WEB_0001.7"})
    void statusBar_noFilter_showsTotalCount() {
        RecipeBrowserView view = setupView(List.of(ALPHA, BETA, GAMMA));
        assertThat(_find(Span.class).stream().map(Span::getText)).contains("Showing 3 recipes");
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.7"})
    void statusBar_afterFilter_showsFilteredCount() {
        RecipeBrowserView view = setupView(List.of(ALPHA, BETA, GAMMA));
        view.applyTextFilter("Alpha");
        assertThat(_find(Span.class).stream().map(Span::getText)).contains("Showing 1 recipes");
    }

    // --- Detail panel (SVC_WEB_0001.8) ---

    @Test
    @SVCs({"atunko:SVC_WEB_0001.8"})
    void detailPanel_initialState_showsEmptyMessage() {
        setupView(List.of(ALPHA));
        Span placeholder = _get(Span.class, spec -> spec.withText("Select a recipe to view details"));
        assertThat(placeholder.getText()).isEqualTo("Select a recipe to view details");
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.8"})
    void detailPanel_tagsDisplay_showsAllTags() {
        RecipeBrowserView view = setupView(List.of(ALPHA));
        view.selectForDetail(ALPHA);
        // ALPHA has tags: java, spring
        List<String> spanTexts = _find(Span.class).stream().map(Span::getText).toList();
        assertThat(spanTexts).anyMatch(t -> t.startsWith("Tags:") && t.contains("java") && t.contains("spring"));
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.8"})
    void detailPanel_compositeRecipe_displaysChildList() {
        RecipeBrowserView view = setupView(List.of(COMPOSITE));
        view.selectForDetail(COMPOSITE);
        List<String> spanTexts = _find(Span.class).stream().map(Span::getText).toList();
        assertThat(spanTexts).anyMatch(t -> t.contains("Child One"));
        assertThat(spanTexts).anyMatch(t -> t.contains("Child Two"));
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.8"})
    void detailPanel_nonCompositeRecipe_noChildList() {
        RecipeBrowserView view = setupView(List.of(ALPHA));
        view.selectForDetail(ALPHA);
        List<String> spanTexts = _find(Span.class).stream().map(Span::getText).toList();
        assertThat(spanTexts).noneMatch(t -> t.startsWith("•"));
    }

    // --- Cascade edge cases (SVC_WEB_0001.9) ---

    @Test
    @SVCs({"atunko:SVC_WEB_0001.9"})
    void cascadeSelection_deepHierarchy_selectRoot_selectsAllLevels() {
        RecipeBrowserView view = setupView(List.of(LEVEL_1));
        view.getCascadeHandler().selectItem(LEVEL_1);
        assertThat(view.getSelectedRecipes()).contains(LEVEL_1, LEVEL_2, LEVEL_3);
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.9"})
    void cascadeSelection_sharedRecipe_appearsUnderTwoParents_selectedOnce() {
        RecipeBrowserView view = setupView(List.of(PARENT_A, PARENT_B));
        view.getCascadeHandler().selectItem(SHARED);
        assertThat(view.getSelectedRecipes()).contains(SHARED);
        // SHARED appears in both parent trees but should only be in selected set once
        long count =
                view.getSelectedRecipes().stream().filter(r -> r.equals(SHARED)).count();
        assertThat(count).isEqualTo(1);
    }
}
