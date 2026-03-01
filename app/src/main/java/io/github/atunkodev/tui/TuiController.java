package io.github.atunkodev.tui;

import io.github.atunkodev.cli.SortOrder;
import io.github.atunkodev.core.config.RunConfig;
import io.github.atunkodev.core.config.RunConfigService;
import io.github.atunkodev.core.engine.ExecutionResult;
import io.github.atunkodev.core.recipe.RecipeInfo;
import io.github.reqstool.annotations.Requirements;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

@Requirements({"CLI_0001"})
public class TuiController {

    private final List<RecipeInfo> allRecipes;
    private final RunConfigService runConfigService;
    private Screen currentScreen = Screen.BROWSER;
    private String searchQuery = "";
    private SortOrder sortOrder = SortOrder.NAME;
    private int highlightedIndex;
    private final Set<String> selectedRecipes = new LinkedHashSet<>();
    private String tagFilter = "";
    private boolean searchMode;
    private ExecutionResult executionResult;
    private boolean lastRunWasDryRun;

    public TuiController(List<RecipeInfo> allRecipes) {
        this(allRecipes, new RunConfigService());
    }

    public TuiController(List<RecipeInfo> allRecipes, RunConfigService runConfigService) {
        this.allRecipes = List.copyOf(allRecipes);
        this.runConfigService = runConfigService;
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

    public boolean isSearchMode() {
        return searchMode;
    }

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
        List<RecipeInfo> visible = recipes();
        if (visible.isEmpty() || highlightedIndex >= visible.size()) {
            return Optional.empty();
        }
        return Optional.of(visible.get(highlightedIndex));
    }

    @Requirements({"CLI_0001.12"})
    public void moveDown() {
        List<RecipeInfo> visible = recipes();
        if (!visible.isEmpty()) {
            highlightedIndex = (highlightedIndex + 1) % visible.size();
        }
    }

    @Requirements({"CLI_0001.12"})
    public void moveUp() {
        List<RecipeInfo> visible = recipes();
        if (!visible.isEmpty()) {
            highlightedIndex = (highlightedIndex - 1 + visible.size()) % visible.size();
        }
    }

    @Requirements({"CLI_0001.5"})
    public void toggleSelection() {
        highlightedRecipe().ifPresent(recipe -> {
            if (!selectedRecipes.remove(recipe.name())) {
                selectedRecipes.add(recipe.name());
            }
        });
    }

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
