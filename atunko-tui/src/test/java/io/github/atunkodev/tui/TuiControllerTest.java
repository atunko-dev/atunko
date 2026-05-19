package io.github.atunkodev.tui;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.atunkodev.core.config.RunConfig;
import io.github.atunkodev.core.config.RunConfigService;
import io.github.atunkodev.core.engine.ExecutionResult;
import io.github.atunkodev.core.engine.FileChange;
import io.github.atunkodev.core.engine.ProjectExecutionResult;
import io.github.atunkodev.core.engine.WorkspaceExecutionEngine;
import io.github.atunkodev.core.engine.WorkspaceExecutionResult;
import io.github.atunkodev.core.project.ProjectEntry;
import io.github.atunkodev.core.project.ProjectInfo;
import io.github.atunkodev.core.project.Workspace;
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
    void initialStateBrowserScreenWithRecipesLoaded() {
        TuiController controller = new TuiController(RECIPES);

        assertThat(controller.currentScreen()).isEqualTo(Screen.BROWSER);
        assertThat(controller.recipes()).hasSize(3);
        assertThat(controller.highlightedIndex()).isZero();
        assertThat(controller.selectedRecipes()).isEmpty();
        assertThat(controller.searchQuery()).isEmpty();
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.1"})
    void recipesExposesFilteredSortedList() {
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
    void enterSearchModeEnablesSearchMode() {
        TuiController controller = new TuiController(RECIPES);

        controller.enterSearchMode();

        assertThat(controller.isSearchMode()).isTrue();
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.3"})
    void exitSearchModeDisablesSearchMode() {
        TuiController controller = new TuiController(RECIPES);
        controller.enterSearchMode();

        controller.exitSearchMode();

        assertThat(controller.isSearchMode()).isFalse();
    }

    // --- Search and Sort ---

    @Test
    @SVCs({"atunko:SVC_TUI_0001.3"})
    void setSearchQueryFiltersRecipesByNameDescriptionAndTags() {
        TuiController controller = new TuiController(RECIPES);

        controller.setSearchQuery("spring");

        assertThat(controller.recipes()).containsExactly(BETA);
        assertThat(controller.searchQuery()).isEqualTo("spring");
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.3"})
    void setSearchQueryMatchesByDescription() {
        TuiController controller = new TuiController(RECIPES);

        controller.setSearchQuery("Third");

        assertThat(controller.recipes()).containsExactly(GAMMA);
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.3"})
    void setSearchQueryEmptyQueryShowsAll() {
        TuiController controller = new TuiController(RECIPES);
        controller.setSearchQuery("spring");

        controller.setSearchQuery("");

        assertThat(controller.recipes()).hasSize(3);
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.3"})
    void setSearchQueryResetsHighlightedIndex() {
        TuiController controller = new TuiController(RECIPES);
        controller.moveDown();
        controller.moveDown();

        controller.setSearchQuery("spring");

        assertThat(controller.highlightedIndex()).isZero();
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.6"})
    void setSortOrderReordersRecipeList() {
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
    void setSortOrderNameOrderIsAlphabetical() {
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
    void highlightedRecipeReturnsCurrentlyHighlightedRecipe() {
        TuiController controller = new TuiController(RECIPES);

        assertThat(controller.highlightedRecipe()).isPresent();
        assertThat(controller.highlightedRecipe().get().name()).isEqualTo("org.test.Alpha");

        controller.moveDown();
        assertThat(controller.highlightedRecipe().get().name()).isEqualTo("org.test.Beta");
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.5"})
    void toggleSelectionAddsAndRemovesRecipes() {
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
    void openDetailSwitchesToDetailScreen() {
        TuiController controller = new TuiController(RECIPES);

        controller.openDetail();

        assertThat(controller.currentScreen()).isEqualTo(Screen.DETAIL);
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.4"})
    void goBackReturnsFromDetailToBrowser() {
        TuiController controller = new TuiController(RECIPES);
        controller.openDetail();

        controller.goBack();

        assertThat(controller.currentScreen()).isEqualTo(Screen.BROWSER);
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.12"})
    void moveDownWrapsAtEnd() {
        TuiController controller = new TuiController(RECIPES);

        controller.moveDown();
        controller.moveDown();
        assertThat(controller.highlightedIndex()).isEqualTo(2);

        controller.moveDown();
        assertThat(controller.highlightedIndex()).isZero();
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.12"})
    void moveUpWrapsAtStart() {
        TuiController controller = new TuiController(RECIPES);

        controller.moveUp();

        assertThat(controller.highlightedIndex()).isEqualTo(2);
    }

    // --- Tag Browser ---

    @Test
    @SVCs({"atunko:SVC_TUI_0001.11"})
    void allTagsReturnsUniqueTagsSorted() {
        TuiController controller = new TuiController(RECIPES);

        List<String> tags = controller.allTags();

        assertThat(tags).containsExactly("java", "spring", "testing");
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.11"})
    void openTagBrowserSwitchesToTagBrowserScreen() {
        TuiController controller = new TuiController(RECIPES);

        controller.openTagBrowser();

        assertThat(controller.currentScreen()).isEqualTo(Screen.TAG_BROWSER);
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.11"})
    void toggleTagAndApplyFiltersRecipesBySelectedTags() {
        TuiController controller = new TuiController(RECIPES);

        controller.toggleTag("spring");
        controller.applyTagFilter();

        assertThat(controller.recipes()).containsExactly(BETA);
        assertThat(controller.currentScreen()).isEqualTo(Screen.BROWSER);
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.11"})
    void toggleTagMultipleTagsFiltersUnion() {
        TuiController controller = new TuiController(RECIPES);

        controller.toggleTag("spring");
        controller.toggleTag("testing");

        // ALPHA has "testing", BETA has "spring"
        assertThat(controller.recipes()).containsExactlyInAnyOrder(ALPHA, BETA);
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.11"})
    void toggleTagDeselectsWhenAlreadySelected() {
        TuiController controller = new TuiController(RECIPES);
        controller.toggleTag("spring");

        controller.toggleTag("spring"); // deselect

        assertThat(controller.selectedTags()).isEmpty();
        assertThat(controller.recipes()).hasSize(3);
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.11"})
    void clearTagFilterShowsAllRecipes() {
        TuiController controller = new TuiController(RECIPES);
        controller.toggleTag("spring");

        controller.clearTagFilter();

        assertThat(controller.recipes()).hasSize(3);
        assertThat(controller.selectedTags()).isEmpty();
    }

    // --- Recipe Options ---

    @Test
    @SVCs({"atunko:SVC_TUI_0001.7"})
    void highlightedRecipeIsAvailableForOptionsLookup() {
        TuiController controller = new TuiController(RECIPES);

        assertThat(controller.highlightedRecipe()).isPresent();
        assertThat(controller.highlightedRecipe().get().name()).isEqualTo("org.test.Alpha");
    }

    // --- Dry-Run and Execution ---

    @Test
    @SVCs({"atunko:SVC_TUI_0001.8"})
    void showDryRunResultStoresResultAndSwitchesToExecutionScreen() {
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
    void showExecutionResultStoresResultAndSwitchesToExecutionScreen() {
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
    void saveRunConfigPersistsSelectedRecipes(@TempDir Path tempDir) throws Exception {
        RunConfigService service = new RunConfigService();
        TuiController controller = new TuiController(RECIPES, service);
        controller.toggleSelection(); // select Alpha
        controller.moveDown();
        controller.toggleSelection(); // select Beta

        Path configFile = tempDir.resolve(".atunko.yml");
        controller.saveRunConfig(configFile);

        RunConfig loaded = service.load(configFile);
        assertThat(loaded.recipes().stream().map(r -> r.name()).toList())
                .containsExactlyInAnyOrder("org.test.Alpha", "org.test.Beta");
    }

    // --- Load Run Configuration ---

    @Test
    @SVCs({"atunko:SVC_TUI_0001.19"})
    void loadRunConfigRestoresRecipeSelection(@TempDir Path tempDir) throws Exception {
        RunConfigService service = new RunConfigService();
        TuiController controller = new TuiController(RECIPES, service);
        controller.toggleSelection(); // select Alpha
        controller.moveDown();
        controller.toggleSelection(); // select Beta
        Path configFile = tempDir.resolve("myconfig.yml");
        controller.saveRunConfig(configFile);
        controller.deselectAll();

        RunConfig loaded = service.load(configFile);
        controller.loadRunConfig(loaded);

        assertThat(controller.selectedRecipes()).containsExactlyInAnyOrder("org.test.Alpha", "org.test.Beta");
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.19.1"})
    void loadRunConfigReplacesExistingSelection(@TempDir Path tempDir) throws Exception {
        RunConfigService service = new RunConfigService();
        TuiController controller = new TuiController(RECIPES, service);
        controller.toggleSelection(); // select Alpha (cursor at 0)
        Path configFile = tempDir.resolve("alpha.yml");
        controller.saveRunConfig(configFile);
        // Switch to a different selection before loading
        controller.deselectAll();
        controller.moveDown();
        controller.moveDown();
        controller.toggleSelection(); // select Gamma
        assertThat(controller.selectedRecipes()).containsExactly("org.test.Gamma");

        RunConfig loaded = service.load(configFile);
        controller.loadRunConfig(loaded);

        assertThat(controller.selectedRecipes()).containsExactly("org.test.Alpha");
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.19.2"})
    void listRunConfigsReturnsEmptyWhenDirectoryAbsent(@TempDir Path tempDir) {
        TuiController controller = new TuiController(RECIPES, new RunConfigService(), null, null, null, tempDir);

        List<Path> configs = controller.listRunConfigs();

        assertThat(configs).isEmpty();
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.19.2"})
    void listRunConfigsReturnsYmlFiles(@TempDir Path tempDir) throws Exception {
        RunConfigService service = new RunConfigService();
        TuiController controller = new TuiController(RECIPES, service, null, null, null, tempDir);
        controller.toggleSelection();
        java.nio.file.Files.createDirectories(tempDir.resolve("atunko/runs"));
        controller.saveRunConfig(tempDir.resolve("atunko/runs/first.yml"));
        controller.saveRunConfig(tempDir.resolve("atunko/runs/second.yml"));

        List<Path> configs = controller.listRunConfigs();

        assertThat(configs).hasSize(2);
        assertThat(configs).allMatch(p -> p.toString().endsWith(".yml"));
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.10"})
    void confirmSaveConfigWritesFileUnderProjectDir(@TempDir Path tempDir) throws Exception {
        RunConfigService service = new RunConfigService();
        TuiController controller = new TuiController(RECIPES, service, null, null, null, tempDir);
        controller.toggleSelection(); // select Alpha
        controller.enterSaveConfigMode();
        controller.setSaveConfigName("my-run");

        controller.confirmSaveConfig();

        Path expected = tempDir.resolve("atunko/runs/my-run.yml");
        assertThat(expected).exists();
        RunConfig loaded = service.load(expected);
        assertThat(loaded.recipes()).hasSize(1);
        assertThat(loaded.recipes().get(0).name()).isEqualTo("org.test.Alpha");
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.10"})
    void confirmSaveConfigWithBlankNameExitsWithoutSaving(@TempDir Path tempDir) throws Exception {
        TuiController controller = new TuiController(RECIPES, new RunConfigService(), null, null, null, tempDir);
        controller.toggleSelection();
        controller.enterSaveConfigMode();
        controller.setSaveConfigName("   ");

        controller.confirmSaveConfig();

        assertThat(controller.isSaveConfigMode()).isFalse();
        assertThat(tempDir.resolve("atunko/runs")).doesNotExist();
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.10"})
    void enterSaveConfigModeSetsModeFlag() {
        TuiController controller = new TuiController(RECIPES);

        controller.enterSaveConfigMode();

        assertThat(controller.isSaveConfigMode()).isTrue();
        assertThat(controller.saveConfigName()).isEmpty();
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.10"})
    void exitSaveConfigModeClearsFlag() {
        TuiController controller = new TuiController(RECIPES);
        controller.enterSaveConfigMode();
        controller.setSaveConfigName("draft");

        controller.exitSaveConfigMode();

        assertThat(controller.isSaveConfigMode()).isFalse();
        assertThat(controller.saveConfigName()).isEmpty();
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.19"})
    void openLoadConfigSetsScreenAndPopulatesFileList(@TempDir Path tempDir) throws Exception {
        RunConfigService service = new RunConfigService();
        TuiController controller = new TuiController(RECIPES, service, null, null, null, tempDir);
        controller.toggleSelection();
        java.nio.file.Files.createDirectories(tempDir.resolve("atunko/runs"));
        controller.saveRunConfig(tempDir.resolve("atunko/runs/saved.yml"));

        controller.openLoadConfig();

        assertThat(controller.currentScreen()).isEqualTo(Screen.LOAD_CONFIG);
        assertThat(controller.configFiles()).hasSize(1);
        assertThat(controller.loadConfigHighlightIndex()).isZero();
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.19"})
    void confirmLoadConfigLoadsFileAndReturnsToBrowser(@TempDir Path tempDir) throws Exception {
        RunConfigService service = new RunConfigService();
        TuiController controller = new TuiController(RECIPES, service, null, null, null, tempDir);
        controller.moveDown();
        controller.toggleSelection(); // select Beta
        java.nio.file.Files.createDirectories(tempDir.resolve("atunko/runs"));
        controller.saveRunConfig(tempDir.resolve("atunko/runs/beta.yml"));
        controller.deselectAll();

        controller.openLoadConfig();
        controller.confirmLoadConfig();

        assertThat(controller.currentScreen()).isEqualTo(Screen.BROWSER);
        assertThat(controller.selectedRecipes()).containsExactly("org.test.Beta");
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.10"})
    void moveLoadConfigDownAndUpNavigatesList(@TempDir Path tempDir) throws Exception {
        RunConfigService service = new RunConfigService();
        TuiController controller = new TuiController(RECIPES, service, null, null, null, tempDir);
        controller.toggleSelection();
        java.nio.file.Files.createDirectories(tempDir.resolve("atunko/runs"));
        controller.saveRunConfig(tempDir.resolve("atunko/runs/aaa.yml"));
        controller.saveRunConfig(tempDir.resolve("atunko/runs/bbb.yml"));
        controller.openLoadConfig();

        controller.moveLoadConfigDown();
        assertThat(controller.loadConfigHighlightIndex()).isEqualTo(1);

        controller.moveLoadConfigDown(); // clamp at 1
        assertThat(controller.loadConfigHighlightIndex()).isEqualTo(1);

        controller.moveLoadConfigUp();
        assertThat(controller.loadConfigHighlightIndex()).isZero();

        controller.moveLoadConfigUp(); // clamp at 0
        assertThat(controller.loadConfigHighlightIndex()).isZero();
    }

    // --- Cycle Selection ---

    @Test
    @SVCs({"atunko:SVC_TUI_0001.5"})
    void cycleSelectionSelectsAllWhenNoneSelected() {
        TuiController controller = new TuiController(RECIPES);

        controller.cycleSelection();

        assertThat(controller.selectedRecipes())
                .containsExactlyInAnyOrder("org.test.Alpha", "org.test.Beta", "org.test.Gamma");
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.5"})
    void cycleSelectionClearsWhenAllSelected() {
        TuiController controller = new TuiController(RECIPES);
        controller.cycleSelection(); // select all

        controller.cycleSelection(); // deselect all

        assertThat(controller.selectedRecipes()).isEmpty();
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.5"})
    void cycleSelectionSelectsAllWhenPartiallySelected() {
        TuiController controller = new TuiController(RECIPES);
        controller.toggleSelection(); // select Alpha only

        controller.cycleSelection(); // should select all since not all are selected

        assertThat(controller.selectedRecipes())
                .containsExactlyInAnyOrder("org.test.Alpha", "org.test.Beta", "org.test.Gamma");
    }

    // --- Composite Recipe Browsing ---

    @Test
    @SVCs({"atunko:SVC_TUI_0001.13"})
    void expandRecipeAddsToExpandedSet() {
        TuiController controller = new TuiController(RECIPES_WITH_COMPOSITE);

        controller.expandRecipe("org.test.Composite");

        assertThat(controller.isExpanded("org.test.Composite")).isTrue();
        assertThat(controller.expandedRecipes()).contains("org.test.Composite");
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.13"})
    void collapseRecipeRemovesFromExpandedSet() {
        TuiController controller = new TuiController(RECIPES_WITH_COMPOSITE);
        controller.expandRecipe("org.test.Composite");

        controller.collapseRecipe("org.test.Composite");

        assertThat(controller.isExpanded("org.test.Composite")).isFalse();
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.13"})
    void findRecipeReturnsCompositeWithSubRecipes() {
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
    void openConfirmRunInitializesRunOrderFromSelection() {
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
    void moveRunHighlightDownNavigatesRunList() {
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
    void moveRunHighlightUpWrapsAround() {
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
    void moveRunRecipeDownReordersRecipes() {
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
    void moveRunRecipeUpReordersRecipes() {
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
    void toggleRunRecipeTogglesIndividualRecipeSelection() {
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
    void cycleRunSelectionCyclesAllNone() {
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
    void flattenRunRecipeReplacesCompositeWithSubRecipes() {
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
    void flattenRunRecipeNonCompositeDoesNothing() {
        TuiController controller = new TuiController(RECIPES);
        controller.toggleSelection(); // select Alpha
        controller.openConfirmRun();

        controller.flattenRunRecipe();

        assertThat(controller.runOrder()).containsExactly("org.test.Alpha");
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.14.1"})
    void flattenAllRunRecipesReplacesAllCompositesWithSubRecipes() {
        TuiController controller = new TuiController(RECIPES_WITH_COMPOSITE);
        controller.moveDown(); // highlight Composite
        controller.toggleSelection(); // select Composite (cascade: Sub1, Sub2 also selected)
        controller.openConfirmRun();

        controller.flattenAllRunRecipes();

        assertThat(controller.runOrder()).containsExactly("org.test.Sub1", "org.test.Sub2");
        assertThat(controller.selectedRecipes()).containsExactlyInAnyOrder("org.test.Sub1", "org.test.Sub2");
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.14.1"})
    void flattenAllRunRecipesHandlesNestedComposites() {
        TuiController controller = new TuiController(RECIPES_WITH_NESTED);
        // Sorted: Alpha(0), Gamma(1), Outer(2) — highlight Outer
        controller.moveDown(); // Gamma
        controller.moveDown(); // Outer
        controller.toggleSelection(); // select Outer (cascade: Composite, Sub1, Sub2, Sub3)
        controller.openConfirmRun();

        controller.flattenAllRunRecipes();

        // Outer flattened to Composite + Sub3, then Composite flattened to Sub1 + Sub2
        assertThat(controller.runOrder()).containsExactly("org.test.Sub1", "org.test.Sub2", "org.test.Sub3");
        assertThat(controller.selectedRecipes())
                .containsExactlyInAnyOrder("org.test.Sub1", "org.test.Sub2", "org.test.Sub3");
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.14.1"})
    void flattenAllRunRecipesAlreadyFlatListIsUnchanged() {
        TuiController controller = new TuiController(RECIPES);
        controller.toggleSelection(); // select Alpha
        controller.moveDown();
        controller.toggleSelection(); // select Beta
        controller.openConfirmRun();

        controller.flattenAllRunRecipes();

        assertThat(controller.runOrder()).containsExactly("org.test.Alpha", "org.test.Beta");
        assertThat(controller.selectedRecipes()).containsExactlyInAnyOrder("org.test.Alpha", "org.test.Beta");
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.14.1"})
    void flattenAllRunRecipesMixedListFlattensOnlyComposites() {
        TuiController controller = new TuiController(RECIPES_WITH_COMPOSITE);
        controller.toggleSelection(); // select Alpha
        controller.moveDown(); // Composite
        controller.toggleSelection(); // select Composite (cascade: Sub1, Sub2)
        controller.openConfirmRun();

        controller.flattenAllRunRecipes();

        assertThat(controller.runOrder()).containsExactly("org.test.Alpha", "org.test.Sub1", "org.test.Sub2");
        assertThat(controller.selectedRecipes())
                .containsExactlyInAnyOrder("org.test.Alpha", "org.test.Sub1", "org.test.Sub2");
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.14.1"})
    void flattenAllRunRecipesDeduplicatesSharedLeaves() {
        // Two composites sharing Sub1 — flattening both should produce Sub1 only once
        RecipeInfo shared = new RecipeInfo("org.test.Shared", "Shared", "Shared leaf", Set.of());
        RecipeInfo compA =
                new RecipeInfo("org.test.CompA", "Comp A", "First composite", Set.of(), List.of(shared, SUB_2));
        RecipeInfo compB =
                new RecipeInfo("org.test.CompB", "Comp B", "Second composite", Set.of(), List.of(shared, SUB_3));
        TuiController controller = new TuiController(List.of(compA, compB));
        controller.toggleSelection(); // select CompA
        controller.moveDown();
        controller.toggleSelection(); // select CompB
        controller.openConfirmRun();

        controller.flattenAllRunRecipes();

        assertThat(controller.runOrder())
                .containsExactlyInAnyOrder("org.test.Shared", "org.test.Sub2", "org.test.Sub3");
        assertThat(controller.runOrder()).doesNotHaveDuplicates();
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.14.1"})
    void flattenAllRunRecipesMultipleLeafListIsUnchanged() {
        TuiController controller = new TuiController(RECIPES);
        controller.toggleSelection(); // select Alpha (leaf)
        controller.moveDown();
        controller.toggleSelection(); // select Beta (leaf)
        controller.openConfirmRun();

        controller.flattenAllRunRecipes();

        assertThat(controller.runOrder()).containsExactly("org.test.Alpha", "org.test.Beta");
        assertThat(controller.selectedRecipes()).containsExactlyInAnyOrder("org.test.Alpha", "org.test.Beta");
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.14"})
    void expandRunRecipeAddsToRunExpandedSet() {
        TuiController controller = new TuiController(RECIPES_WITH_COMPOSITE);
        controller.moveDown(); // highlight Composite
        controller.toggleSelection();
        controller.openConfirmRun();

        controller.expandRunRecipe();

        assertThat(controller.runExpandedRecipes()).contains("org.test.Composite");
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.14"})
    void collapseRunRecipeRemovesFromRunExpandedSet() {
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
    void displayRowsWithoutExpansionContainsOnlyParentRows() {
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
    void displayRowsWithExpansionIncludesSubRecipeRows() {
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
    void moveDownNavigatesExpandedDisplayRows() {
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
    void moveUpNavigatesExpandedDisplayRows() {
        TuiController controller = new TuiController(RECIPES_WITH_COMPOSITE);
        controller.expandRecipe("org.test.Composite");

        controller.moveUp(); // wraps to Gamma (index 4)

        assertThat(controller.highlightedIndex()).isEqualTo(4);
        assertThat(controller.highlightedRecipe()).isPresent();
        assertThat(controller.highlightedRecipe().get().name()).isEqualTo("org.test.Gamma");
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.13"})
    void highlightedDisplayRowReturnsCorrectRow() {
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
    void collapseRecipeClampsHighlightIndex() {
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
    @SVCs({"atunko:SVC_TUI_0001.17"})
    void toggleSelectionSelectingCompositeCascadeSelectsCompositeAndAllSubs() {
        TuiController controller = new TuiController(RECIPES_WITH_COMPOSITE);
        controller.expandRecipe("org.test.Composite");
        controller.moveDown(); // highlight Composite (index 1)

        controller.toggleSelection();

        // Cascade-down: composite + all transitive sub-recipes become selected
        assertThat(controller.selectedRecipes())
                .containsExactlyInAnyOrder("org.test.Composite", "org.test.Sub1", "org.test.Sub2");
        assertThat(controller.partialRecipes()).isEmpty();
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.5"})
    void toggleSelectionOnSubRecipeSelectsSubRecipeIndividually() {
        TuiController controller = new TuiController(RECIPES_WITH_COMPOSITE);
        controller.expandRecipe("org.test.Composite");
        controller.moveDown(); // Composite
        controller.moveDown(); // Sub1

        controller.toggleSelection();

        assertThat(controller.selectedRecipes()).containsExactly("org.test.Sub1");
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.5"})
    void toggleSelectionOnSubRecipeDeselectsSubRecipeIndividually() {
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
    void cycleSelectionSelectsAllVisibleNames() {
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
    void runDisplayRowsWithExpandedCompositeIncludesSubRecipes() {
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
    void moveRunHighlightDownNavigatesExpandedRunDisplayRows() {
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
    void clearAllResetsSearchTagsAndSelections() {
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
    void displayRowsNestedCompositeHasCorrectDepth() {
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
    void displayRowsDeeplyNestedExpandsBothLevels() {
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
    void expandRecipeOnNestedSubRecipeExpandsIt() {
        TuiController controller = new TuiController(RECIPES_WITH_NESTED);
        controller.expandRecipe("org.test.Outer");

        controller.expandRecipe("org.test.Composite");

        assertThat(controller.isExpanded("org.test.Composite")).isTrue();
        assertThat(controller.displayRows()).hasSize(7);
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.5"})
    void toggleSelectionOnNestedSubRecipeSelectsIndividually() {
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
    void toggleSelectionSameRecipeAtDifferentPositionsSelectsByRecipeName() {
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
    void expandRecipeSameCompositeAtDifferentPositionsExpandsAllInstances() {
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

    // --- File Diff Navigation (TUI_0001.19 / TUI_0001.20) ---

    @Test
    @SVCs({"atunko:SVC_TUI_0001.19"})
    void selectedFileIndexStartsAtZero() {
        TuiController controller = new TuiController(RECIPES);

        assertThat(controller.selectedFileIndex()).isZero();
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.19"})
    void moveFileDownIncrementsIndex() {
        TuiController controller = new TuiController(RECIPES);
        ExecutionResult result = new ExecutionResult(
                List.of(new FileChange(Path.of("A.java"), "a", "a2"), new FileChange(Path.of("B.java"), "b", "b2")));
        controller.showDryRunResult(result);

        controller.moveFileDown();

        assertThat(controller.selectedFileIndex()).isEqualTo(1);
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.19"})
    void moveFileDownClampsAtLastIndex() {
        TuiController controller = new TuiController(RECIPES);
        ExecutionResult result = new ExecutionResult(
                List.of(new FileChange(Path.of("A.java"), "a", "a2"), new FileChange(Path.of("B.java"), "b", "b2")));
        controller.showDryRunResult(result);
        controller.moveFileDown(); // 1

        controller.moveFileDown(); // should stay at 1

        assertThat(controller.selectedFileIndex()).isEqualTo(1);
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.19"})
    void moveFileUpDecrementsIndex() {
        TuiController controller = new TuiController(RECIPES);
        ExecutionResult result = new ExecutionResult(
                List.of(new FileChange(Path.of("A.java"), "a", "a2"), new FileChange(Path.of("B.java"), "b", "b2")));
        controller.showDryRunResult(result);
        controller.moveFileDown();

        controller.moveFileUp();

        assertThat(controller.selectedFileIndex()).isZero();
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.19"})
    void moveFileUpClampsAtZero() {
        TuiController controller = new TuiController(RECIPES);
        ExecutionResult result = new ExecutionResult(List.of(new FileChange(Path.of("A.java"), "a", "a2")));
        controller.showDryRunResult(result);

        controller.moveFileUp(); // already 0

        assertThat(controller.selectedFileIndex()).isZero();
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.20"})
    void openFileDiffSwitchesToFileDiffScreen() {
        TuiController controller = new TuiController(RECIPES);
        ExecutionResult result = new ExecutionResult(List.of(new FileChange(Path.of("A.java"), "a", "a2")));
        controller.showDryRunResult(result);

        controller.openFileDiff();

        assertThat(controller.currentScreen()).isEqualTo(Screen.FILE_DIFF);
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.20.2"})
    void openFileDiffDoesNothingWhenNoResults() {
        TuiController controller = new TuiController(RECIPES);

        controller.openFileDiff();

        assertThat(controller.currentScreen()).isEqualTo(Screen.BROWSER);
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.20.2"})
    void openFileDiffDoesNothingWhenEmptyChangesList() {
        TuiController controller = new TuiController(RECIPES);
        controller.showExecutionResult(new ExecutionResult(List.of()));

        controller.openFileDiff();

        assertThat(controller.currentScreen()).isEqualTo(Screen.EXECUTION_RESULTS);
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.20.1"})
    void returnFromFileDiffGoesBackToExecutionResults() {
        TuiController controller = new TuiController(RECIPES);
        ExecutionResult result = new ExecutionResult(List.of(new FileChange(Path.of("A.java"), "a", "a2")));
        controller.showDryRunResult(result);
        controller.openFileDiff();

        controller.returnFromFileDiff();

        assertThat(controller.currentScreen()).isEqualTo(Screen.EXECUTION_RESULTS);
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.19.1"})
    void showDryRunResultResetsSelectedFileIndex() {
        TuiController controller = new TuiController(RECIPES);
        ExecutionResult result = new ExecutionResult(
                List.of(new FileChange(Path.of("A.java"), "a", "a2"), new FileChange(Path.of("B.java"), "b", "b2")));
        controller.showDryRunResult(result);
        controller.moveFileDown();

        controller.showDryRunResult(result); // new result resets index

        assertThat(controller.selectedFileIndex()).isZero();
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.19.1"})
    void showExecutionResultResetsSelectedFileIndex() {
        TuiController controller = new TuiController(RECIPES);
        ExecutionResult result = new ExecutionResult(
                List.of(new FileChange(Path.of("A.java"), "a", "a2"), new FileChange(Path.of("B.java"), "b", "b2")));
        controller.showExecutionResult(result);
        controller.moveFileDown();

        controller.showExecutionResult(result);

        assertThat(controller.selectedFileIndex()).isZero();
    }

    // --- Help Overlay ---

    @Test
    @SVCs({"atunko:SVC_TUI_0001"})
    void toggleHelpTogglesHelpState() {
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
    void coveredRecipesReturnsSubRecipesOfSelectedComposites() {
        TuiController controller = new TuiController(RECIPES_WITH_COMPOSITE);
        controller.moveDown(); // highlight Composite
        controller.toggleSelection(); // select Composite

        Set<String> covered = controller.coveredRecipes();

        assertThat(covered).containsExactlyInAnyOrder("org.test.Sub1", "org.test.Sub2");
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.16.1"})
    void coveredRecipesRecursivelyIncludesNestedSubRecipes() {
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
    void coveredRecipesEmptyWhenNoCompositesSelected() {
        TuiController controller = new TuiController(RECIPES_WITH_COMPOSITE);
        controller.toggleSelection(); // select Alpha (non-composite)

        Set<String> covered = controller.coveredRecipes();

        assertThat(covered).isEmpty();
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.16"})
    void coveredRecipesDoesNotIncludeTheSelectedCompositeItself() {
        TuiController controller = new TuiController(RECIPES_WITH_COMPOSITE);
        controller.moveDown(); // highlight Composite
        controller.toggleSelection(); // select Composite

        Set<String> covered = controller.coveredRecipes();

        assertThat(covered).doesNotContain("org.test.Composite");
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.16.2"})
    void includedInReturnsParentCompositesForCoveredRecipe() {
        TuiController controller = new TuiController(RECIPES_WITH_COMPOSITE);
        controller.moveDown(); // highlight Composite
        controller.toggleSelection(); // select Composite

        List<String> parents = controller.includedIn("org.test.Sub1");

        assertThat(parents).containsExactly("Composite Recipe");
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.16.2"})
    void includedInReturnsEmptyForNonCoveredRecipe() {
        TuiController controller = new TuiController(RECIPES_WITH_COMPOSITE);
        controller.moveDown(); // highlight Composite
        controller.toggleSelection(); // select Composite

        List<String> parents = controller.includedIn("org.test.Alpha");

        assertThat(parents).isEmpty();
    }

    // --- Cascade Multi-Select (TUI_0001.17) ---

    @Test
    @SVCs({"atunko:SVC_TUI_0001.17"})
    void toggleSelectionSelectingCompositeSubRecipesAndCompositeAllSelected() {
        TuiController controller = new TuiController(RECIPES_WITH_COMPOSITE);
        controller.moveDown(); // highlight Composite (sorted: Alpha, Composite, Gamma)

        controller.toggleSelection();

        // Cascade-down: composite + subs all in selectedRecipes; no partial state
        assertThat(controller.selectedRecipes())
                .containsExactlyInAnyOrder("org.test.Composite", "org.test.Sub1", "org.test.Sub2");
        assertThat(controller.partialRecipes()).isEmpty();
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.17.1"})
    void toggleSelectionSelectingAllSubRecipesCascadeUpSelectsComposite() {
        TuiController controller = new TuiController(RECIPES_WITH_COMPOSITE);
        controller.expandRecipe("org.test.Composite");
        // Sorted + expanded: Alpha(0), Composite(1), Sub1(2), Sub2(3), Gamma(4)
        controller.moveDown(); // Composite
        controller.moveDown(); // Sub1

        controller.toggleSelection(); // select Sub1
        assertThat(controller.selectedRecipes()).containsExactly("org.test.Sub1");
        assertThat(controller.partialRecipes()).contains("org.test.Composite");

        controller.moveDown(); // Sub2
        controller.toggleSelection(); // select Sub2

        // All subs selected → cascade-up auto-selects Composite
        assertThat(controller.selectedRecipes())
                .containsExactlyInAnyOrder("org.test.Sub1", "org.test.Sub2", "org.test.Composite");
        assertThat(controller.partialRecipes()).doesNotContain("org.test.Composite");
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.17.2"})
    void toggleSelectionDeselectingCompositeAlsoDeselectsExplicitlySelectedSubs() {
        TuiController controller = new TuiController(RECIPES_WITH_COMPOSITE);
        controller.expandRecipe("org.test.Composite");
        // Sorted + expanded: Alpha(0), Composite(1), Sub1(2), Sub2(3), Gamma(4)
        controller.moveDown(); // Composite
        controller.moveDown(); // Sub1
        controller.toggleSelection(); // select Sub1
        controller.moveDown(); // Sub2
        controller.toggleSelection(); // select Sub2 → cascade-up selects Composite
        assertThat(controller.selectedRecipes())
                .containsExactlyInAnyOrder("org.test.Sub1", "org.test.Sub2", "org.test.Composite");

        // Navigate back to Composite and deselect it (cursor is at Sub2=3, Composite=1: 2 ups)
        controller.moveUp(); // → Sub1
        controller.moveUp(); // → Composite
        controller.toggleSelection(); // deselect Composite → cascade-down removes Sub1, Sub2

        assertThat(controller.selectedRecipes()).isEmpty();
        assertThat(controller.partialRecipes()).isEmpty();
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.17.3"})
    void toggleSelectionSelectingOneSubCompositeBecomesPartial() {
        TuiController controller = new TuiController(RECIPES_WITH_COMPOSITE);
        controller.expandRecipe("org.test.Composite");
        // Sorted + expanded: Alpha(0), Composite(1), Sub1(2), Sub2(3), Gamma(4)
        controller.moveDown(); // Composite
        controller.moveDown(); // Sub1

        controller.toggleSelection(); // select Sub1 only

        // Composite has one selected child and one unselected → indeterminate
        assertThat(controller.partialRecipes()).contains("org.test.Composite");
        assertThat(controller.selectedRecipes()).doesNotContain("org.test.Composite");
        assertThat(controller.selectedRecipes()).containsExactly("org.test.Sub1");
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.17.3"})
    void deselectingOneSubLeavesPartialStateOnComposite() {
        TuiController controller = new TuiController(RECIPES_WITH_COMPOSITE);
        controller.expandRecipe("org.test.Composite");
        // Alpha(0), Composite(1), Sub1(2), Sub2(3), Gamma(4)
        controller.moveDown(); // Composite
        controller.moveDown(); // Sub1
        controller.toggleSelection();
        controller.moveDown(); // Sub2
        controller.toggleSelection();
        // Both subs selected → cascade-up selected Composite
        assertThat(controller.selectedRecipes())
                .containsExactlyInAnyOrder("org.test.Sub1", "org.test.Sub2", "org.test.Composite");

        // Deselect Sub2 — Composite should become partial
        controller.toggleSelection();

        assertThat(controller.selectedRecipes()).containsExactlyInAnyOrder("org.test.Sub1");
        assertThat(controller.partialRecipes()).contains("org.test.Composite");
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.14"})
    void runDialogNotAffectedByCascadeSimpleToggle() {
        TuiController controller = new TuiController(RECIPES_WITH_COMPOSITE);
        controller.moveDown(); // Composite (sorted: Alpha, Composite, Gamma)
        controller.toggleSelection(); // cascade selects Composite + Sub1 + Sub2

        controller.openConfirmRun();

        // Run dialog shows only Composite (sub-recipes deduplicated since composite subsumes them)
        assertThat(controller.runOrder()).containsExactly("org.test.Composite");

        // Toggle Composite off in run dialog — run dialog uses cascade=false, so only Composite removed
        controller.toggleRunRecipe();
        // Sub1 and Sub2 remain in selectedRecipes (run dialog does not cascade-down)
        assertThat(controller.selectedRecipes()).doesNotContain("org.test.Composite");
        assertThat(controller.selectedRecipes()).containsExactlyInAnyOrder("org.test.Sub1", "org.test.Sub2");
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.5"})
    void cycleSelectionWithCascadeRecomputesPartialState() {
        TuiController controller = new TuiController(RECIPES_WITH_COMPOSITE);
        controller.expandRecipe("org.test.Composite");

        controller.cycleSelection(); // select all visible

        // All visible selected; recomputePartialState should not mark Composite as partial
        // (Composite is explicitly in selectedRecipes)
        assertThat(controller.partialRecipes()).isEmpty();

        controller.cycleSelection(); // deselect all

        assertThat(controller.selectedRecipes()).isEmpty();
        assertThat(controller.partialRecipes()).isEmpty();
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.13"})
    void displayRowsPathFieldIsCorrectForAllPositions() {
        TuiController controller = new TuiController(RECIPES_WITH_COMPOSITE);
        controller.expandRecipe("org.test.Composite");

        List<DisplayRow> rows = controller.displayRows();

        assertThat(rows.get(0).path()).isEqualTo("org.test.Alpha");
        assertThat(rows.get(1).path()).isEqualTo("org.test.Composite");
        assertThat(rows.get(2).path()).isEqualTo("org.test.Composite/org.test.Sub1");
        assertThat(rows.get(3).path()).isEqualTo("org.test.Composite/org.test.Sub2");
        assertThat(rows.get(4).path()).isEqualTo("org.test.Gamma");
    }

    // --- Workspace execution ---

    @Test
    @SVCs({"atunko:SVC_TUI_0002.3"})
    void runSelectedRecipesInWorkspaceModeStoresWorkspaceResultAndNavigatesToWorkspaceResultsScreen(
            @TempDir Path workspaceDir, @TempDir Path projectA, @TempDir Path projectB) {
        ProjectInfo info = new ProjectInfo(List.of(), List.of());
        ProjectEntry entryA = new ProjectEntry(projectA, info);
        ProjectEntry entryB = new ProjectEntry(projectB, info);
        List<ProjectEntry> entries = List.of(entryA, entryB);

        WorkspaceExecutionResult fakeResult = new WorkspaceExecutionResult(List.of(
                new ProjectExecutionResult(entryA, new ExecutionResult(List.of()), null),
                new ProjectExecutionResult(
                        entryB, new ExecutionResult(List.of(new FileChange(Path.of("Foo.java"), "a", "b"))), null)));

        WorkspaceExecutionEngine stubEngine = new WorkspaceExecutionEngine(null, null) {
            @Override
            public WorkspaceExecutionResult execute(List<String> recipeNames, Workspace workspace) {
                return fakeResult;
            }
        };

        TuiController controller =
                new TuiController(RECIPES, new RunConfigService(), null, null, null, stubEngine, entries, workspaceDir);
        controller.toggleSelection(); // select Alpha
        controller.openConfirmRun();

        controller.runSelectedRecipes(false);

        assertThat(controller.currentScreen()).isEqualTo(Screen.WORKSPACE_RESULTS);
        assertThat(controller.lastWorkspaceResult()).isSameAs(fakeResult);
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0002.5"})
    void runsDirUsesWorkspaceRootWhenInWorkspaceMode(@TempDir Path workspaceDir) {
        TuiController controller = new TuiController(RECIPES, new RunConfigService(), null, null, null, workspaceDir);

        assertThat(controller.runsDir()).isEqualTo(workspaceDir.resolve("atunko/runs"));
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0002.5"})
    void runsDirUsesProjectDirWhenInSingleProjectMode(@TempDir Path projectDir) {
        TuiController controller = new TuiController(RECIPES, new RunConfigService(), null, null, null, projectDir);

        assertThat(controller.runsDir()).isEqualTo(projectDir.resolve("atunko/runs"));
    }

    // --- Export config state ---

    @Test
    @SVCs({"atunko:SVC_TUI_0001.21"})
    void defaultExportStateIsGradleMinimalAndClosed() {
        TuiController controller = new TuiController(RECIPES);

        assertThat(controller.exportFormat()).isEqualTo(TuiController.ExportFormat.GRADLE);
        assertThat(controller.exportMode())
                .isEqualTo(io.github.atunkodev.core.config.ConfigExportService.ExportMode.MINIMAL);
        assertThat(controller.isShowExport()).isFalse();
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.22"})
    void setExportFormatTogglesFormat() {
        TuiController controller = new TuiController(RECIPES);

        controller.setExportFormat(TuiController.ExportFormat.MAVEN);
        assertThat(controller.exportFormat()).isEqualTo(TuiController.ExportFormat.MAVEN);

        controller.setExportFormat(TuiController.ExportFormat.GRADLE);
        assertThat(controller.exportFormat()).isEqualTo(TuiController.ExportFormat.GRADLE);
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.23"})
    void toggleExportModeCyclesMinimalAndFull() {
        TuiController controller = new TuiController(RECIPES);

        assertThat(controller.exportMode())
                .isEqualTo(io.github.atunkodev.core.config.ConfigExportService.ExportMode.MINIMAL);

        controller.toggleExportMode();
        assertThat(controller.exportMode())
                .isEqualTo(io.github.atunkodev.core.config.ConfigExportService.ExportMode.FULL);

        controller.toggleExportMode();
        assertThat(controller.exportMode())
                .isEqualTo(io.github.atunkodev.core.config.ConfigExportService.ExportMode.MINIMAL);
    }
}
