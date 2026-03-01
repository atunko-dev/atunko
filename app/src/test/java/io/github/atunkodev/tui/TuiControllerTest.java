package io.github.atunkodev.tui;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.atunkodev.cli.SortOrder;
import io.github.atunkodev.core.config.RunConfig;
import io.github.atunkodev.core.config.RunConfigService;
import io.github.atunkodev.core.engine.ExecutionResult;
import io.github.atunkodev.core.engine.FileChange;
import io.github.atunkodev.core.recipe.RecipeInfo;
import io.github.reqstool.annotations.SVCs;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@SVCs({"SVC_CLI_0001"})
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

    private static final List<RecipeInfo> RECIPES = List.of(ALPHA, BETA, GAMMA);
    private static final List<RecipeInfo> RECIPES_WITH_COMPOSITE = List.of(ALPHA, COMPOSITE, GAMMA);

    @Test
    @SVCs({"SVC_CLI_0001"})
    void initialState_browserScreenWithRecipesLoaded() {
        TuiController controller = new TuiController(RECIPES);

        assertThat(controller.currentScreen()).isEqualTo(Screen.BROWSER);
        assertThat(controller.recipes()).hasSize(3);
        assertThat(controller.highlightedIndex()).isZero();
        assertThat(controller.selectedRecipes()).isEmpty();
        assertThat(controller.searchQuery()).isEmpty();
    }

    @Test
    @SVCs({"SVC_CLI_0001.1"})
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
    @SVCs({"SVC_CLI_0001.3"})
    void enterSearchMode_enablesSearchMode() {
        TuiController controller = new TuiController(RECIPES);

        controller.enterSearchMode();

        assertThat(controller.isSearchMode()).isTrue();
    }

    @Test
    @SVCs({"SVC_CLI_0001.3"})
    void exitSearchMode_disablesSearchMode() {
        TuiController controller = new TuiController(RECIPES);
        controller.enterSearchMode();

        controller.exitSearchMode();

        assertThat(controller.isSearchMode()).isFalse();
    }

    // --- Search and Sort ---

    @Test
    @SVCs({"SVC_CLI_0001.3"})
    void setSearchQuery_filtersRecipesByNameDescriptionAndTags() {
        TuiController controller = new TuiController(RECIPES);

        controller.setSearchQuery("spring");

        assertThat(controller.recipes()).containsExactly(BETA);
        assertThat(controller.searchQuery()).isEqualTo("spring");
    }

    @Test
    @SVCs({"SVC_CLI_0001.3"})
    void setSearchQuery_matchesByDescription() {
        TuiController controller = new TuiController(RECIPES);

        controller.setSearchQuery("Third");

        assertThat(controller.recipes()).containsExactly(GAMMA);
    }

    @Test
    @SVCs({"SVC_CLI_0001.3"})
    void setSearchQuery_emptyQueryShowsAll() {
        TuiController controller = new TuiController(RECIPES);
        controller.setSearchQuery("spring");

        controller.setSearchQuery("");

        assertThat(controller.recipes()).hasSize(3);
    }

    @Test
    @SVCs({"SVC_CLI_0001.3"})
    void setSearchQuery_resetsHighlightedIndex() {
        TuiController controller = new TuiController(RECIPES);
        controller.moveDown();
        controller.moveDown();

        controller.setSearchQuery("spring");

        assertThat(controller.highlightedIndex()).isZero();
    }

    @Test
    @SVCs({"SVC_CLI_0001.6"})
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
    @SVCs({"SVC_CLI_0001.6"})
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
    @SVCs({"SVC_CLI_0001.2"})
    void highlightedRecipe_returnsCurrentlyHighlightedRecipe() {
        TuiController controller = new TuiController(RECIPES);

        assertThat(controller.highlightedRecipe()).isPresent();
        assertThat(controller.highlightedRecipe().get().name()).isEqualTo("org.test.Alpha");

        controller.moveDown();
        assertThat(controller.highlightedRecipe().get().name()).isEqualTo("org.test.Beta");
    }

    @Test
    @SVCs({"SVC_CLI_0001.5"})
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
    @SVCs({"SVC_CLI_0001.4"})
    void openDetail_switchesToDetailScreen() {
        TuiController controller = new TuiController(RECIPES);

        controller.openDetail();

        assertThat(controller.currentScreen()).isEqualTo(Screen.DETAIL);
    }

    @Test
    @SVCs({"SVC_CLI_0001.4"})
    void goBack_returnsFromDetailToBrowser() {
        TuiController controller = new TuiController(RECIPES);
        controller.openDetail();

        controller.goBack();

        assertThat(controller.currentScreen()).isEqualTo(Screen.BROWSER);
    }

    @Test
    @SVCs({"SVC_CLI_0001.12"})
    void moveDown_wrapsAtEnd() {
        TuiController controller = new TuiController(RECIPES);

        controller.moveDown();
        controller.moveDown();
        assertThat(controller.highlightedIndex()).isEqualTo(2);

        controller.moveDown();
        assertThat(controller.highlightedIndex()).isZero();
    }

    @Test
    @SVCs({"SVC_CLI_0001.12"})
    void moveUp_wrapsAtStart() {
        TuiController controller = new TuiController(RECIPES);

        controller.moveUp();

        assertThat(controller.highlightedIndex()).isEqualTo(2);
    }

    // --- Tag Browser ---

    @Test
    @SVCs({"SVC_CLI_0001.11"})
    void allTags_returnsUniqueTagsSorted() {
        TuiController controller = new TuiController(RECIPES);

        List<String> tags = controller.allTags();

        assertThat(tags).containsExactly("java", "spring", "testing");
    }

    @Test
    @SVCs({"SVC_CLI_0001.11"})
    void openTagBrowser_switchesToTagBrowserScreen() {
        TuiController controller = new TuiController(RECIPES);

        controller.openTagBrowser();

        assertThat(controller.currentScreen()).isEqualTo(Screen.TAG_BROWSER);
    }

    @Test
    @SVCs({"SVC_CLI_0001.11"})
    void filterByTag_filtersRecipesToThoseWithTag() {
        TuiController controller = new TuiController(RECIPES);

        controller.filterByTag("spring");

        assertThat(controller.recipes()).containsExactly(BETA);
        assertThat(controller.currentScreen()).isEqualTo(Screen.BROWSER);
    }

    @Test
    @SVCs({"SVC_CLI_0001.11"})
    void clearTagFilter_showsAllRecipes() {
        TuiController controller = new TuiController(RECIPES);
        controller.filterByTag("spring");

        controller.clearTagFilter();

        assertThat(controller.recipes()).hasSize(3);
    }

    // --- Recipe Options ---

    @Test
    @SVCs({"SVC_CLI_0001.7"})
    void highlightedRecipe_isAvailableForOptionsLookup() {
        TuiController controller = new TuiController(RECIPES);

        assertThat(controller.highlightedRecipe()).isPresent();
        assertThat(controller.highlightedRecipe().get().name()).isEqualTo("org.test.Alpha");
    }

    // --- Dry-Run and Execution ---

    @Test
    @SVCs({"SVC_CLI_0001.8"})
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
    @SVCs({"SVC_CLI_0001.9"})
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
    @SVCs({"SVC_CLI_0001.10"})
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
    @SVCs({"SVC_CLI_0001.5"})
    void cycleSelection_selectsAllWhenNoneSelected() {
        TuiController controller = new TuiController(RECIPES);

        controller.cycleSelection();

        assertThat(controller.selectedRecipes())
                .containsExactlyInAnyOrder("org.test.Alpha", "org.test.Beta", "org.test.Gamma");
    }

    @Test
    @SVCs({"SVC_CLI_0001.5"})
    void cycleSelection_clearsWhenAllSelected() {
        TuiController controller = new TuiController(RECIPES);
        controller.cycleSelection(); // select all

        controller.cycleSelection(); // deselect all

        assertThat(controller.selectedRecipes()).isEmpty();
    }

    @Test
    @SVCs({"SVC_CLI_0001.5"})
    void cycleSelection_selectsAllWhenPartiallySelected() {
        TuiController controller = new TuiController(RECIPES);
        controller.toggleSelection(); // select Alpha only

        controller.cycleSelection(); // should select all since not all are selected

        assertThat(controller.selectedRecipes())
                .containsExactlyInAnyOrder("org.test.Alpha", "org.test.Beta", "org.test.Gamma");
    }

    // --- Composite Recipe Browsing ---

    @Test
    @SVCs({"SVC_CLI_0001.13"})
    void expandRecipe_addsToExpandedSet() {
        TuiController controller = new TuiController(RECIPES_WITH_COMPOSITE);

        controller.expandRecipe("org.test.Composite");

        assertThat(controller.isExpanded("org.test.Composite")).isTrue();
        assertThat(controller.expandedRecipes()).contains("org.test.Composite");
    }

    @Test
    @SVCs({"SVC_CLI_0001.13"})
    void collapseRecipe_removesFromExpandedSet() {
        TuiController controller = new TuiController(RECIPES_WITH_COMPOSITE);
        controller.expandRecipe("org.test.Composite");

        controller.collapseRecipe("org.test.Composite");

        assertThat(controller.isExpanded("org.test.Composite")).isFalse();
    }

    @Test
    @SVCs({"SVC_CLI_0001.13"})
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
    @SVCs({"SVC_CLI_0001.14"})
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
    @SVCs({"SVC_CLI_0001.14"})
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
    @SVCs({"SVC_CLI_0001.14"})
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
    @SVCs({"SVC_CLI_0001.14"})
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
    @SVCs({"SVC_CLI_0001.14"})
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
    @SVCs({"SVC_CLI_0001.14"})
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
    @SVCs({"SVC_CLI_0001.14"})
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
    @SVCs({"SVC_CLI_0001.14"})
    void flattenRunRecipe_replacesCompositeWithSubRecipes() {
        TuiController controller = new TuiController(RECIPES_WITH_COMPOSITE);
        controller.moveDown(); // highlight Composite
        controller.toggleSelection(); // select Composite
        controller.openConfirmRun();

        controller.flattenRunRecipe(); // flatten Composite at index 0

        assertThat(controller.runOrder()).containsExactly("org.test.Sub1", "org.test.Sub2");
        assertThat(controller.selectedRecipes()).containsExactlyInAnyOrder("org.test.Sub1", "org.test.Sub2");
    }

    @Test
    @SVCs({"SVC_CLI_0001.14"})
    void flattenRunRecipe_nonComposite_doesNothing() {
        TuiController controller = new TuiController(RECIPES);
        controller.toggleSelection(); // select Alpha
        controller.openConfirmRun();

        controller.flattenRunRecipe();

        assertThat(controller.runOrder()).containsExactly("org.test.Alpha");
    }

    @Test
    @SVCs({"SVC_CLI_0001.14"})
    void expandRunRecipe_addsToRunExpandedSet() {
        TuiController controller = new TuiController(RECIPES_WITH_COMPOSITE);
        controller.moveDown(); // highlight Composite
        controller.toggleSelection();
        controller.openConfirmRun();

        controller.expandRunRecipe();

        assertThat(controller.runExpandedRecipes()).contains("org.test.Composite");
    }

    @Test
    @SVCs({"SVC_CLI_0001.14"})
    void collapseRunRecipe_removesFromRunExpandedSet() {
        TuiController controller = new TuiController(RECIPES_WITH_COMPOSITE);
        controller.moveDown();
        controller.toggleSelection();
        controller.openConfirmRun();
        controller.expandRunRecipe();

        controller.collapseRunRecipe();

        assertThat(controller.runExpandedRecipes()).doesNotContain("org.test.Composite");
    }
}
