package io.github.atunkodev.tui;

import io.github.atunkodev.cli.SortOrder;
import io.github.atunkodev.core.config.RunConfig;
import io.github.atunkodev.core.config.RunConfigService;
import io.github.atunkodev.core.engine.ChangeApplier;
import io.github.atunkodev.core.engine.ExecutionResult;
import io.github.atunkodev.core.engine.FileChange;
import io.github.atunkodev.core.engine.RecipeExecutionEngine;
import io.github.atunkodev.core.project.GradleProjectScanner;
import io.github.atunkodev.core.project.ProjectInfo;
import io.github.atunkodev.core.project.ProjectSourceParser;
import io.github.atunkodev.core.recipe.RecipeInfo;
import io.github.reqstool.annotations.Requirements;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.openrewrite.SourceFile;

@Requirements({"CLI_0001"})
public class TuiController {

    public record DisplayRow(RecipeInfo recipe, boolean isSubRecipe, String parentName) {}

    private static final Logger LOG = Logger.getLogger(TuiController.class.getName());

    private final List<RecipeInfo> allRecipes;
    private final RunConfigService runConfigService;
    private final RecipeExecutionEngine engine;
    private final GradleProjectScanner projectScanner;
    private final ProjectSourceParser sourceParser;
    private final ChangeApplier changeApplier;
    private final Path projectDir;
    private Screen currentScreen = Screen.BROWSER;
    private String searchQuery = "";
    private SortOrder sortOrder = SortOrder.NAME;
    private int highlightedIndex;
    private final Set<String> selectedRecipes = new LinkedHashSet<>();
    private String tagFilter = "";
    private boolean searchMode;
    private ExecutionResult executionResult;
    private boolean lastRunWasDryRun;
    private final Set<String> expandedRecipes = new LinkedHashSet<>();

    // Run dialog state
    private List<String> runOrder = new ArrayList<>();
    private int runHighlightIndex;
    private final Set<String> runExpandedRecipes = new LinkedHashSet<>();

    public TuiController(List<RecipeInfo> allRecipes) {
        this(allRecipes, new RunConfigService());
    }

    public TuiController(List<RecipeInfo> allRecipes, RunConfigService runConfigService) {
        this(allRecipes, runConfigService, null, null, null, null, Path.of("."));
    }

    public TuiController(
            List<RecipeInfo> allRecipes,
            RunConfigService runConfigService,
            RecipeExecutionEngine engine,
            GradleProjectScanner projectScanner,
            ProjectSourceParser sourceParser,
            ChangeApplier changeApplier,
            Path projectDir) {
        this.allRecipes = List.copyOf(allRecipes);
        this.runConfigService = runConfigService;
        this.engine = engine;
        this.projectScanner = projectScanner;
        this.sourceParser = sourceParser;
        this.changeApplier = changeApplier;
        this.projectDir = projectDir;
    }

    public Screen currentScreen() {
        return currentScreen;
    }

    public String searchQuery() {
        return searchQuery;
    }

    public SortOrder sortOrder() {
        return sortOrder;
    }

    public int highlightedIndex() {
        return highlightedIndex;
    }

    public Set<String> selectedRecipes() {
        return Set.copyOf(selectedRecipes);
    }

    public List<RecipeInfo> recipes() {
        List<RecipeInfo> filtered = filterRecipes();
        List<RecipeInfo> sorted = new ArrayList<>(filtered);
        sorted.sort(sortOrder.comparator());
        return List.copyOf(sorted);
    }

    public String tagFilter() {
        return tagFilter;
    }

    public boolean isSearchMode() {
        return searchMode;
    }

    // --- Expanded recipes (browser) ---

    public Set<String> expandedRecipes() {
        return Set.copyOf(expandedRecipes);
    }

    @Requirements({"CLI_0001.13"})
    public void expandRecipe(String recipeName) {
        expandedRecipes.add(recipeName);
    }

    @Requirements({"CLI_0001.13"})
    public void collapseRecipe(String recipeName) {
        expandedRecipes.remove(recipeName);
        clampHighlightIndex();
    }

    private void clampHighlightIndex() {
        List<DisplayRow> rows = displayRows();
        if (!rows.isEmpty() && highlightedIndex >= rows.size()) {
            highlightedIndex = rows.size() - 1;
        }
    }

    public boolean isExpanded(String recipeName) {
        return expandedRecipes.contains(recipeName);
    }

    public Optional<RecipeInfo> findRecipe(String name) {
        return allRecipes.stream().filter(r -> r.name().equals(name)).findFirst();
    }

