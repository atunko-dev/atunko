package io.github.atunkodev.tui;

import dev.tamboui.widgets.input.TextInputState;
import io.github.atunkodev.core.config.ConfigExportService;
import io.github.atunkodev.core.config.RecipeEntry;
import io.github.atunkodev.core.config.RunConfig;
import io.github.atunkodev.core.config.RunConfigService;
import io.github.atunkodev.core.engine.ChangeApplier;
import io.github.atunkodev.core.engine.ExecutionResult;
import io.github.atunkodev.core.engine.FileChange;
import io.github.atunkodev.core.engine.RecipeExecutionEngine;
import io.github.atunkodev.core.engine.WorkspaceExecutionEngine;
import io.github.atunkodev.core.engine.WorkspaceExecutionResult;
import io.github.atunkodev.core.project.ProjectEntry;
import io.github.atunkodev.core.project.ProjectInfo;
import io.github.atunkodev.core.project.ProjectSourceParser;
import io.github.atunkodev.core.project.SessionHolder;
import io.github.atunkodev.core.project.Workspace;
import io.github.atunkodev.core.recipe.RecipeCoverageUtils;
import io.github.atunkodev.core.recipe.RecipeInfo;
import io.github.atunkodev.core.recipe.SortOrder;
import io.github.reqstool.annotations.Requirements;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.openrewrite.SourceFile;

@Requirements({"atunko:TUI_0001"})
public class TuiController {

    public record DisplayRow(RecipeInfo recipe, boolean isSubRecipe, String parentName, int depth, String path) {}

    public static final class RecipeListState {

        @FunctionalInterface
        public interface RecipeSource {
            List<RecipeInfo> topLevelRecipes();
        }

        private final RecipeSource source;
        private final Set<String> selectedRecipes;
        private final Set<String> expandedRecipes = new LinkedHashSet<>();
        private final Set<String> partialRecipes = new LinkedHashSet<>();
        private final boolean cascade;
        private int highlightedIndex;

        public RecipeListState(RecipeSource source, Set<String> selectedRecipes) {
            this(source, selectedRecipes, true);
        }

        public RecipeListState(RecipeSource source, Set<String> selectedRecipes, boolean cascade) {
            this.source = source;
            this.selectedRecipes = selectedRecipes;
            this.cascade = cascade;
        }

        public List<DisplayRow> displayRows() {
            List<DisplayRow> rows = new ArrayList<>();
            for (RecipeInfo r : source.topLevelRecipes()) {
                String path = r.name();
                rows.add(new DisplayRow(r, false, null, 0, path));
                if (expandedRecipes.contains(r.name()) && r.isComposite()) {
                    addSubRows(rows, r, 1, path);
                }
            }
            return List.copyOf(rows);
        }

        private void addSubRows(List<DisplayRow> rows, RecipeInfo parent, int depth, String parentPath) {
            for (RecipeInfo sub : parent.recipeList()) {
                String path = parentPath + "/" + sub.name();
                rows.add(new DisplayRow(sub, true, parent.name(), depth, path));
                if (expandedRecipes.contains(sub.name()) && sub.isComposite()) {
                    addSubRows(rows, sub, depth + 1, path);
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
                RecipeInfo recipe = row.recipe();
                String name = recipe.name();
                if (selectedRecipes.contains(name)) {
                    selectedRecipes.remove(name);
                    if (cascade && recipe.isComposite()) {
                        // Cascade-down deselect: also remove any sub-recipes that were
                        // explicitly selected (handles cascade-up scenario where all subs
                        // were individually selected and then composite is deselected)
                        Set<String> subs = new LinkedHashSet<>();
                        collectAllSubRecipeNames(recipe, subs);
                        selectedRecipes.removeAll(subs);
                    }
                } else {
                    selectedRecipes.add(name);
                    if (cascade && recipe.isComposite()) {
                        // Cascade-down select: add all transitive sub-recipes so that
                        // recomputePartialState can correctly compute composite state
                        Set<String> subs = new LinkedHashSet<>();
                        collectAllSubRecipeNames(recipe, subs);
                        selectedRecipes.addAll(subs);
                    }
                }
                if (cascade) {
                    recomputePartialState();
                }
            });
        }

        public void selectAll() {
            Set<String> visibleNames =
                    displayRows().stream().map(r -> r.recipe().name()).collect(Collectors.toSet());
            selectedRecipes.addAll(visibleNames);
            if (cascade) {
                recomputePartialState();
            }
        }

