package io.github.atunkodev.web.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;
import io.github.atunkodev.core.recipe.RecipeInfo;
import io.github.atunkodev.web.RecipeHolder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Route("")
public class RecipeBrowserView extends AppLayout {

    private final TreeGrid<TreeNode> treeGrid = new TreeGrid<>();
    private final Span statusBar = new Span();
    private final VerticalLayout detailPanel = new VerticalLayout();
    private final MultiSelectComboBox<String> tagFilter = new MultiSelectComboBox<>();
    private final TextField searchField = new TextField();

    private List<RecipeInfo> allRecipes;
    private String currentTextQuery = "";
    private Set<String> currentTagFilter = Set.of();
    private RecipeInfo detailRecipe;
    private CascadeSelectionHandler cascadeHandler;
    private boolean inCascadeUpdate = false;

    public RecipeBrowserView() {
        allRecipes = RecipeHolder.getRecipes();

        addToNavbar(new H2("atunko"));

        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();
        content.setPadding(false);
        content.setSpacing(false);

        content.add(buildSearchBar());
        content.addAndExpand(buildSplitLayout());
        content.add(statusBar);

        setContent(content);

        buildTreeGrid();
        buildDetailPanel();
        applyFilters();
    }

    private Component buildSearchBar() {
        searchField.setPlaceholder("Search recipes...");
        searchField.setWidthFull();
        searchField.setValueChangeMode(ValueChangeMode.EAGER);
        searchField.addValueChangeListener(e -> {
            currentTextQuery = e.getValue();
            applyFilters();
        });
        return searchField;
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

    private void buildTreeGrid() {
        treeGrid.setSizeFull();
        treeGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        treeGrid.setSelectionMode(TreeGrid.SelectionMode.MULTI);

        treeGrid.addHierarchyColumn(node -> node.recipe().displayName())
                .setHeader("Name")
                .setSortable(true);

        Grid.Column<TreeNode> tagsColumn = treeGrid.addColumn(
                        node -> String.join(", ", node.recipe().tags()))
                .setHeader("Tags")
                .setSortable(false);

        // Tag filter in the Tags column header
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

        // Cascade-aware selection listener
        treeGrid.asMultiSelect().addSelectionListener(e -> {
            if (inCascadeUpdate) {
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
            Set<TreeNode> all = treeGrid.getSelectedItems();
            if (!all.isEmpty()) {
                selectForDetail(all.iterator().next().recipe());
            }
        });
    }

    private void buildDetailPanel() {
        detailPanel.add(new Span("Select a recipe to view details"));
    }

    private void applyFilters() {
        List<RecipeInfo> filtered = RecipeFilter.filter(allRecipes, currentTextQuery, currentTagFilter);

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
                continue; // cycle guard
            }
            String childPath = parentNode.path() + "/" + child.name();
            if (!addedChildPaths.add(childPath)) {
                continue; // duplicate entry in recipeList
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

    // --- Testability hooks ---

    public List<RecipeInfo> getVisibleRecipes() {
        return RecipeFilter.filter(allRecipes, currentTextQuery, currentTagFilter);
    }

    public void applyTextFilter(String query) {
        currentTextQuery = query;
        applyFilters();
    }

    public void applyTagFilter(Set<String> tags) {
        currentTagFilter = tags;
        applyFilters();
    }

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
}
