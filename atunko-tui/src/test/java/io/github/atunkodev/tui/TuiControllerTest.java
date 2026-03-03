package io.github.atunkodev.tui;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.atunkodev.core.config.RunConfig;
import io.github.atunkodev.core.config.RunConfigService;
import io.github.atunkodev.core.engine.ExecutionResult;
import io.github.atunkodev.core.engine.FileChange;
import io.github.atunkodev.core.recipe.RecipeInfo;
import io.github.atunkodev.core.recipe.SortOrder;
import io.github.atunkodev.tui.TuiController.DisplayRow;
import io.github.reqstool.annotations.SVCs;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@SVCs({"atunko:SVC_TUI_0001"})
class TuiControllerTest {

    private static final RecipeInfo ALPHA =
            new RecipeInfo("org.test.Alpha", "Alpha Recipe", "First recipe", Set.of("java", "testing"));
    private static final RecipeInfo BETA =
            new RecipeInfo("org.test.Beta", "Beta Recipe", "Second recipe", Set.of("spring"));
    private static final RecipeInfo GAMMA =
            new RecipeInfo("org.test.Gamma", "Gamma Recipe", "Third recipe", Set.of("java"));

    private static final RecipeInfo SUB_1 =
            new RecipeInfo("org.test.Sub1", "Sub Recipe 1", "First sub-recipe", Set.of("java"));
    private static final RecipeInfo SUB_2 =
            new RecipeInfo("org.test.Sub2", "Sub Recipe 2", "Second sub-recipe", Set.of("java"));
    private static final RecipeInfo COMPOSITE = new RecipeInfo(
            "org.test.Composite", "Composite Recipe", "A composite recipe", Set.of("java"), List.of(SUB_1, SUB_2));

    // Nested composite: OUTER contains COMPOSITE and SUB_3
    private static final RecipeInfo SUB_3 =
            new RecipeInfo("org.test.Sub3", "Sub Recipe 3", "Third sub-recipe", Set.of("java"));
    private static final RecipeInfo OUTER = new RecipeInfo(
            "org.test.Outer", "Outer Composite", "A nested composite", Set.of("java"), List.of(COMPOSITE, SUB_3));

    private static final List<RecipeInfo> RECIPES = List.of(ALPHA, BETA, GAMMA);
    private static final List<RecipeInfo> RECIPES_WITH_COMPOSITE = List.of(ALPHA, COMPOSITE, GAMMA);
    private static final List<RecipeInfo> RECIPES_WITH_NESTED = List.of(ALPHA, OUTER, GAMMA);

