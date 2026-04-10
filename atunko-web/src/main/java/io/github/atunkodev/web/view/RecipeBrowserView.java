package io.github.atunkodev.web.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;
import io.github.atunkodev.core.AppServices;
import io.github.atunkodev.core.config.RecipeEntry;
import io.github.atunkodev.core.config.RunConfig;
import io.github.atunkodev.core.config.RunConfigService;
import io.github.atunkodev.core.engine.ExecutionResult;
import io.github.atunkodev.core.engine.FileChange;
import io.github.atunkodev.core.project.ProjectInfo;
import io.github.atunkodev.core.project.SessionHolder;
import io.github.atunkodev.core.recipe.RecipeInfo;
import io.github.atunkodev.core.recipe.SortOrder;
import io.github.atunkodev.web.RecipeHolder;
import io.github.reqstool.annotations.Requirements;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.openrewrite.SourceFile;

@Route("")
@Requirements({"atunko:WEB_0001.1"})
public class RecipeBrowserView extends AppLayout {

    private final TreeGrid<TreeNode> treeGrid = new TreeGrid<>();
    private final Span statusBar = new Span();
    private final VerticalLayout detailPanel = new VerticalLayout();
    private final MultiSelectComboBox<String> tagFilter = new MultiSelectComboBox<>();
    private final TextField searchField = new TextField();
    private final Button dryRunButton = new Button("Dry Run", VaadinIcon.EYE.create());
    private final Button executeButton = new Button("Execute", VaadinIcon.PLAY.create());
    private final Button exportButton = new Button("Export", VaadinIcon.DOWNLOAD_ALT.create());

    private List<RecipeInfo> allRecipes;
    private String currentTextQuery = "";
    private Set<String> currentTagFilter = Set.of();
    private RecipeInfo detailRecipe;
    private CascadeSelectionHandler cascadeHandler;
    private boolean inCascadeUpdate = false;
    private Map<RecipeInfo, List<RecipeInfo>> childToParents;
    private Set<RecipeInfo> coveredRecipes = Set.of();
    private SortOrder currentSortOrder = SortOrder.NAME;
    private final RunConfigService runConfigService = new RunConfigService();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public RecipeBrowserView() {
        allRecipes = RecipeHolder.getRecipes();
        childToParents = RecipeCoverageUtils.buildReverseIndex(allRecipes);

        H2 title = new H2("atunko");
        title.getStyle().set("margin", "0 auto");
        addToNavbar(title);

        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();
        content.setPadding(false);
        content.setSpacing(false);
        content.getStyle().set("overflow", "hidden");

        content.add(buildSearchBar());
        content.addAndExpand(buildSplitLayout());
        content.add(buildStatusBar());

        setContent(content);

        buildTreeGrid();
        buildDetailPanel();
        applyFilters();
    }

    @Requirements({"atunko:WEB_0001.13"})
    private Component buildSearchBar() {
        searchField.setPlaceholder("Search recipes...");
        searchField.setWidthFull();
        searchField.setValueChangeMode(ValueChangeMode.EAGER);
        searchField.addValueChangeListener(e -> {
            currentTextQuery = e.getValue();
            applyFilters();
        });

        Select<SortOrder> sortSelect = new Select<>();
        sortSelect.setItems(SortOrder.NAME, SortOrder.TAGS);
        sortSelect.setValue(SortOrder.NAME);
        sortSelect.setItemLabelGenerator(s -> s == SortOrder.NAME ? "Sort: Name" : "Sort: Tags");
        sortSelect.addValueChangeListener(e -> {
            currentSortOrder = e.getValue();
            applyFilters();
        });

        HorizontalLayout bar = new HorizontalLayout(searchField, sortSelect);
        bar.setWidthFull();
        bar.expand(searchField);
        return bar;
    }

    private Component buildSplitLayout() {
        SplitLayout split = new SplitLayout();
        split.setSizeFull();

        VerticalLayout leftPane = new VerticalLayout(treeGrid);
        leftPane.setSizeFull();
        leftPane.setPadding(false);

        detailPanel.setWidth("100%");
        detailPanel.setHeightFull();

        split.addToPrimary(leftPane);
        split.addToSecondary(detailPanel);
        return split;
    }

    @Requirements({
        "atunko:WEB_0001.9",
        "atunko:WEB_0001.11",
        "atunko:WEB_0001.12",
        "atunko:WEB_0001.14",
        "atunko:WEB_0001.17"
    })
    private Component buildStatusBar() {
        Button selectAllButton = new Button("Select All", VaadinIcon.CHECK_SQUARE_O.create(), e -> selectAllVisible());
        selectAllButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);