    @Requirements({"CLI_0001.12", "CLI_0001.13"})
    public List<DisplayRow> displayRows() {
        List<DisplayRow> rows = new ArrayList<>();
        for (RecipeInfo r : recipes()) {
            rows.add(new DisplayRow(r, false, null));
            if (isExpanded(r.name()) && r.isComposite()) {
                for (RecipeInfo sub : r.recipeList()) {
                    rows.add(new DisplayRow(sub, true, r.name()));
                }
            }
        }
        return List.copyOf(rows);
    }

    public Optional<DisplayRow> highlightedDisplayRow() {
        List<DisplayRow> rows = displayRows();
        if (rows.isEmpty() || highlightedIndex >= rows.size()) {
            return Optional.empty();
        }
        return Optional.of(rows.get(highlightedIndex));
    }

    // --- Search ---

    @Requirements({"CLI_0001.3"})
    public void enterSearchMode() {
        this.searchMode = true;
    }

    public void exitSearchMode() {
        this.searchMode = false;
    }

    @Requirements({"CLI_0001.3"})
    public void setSearchQuery(String query) {
        this.searchQuery = query;
        this.highlightedIndex = 0;
    }

    @Requirements({"CLI_0001.6"})
    public void setSortOrder(SortOrder order) {
        this.sortOrder = order;
    }

    public Optional<RecipeInfo> highlightedRecipe() {
        return highlightedDisplayRow().map(DisplayRow::recipe);
    }

    // --- Navigation ---

    @Requirements({"CLI_0001.12"})
    public void moveDown() {
        List<DisplayRow> rows = displayRows();
        if (!rows.isEmpty()) {
            highlightedIndex = (highlightedIndex + 1) % rows.size();
        }
    }

    @Requirements({"CLI_0001.12"})
    public void moveUp() {
        List<DisplayRow> rows = displayRows();
        if (!rows.isEmpty()) {
            highlightedIndex = (highlightedIndex - 1 + rows.size()) % rows.size();
        }
    }

    // --- Selection ---

    @Requirements({"CLI_0001.5"})
    public void toggleSelection() {
        highlightedDisplayRow().ifPresent(row -> {
            RecipeInfo recipe = row.recipe();
            if (selectedRecipes.contains(recipe.name())) {
                selectedRecipes.remove(recipe.name());
                if (!row.isSubRecipe() && recipe.isComposite()) {
                    recipe.recipeList().forEach(sub -> selectedRecipes.remove(sub.name()));
                }
            } else {
                selectedRecipes.add(recipe.name());
                if (!row.isSubRecipe() && recipe.isComposite()) {
                    recipe.recipeList().forEach(sub -> selectedRecipes.add(sub.name()));
                }
            }
        });
    }

    @Requirements({"CLI_0001.5"})
    public void cycleSelection() {
        Set<String> visibleNames =
                displayRows().stream().map(r -> r.recipe().name()).collect(Collectors.toSet());
        boolean allSelected = visibleNames.stream().allMatch(selectedRecipes::contains);
        if (allSelected) {
            selectedRecipes.clear();
        } else {
            selectedRecipes.addAll(visibleNames);
        }
        LOG.fine(() -> "Cycle selection: " + selectedRecipes.size() + " selected");
    }

    // --- Screen navigation ---

    @Requirements({"CLI_0001.4"})
    public void openDetail() {
        currentScreen = Screen.DETAIL;
    }

    public void goBack() {
        currentScreen = Screen.BROWSER;
    }

    @Requirements({"CLI_0001.11"})
    public void openTagBrowser() {
        currentScreen = Screen.TAG_BROWSER;
    }

    @Requirements({"CLI_0001.11"})
    public List<String> allTags() {
        Set<String> tags = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        allRecipes.forEach(r -> tags.addAll(r.tags()));
        return List.copyOf(tags);
    }

    @Requirements({"CLI_0001.11"})
    public void filterByTag(String tag) {
        this.tagFilter = tag;
        this.highlightedIndex = 0;
        this.currentScreen = Screen.BROWSER;
    }

    @Requirements({"CLI_0001.11"})
    public void clearTagFilter() {
        this.tagFilter = "";
        this.highlightedIndex = 0;
    }

    public void clearFilters() {
        this.searchQuery = "";
        this.tagFilter = "";
        this.highlightedIndex = 0;
    }

    // --- Run dialog ---

    public List<String> runOrder() {
        return List.copyOf(runOrder);
    }

    public int runHighlightIndex() {
        return runHighlightIndex;
    }

    public Set<String> runExpandedRecipes() {
        return Set.copyOf(runExpandedRecipes);
    }

    @Requirements({"CLI_0001.14"})
    public void openConfirmRun() {
        // Only include top-level selected recipes in run order (not sub-recipe names).
        // Sub-recipe selection state is maintained in selectedRecipes for display/toggle.
        Set<String> topLevelNames = recipes().stream().map(RecipeInfo::name).collect(Collectors.toSet());
        runOrder = selectedRecipes.stream()
                .filter(topLevelNames::contains)
                .collect(Collectors.toCollection(ArrayList::new));
        runHighlightIndex = 0;
        runExpandedRecipes.clear();
        currentScreen = Screen.CONFIRM_RUN;
        LOG.fine(() -> "Opened run dialog with " + runOrder.size() + " recipes");
    }