    @Test
    @SVCs({"atunko:SVC_TUI_0001"})
    void initialState_browserScreenWithRecipesLoaded() {
        TuiController controller = new TuiController(RECIPES);

        assertThat(controller.currentScreen()).isEqualTo(Screen.BROWSER);
        assertThat(controller.recipes()).hasSize(3);
        assertThat(controller.highlightedIndex()).isZero();
        assertThat(controller.selectedRecipes()).isEmpty();
        assertThat(controller.searchQuery()).isEmpty();
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.1"})
    void recipes_exposesFilteredSortedList() {
        TuiController controller = new TuiController(RECIPES);

        List<RecipeInfo> visible = controller.recipes();

        assertThat(visible).containsExactly(ALPHA, BETA, GAMMA);
        assertThat(visible.get(0).name()).isEqualTo("org.test.Alpha");
        assertThat(visible.get(0).description()).isEqualTo("First recipe");
        assertThat(visible.get(0).tags()).containsExactlyInAnyOrder("java", "testing");
    }

    // --- Search Mode ---

    @Test
    @SVCs({"atunko:SVC_TUI_0001.3"})
    void enterSearchMode_enablesSearchMode() {
        TuiController controller = new TuiController(RECIPES);

        controller.enterSearchMode();

        assertThat(controller.isSearchMode()).isTrue();
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.3"})
    void exitSearchMode_disablesSearchMode() {
        TuiController controller = new TuiController(RECIPES);
        controller.enterSearchMode();

        controller.exitSearchMode();

        assertThat(controller.isSearchMode()).isFalse();
    }

    // --- Search and Sort ---

    @Test
    @SVCs({"atunko:SVC_TUI_0001.3"})
    void setSearchQuery_filtersRecipesByNameDescriptionAndTags() {
        TuiController controller = new TuiController(RECIPES);

        controller.setSearchQuery("spring");

        assertThat(controller.recipes()).containsExactly(BETA);
        assertThat(controller.searchQuery()).isEqualTo("spring");
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.3"})
    void setSearchQuery_matchesByDescription() {
        TuiController controller = new TuiController(RECIPES);

        controller.setSearchQuery("Third");

        assertThat(controller.recipes()).containsExactly(GAMMA);
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.3"})
    void setSearchQuery_emptyQueryShowsAll() {
        TuiController controller = new TuiController(RECIPES);
        controller.setSearchQuery("spring");

        controller.setSearchQuery("");

        assertThat(controller.recipes()).hasSize(3);
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.3"})
    void setSearchQuery_resetsHighlightedIndex() {
        TuiController controller = new TuiController(RECIPES);
        controller.moveDown();
        controller.moveDown();

        controller.setSearchQuery("spring");

        assertThat(controller.highlightedIndex()).isZero();
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.6"})
    void setSortOrder_reordersRecipeList() {
        TuiController controller = new TuiController(RECIPES);

        controller.setSortOrder(SortOrder.TAGS);

        List<RecipeInfo> sorted = controller.recipes();
        // TAGS sort: min tag first, then by name
        // ALPHA (min="java"), GAMMA (min="java"), BETA (min="spring")
        // Same first-tag "java" → sorted by name: Alpha < Gamma
        assertThat(sorted.get(0).name()).isEqualTo("org.test.Alpha");
        assertThat(sorted.get(1).name()).isEqualTo("org.test.Gamma");
        assertThat(sorted.get(2).name()).isEqualTo("org.test.Beta");
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.6"})
    void setSortOrder_nameOrderIsAlphabetical() {
        TuiController controller = new TuiController(RECIPES);

        controller.setSortOrder(SortOrder.NAME);

        List<RecipeInfo> sorted = controller.recipes();
        assertThat(sorted)
                .extracting(RecipeInfo::name)
                .containsExactly("org.test.Alpha", "org.test.Beta", "org.test.Gamma");
    }

    // --- Selection and Navigation ---

    @Test
    @SVCs({"atunko:SVC_TUI_0001.2"})
    void highlightedRecipe_returnsCurrentlyHighlightedRecipe() {
        TuiController controller = new TuiController(RECIPES);

        assertThat(controller.highlightedRecipe()).isPresent();
        assertThat(controller.highlightedRecipe().get().name()).isEqualTo("org.test.Alpha");

        controller.moveDown();
        assertThat(controller.highlightedRecipe().get().name()).isEqualTo("org.test.Beta");
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.5"})
    void toggleSelection_addsAndRemovesRecipes() {
        TuiController controller = new TuiController(RECIPES);

        controller.toggleSelection();
        assertThat(controller.selectedRecipes()).containsExactly("org.test.Alpha");

        controller.moveDown();
        controller.toggleSelection();
        assertThat(controller.selectedRecipes()).containsExactlyInAnyOrder("org.test.Alpha", "org.test.Beta");

        controller.moveUp();
        controller.toggleSelection();
        assertThat(controller.selectedRecipes()).containsExactly("org.test.Beta");
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.4"})
    void openDetail_switchesToDetailScreen() {
        TuiController controller = new TuiController(RECIPES);

        controller.openDetail();

        assertThat(controller.currentScreen()).isEqualTo(Screen.DETAIL);
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.4"})
    void goBack_returnsFromDetailToBrowser() {
        TuiController controller = new TuiController(RECIPES);
        controller.openDetail();

        controller.goBack();

        assertThat(controller.currentScreen()).isEqualTo(Screen.BROWSER);
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.12"})
    void moveDown_wrapsAtEnd() {
        TuiController controller = new TuiController(RECIPES);

        controller.moveDown();
        controller.moveDown();
        assertThat(controller.highlightedIndex()).isEqualTo(2);

        controller.moveDown();
        assertThat(controller.highlightedIndex()).isZero();
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.12"})
    void moveUp_wrapsAtStart() {
        TuiController controller = new TuiController(RECIPES);

        controller.moveUp();

        assertThat(controller.highlightedIndex()).isEqualTo(2);
    }

    // --- Tag Browser ---

    @Test
    @SVCs({"atunko:SVC_TUI_0001.11"})
    void allTags_returnsUniqueTagsSorted() {
        TuiController controller = new TuiController(RECIPES);

        List<String> tags = controller.allTags();

        assertThat(tags).containsExactly("java", "spring", "testing");
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.11"})
    void openTagBrowser_switchesToTagBrowserScreen() {
        TuiController controller = new TuiController(RECIPES);

        controller.openTagBrowser();

        assertThat(controller.currentScreen()).isEqualTo(Screen.TAG_BROWSER);
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.11"})
    void toggleTag_andApply_filtersRecipesBySelectedTags() {
        TuiController controller = new TuiController(RECIPES);

        controller.toggleTag("spring");
        controller.applyTagFilter();

        assertThat(controller.recipes()).containsExactly(BETA);
        assertThat(controller.currentScreen()).isEqualTo(Screen.BROWSER);
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.11"})
    void toggleTag_multipleTagsFiltersUnion() {
        TuiController controller = new TuiController(RECIPES);

        controller.toggleTag("spring");
        controller.toggleTag("testing");

        // ALPHA has "testing", BETA has "spring"
        assertThat(controller.recipes()).containsExactlyInAnyOrder(ALPHA, BETA);
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.11"})
    void toggleTag_deselectsWhenAlreadySelected() {
        TuiController controller = new TuiController(RECIPES);
        controller.toggleTag("spring");

        controller.toggleTag("spring"); // deselect

        assertThat(controller.selectedTags()).isEmpty();
        assertThat(controller.recipes()).hasSize(3);
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.11"})
    void clearTagFilter_showsAllRecipes() {
        TuiController controller = new TuiController(RECIPES);
        controller.toggleTag("spring");

        controller.clearTagFilter();

        assertThat(controller.recipes()).hasSize(3);
        assertThat(controller.selectedTags()).isEmpty();
    }

    // --- Recipe Options ---

    @Test
    @SVCs({"atunko:SVC_TUI_0001.7"})
    void highlightedRecipe_isAvailableForOptionsLookup() {
        TuiController controller = new TuiController(RECIPES);

        assertThat(controller.highlightedRecipe()).isPresent();
        assertThat(controller.highlightedRecipe().get().name()).isEqualTo("org.test.Alpha");
    }

    // --- Dry-Run and Execution ---

    @Test
    @SVCs({"atunko:SVC_TUI_0001.8"})
    void showDryRunResult_storesResultAndSwitchesToExecutionScreen() {
        TuiController controller = new TuiController(RECIPES);
        ExecutionResult result = new ExecutionResult(List.of(new FileChange(Path.of("Foo.java"), "before", "after")));

        controller.showDryRunResult(result);

        assertThat(controller.currentScreen()).isEqualTo(Screen.EXECUTION_RESULTS);
        assertThat(controller.executionResult()).isPresent();
        assertThat(controller.executionResult().get().changes()).hasSize(1);
        assertThat(controller.lastRunWasDryRun()).isTrue();
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.9"})
    void showExecutionResult_storesResultAndSwitchesToExecutionScreen() {
        TuiController controller = new TuiController(RECIPES);
        ExecutionResult result = new ExecutionResult(List.of(new FileChange(Path.of("Foo.java"), "before", "after")));

        controller.showExecutionResult(result);

        assertThat(controller.currentScreen()).isEqualTo(Screen.EXECUTION_RESULTS);
        assertThat(controller.executionResult()).isPresent();
        assertThat(controller.lastRunWasDryRun()).isFalse();
    }

    // --- Save Run Configuration ---

    @Test
    @SVCs({"atunko:SVC_TUI_0001.10"})
    void saveRunConfig_persistsSelectedRecipes(@TempDir Path tempDir) throws Exception {
        RunConfigService service = new RunConfigService();
        TuiController controller = new TuiController(RECIPES, service);
        controller.toggleSelection(); // select Alpha
        controller.moveDown();
        controller.toggleSelection(); // select Beta

        Path configFile = tempDir.resolve(".atunko.yml");
        controller.saveRunConfig(configFile);

        RunConfig loaded = service.load(configFile);
        assertThat(loaded.recipes()).containsExactlyInAnyOrder("org.test.Alpha", "org.test.Beta");
    }

    // --- Cycle Selection ---

    @Test
    @SVCs({"atunko:SVC_TUI_0001.5"})
    void cycleSelection_selectsAllWhenNoneSelected() {
        TuiController controller = new TuiController(RECIPES);

        controller.cycleSelection();

        assertThat(controller.selectedRecipes())
                .containsExactlyInAnyOrder("org.test.Alpha", "org.test.Beta", "org.test.Gamma");
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.5"})
    void cycleSelection_clearsWhenAllSelected() {
        TuiController controller = new TuiController(RECIPES);
        controller.cycleSelection(); // select all

        controller.cycleSelection(); // deselect all

        assertThat(controller.selectedRecipes()).isEmpty();
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.5"})
    void cycleSelection_selectsAllWhenPartiallySelected() {
        TuiController controller = new TuiController(RECIPES);
        controller.toggleSelection(); // select Alpha only

        controller.cycleSelection(); // should select all since not all are selected

        assertThat(controller.selectedRecipes())
                .containsExactlyInAnyOrder("org.test.Alpha", "org.test.Beta", "org.test.Gamma");
    }

    // --- Composite Recipe Browsing ---

    @Test
    @SVCs({"atunko:SVC_TUI_0001.13"})
    void expandRecipe_addsToExpandedSet() {
        TuiController controller = new TuiController(RECIPES_WITH_COMPOSITE);

        controller.expandRecipe("org.test.Composite");

        assertThat(controller.isExpanded("org.test.Composite")).isTrue();
        assertThat(controller.expandedRecipes()).contains("org.test.Composite");
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.13"})
    void collapseRecipe_removesFromExpandedSet() {
        TuiController controller = new TuiController(RECIPES_WITH_COMPOSITE);
        controller.expandRecipe("org.test.Composite");

        controller.collapseRecipe("org.test.Composite");

        assertThat(controller.isExpanded("org.test.Composite")).isFalse();
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.13"})
    void findRecipe_returnsCompositeWithSubRecipes() {
        TuiController controller = new TuiController(RECIPES_WITH_COMPOSITE);

        assertThat(controller.findRecipe("org.test.Composite")).isPresent();
        assertThat(controller.findRecipe("org.test.Composite").get().isComposite())
                .isTrue();
        assertThat(controller.findRecipe("org.test.Composite").get().recipeList())
                .hasSize(2);
    }

    // --- Run Dialog ---

    @Test
    @SVCs({"atunko:SVC_TUI_0001.14"})
    void openConfirmRun_initializesRunOrderFromSelection() {
        TuiController controller = new TuiController(RECIPES);
        controller.toggleSelection(); // select Alpha
        controller.moveDown();
        controller.toggleSelection(); // select Beta

        controller.openConfirmRun();

        assertThat(controller.currentScreen()).isEqualTo(Screen.CONFIRM_RUN);
        assertThat(controller.runOrder()).containsExactly("org.test.Alpha", "org.test.Beta");
        assertThat(controller.runHighlightIndex()).isZero();
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.14"})
    void moveRunHighlightDown_navigatesRunList() {
        TuiController controller = new TuiController(RECIPES);
        controller.toggleSelection();
        controller.moveDown();
        controller.toggleSelection();
        controller.openConfirmRun();

        controller.moveRunHighlightDown();

        assertThat(controller.runHighlightIndex()).isEqualTo(1);
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.14"})
    void moveRunHighlightUp_wrapsAround() {
        TuiController controller = new TuiController(RECIPES);
        controller.toggleSelection();
        controller.moveDown();
        controller.toggleSelection();
        controller.openConfirmRun();

        controller.moveRunHighlightUp(); // wraps to end

        assertThat(controller.runHighlightIndex()).isEqualTo(1);
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.14"})
    void moveRunRecipeDown_reordersRecipes() {
        TuiController controller = new TuiController(RECIPES);
        controller.toggleSelection(); // select Alpha
        controller.moveDown();
        controller.toggleSelection(); // select Beta
        controller.moveDown();
        controller.toggleSelection(); // select Gamma
        controller.openConfirmRun();

        controller.moveRunRecipeDown(); // move Alpha down

        assertThat(controller.runOrder()).containsExactly("org.test.Beta", "org.test.Alpha", "org.test.Gamma");
        assertThat(controller.runHighlightIndex()).isEqualTo(1);
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.14"})
    void moveRunRecipeUp_reordersRecipes() {
        TuiController controller = new TuiController(RECIPES);
        controller.toggleSelection();
        controller.moveDown();
        controller.toggleSelection();
        controller.moveDown();
        controller.toggleSelection();
        controller.openConfirmRun();
        controller.moveRunHighlightDown(); // highlight Beta

        controller.moveRunRecipeUp(); // move Beta up

        assertThat(controller.runOrder()).containsExactly("org.test.Beta", "org.test.Alpha", "org.test.Gamma");
        assertThat(controller.runHighlightIndex()).isZero();
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.14"})
    void toggleRunRecipe_togglesIndividualRecipeSelection() {
        TuiController controller = new TuiController(RECIPES);
        controller.toggleSelection();
        controller.moveDown();
        controller.toggleSelection();
        controller.openConfirmRun();

        controller.toggleRunRecipe(); // deselect Alpha

        assertThat(controller.selectedRecipes()).containsExactly("org.test.Beta");

        controller.toggleRunRecipe(); // reselect Alpha

        assertThat(controller.selectedRecipes()).containsExactlyInAnyOrder("org.test.Alpha", "org.test.Beta");
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.14"})
    void cycleRunSelection_cyclesAllNone() {
        TuiController controller = new TuiController(RECIPES);
        controller.toggleSelection();
        controller.moveDown();
        controller.toggleSelection();
        controller.openConfirmRun();

        controller.cycleRunSelection(); // already all selected → deselect all

        assertThat(controller.selectedRecipes()).isEmpty();

        controller.cycleRunSelection(); // none selected → select all in run order

        assertThat(controller.selectedRecipes()).containsExactlyInAnyOrder("org.test.Alpha", "org.test.Beta");
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.14"})
    void flattenRunRecipe_replacesCompositeWithSubRecipes() {
        TuiController controller = new TuiController(RECIPES_WITH_COMPOSITE);
        controller.moveDown(); // highlight Composite
        controller.toggleSelection(); // select Composite only
        controller.openConfirmRun();

        controller.flattenRunRecipe(); // flatten Composite at index 0

        assertThat(controller.runOrder()).containsExactly("org.test.Sub1", "org.test.Sub2");
        assertThat(controller.selectedRecipes()).containsExactlyInAnyOrder("org.test.Sub1", "org.test.Sub2");
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.14"})
    void flattenRunRecipe_nonComposite_doesNothing() {
        TuiController controller = new TuiController(RECIPES);
        controller.toggleSelection(); // select Alpha
        controller.openConfirmRun();

        controller.flattenRunRecipe();

        assertThat(controller.runOrder()).containsExactly("org.test.Alpha");
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.14"})
    void expandRunRecipe_addsToRunExpandedSet() {
        TuiController controller = new TuiController(RECIPES_WITH_COMPOSITE);
        controller.moveDown(); // highlight Composite
        controller.toggleSelection();
        controller.openConfirmRun();

        controller.expandRunRecipe();

        assertThat(controller.runExpandedRecipes()).contains("org.test.Composite");
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.14"})
    void collapseRunRecipe_removesFromRunExpandedSet() {
        TuiController controller = new TuiController(RECIPES_WITH_COMPOSITE);
        controller.moveDown();
        controller.toggleSelection();
        controller.openConfirmRun();
        controller.expandRunRecipe();

        controller.collapseRunRecipe();

        assertThat(controller.runExpandedRecipes()).doesNotContain("org.test.Composite");
    }

    // --- Display Rows (flat list model) ---

    @Test
    @SVCs({"atunko:SVC_TUI_0001.13"})
    void displayRows_withoutExpansion_containsOnlyParentRows() {
        TuiController controller = new TuiController(RECIPES_WITH_COMPOSITE);

        List<DisplayRow> rows = controller.displayRows();

        assertThat(rows).hasSize(3);
        assertThat(rows).allMatch(r -> !r.isSubRecipe());
        assertThat(rows)
                .extracting(r -> r.recipe().name())
                .containsExactly("org.test.Alpha", "org.test.Composite", "org.test.Gamma");
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.13"})
    void displayRows_withExpansion_includesSubRecipeRows() {
        TuiController controller = new TuiController(RECIPES_WITH_COMPOSITE);
        controller.expandRecipe("org.test.Composite");

        List<DisplayRow> rows = controller.displayRows();

        assertThat(rows).hasSize(5);
        assertThat(rows.get(0).recipe().name()).isEqualTo("org.test.Alpha");
        assertThat(rows.get(0).isSubRecipe()).isFalse();
        assertThat(rows.get(1).recipe().name()).isEqualTo("org.test.Composite");
        assertThat(rows.get(1).isSubRecipe()).isFalse();
        assertThat(rows.get(2).recipe().name()).isEqualTo("org.test.Sub1");
        assertThat(rows.get(2).isSubRecipe()).isTrue();
        assertThat(rows.get(2).parentName()).isEqualTo("org.test.Composite");
        assertThat(rows.get(3).recipe().name()).isEqualTo("org.test.Sub2");
        assertThat(rows.get(3).isSubRecipe()).isTrue();
        assertThat(rows.get(4).recipe().name()).isEqualTo("org.test.Gamma");
        assertThat(rows.get(4).isSubRecipe()).isFalse();
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.12"})
    void moveDown_navigatesExpandedDisplayRows() {
        TuiController controller = new TuiController(RECIPES_WITH_COMPOSITE);
        controller.expandRecipe("org.test.Composite");

        controller.moveDown(); // → Composite (index 1)
        controller.moveDown(); // → Sub1 (index 2)
        controller.moveDown(); // → Sub2 (index 3)
        controller.moveDown(); // → Gamma (index 4)

        assertThat(controller.highlightedIndex()).isEqualTo(4);
        assertThat(controller.highlightedRecipe()).isPresent();
        assertThat(controller.highlightedRecipe().get().name()).isEqualTo("org.test.Gamma");
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.12"})
    void moveUp_navigatesExpandedDisplayRows() {
        TuiController controller = new TuiController(RECIPES_WITH_COMPOSITE);
        controller.expandRecipe("org.test.Composite");

        controller.moveUp(); // wraps to Gamma (index 4)

        assertThat(controller.highlightedIndex()).isEqualTo(4);
        assertThat(controller.highlightedRecipe()).isPresent();
        assertThat(controller.highlightedRecipe().get().name()).isEqualTo("org.test.Gamma");
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.13"})
    void highlightedDisplayRow_returnsCorrectRow() {
        TuiController controller = new TuiController(RECIPES_WITH_COMPOSITE);
        controller.expandRecipe("org.test.Composite");
        controller.moveDown(); // Composite
        controller.moveDown(); // Sub1

        assertThat(controller.highlightedDisplayRow()).isPresent();
        DisplayRow row = controller.highlightedDisplayRow().get();
        assertThat(row.recipe().name()).isEqualTo("org.test.Sub1");
        assertThat(row.isSubRecipe()).isTrue();
        assertThat(row.parentName()).isEqualTo("org.test.Composite");
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.13"})
    void collapseRecipe_clampsHighlightIndex() {
        TuiController controller = new TuiController(RECIPES_WITH_COMPOSITE);
        controller.expandRecipe("org.test.Composite");
        // Navigate to Sub2 (index 3)
        controller.moveDown(); // 1 - Composite
        controller.moveDown(); // 2 - Sub1
        controller.moveDown(); // 3 - Sub2

        controller.collapseRecipe("org.test.Composite");

        // After collapse, only 3 rows (Alpha, Composite, Gamma) — index clamped to 2
        assertThat(controller.highlightedIndex())
                .isLessThan(controller.displayRows().size());
    }

    // --- Sub-recipe selection (individual) ---

    @Test
    @SVCs({"atunko:SVC_TUI_0001.5"})
    void toggleSelection_selectingComposite_selectsOnlyThatRecipe() {
        TuiController controller = new TuiController(RECIPES_WITH_COMPOSITE);
        controller.expandRecipe("org.test.Composite");
        controller.moveDown(); // highlight Composite (index 1)

        controller.toggleSelection();

        assertThat(controller.selectedRecipes()).containsExactly("org.test.Composite");
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.5"})
    void toggleSelection_onSubRecipe_selectsSubRecipeIndividually() {
        TuiController controller = new TuiController(RECIPES_WITH_COMPOSITE);
        controller.expandRecipe("org.test.Composite");
        controller.moveDown(); // Composite
        controller.moveDown(); // Sub1

        controller.toggleSelection();

        assertThat(controller.selectedRecipes()).containsExactly("org.test.Sub1");
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.5"})
    void toggleSelection_onSubRecipe_deselectsSubRecipeIndividually() {
        TuiController controller = new TuiController(RECIPES_WITH_COMPOSITE);
        controller.expandRecipe("org.test.Composite");
        controller.moveDown(); // Composite
        controller.moveDown(); // Sub1
        controller.toggleSelection(); // select Sub1

        controller.toggleSelection(); // deselect Sub1

        assertThat(controller.selectedRecipes()).isEmpty();
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.5"})
    void cycleSelection_selectsAllVisibleNames() {
        TuiController controller = new TuiController(RECIPES_WITH_COMPOSITE);
        controller.expandRecipe("org.test.Composite");

        controller.cycleSelection();

        assertThat(controller.selectedRecipes())
                .containsExactlyInAnyOrder(
                        "org.test.Alpha", "org.test.Composite", "org.test.Sub1", "org.test.Sub2", "org.test.Gamma");
    }

    // --- Run dialog display rows ---

    @Test
    @SVCs({"atunko:SVC_TUI_0001.14"})
    void runDisplayRows_withExpandedComposite_includesSubRecipes() {
        TuiController controller = new TuiController(RECIPES_WITH_COMPOSITE);
        controller.moveDown(); // highlight Composite
        controller.toggleSelection(); // select Composite
        controller.openConfirmRun();
        controller.expandRunRecipe(); // expand Composite in run dialog

        List<DisplayRow> rows = controller.runDisplayRows();

        assertThat(rows).hasSize(3);
        assertThat(rows.get(0).recipe().name()).isEqualTo("org.test.Composite");
        assertThat(rows.get(0).isSubRecipe()).isFalse();
        assertThat(rows.get(1).recipe().name()).isEqualTo("org.test.Sub1");
        assertThat(rows.get(1).isSubRecipe()).isTrue();
        assertThat(rows.get(2).recipe().name()).isEqualTo("org.test.Sub2");
        assertThat(rows.get(2).isSubRecipe()).isTrue();
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.14"})
    void moveRunHighlightDown_navigatesExpandedRunDisplayRows() {
        TuiController controller = new TuiController(RECIPES_WITH_COMPOSITE);
        controller.moveDown(); // highlight Composite
        controller.toggleSelection(); // select Composite
        controller.openConfirmRun();
        controller.expandRunRecipe();

        controller.moveRunHighlightDown(); // Sub1 (index 1)
        controller.moveRunHighlightDown(); // Sub2 (index 2)

        assertThat(controller.runHighlightIndex()).isEqualTo(2);
    }

    // --- Clear all ---

    @Test
    @SVCs({"atunko:SVC_TUI_0001"})
    void clearAll_resetsSearchTagsAndSelections() {
        TuiController controller = new TuiController(RECIPES);
        controller.setSearchQuery("alpha");
        controller.toggleTag("java");
        controller.toggleSelection(); // select Alpha

        controller.clearAll();

        assertThat(controller.searchQuery()).isEmpty();
        assertThat(controller.selectedTags()).isEmpty();
        assertThat(controller.selectedRecipes()).isEmpty();
        assertThat(controller.highlightedIndex()).isZero();
    }

    // --- Nested Composites ---

    @Test
    @SVCs({"atunko:SVC_TUI_0001.13"})
    void displayRows_nestedComposite_hasCorrectDepth() {
        TuiController controller = new TuiController(RECIPES_WITH_NESTED);
        controller.expandRecipe("org.test.Outer");

        List<DisplayRow> rows = controller.displayRows();

        // Sorted by name: Alpha, Gamma, Outer + expanded children (Composite, Sub3)
        assertThat(rows).hasSize(5);
        assertThat(rows.get(0).recipe().name()).isEqualTo("org.test.Alpha");
        assertThat(rows.get(0).depth()).isZero();
        assertThat(rows.get(1).recipe().name()).isEqualTo("org.test.Gamma");
        assertThat(rows.get(1).depth()).isZero();
        assertThat(rows.get(2).recipe().name()).isEqualTo("org.test.Outer");
        assertThat(rows.get(2).depth()).isZero();
        assertThat(rows.get(3).recipe().name()).isEqualTo("org.test.Composite");
        assertThat(rows.get(3).depth()).isEqualTo(1);
        assertThat(rows.get(3).isSubRecipe()).isTrue();
        assertThat(rows.get(3).recipe().isComposite()).isTrue();
        assertThat(rows.get(4).recipe().name()).isEqualTo("org.test.Sub3");
        assertThat(rows.get(4).depth()).isEqualTo(1);
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.13"})
    void displayRows_deeplyNested_expandsBothLevels() {
        TuiController controller = new TuiController(RECIPES_WITH_NESTED);
        controller.expandRecipe("org.test.Outer");
        controller.expandRecipe("org.test.Composite");

        List<DisplayRow> rows = controller.displayRows();

        // Sorted: Alpha, Gamma, Outer, Composite (d1), Sub1 (d2), Sub2 (d2), Sub3 (d1)
        assertThat(rows).hasSize(7);
        assertThat(rows.get(0).recipe().name()).isEqualTo("org.test.Alpha");
        assertThat(rows.get(1).recipe().name()).isEqualTo("org.test.Gamma");
        assertThat(rows.get(2).recipe().name()).isEqualTo("org.test.Outer");
        assertThat(rows.get(3).recipe().name()).isEqualTo("org.test.Composite");
        assertThat(rows.get(3).depth()).isEqualTo(1);
        assertThat(rows.get(3).path()).isEqualTo("org.test.Outer/org.test.Composite");
        assertThat(rows.get(4).recipe().name()).isEqualTo("org.test.Sub1");
        assertThat(rows.get(4).depth()).isEqualTo(2);
        assertThat(rows.get(4).parentName()).isEqualTo("org.test.Composite");
        assertThat(rows.get(4).path()).isEqualTo("org.test.Outer/org.test.Composite/org.test.Sub1");
        assertThat(rows.get(5).recipe().name()).isEqualTo("org.test.Sub2");
        assertThat(rows.get(5).depth()).isEqualTo(2);
        assertThat(rows.get(6).recipe().name()).isEqualTo("org.test.Sub3");
        assertThat(rows.get(6).depth()).isEqualTo(1);
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.13"})
    void expandRecipe_onNestedSubRecipe_expandsIt() {
        TuiController controller = new TuiController(RECIPES_WITH_NESTED);
        controller.expandRecipe("org.test.Outer");

        controller.expandRecipe("org.test.Composite");

        assertThat(controller.isExpanded("org.test.Composite")).isTrue();
        assertThat(controller.displayRows()).hasSize(7);
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.5"})
    void toggleSelection_onNestedSubRecipe_selectsIndividually() {
        TuiController controller = new TuiController(RECIPES_WITH_NESTED);
        controller.expandRecipe("org.test.Outer");
        controller.expandRecipe("org.test.Composite");
        // Sorted: Alpha(0), Gamma(1), Outer(2), Composite(3), Sub1(4), Sub2(5), Sub3(6)
        controller.moveDown(); // 1 - Gamma
        controller.moveDown(); // 2 - Outer
        controller.moveDown(); // 3 - Composite
        controller.moveDown(); // 4 - Sub1

        controller.toggleSelection();

        assertThat(controller.selectedRecipes()).containsExactly("org.test.Sub1");
    }

    // --- Path-based IDs: same recipe at different tree positions ---

    // A recipe that appears both at top-level and as a sub-recipe of a composite
    private static final RecipeInfo COMMON =
            new RecipeInfo("org.test.Common", "Common Recipe", "Appears everywhere", Set.of("java"));
    private static final RecipeInfo COMPOSITE_WITH_COMMON = new RecipeInfo(
            "org.test.CompositeCommon",
            "Composite With Common",
            "Contains Common",
            Set.of("java"),
            List.of(COMMON, SUB_2));
    private static final List<RecipeInfo> RECIPES_WITH_DUPLICATE = List.of(COMMON, COMPOSITE_WITH_COMMON);

    @Test
    @SVCs({"atunko:SVC_TUI_0001.5"})
    void toggleSelection_sameRecipeAtDifferentPositions_selectsByRecipeName() {
        TuiController controller = new TuiController(RECIPES_WITH_DUPLICATE);
        // Sorted by name: Common(0), CompositeCommon(1)
        controller.expandRecipe("org.test.CompositeCommon");
        // Rows: Common(0), CompositeCommon(1), Common-sub(2), Sub2-sub(3)

        // Select top-level Common — uses recipe name as key
        controller.toggleSelection();
        assertThat(controller.selectedRecipes()).containsExactly("org.test.Common");

        // Move to sub-recipe Common and toggle — same recipe name, so it deselects
        controller.moveDown(); // CompositeCommon (1)
        controller.moveDown(); // Common sub-recipe (2)
        controller.toggleSelection();
        assertThat(controller.selectedRecipes()).isEmpty();

        // Toggle again from the sub-recipe position — selects the recipe by name
        controller.toggleSelection();
        assertThat(controller.selectedRecipes()).containsExactly("org.test.Common");
    }

    // A composite that appears both at top-level and nested inside another composite
    private static final RecipeInfo NESTED_HOST = new RecipeInfo(
            "org.test.NestedHost", "Nested Host", "Contains Composite", Set.of("java"), List.of(COMPOSITE, SUB_3));
    private static final List<RecipeInfo> RECIPES_WITH_DUPLICATE_COMPOSITE = List.of(COMPOSITE, NESTED_HOST);

    @Test
    @SVCs({"atunko:SVC_TUI_0001.13"})
    void expandRecipe_sameCompositeAtDifferentPositions_expandsAllInstances() {
        TuiController controller = new TuiController(RECIPES_WITH_DUPLICATE_COMPOSITE);
        // Sorted: Composite(0), NestedHost(1)
        controller.expandRecipe("org.test.NestedHost");
        // Rows: Composite(0), NestedHost(1), Composite-sub(2), Sub3-sub(3)

        // Expand Composite by name — expands ALL instances (recipe name identity)
        controller.expandRecipe("org.test.Composite");

        assertThat(controller.isExpanded("org.test.Composite")).isTrue();

        List<DisplayRow> rows = controller.displayRows();
        // Top-level Composite expanded: Composite(0), Sub1(1), Sub2(2)
        // NestedHost(3), nested Composite expanded: Composite(4), Sub1(5), Sub2(6), Sub3(7)
        assertThat(rows).hasSize(8);
        assertThat(rows.get(0).recipe().name()).isEqualTo("org.test.Composite");
        assertThat(rows.get(0).depth()).isZero();
        assertThat(rows.get(1).recipe().name()).isEqualTo("org.test.Sub1");
        assertThat(rows.get(1).depth()).isEqualTo(1);
        assertThat(rows.get(2).recipe().name()).isEqualTo("org.test.Sub2");
        assertThat(rows.get(2).depth()).isEqualTo(1);
        assertThat(rows.get(3).recipe().name()).isEqualTo("org.test.NestedHost");
        assertThat(rows.get(4).recipe().name()).isEqualTo("org.test.Composite");
        assertThat(rows.get(4).depth()).isEqualTo(1);
        assertThat(rows.get(5).recipe().name()).isEqualTo("org.test.Sub1");
        assertThat(rows.get(5).depth()).isEqualTo(2);
        assertThat(rows.get(6).recipe().name()).isEqualTo("org.test.Sub2");
        assertThat(rows.get(6).depth()).isEqualTo(2);
        assertThat(rows.get(7).recipe().name()).isEqualTo("org.test.Sub3");
        assertThat(rows.get(7).depth()).isEqualTo(1);
    }

    // --- Help Overlay ---

    @Test
    @SVCs({"atunko:SVC_TUI_0001"})
    void toggleHelp_togglesHelpState() {
        TuiController controller = new TuiController(RECIPES);

        assertThat(controller.isShowHelp()).isFalse();

        controller.toggleHelp();

        assertThat(controller.isShowHelp()).isTrue();

        controller.toggleHelp();

        assertThat(controller.isShowHelp()).isFalse();
    }

    // --- Recipe Coverage Indicators ---

    @Test
    @SVCs({"atunko:SVC_TUI_0001.16"})
    void coveredRecipes_returnsSubRecipesOfSelectedComposites() {
        TuiController controller = new TuiController(RECIPES_WITH_COMPOSITE);
        controller.moveDown(); // highlight Composite
        controller.toggleSelection(); // select Composite

        Set<String> covered = controller.coveredRecipes();

        assertThat(covered).containsExactlyInAnyOrder("org.test.Sub1", "org.test.Sub2");
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.16.1"})
    void coveredRecipes_recursivelyIncludesNestedSubRecipes() {
        TuiController controller = new TuiController(RECIPES_WITH_NESTED);
        // Sorted: Alpha(0), Gamma(1), Outer(2) — highlight Outer
        controller.moveDown(); // Gamma
        controller.moveDown(); // Outer
        controller.toggleSelection(); // select Outer

        Set<String> covered = controller.coveredRecipes();

        // Outer contains Composite and Sub3; Composite contains Sub1 and Sub2
        assertThat(covered)
                .containsExactlyInAnyOrder("org.test.Composite", "org.test.Sub3", "org.test.Sub1", "org.test.Sub2");
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.16"})
    void coveredRecipes_emptyWhenNoCompositesSelected() {
        TuiController controller = new TuiController(RECIPES_WITH_COMPOSITE);
        controller.toggleSelection(); // select Alpha (non-composite)

        Set<String> covered = controller.coveredRecipes();

        assertThat(covered).isEmpty();
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.16"})
    void coveredRecipes_doesNotIncludeTheSelectedCompositeItself() {
        TuiController controller = new TuiController(RECIPES_WITH_COMPOSITE);
        controller.moveDown(); // highlight Composite
        controller.toggleSelection(); // select Composite

        Set<String> covered = controller.coveredRecipes();

        assertThat(covered).doesNotContain("org.test.Composite");
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.16.2"})
    void includedIn_returnsParentCompositesForCoveredRecipe() {
        TuiController controller = new TuiController(RECIPES_WITH_COMPOSITE);
        controller.moveDown(); // highlight Composite
        controller.toggleSelection(); // select Composite

        List<String> parents = controller.includedIn("org.test.Sub1");

        assertThat(parents).containsExactly("Composite Recipe");
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.16.2"})
    void includedIn_returnsEmptyForNonCoveredRecipe() {
        TuiController controller = new TuiController(RECIPES_WITH_COMPOSITE);
        controller.moveDown(); // highlight Composite
        controller.toggleSelection(); // select Composite

        List<String> parents = controller.includedIn("org.test.Alpha");

        assertThat(parents).isEmpty();
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.13"})
    void displayRows_pathFieldIsCorrectForAllPositions() {
        TuiController controller = new TuiController(RECIPES_WITH_COMPOSITE);
        controller.expandRecipe("org.test.Composite");

        List<DisplayRow> rows = controller.displayRows();

        assertThat(rows.get(0).path()).isEqualTo("org.test.Alpha");
        assertThat(rows.get(1).path()).isEqualTo("org.test.Composite");
        assertThat(rows.get(2).path()).isEqualTo("org.test.Composite/org.test.Sub1");
        assertThat(rows.get(3).path()).isEqualTo("org.test.Composite/org.test.Sub2");
        assertThat(rows.get(4).path()).isEqualTo("org.test.Gamma");
    }
}
