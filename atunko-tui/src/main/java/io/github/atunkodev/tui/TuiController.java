package io.github.atunkodev.tui;

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
import io.github.atunkodev.core.recipe.SortOrder;
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

@Requirements({"atunko:CLI_0001"})
public class TuiController {

    public record DisplayRow(RecipeInfo recipe, boolean isSubRecipe, String parentName, int depth) {}

    public static final class RecipeListState {

        @FunctionalInterface
        public interface RecipeSource {
            List<RecipeInfo> topLevelRecipes();
        }

        private final RecipeSource source;
        private final Set<String> selectedRecipes;
        private final Set<String> expandedRecipes = new LinkedHashSet<>();
        private int highlightedIndex;

        public RecipeListState(RecipeSource source, Set<String> selectedRecipes) {
            this.source = source;
            this.selectedRecipes = selectedRecipes;
        }

        public List<DisplayRow> displayRows() {
            List<DisplayRow> rows = new ArrayList<>();
            for (RecipeInfo r : source.topLevelRecipes()) {
                rows.add(new DisplayRow(r, false, null, 0));
                if (expandedRecipes.contains(r.name()) && r.isComposite()) {
                    addSubRows(rows, r, 1);
                }
            }
            return List.copyOf(rows);
        }

        private void addSubRows(List<DisplayRow> rows, RecipeInfo parent, int depth) {
            for (RecipeInfo sub : parent.recipeList()) {
                rows.add(new DisplayRow(sub, true, parent.name(), depth));
                if (expandedRecipes.contains(sub.name()) && sub.isComposite()) {
                    addSubRows(rows, sub, depth + 1);
                }
            }
        }

        public void moveDown() {
            List<DisplayRow> rows = displayRows();
            if (!rows.isEmpty()) {
                highlightedIndex = (highlightedIndex + 1) % rows.size();
            }
        }

        public void moveUp() {
            List<DisplayRow> rows = displayRows();
            if (!rows.isEmpty()) {
                highlightedIndex = (highlightedIndex - 1 + rows.size()) % rows.size();
            }
        }

        public void toggleSelection() {
            highlightedRow().ifPresent(row -> {
                String name = row.recipe().name();
                if (!selectedRecipes.remove(name)) {
                    selectedRecipes.add(name);
                }
            });
        }

        public void cycleSelection(boolean clearAllOnDeselect) {
            Set<String> visibleNames =
                    displayRows().stream().map(r -> r.recipe().name()).collect(Collectors.toSet());
            boolean allSelected = visibleNames.stream().allMatch(selectedRecipes::contains);
            if (allSelected) {
                if (clearAllOnDeselect) {
                    selectedRecipes.clear();
                } else {
                    visibleNames.forEach(selectedRecipes::remove);
                }
            } else {
                selectedRecipes.addAll(visibleNames);
            }
        }

        public void expand(String recipeName) {
            expandedRecipes.add(recipeName);
        }

        public void collapse(String recipeName) {
            expandedRecipes.remove(recipeName);
            clampHighlightIndex();
        }

        public boolean isExpanded(String recipeName) {
            return expandedRecipes.contains(recipeName);
        }

        public Set<String> expandedRecipes() {
            return Set.copyOf(expandedRecipes);
        }

        public void expandHighlighted() {
            highlightedRow().ifPresent(row -> {
                if (row.recipe().isComposite()) {
                    expandedRecipes.add(row.recipe().name());
                }
            });
        }

        public void collapseHighlighted() {
            highlightedRow().ifPresent(row -> {
                if (expandedRecipes.contains(row.recipe().name())) {
                    expandedRecipes.remove(row.recipe().name());
                } else if (row.isSubRecipe() && row.parentName() != null) {
                    expandedRecipes.remove(row.parentName());
                }
                clampHighlightIndex();
            });
        }

        public int highlightedIndex() {
            return highlightedIndex;
        }

        public Optional<DisplayRow> highlightedRow() {
            List<DisplayRow> rows = displayRows();
            if (rows.isEmpty() || highlightedIndex >= rows.size()) {
                return Optional.empty();
            }
            return Optional.of(rows.get(highlightedIndex));
        }

        public Optional<RecipeInfo> highlightedRecipe() {
            return highlightedRow().map(DisplayRow::recipe);
        }

        public void resetHighlight() {
            highlightedIndex = 0;
        }

        public void setHighlightedIndex(int index) {
            highlightedIndex = index;
        }

        public void clearExpanded() {
            expandedRecipes.clear();
        }

        private void clampHighlightIndex() {
            List<DisplayRow> rows = displayRows();
            if (!rows.isEmpty() && highlightedIndex >= rows.size()) {
                highlightedIndex = rows.size() - 1;
            }
        }
    }

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
    private final Set<String> selectedRecipes = new LinkedHashSet<>();
    private final Set<String> selectedTags = new LinkedHashSet<>();
    private boolean searchMode;
    private ExecutionResult executionResult;
    private boolean lastRunWasDryRun;

    private final RecipeListState browserState;

