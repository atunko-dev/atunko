package io.github.atunkodev.web.view;

import static com.github.mvysny.kaributesting.v10.LocatorJ._find;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.github.mvysny.kaributesting.v10.Routes;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.treegrid.TreeGrid;
import io.github.atunkodev.core.AppServices;
import io.github.atunkodev.core.config.RecipeEntry;
import io.github.atunkodev.core.config.RunConfig;
import io.github.atunkodev.core.engine.ChangeApplier;
import io.github.atunkodev.core.engine.ExecutionResult;
import io.github.atunkodev.core.engine.FileChange;
import io.github.atunkodev.core.engine.RecipeExecutionEngine;
import io.github.atunkodev.core.project.ProjectInfo;
import io.github.atunkodev.core.project.ProjectSourceParser;
import io.github.atunkodev.core.project.SessionHolder;
import io.github.atunkodev.core.recipe.RecipeInfo;
import io.github.atunkodev.core.recipe.SortOrder;
import io.github.atunkodev.web.RecipeHolder;
import io.github.reqstool.annotations.SVCs;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
        RecipeHolder.init(recipes);
        MockVaadin.setup(ROUTES);
        return _get(RecipeBrowserView.class);
    }

    @BeforeEach
    void resetServices() {
        AppServices.init(null, null, null);
        SessionHolder.init(Path.of("."), null);
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

    // --- Bulk select/deselect (SVC_WEB_0001.22, SVC_WEB_0001.23) ---

    @Test
    @SVCs({"atunko:SVC_WEB_0001.22"})
    void selectAll_selectsAllVisibleRecipes() {
        RecipeBrowserView view = setupView(List.of(ALPHA, BETA, GAMMA));
        view.selectAllVisible();
        assertThat(view.getSelectedRecipes()).containsExactlyInAnyOrder(ALPHA, BETA, GAMMA);
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.23"})
    void deselectAll_clearsAllSelections() {
        RecipeBrowserView view = setupView(List.of(ALPHA, BETA));
        view.selectAllVisible();
        view.deselectAll();
        assertThat(view.getSelectedRecipes()).isEmpty();
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.22"})
    void selectAll_respectsActiveFilter() {
        RecipeBrowserView view = setupView(List.of(ALPHA, BETA, GAMMA));
        view.applyTextFilter("Alpha");
        view.selectAllVisible();
        assertThat(view.getSelectedRecipes()).containsExactly(ALPHA);
    }

    // --- Sorting (SVC_WEB_0001.20, SVC_WEB_0001.21) ---

    @Test
    @SVCs({"atunko:SVC_WEB_0001.20"})
    void sortByName_ordersAlphabetically() {
        RecipeBrowserView view = setupView(List.of(GAMMA, ALPHA, BETA));
        view.applySortOrder(SortOrder.NAME);
        List<String> names =
                view.getVisibleRecipes().stream().map(RecipeInfo::name).toList();
        assertThat(names).containsExactly("org.test.Alpha", "org.test.Beta", "org.test.Gamma");
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.21"})
    void sortByTags_groupsByFirstTag() {
        RecipeBrowserView view = setupView(List.of(ALPHA, BETA, GAMMA));
        view.applySortOrder(SortOrder.TAGS);
        List<String> names =
                view.getVisibleRecipes().stream().map(RecipeInfo::name).toList();
        // ALPHA tags: java, spring → first tag min = "java"
        // BETA tags: spring → first tag = "spring"
        // GAMMA tags: java → first tag = "java"
        // Sorted: java group (ALPHA, GAMMA by name), then spring group (BETA)
        assertThat(names).containsExactly("org.test.Alpha", "org.test.Gamma", "org.test.Beta");
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.20"})
    void sortChange_preservesFilter() {
        RecipeBrowserView view = setupView(List.of(GAMMA, ALPHA, BETA));
        view.applyTextFilter("Alpha");
        view.applySortOrder(SortOrder.NAME);
        assertThat(view.getVisibleRecipes()).containsExactly(ALPHA);
    }

    // --- Coverage indicators (SVC_WEB_0001.24, SVC_WEB_0001.25) ---

    @Test
    @SVCs({"atunko:SVC_WEB_0001.24"})
    void coverageIndicator_selectAll_childrenAreCovered() {
        RecipeBrowserView view = setupView(List.of(COMPOSITE, GAMMA));
        view.selectAllVisible();
        assertThat(view.getCoveredRecipes()).contains(CHILD_1, CHILD_2);
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.25"})
    void coverageIndicator_deselectAll_noCoveredRecipes() {
        RecipeBrowserView view = setupView(List.of(COMPOSITE, GAMMA));
        view.selectAllVisible();
        view.deselectAll();
        assertThat(view.getCoveredRecipes()).isEmpty();
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.24"})
    void coverageIndicator_compositeItselfNotCovered() {
        RecipeBrowserView view = setupView(List.of(COMPOSITE));
        view.selectAllVisible();
        assertThat(view.getCoveredRecipes()).doesNotContain(COMPOSITE);
    }

    // --- Included-in reverse lookup (SVC_WEB_0001.26, SVC_WEB_0001.27) ---

    @Test
    @SVCs({"atunko:SVC_WEB_0001.26"})
    void detailPanel_includedRecipe_showsIncludedInSection() {
        RecipeBrowserView view = setupView(List.of(COMPOSITE));
        view.selectForDetail(CHILD_1);
        List<String> texts = _find(Span.class).stream().map(Span::getText).toList();
        assertThat(texts).anyMatch(t -> t.startsWith("Included in:") && t.contains("Composite Recipe"));
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.27"})
    void detailPanel_notIncludedRecipe_noIncludedInSection() {
        RecipeBrowserView view = setupView(List.of(ALPHA, COMPOSITE));
        view.selectForDetail(ALPHA);
        List<String> texts = _find(Span.class).stream().map(Span::getText).toList();
        assertThat(texts).noneMatch(t -> t.startsWith("Included in:"));
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.26"})
    void detailPanel_sharedRecipe_showsMultipleParents() {
        RecipeBrowserView view = setupView(List.of(PARENT_A, PARENT_B));
        view.selectForDetail(SHARED);
        List<String> texts = _find(Span.class).stream().map(Span::getText).toList();
        assertThat(texts).anyMatch(t -> t.startsWith("Included in:") && t.contains("ParentA") && t.contains("ParentB"));
    }

    // --- Save/Load run config (SVC_WEB_0001.17, SVC_WEB_0001.18, SVC_WEB_0001.19) ---

    @Test
    @SVCs({"atunko:SVC_WEB_0001.17"})
    void applyRunConfig_selectsMatchingRecipes() {
        RecipeBrowserView view = setupView(List.of(ALPHA, BETA, GAMMA));
        RunConfig config = new RunConfig(List.of(new RecipeEntry("org.test.Alpha"), new RecipeEntry("org.test.Gamma")));
        view.applyRunConfig(config);
        assertThat(view.getSelectedRecipes()).containsExactlyInAnyOrder(ALPHA, GAMMA);
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.18"})
    void applyRunConfig_ignoresUnknownRecipes() {
        RecipeBrowserView view = setupView(List.of(ALPHA, BETA));
        RunConfig config =
                new RunConfig(List.of(new RecipeEntry("org.test.Alpha"), new RecipeEntry("org.test.NonExistent")));
        view.applyRunConfig(config);
        assertThat(view.getSelectedRecipes()).containsExactly(ALPHA);
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.19"})
    void applyRunConfig_resolvesChildRecipeName() {
        RecipeBrowserView view = setupView(List.of(COMPOSITE));
        RunConfig config = new RunConfig(List.of(new RecipeEntry("org.test.Child1")));
        view.applyRunConfig(config);
        assertThat(view.getSelectedRecipes()).contains(CHILD_1);
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.17"})
    void applyRunConfig_clearsExistingSelectionFirst() {
        RecipeBrowserView view = setupView(List.of(ALPHA, BETA, GAMMA));
        view.getCascadeHandler().selectItem(BETA);
        RunConfig config = new RunConfig(List.of(new RecipeEntry("org.test.Alpha")));
        view.applyRunConfig(config);
        assertThat(view.getSelectedRecipes()).containsExactly(ALPHA);
    }

    // --- Dry Run / Execute buttons (SVC_WEB_0001.13) ---

    @Test
    @SVCs({"atunko:SVC_WEB_0001.13"})
    void dryRunButton_isPresent() {
        RecipeBrowserView view = setupView(List.of(ALPHA));
        assertThat(view.getDryRunButton().getText()).isEqualTo("Dry Run");
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.13"})
    void executeButton_isPresent() {
        RecipeBrowserView view = setupView(List.of(ALPHA));
        assertThat(view.getExecuteButton().getText()).isEqualTo("Execute");
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.13"})
    void runRecipes_noServicesInitialised_doesNotThrow() {
        RecipeBrowserView view = setupView(List.of(ALPHA));
        view.getCascadeHandler().selectItem(ALPHA);
        // AppServices not initialised — should be a no-op, not throw
        view.runRecipes(true);
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.13"})
    void runRecipes_noSelection_doesNotThrow() {
        RecipeExecutionEngine engine = new RecipeExecutionEngine(null);
        AppServices.init(engine, new ProjectSourceParser(), new ChangeApplier());
        SessionHolder.init(Path.of("."), new ProjectInfo(List.of(), List.of(Path.of("."))));
        RecipeBrowserView view = setupView(List.of(ALPHA));
        // Nothing selected — should be a no-op, not throw
        view.runRecipes(true);
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.13"})
    void runRecipes_dryRun_doesNotApplyChanges() {
        List<FileChange>[] appliedChanges = new List[] {null};
        ChangeApplier trackingApplier = new ChangeApplier() {
            @Override
            public void apply(Path projectDir, List<FileChange> changes) {
                appliedChanges[0] = changes;
            }
        };
        RecipeExecutionEngine noOpEngine = new RecipeExecutionEngine(null) {
            @Override
            public ExecutionResult execute(String recipeName, List<org.openrewrite.SourceFile> sources) {
                return new ExecutionResult(List.of());
            }
        };
        ProjectSourceParser noOpParser = new ProjectSourceParser() {
            @Override
            public List<org.openrewrite.SourceFile> parse(io.github.atunkodev.core.project.ProjectInfo info) {
                return List.of();
            }
        };
        AppServices.init(noOpEngine, noOpParser, trackingApplier);
        SessionHolder.init(Path.of("."), new ProjectInfo(List.of(), List.of(Path.of("."))));

        RecipeBrowserView view = setupView(List.of(ALPHA));
        view.getCascadeHandler().selectItem(ALPHA);
        view.runRecipes(true);

        assertThat(appliedChanges[0]).isNull();
    }
}
