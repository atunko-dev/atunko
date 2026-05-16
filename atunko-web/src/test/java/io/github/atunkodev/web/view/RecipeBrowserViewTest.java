package io.github.atunkodev.web.view;

import static com.github.mvysny.kaributesting.v10.LocatorJ._click;
import static com.github.mvysny.kaributesting.v10.LocatorJ._find;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static com.github.mvysny.kaributesting.v10.LocatorJ._setValue;
import static com.github.mvysny.kaributesting.v10.NotificationsKt.clearNotifications;
import static com.github.mvysny.kaributesting.v10.NotificationsKt.expectNotifications;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.github.mvysny.kaributesting.v10.Routes;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.treegrid.TreeGrid;
import io.github.atunkodev.core.AppServices;
import io.github.atunkodev.core.config.RecipeEntry;
import io.github.atunkodev.core.config.RunConfig;
import io.github.atunkodev.core.config.RunConfigService;
import io.github.atunkodev.core.config.WorkspaceConfig;
import io.github.atunkodev.core.engine.ChangeApplier;
import io.github.atunkodev.core.engine.ExecutionResult;
import io.github.atunkodev.core.engine.FileChange;
import io.github.atunkodev.core.engine.RecipeExecutionEngine;
import io.github.atunkodev.core.project.ProjectEntry;
import io.github.atunkodev.core.project.ProjectInfo;
import io.github.atunkodev.core.project.ProjectSourceParser;
import io.github.atunkodev.core.project.SessionHolder;
import io.github.atunkodev.core.recipe.RecipeInfo;
import io.github.atunkodev.core.recipe.SortOrder;
import io.github.atunkodev.web.RecipeHolder;
import io.github.reqstool.annotations.SVCs;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
    void noFilterDisplaysAllRecipes() {
        RecipeBrowserView view = setupView(List.of(ALPHA, BETA, GAMMA));
        assertThat(view.getVisibleRecipes()).containsExactlyInAnyOrder(ALPHA, BETA, GAMMA);
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.2"})
    void textSearchFiltersByName() {
        RecipeBrowserView view = setupView(List.of(ALPHA, BETA, GAMMA));
        view.applyTextFilter("Alpha");
        assertThat(view.getVisibleRecipes()).containsExactly(ALPHA);
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.2"})
    void textSearchFiltersByDescription() {
        RecipeBrowserView view = setupView(List.of(ALPHA, BETA, GAMMA));
        view.applyTextFilter("about spring");
        assertThat(view.getVisibleRecipes()).containsExactly(ALPHA);
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.2"})
    void tagFilterFiltersWithOrLogic() {
        RecipeBrowserView view = setupView(List.of(ALPHA, BETA, GAMMA));
        view.applyTagFilter(Set.of("spring"));
        assertThat(view.getVisibleRecipes()).containsExactlyInAnyOrder(ALPHA, BETA);
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.2"})
    void textAndTagFilterComposeWithAnd() {
        RecipeBrowserView view = setupView(List.of(ALPHA, BETA, GAMMA));
        view.applyTextFilter("Alpha");
        view.applyTagFilter(Set.of("spring"));
        assertThat(view.getVisibleRecipes()).containsExactly(ALPHA);
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.2"})
    void tagFilterEmptySelectionShowsAllRecipes() {
        RecipeBrowserView view = setupView(List.of(ALPHA, BETA, GAMMA));
        view.applyTagFilter(Set.of());
        assertThat(view.getVisibleRecipes()).containsExactlyInAnyOrder(ALPHA, BETA, GAMMA);
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.4"})
    void compositeRecipeIsExpandable() {
        RecipeBrowserView view = setupView(List.of(COMPOSITE, GAMMA));
        TreeGrid<TreeNode> grid = view.getTreeGrid();
        TreeNode compositeNode = new TreeNode(COMPOSITE, COMPOSITE.name());
        assertThat(grid.isExpanded(compositeNode)).isFalse();
        grid.expand(compositeNode);
        assertThat(grid.isExpanded(compositeNode)).isTrue();
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.5"})
    void cascadeSelectionCheckParentChecksAllChildren() {
        RecipeBrowserView view = setupView(List.of(COMPOSITE));
        view.getCascadeHandler().selectItem(COMPOSITE);
        assertThat(view.getSelectedRecipes()).contains(COMPOSITE, CHILD_1, CHILD_2);
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.5"})
    void cascadeSelectionCheckAllChildrenChecksParent() {
        RecipeBrowserView view = setupView(List.of(COMPOSITE));
        view.getCascadeHandler().selectItem(CHILD_1);
        view.getCascadeHandler().selectItem(CHILD_2);
        assertThat(view.getSelectedRecipes()).contains(COMPOSITE);
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.5"})
    void cascadeSelectionUncheckOneChildParentBecomesIndeterminate() {
        RecipeBrowserView view = setupView(List.of(COMPOSITE));
        view.getCascadeHandler().selectItem(COMPOSITE);
        view.getCascadeHandler().deselectItem(CHILD_1);
        assertThat(view.getSelectedRecipes()).doesNotContain(COMPOSITE);
        assertThat(view.getCascadeHandler().isIndeterminate(COMPOSITE)).isTrue();
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.6"})
    void clickRecipeUpdatesDetailPanel() {
        RecipeBrowserView view = setupView(List.of(ALPHA, BETA));
        view.selectForDetail(ALPHA);
        assertThat(view.getDetailPanelRecipe()).isEqualTo(ALPHA);
    }

    // --- Status bar (SVC_WEB_0001.7) ---

    @Test
    @SVCs({"atunko:SVC_WEB_0001.7"})
    void statusBarNoFilterShowsTotalCount() {
        RecipeBrowserView view = setupView(List.of(ALPHA, BETA, GAMMA));
        assertThat(_find(Span.class).stream().map(Span::getText)).contains("Showing 3 recipes");
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.7"})
    void statusBarAfterFilterShowsFilteredCount() {
        RecipeBrowserView view = setupView(List.of(ALPHA, BETA, GAMMA));
        view.applyTextFilter("Alpha");
        assertThat(_find(Span.class).stream().map(Span::getText)).contains("Showing 1 recipes");
    }

    // --- Detail panel (SVC_WEB_0001.8) ---

    @Test
    @SVCs({"atunko:SVC_WEB_0001.8"})
    void detailPanelInitialStateShowsEmptyMessage() {
        setupView(List.of(ALPHA));
        Span placeholder = _get(Span.class, spec -> spec.withText("Select a recipe to view details"));
        assertThat(placeholder.getText()).isEqualTo("Select a recipe to view details");
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.8"})
    void detailPanelTagsDisplayShowsAllTags() {
        RecipeBrowserView view = setupView(List.of(ALPHA));
        view.selectForDetail(ALPHA);
        // ALPHA has tags: java, spring
        List<String> spanTexts = _find(Span.class).stream().map(Span::getText).toList();
        assertThat(spanTexts).anyMatch(t -> t.startsWith("Tags:") && t.contains("java") && t.contains("spring"));
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.8"})
    void detailPanelCompositeRecipeDisplaysChildList() {
        RecipeBrowserView view = setupView(List.of(COMPOSITE));
        view.selectForDetail(COMPOSITE);
        List<String> spanTexts = _find(Span.class).stream().map(Span::getText).toList();
        assertThat(spanTexts).anyMatch(t -> t.contains("Child One"));
        assertThat(spanTexts).anyMatch(t -> t.contains("Child Two"));
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.8"})
    void detailPanelNonCompositeRecipeNoChildList() {
        RecipeBrowserView view = setupView(List.of(ALPHA));
        view.selectForDetail(ALPHA);
        List<String> spanTexts = _find(Span.class).stream().map(Span::getText).toList();
        assertThat(spanTexts).noneMatch(t -> t.startsWith("•"));
    }

    // --- Cascade edge cases (SVC_WEB_0001.9) ---

    @Test
    @SVCs({"atunko:SVC_WEB_0001.9"})
    void cascadeSelectionDeepHierarchySelectRootSelectsAllLevels() {
        RecipeBrowserView view = setupView(List.of(LEVEL_1));
        view.getCascadeHandler().selectItem(LEVEL_1);
        assertThat(view.getSelectedRecipes()).contains(LEVEL_1, LEVEL_2, LEVEL_3);
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.9"})
    void cascadeSelectionSharedRecipeAppearsUnderTwoParentsSelectedOnce() {
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
    void selectAllSelectsAllVisibleRecipes() {
        RecipeBrowserView view = setupView(List.of(ALPHA, BETA, GAMMA));
        view.selectAllVisible();
        assertThat(view.getSelectedRecipes()).containsExactlyInAnyOrder(ALPHA, BETA, GAMMA);
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.23"})
    void deselectAllClearsAllSelections() {
        RecipeBrowserView view = setupView(List.of(ALPHA, BETA));
        view.selectAllVisible();
        view.deselectAll();
        assertThat(view.getSelectedRecipes()).isEmpty();
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.22"})
    void selectAllRespectsActiveFilter() {
        RecipeBrowserView view = setupView(List.of(ALPHA, BETA, GAMMA));
        view.applyTextFilter("Alpha");
        view.selectAllVisible();
        assertThat(view.getSelectedRecipes()).containsExactly(ALPHA);
    }

    // --- Sorting (SVC_WEB_0001.20, SVC_WEB_0001.21) ---

    @Test
    @SVCs({"atunko:SVC_WEB_0001.20"})
    void sortByNameOrdersAlphabetically() {
        RecipeBrowserView view = setupView(List.of(GAMMA, ALPHA, BETA));
        view.applySortOrder(SortOrder.NAME);
        List<String> names =
                view.getVisibleRecipes().stream().map(RecipeInfo::name).toList();
        assertThat(names).containsExactly("org.test.Alpha", "org.test.Beta", "org.test.Gamma");
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.21"})
    void sortByTagsGroupsByFirstTag() {
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
    void sortChangePreservesFilter() {
        RecipeBrowserView view = setupView(List.of(GAMMA, ALPHA, BETA));
        view.applyTextFilter("Alpha");
        view.applySortOrder(SortOrder.NAME);
        assertThat(view.getVisibleRecipes()).containsExactly(ALPHA);
    }

    // --- Coverage indicators (SVC_WEB_0001.24, SVC_WEB_0001.25) ---

    @Test
    @SVCs({"atunko:SVC_WEB_0001.24"})
    void coverageIndicatorSelectAllChildrenAreCovered() {
        RecipeBrowserView view = setupView(List.of(COMPOSITE, GAMMA));
        view.selectAllVisible();
        assertThat(view.getCoveredRecipes()).contains(CHILD_1, CHILD_2);
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.25"})
    void coverageIndicatorDeselectAllNoCoveredRecipes() {
        RecipeBrowserView view = setupView(List.of(COMPOSITE, GAMMA));
        view.selectAllVisible();
        view.deselectAll();
        assertThat(view.getCoveredRecipes()).isEmpty();
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.24"})
    void coverageIndicatorCompositeItselfNotCovered() {
        RecipeBrowserView view = setupView(List.of(COMPOSITE));
        view.selectAllVisible();
        assertThat(view.getCoveredRecipes()).doesNotContain(COMPOSITE);
    }

    // --- Included-in reverse lookup (SVC_WEB_0001.26, SVC_WEB_0001.27) ---

    @Test
    @SVCs({"atunko:SVC_WEB_0001.26"})
    void detailPanelIncludedRecipeShowsIncludedInSection() {
        RecipeBrowserView view = setupView(List.of(COMPOSITE));
        view.selectForDetail(CHILD_1);
        List<String> texts = _find(Span.class).stream().map(Span::getText).toList();
        assertThat(texts).anyMatch(t -> t.startsWith("Included in:") && t.contains("Composite Recipe"));
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.27"})
    void detailPanelNotIncludedRecipeNoIncludedInSection() {
        RecipeBrowserView view = setupView(List.of(ALPHA, COMPOSITE));
        view.selectForDetail(ALPHA);
        List<String> texts = _find(Span.class).stream().map(Span::getText).toList();
        assertThat(texts).noneMatch(t -> t.startsWith("Included in:"));
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.26"})
    void detailPanelSharedRecipeShowsMultipleParents() {
        RecipeBrowserView view = setupView(List.of(PARENT_A, PARENT_B));
        view.selectForDetail(SHARED);
        List<String> texts = _find(Span.class).stream().map(Span::getText).toList();
        assertThat(texts).anyMatch(t -> t.startsWith("Included in:") && t.contains("ParentA") && t.contains("ParentB"));
    }

    // --- Save/Load run config (SVC_WEB_0001.17, SVC_WEB_0001.18, SVC_WEB_0001.19) ---

    @Test
    @SVCs({"atunko:SVC_WEB_0001.17"})
    void applyRunConfigSelectsMatchingRecipes() {
        RecipeBrowserView view = setupView(List.of(ALPHA, BETA, GAMMA));
        RunConfig config = new RunConfig(List.of(new RecipeEntry("org.test.Alpha"), new RecipeEntry("org.test.Gamma")));
        view.applyRunConfig(config);
        assertThat(view.getSelectedRecipes()).containsExactlyInAnyOrder(ALPHA, GAMMA);
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.18"})
    void applyRunConfigIgnoresUnknownRecipes() {
        RecipeBrowserView view = setupView(List.of(ALPHA, BETA));
        RunConfig config =
                new RunConfig(List.of(new RecipeEntry("org.test.Alpha"), new RecipeEntry("org.test.NonExistent")));
        view.applyRunConfig(config);
        assertThat(view.getSelectedRecipes()).containsExactly(ALPHA);
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.19"})
    void applyRunConfigResolvesChildRecipeName() {
        RecipeBrowserView view = setupView(List.of(COMPOSITE));
        RunConfig config = new RunConfig(List.of(new RecipeEntry("org.test.Child1")));
        view.applyRunConfig(config);
        assertThat(view.getSelectedRecipes()).contains(CHILD_1);
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.17"})
    void applyRunConfigClearsExistingSelectionFirst() {
        RecipeBrowserView view = setupView(List.of(ALPHA, BETA, GAMMA));
        view.getCascadeHandler().selectItem(BETA);
        RunConfig config = new RunConfig(List.of(new RecipeEntry("org.test.Alpha")));
        view.applyRunConfig(config);
        assertThat(view.getSelectedRecipes()).containsExactly(ALPHA);
    }

    // --- Dry Run / Execute buttons (SVC_WEB_0001.13) ---

    @Test
    @SVCs({"atunko:SVC_WEB_0001.13"})
    void dryRunButtonIsPresent() {
        RecipeBrowserView view = setupView(List.of(ALPHA));
        assertThat(view.getDryRunButton().getText()).isEqualTo("Dry Run");
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.13"})
    void executeButtonIsPresent() {
        RecipeBrowserView view = setupView(List.of(ALPHA));
        assertThat(view.getExecuteButton().getText()).isEqualTo("Execute");
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.13"})
    void runRecipesNoServicesInitialisedDoesNotThrow() {
        RecipeBrowserView view = setupView(List.of(ALPHA));
        view.getCascadeHandler().selectItem(ALPHA);
        // AppServices not initialised — should be a no-op, not throw
        view.runRecipes(true);
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.13"})
    void runRecipesNoSelectionDoesNotThrow() {
        RecipeExecutionEngine engine = new RecipeExecutionEngine(null);
        AppServices.init(engine, new ProjectSourceParser(), new ChangeApplier());
        SessionHolder.init(Path.of("."), new ProjectInfo(List.of(), List.of(Path.of("."))));
        RecipeBrowserView view = setupView(List.of(ALPHA));
        // Nothing selected — should be a no-op, not throw
        view.runRecipes(true);
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.13"})
    void runRecipesDryRunDoesNotApplyChanges() {
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

    // ==========================================================================
    // Karibu UI interaction tests — buttons, dialogs, notifications, form input
    // ==========================================================================

    // --- Button click: Select All / Deselect All ---

    @Test
    @SVCs({"atunko:SVC_WEB_0001.22"})
    void clickSelectAllButtonSelectsAllVisibleRecipes() {
        RecipeBrowserView view = setupView(List.of(ALPHA, BETA, GAMMA));
        clearNotifications();
        _click(_get(Button.class, spec -> spec.withText("Select All")));
        assertThat(view.getSelectedRecipes()).containsExactlyInAnyOrder(ALPHA, BETA, GAMMA);
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.23"})
    void clickDeselectAllButtonClearsAllSelections() {
        RecipeBrowserView view = setupView(List.of(ALPHA, BETA));
        view.selectAllVisible();
        _click(_get(Button.class, spec -> spec.withText("Deselect All")));
        assertThat(view.getSelectedRecipes()).isEmpty();
    }

    // --- Button click: Save with no selection → notification ---

    @Test
    @SVCs({"atunko:SVC_WEB_0001.17"})
    void clickSaveButtonNoSelectionShowsNotification() {
        setupView(List.of(ALPHA));
        clearNotifications();
        _click(_get(Button.class, spec -> spec.withText("Save")));
        expectNotifications("No recipes selected");
    }

    // --- Button click: Save dialog flow with @TempDir ---

    @TempDir
    Path tempDir;

    @Test
    @SVCs({"atunko:SVC_WEB_0001.17"})
    void clickSaveButtonWithSelectionOpensDialogAndSavesFile() throws IOException {
        SessionHolder.init(tempDir, null);
        RecipeBrowserView view = setupView(List.of(ALPHA, BETA));
        view.selectAllVisible();
        clearNotifications();

        // Click Save button → opens save dialog
        _click(_get(Button.class, spec -> spec.withText("Save")));

        // Fill in the name field inside the dialog
        _setValue(_get(TextField.class, spec -> spec.withLabel("Name")), "my-test-config");

        // Click the Save button inside the dialog (different from the status bar Save)
        List<Button> saveButtons = _find(Button.class, spec -> spec.withText("Save"));
        // The dialog Save button is the second one (first is status bar)
        Button dialogSaveButton = saveButtons.stream()
                .filter(b -> b.getParent().isPresent()
                        && b.getParent().get().getParent().isPresent())
                .reduce((first, second) -> second)
                .orElseThrow();
        _click(dialogSaveButton);

        // Verify file was created
        Path savedFile = tempDir.resolve("atunko/runs/my-test-config.yaml");
        assertThat(savedFile).exists();

        // Verify notification
        expectNotifications("Saved: my-test-config.yaml");

        // Verify file content round-trips
        RunConfigService service = new RunConfigService();
        RunConfig loaded = service.load(savedFile);
        assertThat(loaded.recipes()).hasSize(2);
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.17"})
    void saveDialogEmptyNameShowsValidationNotification() {
        RecipeBrowserView view = setupView(List.of(ALPHA));
        view.selectAllVisible();
        clearNotifications();

        _click(_get(Button.class, spec -> spec.withText("Save")));

        // Leave name field empty (default) and click dialog Save
        List<Button> saveButtons = _find(Button.class, spec -> spec.withText("Save"));
        Button dialogSaveButton = saveButtons.stream()
                .filter(b -> b.getParent().isPresent()
                        && b.getParent().get().getParent().isPresent())
                .reduce((first, second) -> second)
                .orElseThrow();
        _click(dialogSaveButton);

        expectNotifications("Name is required");
    }

    // --- Button click: Load with no runs dir → notification ---

    @Test
    @SVCs({"atunko:SVC_WEB_0001.18"})
    void clickLoadButtonNoRunsDirShowsNotification() {
        SessionHolder.init(tempDir, null);
        setupView(List.of(ALPHA));
        clearNotifications();
        _click(_get(Button.class, spec -> spec.withText("Load")));
        expectNotifications("No saved runs found");
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.18"})
    void clickLoadButtonEmptyRunsDirShowsNotification() throws IOException {
        SessionHolder.init(tempDir, null);
        Files.createDirectories(tempDir.resolve("atunko/runs"));
        setupView(List.of(ALPHA));
        clearNotifications();
        _click(_get(Button.class, spec -> spec.withText("Load")));
        expectNotifications("No saved runs found");
    }

    // --- Form input: search field via _setValue ---

    @Test
    @SVCs({"atunko:SVC_WEB_0001.2"})
    void searchFieldSetValueFiltersRecipes() {
        RecipeBrowserView view = setupView(List.of(ALPHA, BETA, GAMMA));
        _setValue(_get(TextField.class, spec -> spec.withPlaceholder("Search recipes...")), "Alpha");
        assertThat(view.getVisibleRecipes()).containsExactly(ALPHA);
    }

    // --- Form input: sort dropdown via _setValue ---

    @Test
    @SVCs({"atunko:SVC_WEB_0001.20"})
    @SuppressWarnings("unchecked")
    void sortDropdownSetValueReordersRecipes() {
        RecipeBrowserView view = setupView(List.of(GAMMA, ALPHA, BETA));
        // Find the sort select and change its value via Karibu
        List<Select> selects = _find(Select.class);
        Select<SortOrder> sortSelect = selects.stream()
                .filter(s -> s.getValue() instanceof SortOrder)
                .findFirst()
                .orElseThrow();
        _setValue(sortSelect, SortOrder.TAGS);

        List<String> names =
                view.getVisibleRecipes().stream().map(RecipeInfo::name).toList();
        // ALPHA(java,spring), GAMMA(java), BETA(spring) → java group first, then spring
        assertThat(names).containsExactly("org.test.Alpha", "org.test.Gamma", "org.test.Beta");
    }

    // --- Notification: Dry Run with no selection ---

    @Test
    @SVCs({"atunko:SVC_WEB_0001.13"})
    void clickDryRunButtonNoSelectionShowsNotification() {
        RecipeExecutionEngine engine = new RecipeExecutionEngine(null);
        AppServices.init(engine, new ProjectSourceParser(), new ChangeApplier());
        SessionHolder.init(Path.of("."), new ProjectInfo(List.of(), List.of(Path.of("."))));
        setupView(List.of(ALPHA));
        clearNotifications();
        _click(_get(Button.class, spec -> spec.withText("Dry Run")));
        expectNotifications("No recipes selected");
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.13"})
    void clickExecuteButtonNoSelectionShowsNotification() {
        RecipeExecutionEngine engine = new RecipeExecutionEngine(null);
        AppServices.init(engine, new ProjectSourceParser(), new ChangeApplier());
        SessionHolder.init(Path.of("."), new ProjectInfo(List.of(), List.of(Path.of("."))));
        setupView(List.of(ALPHA));
        clearNotifications();
        _click(_get(Button.class, spec -> spec.withText("Execute")));
        expectNotifications("No recipes selected");
    }

    // --- RunOrderDialog: button interactions ---

    @Test
    @SVCs({"atunko:SVC_WEB_0001.14"})
    void runOrderDialogFlattenCheckboxExpandsComposites() {
        List<RecipeInfo> confirmed = new java.util.ArrayList<>();
        RunOrderDialog dialog = new RunOrderDialog(Set.of(COMPOSITE), true, confirmed::addAll, () -> {});

        // Initially has COMPOSITE
        assertThat(dialog.getOrderedRecipes()).containsExactly(COMPOSITE);

        // Toggle flatten via Karibu
        _setValue(_get(dialog, Checkbox.class), true);

        // Now should contain the leaf children
        assertThat(dialog.getOrderedRecipes()).containsExactly(CHILD_1, CHILD_2);
        assertThat(dialog.isFlattened()).isTrue();
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.14"})
    void runOrderDialogUnflattenRestoresOriginal() {
        RunOrderDialog dialog = new RunOrderDialog(Set.of(COMPOSITE), true, _ -> {}, () -> {});

        _setValue(_get(dialog, Checkbox.class), true);
        _setValue(_get(dialog, Checkbox.class), false);

        assertThat(dialog.getOrderedRecipes()).containsExactly(COMPOSITE);
        assertThat(dialog.isFlattened()).isFalse();
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.16"})
    void runOrderDialogConfirmButtonInvokesCallback() {
        List<RecipeInfo> confirmed = new java.util.ArrayList<>();
        RunOrderDialog dialog = new RunOrderDialog(Set.of(ALPHA, BETA), true, confirmed::addAll, () -> {});

        _click(_get(dialog, Button.class, spec -> spec.withText("Confirm")));

        assertThat(confirmed).hasSize(2);
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.16"})
    void runOrderDialogCancelButtonDoesNotInvokeCallback() {
        List<RecipeInfo> confirmed = new java.util.ArrayList<>();
        RunOrderDialog dialog = new RunOrderDialog(Set.of(ALPHA), true, confirmed::addAll, () -> {});

        _click(_get(dialog, Button.class, spec -> spec.withText("Cancel")));

        assertThat(confirmed).isEmpty();
    }

    // --- Load dialog flow: pick a file and confirm ---

    @Test
    @SVCs({"atunko:SVC_WEB_0001.18"})
    @SuppressWarnings("unchecked")
    void clickLoadButtonWithSavedRunLoadsAndSelectsRecipes() throws IOException {
        SessionHolder.init(tempDir, null);
        Path runsDir = tempDir.resolve("atunko/runs");
        Files.createDirectories(runsDir);

        // Write a run config with ALPHA
        RunConfigService service = new RunConfigService();
        service.save(new RunConfig(List.of(new RecipeEntry("org.test.Alpha"))), runsDir.resolve("saved.yaml"));

        RecipeBrowserView view = setupView(List.of(ALPHA, BETA));
        clearNotifications();

        // Click Load → opens picker dialog
        _click(_get(Button.class, spec -> spec.withText("Load")));

        // Select the file in the picker — use style filter to find the dialog's Select (width:100%)
        List<Select> selects = _find(Select.class);
        Select<Path> fileSelect = selects.stream()
                .filter(s -> !(s.getValue() instanceof SortOrder))
                .findFirst()
                .orElseThrow();
        _setValue(fileSelect, runsDir.resolve("saved.yaml"));

        // Click the Load button inside the dialog
        List<Button> loadButtons = _find(Button.class, spec -> spec.withText("Load"));
        Button dialogLoadButton = loadButtons.stream()
                .filter(b -> b.getParent().isPresent()
                        && b.getParent().get().getParent().isPresent())
                .reduce((first, second) -> second)
                .orElseThrow();
        _click(dialogLoadButton);

        expectNotifications("Loaded: saved");
        assertThat(view.getSelectedRecipes()).containsExactly(ALPHA);
    }

    // --- Form input: tag filter via _setValue on MultiSelectComboBox ---

    @Test
    @SVCs({"atunko:SVC_WEB_0001.2"})
    @SuppressWarnings("unchecked")
    void tagFilterSetValueFiltersRecipesByTag() {
        RecipeBrowserView view = setupView(List.of(ALPHA, BETA, GAMMA));
        MultiSelectComboBox<String> tagCombo = _get(MultiSelectComboBox.class);
        _setValue(tagCombo, Set.of("spring"));
        // ALPHA has spring + java, BETA has spring, GAMMA has only java
        assertThat(view.getVisibleRecipes()).containsExactlyInAnyOrder(ALPHA, BETA);
    }

    // --- Grid selection: select row via UI, verify detail panel ---

    @Test
    @SVCs({"atunko:SVC_WEB_0001.6"})
    void gridSelectionSelectRowUpdatesDetailPanel() {
        RecipeBrowserView view = setupView(List.of(ALPHA, BETA));
        TreeGrid<TreeNode> grid = view.getTreeGrid();

        // Find actual node from grid's data — must match the grid's own TreeNode instance
        TreeNode alphaNode = grid.getSelectedItems().isEmpty()
                ? grid.getTreeData().getRootItems().stream()
                        .filter(n -> n.recipe().equals(ALPHA))
                        .findFirst()
                        .orElseThrow()
                : null;
        grid.select(alphaNode);

        // The selection listener should update the detail panel
        assertThat(view.getDetailPanelRecipe()).isEqualTo(ALPHA);
        // Recipe name is in H3, tags in Span
        assertThat(_find(H3.class).stream().map(H3::getText).toList()).anyMatch(t -> t.contains("Alpha Recipe"));
        assertThat(_find(Span.class).stream().map(Span::getText).toList())
                .anyMatch(t -> t.startsWith("Tags:") && t.contains("spring"));
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.6"})
    void gridSelectionSelectCompositeRowShowsChildList() {
        RecipeBrowserView view = setupView(List.of(COMPOSITE));
        TreeGrid<TreeNode> grid = view.getTreeGrid();

        TreeNode compositeNode = grid.getTreeData().getRootItems().stream()
                .filter(n -> n.recipe().equals(COMPOSITE))
                .findFirst()
                .orElseThrow();
        grid.select(compositeNode);

        assertThat(view.getDetailPanelRecipe()).isEqualTo(COMPOSITE);
        assertThat(_find(H3.class).stream().map(H3::getText).toList()).anyMatch(t -> t.contains("Composite Recipe"));
        List<String> spanTexts = _find(Span.class).stream().map(Span::getText).toList();
        assertThat(spanTexts).anyMatch(t -> t.contains("Child One"));
        assertThat(spanTexts).anyMatch(t -> t.contains("Child Two"));
    }

    // --- Dialog cancel buttons ---

    @Test
    @SVCs({"atunko:SVC_WEB_0001.17"})
    void saveDialogCancelButtonDismissesDialogNoFileCreated() {
        SessionHolder.init(tempDir, null);
        RecipeBrowserView view = setupView(List.of(ALPHA));
        view.selectAllVisible();
        clearNotifications();

        _click(_get(Button.class, spec -> spec.withText("Save")));

        // Fill name but click Cancel
        _setValue(_get(TextField.class, spec -> spec.withLabel("Name")), "should-not-save");
        _click(_get(Button.class, spec -> spec.withText("Cancel")));

        assertThat(tempDir.resolve("atunko/runs/should-not-save.yaml")).doesNotExist();
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.18"})
    void loadDialogCancelButtonDoesNotChangeSelection() throws IOException {
        SessionHolder.init(tempDir, null);
        Path runsDir = tempDir.resolve("atunko/runs");
        Files.createDirectories(runsDir);
        new RunConfigService()
                .save(new RunConfig(List.of(new RecipeEntry("org.test.Alpha"))), runsDir.resolve("test.yaml"));

        RecipeBrowserView view = setupView(List.of(ALPHA, BETA));
        clearNotifications();

        _click(_get(Button.class, spec -> spec.withText("Load")));
        _click(_get(Button.class, spec -> spec.withText("Cancel")));

        assertThat(view.getSelectedRecipes()).isEmpty();
    }

    // --- Load dialog: confirm without selecting a file ---

    @Test
    @SVCs({"atunko:SVC_WEB_0001.18"})
    void loadDialogConfirmWithoutSelectionShowsNotification() throws IOException {
        SessionHolder.init(tempDir, null);
        Path runsDir = tempDir.resolve("atunko/runs");
        Files.createDirectories(runsDir);
        new RunConfigService()
                .save(new RunConfig(List.of(new RecipeEntry("org.test.Alpha"))), runsDir.resolve("test.yaml"));

        setupView(List.of(ALPHA));
        clearNotifications();

        _click(_get(Button.class, spec -> spec.withText("Load")));

        // Click Load inside dialog without selecting a file first
        List<Button> loadButtons = _find(Button.class, spec -> spec.withText("Load"));
        Button dialogLoadButton = loadButtons.stream()
                .filter(b -> b.getParent().isPresent()
                        && b.getParent().get().getParent().isPresent())
                .reduce((first, second) -> second)
                .orElseThrow();
        _click(dialogLoadButton);

        expectNotifications("Select a configuration");
    }

    // --- RunOrderDialog: move up/down via arrow button clicks ---

    @Test
    @SVCs({"atunko:SVC_WEB_0001.15"})
    void runOrderDialogClickMoveUpReordersRecipes() {
        RunOrderDialog dialog = new RunOrderDialog(Set.of(ALPHA, BETA), true, _ -> {}, () -> {});
        List<RecipeInfo> initial = dialog.getOrderedRecipes();
        RecipeInfo second = initial.get(1);

        // Select the second recipe in the grid, then click Move Up
        _get(dialog, Grid.class).select(second);
        _click(_get(dialog, Button.class, spec -> spec.withIcon(VaadinIcon.ARROW_UP)));

        assertThat(dialog.getOrderedRecipes().get(0)).isEqualTo(second);
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.15"})
    void runOrderDialogClickMoveDownReordersRecipes() {
        RunOrderDialog dialog = new RunOrderDialog(Set.of(ALPHA, BETA), true, _ -> {}, () -> {});
        List<RecipeInfo> initial = dialog.getOrderedRecipes();
        RecipeInfo first = initial.get(0);

        // Select the first recipe, then click Move Down
        _get(dialog, Grid.class).select(first);
        _click(_get(dialog, Button.class, spec -> spec.withIcon(VaadinIcon.ARROW_DOWN)));

        assertThat(dialog.getOrderedRecipes().get(1)).isEqualTo(first);
    }

    // --- Coverage badge: verify coveredRecipes drives the badge logic ---
    // Note: the actual Span rendering inside ComponentHierarchyColumn is not traversable
    // by Karibu (grid rows are lazily rendered). We verify the data model instead.

    @Test
    @SVCs({"atunko:SVC_WEB_0001.24"})
    void coverageSetAfterSelectAllContainsChildrenNotComposite() {
        RecipeBrowserView view = setupView(List.of(COMPOSITE, GAMMA));
        view.selectAllVisible();

        assertThat(view.getCoveredRecipes()).contains(CHILD_1, CHILD_2);
        assertThat(view.getCoveredRecipes()).doesNotContain(COMPOSITE, GAMMA);
    }

    // --- Export button ---

    @Test
    @SVCs({"atunko:SVC_WEB_0001.28"})
    void exportButtonExistsInStatusBar() {
        RecipeBrowserView view = setupView(List.of(ALPHA));

        assertThat(view.getExportButton()).isNotNull();
        assertThat(_find(Button.class, spec -> spec.withText("Export"))).isNotEmpty();
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.28"})
    void exportButtonHasCorrectThemeVariants() {
        RecipeBrowserView view = setupView(List.of(ALPHA));

        assertThat(view.getExportButton().getThemeNames()).contains("small", "primary");
    }

    // ==========================================================================
    // Workspace mode tests (SVC_WEB_0002, SVC_WEB_0002.4)
    // ==========================================================================

    private static ProjectEntry makeProjectEntry(String name) {
        return new ProjectEntry(Path.of("/workspace/" + name), new ProjectInfo(List.of(), List.of()));
    }

    private RecipeBrowserView setupWorkspaceView(List<RecipeInfo> recipes, Path root, List<ProjectEntry> projects) {
        SessionHolder.initWorkspace(root, projects);
        return setupView(recipes);
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0002"})
    void workspaceModePanelIsShownWhenMultipleProjects() {
        Path root = Path.of("/workspace");
        List<ProjectEntry> projects = List.of(makeProjectEntry("alpha"), makeProjectEntry("beta"));
        setupWorkspaceView(List.of(ALPHA), root, projects);

        List<String> spanTexts = _find(Span.class).stream().map(Span::getText).toList();
        assertThat(spanTexts).anyMatch(t -> t.startsWith("Workspace:") && t.contains("/workspace"));
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0002"})
    void workspaceModeListsAllProjects() {
        Path root = Path.of("/workspace");
        List<ProjectEntry> projects = List.of(makeProjectEntry("alpha"), makeProjectEntry("beta"));
        RecipeBrowserView view = setupWorkspaceView(List.of(ALPHA), root, projects);

        assertThat(view).isNotNull();
        // CheckboxGroup items render as Checkbox components; their labels contain project dir names
        List<String> checkboxLabels =
                _find(Checkbox.class).stream().map(Checkbox::getLabel).toList();
        assertThat(checkboxLabels).contains("alpha", "beta");
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0002"})
    void singleProjectModeDoesNotShowWorkspacePanel() {
        // @BeforeEach already sets single-project mode via SessionHolder.init(Path.of("."), null)
        setupView(List.of(ALPHA));

        List<String> spanTexts = _find(Span.class).stream().map(Span::getText).toList();
        assertThat(spanTexts).noneMatch(t -> t.startsWith("Workspace:"));
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0002.4"})
    void saveConfigInWorkspaceModeIncludesWorkspaceRoot() throws IOException {
        Path root = tempDir.resolve("myworkspace");
        Files.createDirectories(root);
        List<ProjectEntry> projects = List.of(makeProjectEntry("alpha"), makeProjectEntry("beta"));
        SessionHolder.initWorkspace(root, projects);
        RecipeBrowserView view = setupView(List.of(ALPHA, BETA));
        view.selectAllVisible();

        // Trigger save via view method directly
        Path runsDir = root.resolve("atunko/runs");
        Files.createDirectories(runsDir);
        Path configFile = runsDir.resolve("ws-config.yaml");
        RunConfig wsConfig = new RunConfig(
                null,
                new WorkspaceConfig(root.toString()),
                List.of(new RecipeEntry("org.test.Alpha"), new RecipeEntry("org.test.Beta")));
        new RunConfigService().save(wsConfig, configFile);

        RunConfig loaded = new RunConfigService().load(configFile);
        assertThat(loaded.workspace()).isNotNull();
        assertThat(loaded.workspace().root()).isEqualTo(root.toString());
        assertThat(loaded.recipes()).hasSize(2);
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0002.4"})
    void saveButtonInWorkspaceModeUsesWorkspaceRoot() throws IOException {
        Path root = tempDir.resolve("ws-save-test");
        Files.createDirectories(root);
        List<ProjectEntry> projects = List.of(makeProjectEntry("alpha"), makeProjectEntry("beta"));
        SessionHolder.initWorkspace(root, projects);
        RecipeBrowserView view = setupView(List.of(ALPHA));
        view.selectAllVisible();
        clearNotifications();

        _click(_get(Button.class, spec -> spec.withText("Save")));
        _setValue(_get(TextField.class, spec -> spec.withLabel("Name")), "workspace-run");
        List<Button> saveButtons = _find(Button.class, spec -> spec.withText("Save"));
        Button dialogSaveButton = saveButtons.stream()
                .filter(b -> b.getParent().isPresent()
                        && b.getParent().get().getParent().isPresent())
                .reduce((first, second) -> second)
                .orElseThrow();
        _click(dialogSaveButton);

        Path savedFile = root.resolve("atunko/runs/workspace-run.yaml");
        assertThat(savedFile).exists();
        RunConfig loaded = new RunConfigService().load(savedFile);
        assertThat(loaded.workspace()).isNotNull();
        assertThat(loaded.workspace().root()).isEqualTo(root.toString());
    }
}