        Button deselectAllButton = new Button("Deselect All", VaadinIcon.THIN_SQUARE.create(), e -> deselectAll());
        deselectAllButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);

        Button saveButton = new Button("Save", VaadinIcon.DOWNLOAD.create(), e -> saveConfig());
        saveButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);

        Button loadButton = new Button("Load", VaadinIcon.UPLOAD.create(), e -> loadConfig());
        loadButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);

        exportButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
        exportButton.addClickListener(e -> openExportDialog());

        dryRunButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
        dryRunButton.addClickListener(e -> runRecipes(true));

        executeButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
        executeButton.addClickListener(e -> runRecipes(false));

        HorizontalLayout bar = new HorizontalLayout(
                statusBar,
                selectAllButton,
                deselectAllButton,
                saveButton,
                loadButton,
                exportButton,
                dryRunButton,
                executeButton);
        bar.setWidthFull();
        bar.setAlignItems(HorizontalLayout.Alignment.CENTER);
        return bar;
    }

    @Requirements({"atunko:WEB_0001.1", "atunko:WEB_0001.4"})
    private void buildTreeGrid() {
        treeGrid.setSizeFull();
        treeGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        treeGrid.setSelectionMode(TreeGrid.SelectionMode.MULTI);

        treeGrid.addComponentHierarchyColumn(node -> {
                    Span name = new Span(node.recipe().displayName());
                    if (coveredRecipes.contains(node.recipe())) {
                        Span badge = new Span("covered");
                        badge.getElement().getThemeList().addAll(List.of("badge", "contrast", "small"));
                        HorizontalLayout row = new HorizontalLayout(name, badge);
                        row.setAlignItems(HorizontalLayout.Alignment.CENTER);
                        row.setSpacing(true);
                        return row;
                    }
                    return name;
                })
                .setHeader("Name")
                .setSortable(true);

        Grid.Column<TreeNode> tagsColumn = treeGrid.addColumn(
                        node -> String.join(", ", node.recipe().tags()))
                .setHeader("Tags")
                .setSortable(false);

        Set<String> allTags =
                allRecipes.stream().flatMap(r -> r.tags().stream()).collect(Collectors.toCollection(TreeSet::new));
        tagFilter.setPlaceholder("Filter by tag...");
        tagFilter.setItems(allTags);
        tagFilter.addValueChangeListener(e -> {
            currentTagFilter = e.getValue();
            applyFilters();
        });
        HeaderRow filterRow = treeGrid.appendHeaderRow();
        filterRow.getCell(tagsColumn).setComponent(tagFilter);

        treeGrid.asMultiSelect().addSelectionListener(e -> {
            if (inCascadeUpdate || cascadeHandler == null) {
                return;
            }
            inCascadeUpdate = true;
            try {
                Set<RecipeInfo> added =
                        e.getAddedSelection().stream().map(TreeNode::recipe).collect(Collectors.toSet());
                Set<RecipeInfo> removed =
                        e.getRemovedSelection().stream().map(TreeNode::recipe).collect(Collectors.toSet());
                for (RecipeInfo recipe : added) {
                    cascadeHandler.selectItem(recipe);
                }
                for (RecipeInfo recipe : removed) {
                    cascadeHandler.deselectItem(recipe);
                }
            } finally {
                inCascadeUpdate = false;
            }
            updateCoveredRecipes();
            Set<TreeNode> all = treeGrid.getSelectedItems();
            if (!all.isEmpty()) {
                selectForDetail(all.iterator().next().recipe());
            }
        });
    }

    private void buildDetailPanel() {
        detailPanel.add(new Span("Select a recipe to view details"));
    }

    @Requirements({"atunko:WEB_0001.15"})
    private void updateCoveredRecipes() {
        if (cascadeHandler == null) {
            return;
        }
        coveredRecipes = RecipeCoverageUtils.computeCovered(cascadeHandler.getSelectedItems());
        treeGrid.getDataProvider().refreshAll();
    }

    private void applyFilters() {
        List<RecipeInfo> filtered = RecipeFilter.filter(allRecipes, currentTextQuery, currentTagFilter).stream()
                .sorted(currentSortOrder.comparator())
                .toList();

        TreeData<TreeNode> treeData = new TreeData<>();
        Map<RecipeInfo, List<TreeNode>> recipeToNodes = new HashMap<>();
        Map<TreeNode, TreeNode> parentMap = new HashMap<>();
        Map<TreeNode, List<TreeNode>> childrenMap = new HashMap<>();

        for (RecipeInfo recipe : filtered) {
            TreeNode rootNode = new TreeNode(recipe, recipe.name());
            treeData.addItem(null, rootNode);
            recipeToNodes.computeIfAbsent(recipe, k -> new ArrayList<>()).add(rootNode);

            Set<RecipeInfo> ancestors = new HashSet<>();
            ancestors.add(recipe);
            addChildren(rootNode, recipe, treeData, recipeToNodes, parentMap, childrenMap, ancestors);
        }

        treeGrid.setDataProvider(new TreeDataProvider<>(treeData));
        cascadeHandler = new CascadeSelectionHandler(treeGrid, recipeToNodes, parentMap, childrenMap);
        statusBar.setText("Showing " + filtered.size() + " recipes");
    }

    private void addChildren(
            TreeNode parentNode,
            RecipeInfo parent,
            TreeData<TreeNode> treeData,
            Map<RecipeInfo, List<TreeNode>> recipeToNodes,
            Map<TreeNode, TreeNode> parentMap,
            Map<TreeNode, List<TreeNode>> childrenMap,
            Set<RecipeInfo> ancestors) {
        if (!parent.isComposite()) {
            return;
        }
        List<TreeNode> children = new ArrayList<>();
        Set<String> addedChildPaths = new HashSet<>();
        for (RecipeInfo child : parent.recipeList()) {
            if (ancestors.contains(child)) {
                continue;
            }
            String childPath = parentNode.path() + "/" + child.name();
            if (!addedChildPaths.add(childPath)) {
                continue;
            }
            TreeNode childNode = new TreeNode(child, childPath);
            treeData.addItem(parentNode, childNode);
            recipeToNodes.computeIfAbsent(child, k -> new ArrayList<>()).add(childNode);
            parentMap.put(childNode, parentNode);
            children.add(childNode);

            ancestors.add(child);
            addChildren(childNode, child, treeData, recipeToNodes, parentMap, childrenMap, ancestors);
            ancestors.remove(child);
        }
        if (!children.isEmpty()) {
            childrenMap.put(parentNode, children);
        }
    }

    @Requirements({"atunko:WEB_0001.9", "atunko:WEB_0001.10"})
    void runRecipes(boolean dryRun) {
        if (AppServices.getEngine() == null || AppServices.getSourceParser() == null) {
            return;
        }
        Set<RecipeInfo> selected = cascadeHandler.getSelectedItems();
        if (selected.isEmpty()) {
            Notification.show("No recipes selected", 3000, Notification.Position.MIDDLE);
            return;
        }

        dryRunButton.setEnabled(false);
        executeButton.setEnabled(false);

        Runnable onCancel = () -> {
            dryRunButton.setEnabled(true);
            executeButton.setEnabled(true);
        };

        new RunOrderDialog(selected, dryRun, ordered -> executeRecipes(ordered, dryRun), onCancel).open();
    }

    private void executeRecipes(List<RecipeInfo> recipes, boolean dryRun) {
        Dialog progressDialog = new Dialog();
        progressDialog.setCloseOnEsc(false);
        progressDialog.setCloseOnOutsideClick(false);
        progressDialog.setHeaderTitle(dryRun ? "Running Dry Run..." : "Executing Recipes...");
        ProgressBar bar = new ProgressBar();
        bar.setIndeterminate(true);
        bar.setWidth("300px");
        VerticalLayout progressContent = new VerticalLayout(new Span(recipes.size() + " recipe(s) selected"), bar);
        progressContent.setAlignItems(VerticalLayout.Alignment.CENTER);
        progressDialog.add(progressContent);
        progressDialog.open();

        UI ui = UI.getCurrent();

        executor.submit(() -> {
            try {
                ProjectInfo projectInfo = SessionHolder.getProjectInfo();
                Path projectDir = SessionHolder.getProjectDir();
                List<SourceFile> sources;
                if (projectInfo != null) {
                    sources = AppServices.getSourceParser().parse(projectInfo);
                } else {
                    sources = AppServices.getSourceParser().parse(new ProjectInfo(List.of(), List.of(projectDir)));
                }

                List<FileChange> allChanges = new ArrayList<>();
                List<String> failedRecipes = new ArrayList<>();
                for (RecipeInfo recipe : recipes) {
                    try {
                        ExecutionResult result = AppServices.getEngine().execute(recipe.name(), sources);
                        allChanges.addAll(result.changes());
                    } catch (Exception recipeEx) {
                        failedRecipes.add(recipe.displayName() + ": " + recipeEx.getMessage());
                    }
                }
                ExecutionResult combined = new ExecutionResult(allChanges);

                if (!dryRun && AppServices.getChangeApplier() != null) {
                    AppServices.getChangeApplier().apply(projectDir, combined.changes());
                }

                ui.access(() -> {
                    progressDialog.close();
                    new DiffDialog(combined, dryRun).open();
                    if (!failedRecipes.isEmpty()) {
                        Notification.show(
                                failedRecipes.size() + " recipe(s) failed:\n" + String.join("\n", failedRecipes),
                                8000,
                                Notification.Position.MIDDLE);
                    }
                    dryRunButton.setEnabled(true);
                    executeButton.setEnabled(true);
                });
            } catch (Exception e) {
                ui.access(() -> {
                    progressDialog.close();
                    Notification.show("Error: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
                    dryRunButton.setEnabled(true);
                    executeButton.setEnabled(true);
                });
            }
        });
    }

    // --- Testability hooks ---

    public List<RecipeInfo> getVisibleRecipes() {
        return RecipeFilter.filter(allRecipes, currentTextQuery, currentTagFilter).stream()
                .sorted(currentSortOrder.comparator())
                .toList();
    }

    @Requirements({"atunko:WEB_0001.2"})
    public void applyTextFilter(String query) {
        currentTextQuery = query;
        applyFilters();
    }

    @Requirements({"atunko:WEB_0001.2"})
    public void applyTagFilter(Set<String> tags) {
        currentTagFilter = tags;
        applyFilters();
    }

    @Requirements({"atunko:WEB_0001.6", "atunko:WEB_0001.16"})
    public void selectForDetail(RecipeInfo recipe) {
        detailRecipe = recipe;
        detailPanel.removeAll();
        detailPanel.add(new H3(recipe.displayName()));
        detailPanel.add(new Paragraph(recipe.description()));
        Span tags = new Span("Tags: " + String.join(", ", recipe.tags()));
        detailPanel.add(tags);
        if (recipe.isComposite()) {
            detailPanel.add(new Span("Recipe List:"));
            recipe.recipeList().forEach(child -> detailPanel.add(new Span("• " + child.displayName())));
        }
        List<RecipeInfo> parents = childToParents.getOrDefault(recipe, List.of());
        if (!parents.isEmpty()) {
            String parentNames = parents.stream().map(RecipeInfo::displayName).collect(Collectors.joining(", "));
            detailPanel.add(new Span("Included in: " + parentNames));
        }
    }

    public RecipeInfo getDetailPanelRecipe() {
        return detailRecipe;
    }

    public TreeGrid<TreeNode> getTreeGrid() {
        return treeGrid;
    }

    public CascadeSelectionHandler getCascadeHandler() {
        return cascadeHandler;
    }

    public Set<RecipeInfo> getSelectedRecipes() {
        return cascadeHandler.getSelectedItems();
    }

    public Button getExportButton() {
        return exportButton;
    }

    public Button getDryRunButton() {
        return dryRunButton;
    }

    public Button getExecuteButton() {
        return executeButton;
    }

    public Set<RecipeInfo> getCoveredRecipes() {
        return coveredRecipes;
    }

    @Requirements({"atunko:WEB_0001.14"})
    public void selectAllVisible() {
        if (cascadeHandler == null) {
            return;
        }
        inCascadeUpdate = true;
        try {
            for (RecipeInfo recipe : getVisibleRecipes()) {
                cascadeHandler.selectItem(recipe);
            }
        } finally {
            inCascadeUpdate = false;
        }
        updateCoveredRecipes();
    }

    @Requirements({"atunko:WEB_0001.14"})
    public void deselectAll() {
        if (cascadeHandler == null) {
            return;
        }
        inCascadeUpdate = true;
        try {
            for (RecipeInfo recipe : Set.copyOf(cascadeHandler.getSelectedItems())) {
                cascadeHandler.deselectItem(recipe);
            }
        } finally {
            inCascadeUpdate = false;
        }
        updateCoveredRecipes();
    }

    @Requirements({"atunko:WEB_0001.11"})
    void saveConfig() {
        if (cascadeHandler == null) {
            return;
        }
        Set<RecipeInfo> selected = cascadeHandler.getSelectedItems();
        if (selected.isEmpty()) {
            Notification.show("No recipes selected", 3000, Notification.Position.MIDDLE);
            return;
        }

        Dialog nameDialog = new Dialog();
        nameDialog.setHeaderTitle("Save Run Configuration");
        TextField nameField = new TextField("Name");
        nameField.setWidthFull();
        nameDialog.add(nameField);

        Button cancelButton = new Button("Cancel", e -> nameDialog.close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
        Button confirmButton = new Button("Save", e -> {
            String name = nameField.getValue().trim();
            if (name.isEmpty()) {
                Notification.show("Name is required", 3000, Notification.Position.MIDDLE);
                return;
            }
            try {
                Path runsDir = SessionHolder.getProjectDir().resolve("atunko/runs");
                Files.createDirectories(runsDir);
                Path file = runsDir.resolve(name + ".yaml");
                List<RecipeEntry> entries =
                        selected.stream().map(r -> new RecipeEntry(r.name())).toList();
                RunConfig config = new RunConfig(entries);
                runConfigService.save(config, file);
                nameDialog.close();
                Notification.show("Saved: " + file.getFileName(), 3000, Notification.Position.MIDDLE);
            } catch (IOException ex) {
                Notification.show("Save failed: " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
            }
        });
        confirmButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
        nameDialog.getFooter().add(cancelButton, confirmButton);
        nameDialog.open();
    }

    @Requirements({"atunko:WEB_0001.12"})
    void loadConfig() {
        Path runsDir = SessionHolder.getProjectDir().resolve("atunko/runs");
        if (!Files.isDirectory(runsDir)) {
            Notification.show("No saved runs found", 3000, Notification.Position.MIDDLE);
            return;
        }

        List<Path> yamlFiles;
        try (Stream<Path> stream = Files.list(runsDir)) {
            yamlFiles =
                    stream.filter(p -> p.toString().endsWith(".yaml")).sorted().toList();
        } catch (IOException ex) {
            Notification.show("Failed to list runs: " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
            return;
        }
        if (yamlFiles.isEmpty()) {
            Notification.show("No saved runs found", 3000, Notification.Position.MIDDLE);
            return;
        }

        Dialog pickerDialog = new Dialog();
        pickerDialog.setHeaderTitle("Load Run Configuration");

        Select<Path> fileSelect = new Select<>();
        fileSelect.setItems(yamlFiles);
        fileSelect.setItemLabelGenerator(p -> p.getFileName().toString().replace(".yaml", ""));
        fileSelect.setWidthFull();
        pickerDialog.add(fileSelect);

        Button cancelButton = new Button("Cancel", e -> pickerDialog.close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
        Button confirmButton = new Button("Load", e -> {
            Path selected = fileSelect.getValue();
            if (selected == null) {
                Notification.show("Select a configuration", 3000, Notification.Position.MIDDLE);
                return;
            }
            try {
                RunConfig config = runConfigService.load(selected);
                applyRunConfig(config);
                pickerDialog.close();
                Notification.show(
                        "Loaded: " + selected.getFileName().toString().replace(".yaml", ""),
                        3000,
                        Notification.Position.MIDDLE);
            } catch (IOException ex) {
                Notification.show("Load failed: " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
            }
        });
        confirmButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
        pickerDialog.getFooter().add(cancelButton, confirmButton);
        pickerDialog.open();
    }

    @Requirements({"atunko:WEB_0001.17"})
    void openExportDialog() {
        if (cascadeHandler == null) {
            return;
        }
        new ExportConfigDialog(cascadeHandler.getSelectedItems()).open();
    }

    void applyRunConfig(RunConfig config) {
        Map<String, RecipeInfo> recipeLookup = buildRecipeNameLookup();
        deselectAll();

        inCascadeUpdate = true;
        try {
            for (RecipeEntry entry : config.recipes()) {
                RecipeInfo recipe = recipeLookup.get(entry.name());
                if (recipe != null) {
                    cascadeHandler.selectItem(recipe);
                }
            }
        } finally {
            inCascadeUpdate = false;
        }
        updateCoveredRecipes();
    }

    private Map<String, RecipeInfo> buildRecipeNameLookup() {
        Map<String, RecipeInfo> lookup = new HashMap<>();
        for (RecipeInfo recipe : allRecipes) {
            addToLookup(recipe, lookup);
        }
        return lookup;
    }

    private void addToLookup(RecipeInfo recipe, Map<String, RecipeInfo> lookup) {
        lookup.putIfAbsent(recipe.name(), recipe);
        for (RecipeInfo child : recipe.recipeList()) {
            if (!lookup.containsKey(child.name())) {
                addToLookup(child, lookup);
            }
        }
    }

    @Requirements({"atunko:WEB_0001.13"})
    public void applySortOrder(SortOrder order) {
        currentSortOrder = order;
        applyFilters();
    }
}