        public void deselectAll() {
            selectedRecipes.clear();
            if (cascade) {
                recomputePartialState();
            }
        }

        public void expandAll(List<RecipeInfo> recipes) {
            for (RecipeInfo r : recipes) {
                if (r.isComposite()) {
                    expandedRecipes.add(r.name());
                    expandAll(r.recipeList());
                }
            }
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
            if (cascade) {
                recomputePartialState();
            }
        }

        private static void collectAllSubRecipeNames(RecipeInfo composite, Set<String> result) {
            for (RecipeInfo sub : composite.recipeList()) {
                result.add(sub.name());
                if (sub.isComposite()) {
                    collectAllSubRecipeNames(sub, result);
                }
            }
        }

        private void recomputePartialState() {
            partialRecipes.clear();
            for (RecipeInfo r : source.topLevelRecipes()) {
                if (r.isComposite()) {
                    recomputeForComposite(r);
                }
            }
        }

        private void recomputeForComposite(RecipeInfo composite) {
            for (RecipeInfo sub : composite.recipeList()) {
                if (sub.isComposite()) {
                    recomputeForComposite(sub);
                }
            }
            Set<String> allSubs = new LinkedHashSet<>();
            collectAllSubRecipeNames(composite, allSubs);
            if (allSubs.isEmpty()) {
                return;
            }
            long selectedCount =
                    allSubs.stream().filter(selectedRecipes::contains).count();
            if (selectedCount == allSubs.size()) {
                // All sub-recipes selected → ensure composite is selected (cascade-up)
                selectedRecipes.add(composite.name());
                partialRecipes.remove(composite.name());
            } else if (selectedCount > 0) {
                // Some but not all sub-recipes selected → partial/indeterminate
                selectedRecipes.remove(composite.name());
                partialRecipes.add(composite.name());
            } else {
                // No sub-recipes selected
                selectedRecipes.remove(composite.name());
                partialRecipes.remove(composite.name());
            }
        }