    @Requirements({"CLI_0001.14"})
    public List<DisplayRow> runDisplayRows() {
        List<DisplayRow> rows = new ArrayList<>();
        for (String recipeName : runOrder) {
            Optional<RecipeInfo> info = findRecipe(recipeName);
            RecipeInfo recipe = info.orElse(new RecipeInfo(recipeName, recipeName, null, Set.of()));
            rows.add(new DisplayRow(recipe, false, null));
            if (runExpandedRecipes.contains(recipeName) && recipe.isComposite()) {
                for (RecipeInfo sub : recipe.recipeList()) {
                    rows.add(new DisplayRow(sub, true, recipeName));
                }
            }
        }
        return List.copyOf(rows);
    }

    @Requirements({"CLI_0001.14"})
    public void moveRunHighlightUp() {
        List<DisplayRow> rows = runDisplayRows();
        if (!rows.isEmpty()) {
            runHighlightIndex = (runHighlightIndex - 1 + rows.size()) % rows.size();
        }
    }

    @Requirements({"CLI_0001.14"})
    public void moveRunHighlightDown() {
        List<DisplayRow> rows = runDisplayRows();
        if (!rows.isEmpty()) {
            runHighlightIndex = (runHighlightIndex + 1) % rows.size();
        }
    }

    @Requirements({"CLI_0001.14"})
    public void moveRunRecipeUp() {
        List<DisplayRow> rows = runDisplayRows();
        if (rows.isEmpty() || runHighlightIndex >= rows.size()) {
            return;
        }
        DisplayRow row = rows.get(runHighlightIndex);
        if (row.isSubRecipe()) {
            return;
        }
        int orderIndex = runOrder.indexOf(row.recipe().name());
        if (orderIndex > 0) {
            Collections.swap(runOrder, orderIndex, orderIndex - 1);
            // Adjust highlight to follow the swapped item in display rows
            List<DisplayRow> newRows = runDisplayRows();
            for (int i = 0; i < newRows.size(); i++) {
                if (newRows.get(i).recipe().name().equals(row.recipe().name())
                        && !newRows.get(i).isSubRecipe()) {
                    runHighlightIndex = i;
                    break;
                }
            }
        }
    }

    @Requirements({"CLI_0001.14"})
    public void moveRunRecipeDown() {
        List<DisplayRow> rows = runDisplayRows();
        if (rows.isEmpty() || runHighlightIndex >= rows.size()) {
            return;
        }
        DisplayRow row = rows.get(runHighlightIndex);
        if (row.isSubRecipe()) {
            return;
        }
        int orderIndex = runOrder.indexOf(row.recipe().name());
        if (orderIndex >= 0 && orderIndex < runOrder.size() - 1) {
            Collections.swap(runOrder, orderIndex, orderIndex + 1);
            List<DisplayRow> newRows = runDisplayRows();
            for (int i = 0; i < newRows.size(); i++) {
                if (newRows.get(i).recipe().name().equals(row.recipe().name())
                        && !newRows.get(i).isSubRecipe()) {
                    runHighlightIndex = i;
                    break;
                }
            }
        }
    }

    @Requirements({"CLI_0001.14"})
    public void toggleRunRecipe() {
        List<DisplayRow> rows = runDisplayRows();
        if (!rows.isEmpty() && runHighlightIndex < rows.size()) {
            String name = rows.get(runHighlightIndex).recipe().name();
            if (!selectedRecipes.remove(name)) {
                selectedRecipes.add(name);
            }
        }
    }

    @Requirements({"CLI_0001.14"})
    public void cycleRunSelection() {
        Set<String> allRunNames =
                runDisplayRows().stream().map(r -> r.recipe().name()).collect(Collectors.toSet());
        boolean allSelected = allRunNames.stream().allMatch(selectedRecipes::contains);
        if (allSelected) {
            allRunNames.forEach(selectedRecipes::remove);
        } else {
            selectedRecipes.addAll(allRunNames);
        }
    }

    @Requirements({"CLI_0001.14"})
    public void expandRunRecipe() {
        List<DisplayRow> rows = runDisplayRows();
        if (!rows.isEmpty() && runHighlightIndex < rows.size()) {
            DisplayRow row = rows.get(runHighlightIndex);
            if (!row.isSubRecipe()) {
                runExpandedRecipes.add(row.recipe().name());
            }
        }
    }