    // Run dialog state
    private List<String> runOrder = new ArrayList<>();
    private RecipeListState runState;

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
        this.browserState = new RecipeListState(this::recipes, selectedRecipes);
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
        return browserState.highlightedIndex();
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

    public Set<String> selectedTags() {
        return Set.copyOf(selectedTags);
    }

    public boolean isSearchMode() {
        return searchMode;
    }

    // --- Browser list state (delegated to browserState) ---

    public Set<String> expandedRecipes() {
        return browserState.expandedRecipes();
    }

    @Requirements({"atunko:CLI_0001.13"})
    public void expandRecipe(String recipeName) {
        browserState.expand(recipeName);
    }

    @Requirements({"atunko:CLI_0001.13"})
    public void collapseRecipe(String recipeName) {
        browserState.collapse(recipeName);
    }

    public boolean isExpanded(String recipeName) {
        return browserState.isExpanded(recipeName);
    }

    public Optional<RecipeInfo> findRecipe(String name) {
        return allRecipes.stream().filter(r -> r.name().equals(name)).findFirst();
    }

    @Requirements({"atunko:CLI_0001.12", "atunko:CLI_0001.13"})
    public List<DisplayRow> displayRows() {
        return browserState.displayRows();
    }

    public Optional<DisplayRow> highlightedDisplayRow() {
        return browserState.highlightedRow();
    }

    // --- Search ---

    @Requirements({"atunko:CLI_0001.3"})
    public void enterSearchMode() {
        this.searchMode = true;
    }

    public void exitSearchMode() {
        this.searchMode = false;
    }

    @Requirements({"atunko:CLI_0001.3"})
    public void setSearchQuery(String query) {
        this.searchQuery = query;
        browserState.resetHighlight();
    }

    @Requirements({"atunko:CLI_0001.6"})
    public void setSortOrder(SortOrder order) {
        this.sortOrder = order;
    }

    public Optional<RecipeInfo> highlightedRecipe() {
        return browserState.highlightedRecipe();
    }

    // --- Navigation ---

    @Requirements({"atunko:CLI_0001.12"})
    public void moveDown() {
        browserState.moveDown();
    }

    @Requirements({"atunko:CLI_0001.12"})
    public void moveUp() {
        browserState.moveUp();
    }

    // --- Selection ---

    @Requirements({"atunko:CLI_0001.5"})
    public void toggleSelection() {
        browserState.toggleSelection();
    }

    @Requirements({"atunko:CLI_0001.5"})
    public void cycleSelection() {
        browserState.cycleSelection(true);
        LOG.fine(() -> "Cycle selection: " + selectedRecipes.size() + " selected");
    }

    @Requirements({"atunko:CLI_0001.13"})
    public void collapseHighlighted() {
        browserState.collapseHighlighted();
    }

    // --- Screen navigation ---

    @Requirements({"atunko:CLI_0001.4"})
    public void openDetail() {
        currentScreen = Screen.DETAIL;
    }

    public void goBack() {
        currentScreen = Screen.BROWSER;
    }

    @Requirements({"atunko:CLI_0001.11"})
    public void openTagBrowser() {
        currentScreen = Screen.TAG_BROWSER;
    }

    @Requirements({"atunko:CLI_0001.11"})
    public List<String> allTags() {
        Set<String> tags = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        allRecipes.forEach(r -> tags.addAll(r.tags()));
        return List.copyOf(tags);
    }

    @Requirements({"atunko:CLI_0001.11"})
    public void toggleTag(String tag) {
        if (!selectedTags.remove(tag)) {
            selectedTags.add(tag);
        }
    }

    @Requirements({"atunko:CLI_0001.11"})
    public void applyTagFilter() {
        browserState.resetHighlight();
        this.currentScreen = Screen.BROWSER;
    }

    @Requirements({"atunko:CLI_0001.11"})
    public void clearTagFilter() {
        this.selectedTags.clear();
        browserState.resetHighlight();
    }

    public void clearAll() {
        this.searchQuery = "";
        this.selectedTags.clear();
        this.selectedRecipes.clear();
        browserState.resetHighlight();
    }

    // --- Run dialog ---

    public List<String> runOrder() {
        return List.copyOf(runOrder);
    }

    public int runHighlightIndex() {
        return runState != null ? runState.highlightedIndex() : 0;
    }

    public Set<String> runExpandedRecipes() {
        return runState != null ? runState.expandedRecipes() : Set.of();
    }

    @Requirements({"atunko:CLI_0001.14"})
    public void openConfirmRun() {
        runOrder = new ArrayList<>(selectedRecipes);
        runState = new RecipeListState(this::resolveRunRecipes, selectedRecipes);
        currentScreen = Screen.CONFIRM_RUN;
        LOG.fine(() -> "Opened run dialog with " + runOrder.size() + " recipes");
    }

    private List<RecipeInfo> resolveRunRecipes() {
        List<RecipeInfo> result = new ArrayList<>();
        for (String name : runOrder) {
            result.add(findRecipe(name).orElse(new RecipeInfo(name, name, null, Set.of())));
        }
        return result;
    }