        public Set<String> partialRecipes() {
            return Set.copyOf(partialRecipes);
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
    private final ProjectSourceParser sourceParser;
    private final ChangeApplier changeApplier;
    private final WorkspaceExecutionEngine workspaceEngine;
    private final Path projectDir;
    private final List<ProjectEntry> workspaceProjects;
    private Screen currentScreen = Screen.BROWSER;
    private String searchQuery = "";
    private SortOrder sortOrder = SortOrder.NAME;
    private final Set<String> selectedRecipes = new LinkedHashSet<>();
    private final Set<String> selectedTags = new LinkedHashSet<>();
    private boolean searchMode;
    private boolean showHelp;
    private ExecutionResult executionResult;
    private WorkspaceExecutionResult workspaceResult;
    private boolean lastRunWasDryRun;
    private int selectedFileIndex = 0;

    private final RecipeListState browserState;

    // Run dialog state
    private List<String> runOrder = new ArrayList<>();
    private RecipeListState runState;

    public TuiController(List<RecipeInfo> allRecipes) {
        this(allRecipes, new RunConfigService());
    }

    public TuiController(List<RecipeInfo> allRecipes, RunConfigService runConfigService) {
        this(allRecipes, runConfigService, null, null, null, Path.of("."));
    }

    public TuiController(
            List<RecipeInfo> allRecipes,
            RunConfigService runConfigService,
            RecipeExecutionEngine engine,
            ProjectSourceParser sourceParser,
            ChangeApplier changeApplier,
            Path projectDir) {
        this(allRecipes, runConfigService, engine, sourceParser, changeApplier, null, null, projectDir);
    }

    public TuiController(
            List<RecipeInfo> allRecipes,
            RunConfigService runConfigService,
            RecipeExecutionEngine engine,
            ProjectSourceParser sourceParser,
            ChangeApplier changeApplier,
            WorkspaceExecutionEngine workspaceEngine,
            Path projectDir) {
        this(
                allRecipes,
                runConfigService,
                engine,
                sourceParser,
                changeApplier,
                workspaceEngine,
                workspaceEngine != null ? SessionHolder.getProjectEntries() : null,
                projectDir);
    }

    public TuiController(
            List<RecipeInfo> allRecipes,
            RunConfigService runConfigService,
            RecipeExecutionEngine engine,
            ProjectSourceParser sourceParser,
            ChangeApplier changeApplier,
            WorkspaceExecutionEngine workspaceEngine,
            List<ProjectEntry> workspaceProjects,
            Path projectDir) {
        this.allRecipes = List.copyOf(allRecipes);
        this.runConfigService = runConfigService;
        this.engine = engine;
        this.sourceParser = sourceParser;
        this.changeApplier = changeApplier;
        this.workspaceEngine = workspaceEngine;
        this.workspaceProjects = workspaceProjects != null ? List.copyOf(workspaceProjects) : null;
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

    public boolean isShowHelp() {
        return showHelp;
    }

    public void toggleHelp() {
        this.showHelp = !this.showHelp;
    }

    // --- Browser list state (delegated to browserState) ---

    public Set<String> expandedRecipes() {
        return browserState.expandedRecipes();
    }

    @Requirements({"atunko:TUI_0001.13"})
    public void expandRecipe(String recipeName) {
        browserState.expand(recipeName);
    }

    @Requirements({"atunko:TUI_0001.13"})
    public void collapseRecipe(String recipeName) {
        browserState.collapse(recipeName);
    }

    public boolean isExpanded(String recipeName) {
        return browserState.isExpanded(recipeName);
    }

    public Optional<RecipeInfo> findRecipe(String name) {
        return allRecipes.stream().filter(r -> r.name().equals(name)).findFirst();
    }

    private Optional<RecipeInfo> findRecipeDeep(String name) {
        return findRecipeInSubtree(name, allRecipes);
    }

    private static Optional<RecipeInfo> findRecipeInSubtree(String name, List<RecipeInfo> recipes) {
        for (RecipeInfo r : recipes) {
            if (r.name().equals(name)) {
                return Optional.of(r);
            }
            if (r.isComposite()) {
                Optional<RecipeInfo> found = findRecipeInSubtree(name, r.recipeList());
                if (found.isPresent()) {
                    return found;
                }
            }
        }
        return Optional.empty();
    }

    @Requirements({"atunko:TUI_0001.16"})
    public Set<String> coveredRecipes() {
        Set<RecipeInfo> selectedInfos = selectedRecipes.stream()
                .flatMap(name -> findRecipe(name).stream())
                .collect(Collectors.toSet());
        return RecipeCoverageUtils.computeCovered(selectedInfos).stream()
                .map(RecipeInfo::name)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Requirements({"atunko:TUI_0001.16"})
    public List<String> includedIn(String recipeName) {
        Map<RecipeInfo, List<RecipeInfo>> index = RecipeCoverageUtils.buildReverseIndex(allRecipes);
        return findRecipeDeep(recipeName)
                .map(target -> index.getOrDefault(target, List.of()).stream()
                        .filter(p -> selectedRecipes.contains(p.name()))
                        .map(RecipeInfo::displayName)
                        .collect(Collectors.toUnmodifiableList()))
                .orElseGet(List::of);
    }

    @Requirements({"atunko:TUI_0001.12", "atunko:TUI_0001.13"})
    public List<DisplayRow> displayRows() {
        return browserState.displayRows();
    }

    public Optional<DisplayRow> highlightedDisplayRow() {
        return browserState.highlightedRow();
    }

    // --- Search ---

    @Requirements({"atunko:TUI_0001.3"})
    public void enterSearchMode() {
        this.searchMode = true;
    }

    public void exitSearchMode() {
        this.searchMode = false;
    }

    @Requirements({"atunko:TUI_0001.3"})
    public void setSearchQuery(String query) {
        this.searchQuery = query;
        browserState.resetHighlight();
    }

    @Requirements({"atunko:TUI_0001.6"})
    public void setSortOrder(SortOrder order) {
        this.sortOrder = order;
    }

    public Optional<RecipeInfo> highlightedRecipe() {
        return browserState.highlightedRecipe();
    }

    // --- Navigation ---

    @Requirements({"atunko:TUI_0001.12"})
    public void moveDown() {
        browserState.moveDown();
    }

    @Requirements({"atunko:TUI_0001.12"})
    public void moveUp() {
        browserState.moveUp();
    }

    // --- Selection ---

    @Requirements({"atunko:TUI_0001.5", "atunko:TUI_0001.17"})
    public void toggleSelection() {
        browserState.toggleSelection();
    }

    @Requirements({"atunko:TUI_0001.17"})
    public Set<String> partialRecipes() {
        return browserState.partialRecipes();
    }

    @Requirements({"atunko:TUI_0001.5"})
    public void cycleSelection() {
        browserState.cycleSelection(true);
        LOG.fine(() -> "Cycle selection: " + selectedRecipes.size() + " selected");
    }

    @Requirements({"atunko:TUI_0001.5"})
    public void selectAll() {
        browserState.selectAll();
    }

    @Requirements({"atunko:TUI_0001.5"})
    public void deselectAll() {
        browserState.deselectAll();
    }

    public void expandAll() {
        browserState.expandAll(recipes());
    }

    public void collapseAll() {
        browserState.clearExpanded();
    }

    public void nextSearchMatch() {
        int size = displayRows().size();
        if (size == 0) {
            return;
        }
        browserState.setHighlightedIndex((browserState.highlightedIndex() + 1) % size);
    }

    public void prevSearchMatch() {
        int size = displayRows().size();
        if (size == 0) {
            return;
        }
        browserState.setHighlightedIndex((browserState.highlightedIndex() - 1 + size) % size);
    }

    @Requirements({"atunko:TUI_0001.13"})
    public void collapseHighlighted() {
        browserState.collapseHighlighted();
    }

    // --- Screen navigation ---

    @Requirements({"atunko:TUI_0001.4"})
    public void openDetail() {
        currentScreen = Screen.DETAIL;
    }

    public void goBack() {
        currentScreen = Screen.BROWSER;
    }

    @Requirements({"atunko:TUI_0001.11"})
    public void openTagBrowser() {
        currentScreen = Screen.TAG_BROWSER;
    }

    @Requirements({"atunko:TUI_0001.11"})
    public List<String> allTags() {
        Set<String> tags = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        allRecipes.forEach(r -> tags.addAll(r.tags()));
        return List.copyOf(tags);
    }

    @Requirements({"atunko:TUI_0001.11"})
    public void toggleTag(String tag) {
        if (!selectedTags.remove(tag)) {
            selectedTags.add(tag);
        }
    }

    @Requirements({"atunko:TUI_0001.11"})
    public void applyTagFilter() {
        browserState.resetHighlight();
        this.currentScreen = Screen.BROWSER;
    }

    @Requirements({"atunko:TUI_0001.11"})
    public void clearTagFilter() {
        this.selectedTags.clear();
        browserState.resetHighlight();
    }

    public void clearAll() {
        this.searchQuery = "";
        this.selectedTags.clear();
        this.selectedRecipes.clear();
        this.recipeOptions.clear();
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

    @Requirements({"atunko:TUI_0001.14"})
    public void openConfirmRun() {
        // Deduplicate: omit sub-recipes whose parent composite is also selected,
        // since running the composite already executes its sub-recipes.
        Set<String> coveredBySelectedComposites = coveredRecipes();
        runOrder = selectedRecipes.stream()
                .filter(name -> !coveredBySelectedComposites.contains(name))
                .collect(Collectors.toCollection(ArrayList::new));
        runState = new RecipeListState(this::resolveRunRecipes, selectedRecipes, false);
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

    @Requirements({"atunko:TUI_0001.14"})
    public List<DisplayRow> runDisplayRows() {
        return runState != null ? runState.displayRows() : List.of();
    }

    @Requirements({"atunko:TUI_0001.14"})
    public void moveRunHighlightUp() {
        if (runState != null) {
            runState.moveUp();
        }
    }

    @Requirements({"atunko:TUI_0001.14"})
    public void moveRunHighlightDown() {
        if (runState != null) {
            runState.moveDown();
        }
    }

    @Requirements({"atunko:TUI_0001.14"})
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

    @Requirements({"atunko:TUI_0001.14"})
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

    @Requirements({"atunko:TUI_0001.14"})
    public void toggleRunRecipe() {
        if (runState != null) {
            runState.toggleSelection();
        }
    }

    @Requirements({"atunko:TUI_0001.14"})
    public void cycleRunSelection() {
        if (runState != null) {
            runState.cycleSelection(false);
        }
    }

    @Requirements({"atunko:TUI_0001.14"})
    public void selectAllRun() {
        if (runState != null) {
            runState.selectAll();
        }
    }

    @Requirements({"atunko:TUI_0001.14"})
    public void deselectAllRun() {
        if (runState != null) {
            runState.deselectAll();
        }
    }

    @Requirements({"atunko:TUI_0001.14"})
    public void expandRunRecipe() {
        if (runState != null) {
            runState.expandHighlighted();
        }
    }

    @Requirements({"atunko:TUI_0001.14"})
    public void collapseRunRecipe() {
        if (runState != null) {
            runState.collapseHighlighted();
        }
    }

    @Requirements({"atunko:TUI_0001.14.1"})
    public void flattenAllRunRecipes() {
        if (runState == null) {
            return;
        }
        boolean anyChanged = false;
        boolean changed = true;
        while (changed) {
            changed = false;
            LinkedHashSet<String> next = new LinkedHashSet<>();
            for (String name : runOrder) {
                Optional<RecipeInfo> r = findRecipeDeep(name);
                if (r.isPresent() && r.get().isComposite()) {
                    List<String> subs =
                            r.get().recipeList().stream().map(RecipeInfo::name).toList();
                    next.addAll(subs);
                    if (selectedRecipes.remove(name)) {
                        selectedRecipes.addAll(subs);
                    }
                    changed = true;
                    anyChanged = true;
                } else {
                    next.add(name);
                }
            }
            runOrder = new ArrayList<>(next);
        }
        if (anyChanged) {
            runState = new RecipeListState(this::resolveRunRecipes, selectedRecipes, false);
        }
    }

    @Requirements({"atunko:TUI_0001.14"})
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

    @Requirements({"atunko:TUI_0002.4"})
    public WorkspaceExecutionResult lastWorkspaceResult() {
        return workspaceResult;
    }

    public boolean lastRunWasDryRun() {
        return lastRunWasDryRun;
    }

    @Requirements({"atunko:TUI_0001.19", "atunko:TUI_0001.20"})
    public int selectedFileIndex() {
        return selectedFileIndex;
    }

    @Requirements({"atunko:TUI_0001.19"})
    public void moveFileDown() {
        int size = executionResult == null ? 0 : executionResult.changes().size();
        if (size > 0) {
            selectedFileIndex = Math.min(selectedFileIndex + 1, size - 1);
        }
    }

    @Requirements({"atunko:TUI_0001.19"})
    public void moveFileUp() {
        selectedFileIndex = Math.max(0, selectedFileIndex - 1);
    }

    @Requirements({"atunko:TUI_0001.20"})
    public void openFileDiff() {
        if (executionResult != null && !executionResult.changes().isEmpty()) {
            currentScreen = Screen.FILE_DIFF;
        }
    }

    @Requirements({"atunko:TUI_0001.20"})
    public void returnFromFileDiff() {
        currentScreen = Screen.EXECUTION_RESULTS;
    }

    @Requirements({"atunko:TUI_0002"})
    public boolean isWorkspaceMode() {
        return workspaceEngine != null;
    }

    @Requirements({"atunko:TUI_0002.2"})
    public List<ProjectEntry> workspaceProjects() {
        return workspaceProjects != null ? workspaceProjects : List.of();
    }

    @Requirements({"atunko:TUI_0002.5"})
    public Path runsDir() {
        return projectDir.resolve("atunko/runs");
    }

    @Requirements({"atunko:TUI_0001.8"})
    public void showDryRunResult(ExecutionResult result) {
        this.executionResult = result;
        this.lastRunWasDryRun = true;
        this.selectedFileIndex = 0;
        this.currentScreen = Screen.EXECUTION_RESULTS;
    }

    @Requirements({"atunko:TUI_0001.9"})
    public void showExecutionResult(ExecutionResult result) {
        this.executionResult = result;
        this.lastRunWasDryRun = false;
        this.selectedFileIndex = 0;
        this.currentScreen = Screen.EXECUTION_RESULTS;
    }

    public Path projectDir() {
        return projectDir;
    }

    @Requirements({"atunko:TUI_0001.8", "atunko:TUI_0001.9", "atunko:TUI_0002.3"})
    public void runSelectedRecipes(boolean dryRun) {
        LOG.fine(() -> "Running " + (dryRun ? "dry-run" : "execution") + " for " + runOrder.size() + " recipes");

        List<String> recipesToRun =
                runOrder.stream().filter(selectedRecipes::contains).toList();

        if (isWorkspaceMode()) {
            Workspace workspace = new Workspace(projectDir, workspaceProjects);
            WorkspaceExecutionResult wsResult = workspaceEngine.execute(recipesToRun, workspace);
            if (!dryRun && changeApplier != null) {
                wsResult.results().stream()
                        .filter(r -> r.succeeded() && r.result() != null)
                        .forEach(r -> changeApplier.apply(
                                r.entry().projectDir(), r.result().changes()));
            }
            this.workspaceResult = wsResult;
            this.lastRunWasDryRun = dryRun;
            this.currentScreen = Screen.WORKSPACE_RESULTS;
            return;
        }

        if (engine == null || sourceParser == null) {
            return;
        }

        List<SourceFile> sources;
        ProjectInfo projectInfo = SessionHolder.getProjectInfo();
        if (projectInfo != null) {
            sources = sourceParser.parse(projectInfo);
        } else {
            sources = sourceParser.parse(new ProjectInfo(List.of(), List.of(projectDir)));
        }

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

    @Requirements({"atunko:TUI_0001.10"})
    public void saveRunConfig(Path file) throws IOException {
        List<RecipeEntry> entries =
                selectedRecipes.stream().map(RecipeEntry::new).toList();
        RunConfig config = new RunConfig(entries);
        runConfigService.save(config, file);
    }

    @Requirements({"atunko:TUI_0001.19"})
    public void loadRunConfig(RunConfig config) {
        selectedRecipes.clear();
        config.recipes().stream().map(RecipeEntry::name).forEach(selectedRecipes::add);
        browserState.resetHighlight();
    }

    @Requirements({"atunko:TUI_0001.19", "atunko:TUI_0002.5"})
    public List<Path> listRunConfigs() {
        Path dir = runsDir();
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (var stream = Files.list(dir)) {
            return stream.filter(p -> p.toString().endsWith(".yml")).sorted().toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    private int loadConfigHighlightIndex = 0;
    private List<Path> configFiles = List.of();

    public void openLoadConfig() {
        configFiles = listRunConfigs();
        loadConfigHighlightIndex = 0;
        currentScreen = Screen.LOAD_CONFIG;
    }

    public List<Path> configFiles() {
        return configFiles;
    }

    public int loadConfigHighlightIndex() {
        return loadConfigHighlightIndex;
    }

    public void moveLoadConfigDown() {
        if (!configFiles.isEmpty()) {
            loadConfigHighlightIndex = Math.min(loadConfigHighlightIndex + 1, configFiles.size() - 1);
        }
    }

    public void moveLoadConfigUp() {
        loadConfigHighlightIndex = Math.max(0, loadConfigHighlightIndex - 1);
    }

    public void confirmLoadConfig() throws IOException {
        if (configFiles.isEmpty()) {
            return;
        }
        RunConfig config = runConfigService.load(configFiles.get(loadConfigHighlightIndex));
        loadRunConfig(config);
        currentScreen = Screen.BROWSER;
    }

    // Export state
    public enum ExportFormat {
        GRADLE,
        MAVEN
    }

    private ExportFormat exportFormat = ExportFormat.GRADLE;
    private ConfigExportService.ExportMode exportMode = ConfigExportService.ExportMode.MINIMAL;
    private boolean showExport = false;

    @Requirements({"atunko:TUI_0001.21"})
    public boolean isShowExport() {
        return showExport;
    }

    @Requirements({"atunko:TUI_0001.21"})
    public void openExport() {
        this.showExport = true;
    }

    @Requirements({"atunko:TUI_0001.21"})
    public void closeExport() {
        this.showExport = false;
    }

    @Requirements({"atunko:TUI_0001.22"})
    public ExportFormat exportFormat() {
        return exportFormat;
    }

    @Requirements({"atunko:TUI_0001.22"})
    public void setExportFormat(ExportFormat format) {
        this.exportFormat = format;
    }

    @Requirements({"atunko:TUI_0001.23"})
    public ConfigExportService.ExportMode exportMode() {
        return exportMode;
    }

    @Requirements({"atunko:TUI_0001.23"})
    public void toggleExportMode() {
        this.exportMode = exportMode == ConfigExportService.ExportMode.MINIMAL
                ? ConfigExportService.ExportMode.FULL
                : ConfigExportService.ExportMode.MINIMAL;
    }

    // Recipe options state
    private final Map<String, Map<String, Object>> recipeOptions = new HashMap<>();
    private boolean showOptions = false;
    private boolean optionsEditing = false;
    public final TextInputState optionsEditState = new TextInputState();
    private String focusedRecipeForOptions = null;
    private int focusedOptionIndex = 0;

    @Requirements({"atunko:TUI_0001.25"})
    public Map<String, Object> getRecipeOptions(String recipeName) {
        return recipeOptions.getOrDefault(recipeName, Map.of());
    }

    @Requirements({"atunko:TUI_0001.25"})
    public void setRecipeOption(String recipeName, String optionName, Object value) {
        recipeOptions.computeIfAbsent(recipeName, k -> new LinkedHashMap<>()).put(optionName, value);
    }

    @Requirements({"atunko:TUI_0001.25"})
    public void clearRecipeOption(String recipeName, String optionName) {
        Map<String, Object> opts = recipeOptions.get(recipeName);
        if (opts != null) {
            opts.remove(optionName);
            if (opts.isEmpty()) {
                recipeOptions.remove(recipeName);
            }
        }
    }

    @Requirements({"atunko:TUI_0001.24"})
    public boolean isShowOptions() {
        return showOptions;
    }

    @Requirements({"atunko:TUI_0001.24"})
    public void openOptions(String recipeName) {
        this.focusedRecipeForOptions = recipeName;
        this.focusedOptionIndex = 0;
        this.optionsEditing = false;
        this.optionsEditState.clear();
        this.showOptions = true;
    }

    @Requirements({"atunko:TUI_0001.24"})
    public void closeOptions() {
        this.showOptions = false;
        this.optionsEditing = false;
        this.optionsEditState.clear();
        this.focusedRecipeForOptions = null;
        this.focusedOptionIndex = 0;
    }

    @Requirements({"atunko:TUI_0001.24"})
    public boolean isOptionsEditing() {
        return optionsEditing;
    }

    @Requirements({"atunko:TUI_0001.24"})
    public void startOptionsEditing() {
        this.optionsEditing = true;
    }

    @Requirements({"atunko:TUI_0001.24"})
    public void stopOptionsEditing() {
        this.optionsEditing = false;
        this.optionsEditState.clear();
    }

    @Requirements({"atunko:TUI_0001.25"})
    public void cycleRecipeOptionBoolean(String recipeName, String optionName) {
        Object cur = getRecipeOptions(recipeName).get(optionName);
        if (cur == null) {
            setRecipeOption(recipeName, optionName, Boolean.TRUE);
        } else if (Boolean.TRUE.equals(cur)) {
            setRecipeOption(recipeName, optionName, Boolean.FALSE);
        } else {
            clearRecipeOption(recipeName, optionName);
        }
    }

    @Requirements({"atunko:TUI_0001.24"})
    public String focusedRecipeForOptions() {
        return focusedRecipeForOptions;
    }

    @Requirements({"atunko:TUI_0001.24"})
    public int focusedOptionIndex() {
        return focusedOptionIndex;
    }

    @Requirements({"atunko:TUI_0001.24"})
    public void moveOptionHighlightDown(int optionCount) {
        if (optionCount > 0) {
            focusedOptionIndex = (focusedOptionIndex + 1) % optionCount;
        }
    }

    @Requirements({"atunko:TUI_0001.24"})
    public void moveOptionHighlightUp(int optionCount) {
        if (optionCount > 0) {
            focusedOptionIndex = (focusedOptionIndex - 1 + optionCount) % optionCount;
        }
    }

    @Requirements({"atunko:TUI_0001.21", "atunko:TUI_0001.26"})
    public RunConfig buildRunConfig() {
        List<RecipeEntry> entries = runOrder.stream()
                .filter(selectedRecipes::contains)
                .map(name -> {
                    Map<String, Object> opts = recipeOptions.get(name);
                    return new RecipeEntry(name, (opts != null && !opts.isEmpty()) ? opts : null, null);
                })
                .toList();
        return new RunConfig(entries);
    }

    private boolean saveConfigMode = false;
    private String saveConfigName = "";

    public boolean isSaveConfigMode() {
        return saveConfigMode;
    }

    public String saveConfigName() {
        return saveConfigName;
    }

    public void enterSaveConfigMode() {
        saveConfigMode = true;
        saveConfigName = "";
    }

    public void exitSaveConfigMode() {
        saveConfigMode = false;
        saveConfigName = "";
    }

    public void setSaveConfigName(String name) {
        saveConfigName = name;
    }

    public void confirmSaveConfig() throws IOException {
        if (saveConfigName.isBlank()) {
            exitSaveConfigMode();
            return;
        }
        Path dir = runsDir();
        Files.createDirectories(dir);
        saveRunConfig(dir.resolve(saveConfigName + ".yml"));
        exitSaveConfigMode();
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