    @Requirements({"CLI_0001.14"})
    public void collapseRunRecipe() {
        List<DisplayRow> rows = runDisplayRows();
        if (!rows.isEmpty() && runHighlightIndex < rows.size()) {
            DisplayRow row = rows.get(runHighlightIndex);
            String name = row.isSubRecipe() ? row.parentName() : row.recipe().name();
            runExpandedRecipes.remove(name);
            clampRunHighlightIndex();
        }
    }

    private void clampRunHighlightIndex() {
        List<DisplayRow> rows = runDisplayRows();
        if (!rows.isEmpty() && runHighlightIndex >= rows.size()) {
            runHighlightIndex = rows.size() - 1;
        }
    }

    @Requirements({"CLI_0001.14"})
    public void flattenRunRecipe() {
        List<DisplayRow> rows = runDisplayRows();
        if (rows.isEmpty() || runHighlightIndex >= rows.size()) {
            return;
        }
        DisplayRow row = rows.get(runHighlightIndex);
        if (row.isSubRecipe()) {
            return;
        }
        String name = row.recipe().name();
        Optional<RecipeInfo> recipe = findRecipe(name);
        if (recipe.isPresent() && recipe.get().isComposite()) {
            int orderIndex = runOrder.indexOf(name);
            runOrder.remove(orderIndex);
            List<String> subNames =
                    recipe.get().recipeList().stream().map(RecipeInfo::name).toList();
            runOrder.addAll(orderIndex, subNames);
            if (selectedRecipes.remove(name)) {
                selectedRecipes.addAll(subNames);
            }
            runExpandedRecipes.remove(name);
        }
    }

    // --- Execution ---

    public Optional<ExecutionResult> executionResult() {
        return Optional.ofNullable(executionResult);
    }

    public boolean lastRunWasDryRun() {
        return lastRunWasDryRun;
    }

    @Requirements({"CLI_0001.8"})
    public void showDryRunResult(ExecutionResult result) {
        this.executionResult = result;
        this.lastRunWasDryRun = true;
        this.currentScreen = Screen.EXECUTION_RESULTS;
    }

    @Requirements({"CLI_0001.9"})
    public void showExecutionResult(ExecutionResult result) {
        this.executionResult = result;
        this.lastRunWasDryRun = false;
        this.currentScreen = Screen.EXECUTION_RESULTS;
    }

    public Path projectDir() {
        return projectDir;
    }

    @Requirements({"CLI_0001.8", "CLI_0001.9"})
    public void runSelectedRecipes(boolean dryRun) {
        if (engine == null || sourceParser == null) {
            return;
        }
        LOG.fine(() -> "Running " + (dryRun ? "dry-run" : "execution") + " for " + runOrder.size() + " recipes");

        List<SourceFile> sources;
        if (projectScanner != null) {
            ProjectInfo projectInfo = projectScanner.scan(projectDir);
            sources = sourceParser.parse(projectInfo);
        } else {
            sources = sourceParser.parse(new ProjectInfo(List.of(), List.of(projectDir)));
        }

        // Use runOrder filtered to selected recipes, preserving user-defined execution order
        List<String> recipesToRun =
                runOrder.stream().filter(selectedRecipes::contains).toList();

        List<FileChange> allChanges = new ArrayList<>();
        for (String recipeName : recipesToRun) {
            ExecutionResult result = engine.execute(recipeName, sources);
            allChanges.addAll(result.changes());
        }
        ExecutionResult combined = new ExecutionResult(allChanges);
        if (!dryRun && changeApplier != null) {
            changeApplier.apply(projectDir, combined.changes());
        }
        if (dryRun) {
            showDryRunResult(combined);
        } else {
            showExecutionResult(combined);
        }
    }

    @Requirements({"CLI_0001.10"})
    public void saveRunConfig(Path file) throws IOException {
        RunConfig config = new RunConfig(List.copyOf(selectedRecipes));
        runConfigService.save(config, file);
    }

    private List<RecipeInfo> filterRecipes() {
        var stream = allRecipes.stream();
        if (!tagFilter.isBlank()) {
            stream = stream.filter(r -> r.tags().stream().anyMatch(t -> t.equalsIgnoreCase(tagFilter)));
        }
        if (!searchQuery.isBlank()) {
            String lowerQuery = searchQuery.toLowerCase(Locale.ROOT);
            stream = stream.filter(r -> r.name().toLowerCase(Locale.ROOT).contains(lowerQuery)
                    || r.displayName().toLowerCase(Locale.ROOT).contains(lowerQuery)
                    || (r.description() != null
                            && r.description().toLowerCase(Locale.ROOT).contains(lowerQuery))
                    || r.tags().stream()
                            .anyMatch(t -> t.toLowerCase(Locale.ROOT).contains(lowerQuery)));
        }
        return stream.toList();
    }
}