    @Requirements({"atunko:CLI_0001.14"})
    public List<DisplayRow> runDisplayRows() {
        return runState != null ? runState.displayRows() : List.of();
    }

    @Requirements({"atunko:CLI_0001.14"})
    public void moveRunHighlightUp() {
        if (runState != null) {
            runState.moveUp();
        }
    }

    @Requirements({"atunko:CLI_0001.14"})
    public void moveRunHighlightDown() {
        if (runState != null) {
            runState.moveDown();
        }
    }

    @Requirements({"atunko:CLI_0001.14"})
    public void moveRunRecipeUp() {
        if (runState == null) {
            return;
        }
        List<DisplayRow> rows = runState.displayRows();
        int highlightIdx = runState.highlightedIndex();
        if (rows.isEmpty() || highlightIdx >= rows.size()) {
            return;
        }
        DisplayRow row = rows.get(highlightIdx);
        if (row.isSubRecipe()) {
            return;
        }
        int orderIndex = runOrder.indexOf(row.recipe().name());
        if (orderIndex > 0) {
            Collections.swap(runOrder, orderIndex, orderIndex - 1);
            List<DisplayRow> newRows = runState.displayRows();
            for (int i = 0; i < newRows.size(); i++) {
                if (newRows.get(i).recipe().name().equals(row.recipe().name())
                        && !newRows.get(i).isSubRecipe()) {
                    runState.setHighlightedIndex(i);
                    break;
                }
            }
        }
    }

    @Requirements({"atunko:CLI_0001.14"})
    public void moveRunRecipeDown() {
        if (runState == null) {
            return;
        }
        List<DisplayRow> rows = runState.displayRows();
        int highlightIdx = runState.highlightedIndex();
        if (rows.isEmpty() || highlightIdx >= rows.size()) {
            return;
        }
        DisplayRow row = rows.get(highlightIdx);
        if (row.isSubRecipe()) {
            return;
        }
        int orderIndex = runOrder.indexOf(row.recipe().name());
        if (orderIndex >= 0 && orderIndex < runOrder.size() - 1) {
            Collections.swap(runOrder, orderIndex, orderIndex + 1);
            List<DisplayRow> newRows = runState.displayRows();
            for (int i = 0; i < newRows.size(); i++) {
                if (newRows.get(i).recipe().name().equals(row.recipe().name())
                        && !newRows.get(i).isSubRecipe()) {
                    runState.setHighlightedIndex(i);
                    break;
                }
            }
        }
    }

    @Requirements({"atunko:CLI_0001.14"})
    public void toggleRunRecipe() {
        if (runState != null) {
            runState.toggleSelection();
        }
    }

    @Requirements({"atunko:CLI_0001.14"})
    public void cycleRunSelection() {
        if (runState != null) {
            runState.cycleSelection(false);
        }
    }

    @Requirements({"atunko:CLI_0001.14"})
    public void expandRunRecipe() {
        if (runState != null) {
            runState.expandHighlighted();
        }
    }

    @Requirements({"atunko:CLI_0001.14"})
    public void collapseRunRecipe() {
        if (runState != null) {
            runState.collapseHighlighted();
        }
    }

    @Requirements({"atunko:CLI_0001.14"})
    public void flattenRunRecipe() {
        if (runState == null) {
            return;
        }
        List<DisplayRow> rows = runState.displayRows();
        int highlightIdx = runState.highlightedIndex();
        if (rows.isEmpty() || highlightIdx >= rows.size()) {
            return;
        }
        DisplayRow row = rows.get(highlightIdx);
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
            runState.collapse(name);
        }
    }

    // --- Execution ---

    public Optional<ExecutionResult> executionResult() {
        return Optional.ofNullable(executionResult);
    }

    public boolean lastRunWasDryRun() {
        return lastRunWasDryRun;
    }

    @Requirements({"atunko:CLI_0001.8"})
    public void showDryRunResult(ExecutionResult result) {
        this.executionResult = result;
        this.lastRunWasDryRun = true;
        this.currentScreen = Screen.EXECUTION_RESULTS;
    }

    @Requirements({"atunko:CLI_0001.9"})
    public void showExecutionResult(ExecutionResult result) {
        this.executionResult = result;
        this.lastRunWasDryRun = false;
        this.currentScreen = Screen.EXECUTION_RESULTS;
    }

    public Path projectDir() {
        return projectDir;
    }

    @Requirements({"atunko:CLI_0001.8", "atunko:CLI_0001.9"})
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

    @Requirements({"atunko:CLI_0001.10"})
    public void saveRunConfig(Path file) throws IOException {
        RunConfig config = new RunConfig(List.copyOf(selectedRecipes));
        runConfigService.save(config, file);
    }

    private List<RecipeInfo> filterRecipes() {
        var stream = allRecipes.stream();
        if (!selectedTags.isEmpty()) {
            stream = stream.filter(r ->
                    r.tags().stream().anyMatch(t -> selectedTags.stream().anyMatch(st -> st.equalsIgnoreCase(t))));
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
