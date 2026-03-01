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

    private static final List<RecipeInfo> RECIPES = List.of(ALPHA, BETA, GAMMA);

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
}
