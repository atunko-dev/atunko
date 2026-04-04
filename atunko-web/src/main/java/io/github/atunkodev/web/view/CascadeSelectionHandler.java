package io.github.atunkodev.web.view;

import com.vaadin.flow.component.treegrid.TreeGrid;
import io.github.atunkodev.core.recipe.RecipeInfo;
import io.github.reqstool.annotations.Requirements;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Encapsulates cascade-both-ways checkbox selection logic for a TreeGrid of TreeNode:
 *
 * <ul>
 *   <li>Selecting a parent selects all its descendants (follows recipeList recursively)
 *   <li>Selecting all visual children of a parent selects the parent
 *   <li>Deselecting one child removes the parent from selection (indeterminate state)
 * </ul>
 *
 * <p>Public API uses {@link RecipeInfo} — the handler maps to all {@link TreeNode} instances
 * wrapping that recipe so that duplicates at different tree positions stay in sync.
 */
@Requirements({"atunko:WEB_0001.5"})
public class CascadeSelectionHandler {

    private final TreeGrid<TreeNode> grid;
    /** All tree nodes for each recipe (may be more than one if recipe appears at multiple positions). */
    private final Map<RecipeInfo, List<TreeNode>> recipeToNodes;
    /** Maps each child TreeNode to its visual parent TreeNode. */
    private final Map<TreeNode, TreeNode> parentMap;
    /** Maps each parent TreeNode to its visual children. */
    private final Map<TreeNode, List<TreeNode>> childrenMap;

    private final Set<TreeNode> indeterminate = new HashSet<>();

    public CascadeSelectionHandler(
            TreeGrid<TreeNode> grid,
            Map<RecipeInfo, List<TreeNode>> recipeToNodes,
            Map<TreeNode, TreeNode> parentMap,
            Map<TreeNode, List<TreeNode>> childrenMap) {
        this.grid = grid;
        this.recipeToNodes = recipeToNodes;
        this.parentMap = parentMap;
        this.childrenMap = childrenMap;
    }

    /** Selects all tree nodes for this recipe and cascades down to all descendants. */
    public void selectItem(RecipeInfo recipe) {
        selectRecursive(recipe, new HashSet<>());
        for (TreeNode node : recipeToNodes.getOrDefault(recipe, List.of())) {
            updateParentState(node);
        }
    }

    /** Deselects all tree nodes for this recipe and cascades down to all descendants. */
    public void deselectItem(RecipeInfo recipe) {
        deselectRecursive(recipe, new HashSet<>());
        for (TreeNode node : recipeToNodes.getOrDefault(recipe, List.of())) {
            updateParentState(node);
        }
    }

    public boolean isIndeterminate(RecipeInfo recipe) {
        return recipeToNodes.getOrDefault(recipe, List.of()).stream().anyMatch(indeterminate::contains);
    }

    public Set<RecipeInfo> getSelectedItems() {
        Set<RecipeInfo> result = new HashSet<>();
        for (TreeNode node : grid.getSelectedItems()) {
            result.add(node.recipe());
        }
        return result;
    }

    private void selectRecursive(RecipeInfo recipe, Set<RecipeInfo> visited) {
        if (!visited.add(recipe)) {
            return;
        }
        for (TreeNode node : recipeToNodes.getOrDefault(recipe, List.of())) {
            grid.select(node);
            indeterminate.remove(node);
        }
        for (RecipeInfo child : recipe.recipeList()) {
            selectRecursive(child, visited);
        }
    }

    private void deselectRecursive(RecipeInfo recipe, Set<RecipeInfo> visited) {
        if (!visited.add(recipe)) {
            return;
        }
        for (TreeNode node : recipeToNodes.getOrDefault(recipe, List.of())) {
            grid.deselect(node);
            indeterminate.remove(node);
        }
        for (RecipeInfo child : recipe.recipeList()) {
            deselectRecursive(child, visited);
        }
    }

    /**
     * Updates the visual selection state of the parent of {@code changed}.
     *
     * <p>When all siblings are selected, all tree nodes for the parent recipe are selected. This
     * relies on the OpenRewrite data model guarantee that the same {@link RecipeInfo} object always
     * has the same {@code recipeList()} regardless of where it appears in the tree — so if all
     * children of one parent instance are selected, the children of every other instance of that
     * parent are also selected.
     */
    private void updateParentState(TreeNode changed) {
        TreeNode parent = parentMap.get(changed);
        if (parent == null) {
            return;
        }
        List<TreeNode> siblings = childrenMap.getOrDefault(parent, List.of());
        Set<TreeNode> selected = grid.getSelectedItems();
        long selectedCount = siblings.stream().filter(selected::contains).count();

        if (selectedCount == siblings.size()) {
            for (TreeNode pn : recipeToNodes.getOrDefault(parent.recipe(), List.of())) {
                grid.select(pn);
                indeterminate.remove(pn);
            }
        } else if (selectedCount == 0) {
            for (TreeNode pn : recipeToNodes.getOrDefault(parent.recipe(), List.of())) {
                grid.deselect(pn);
                indeterminate.remove(pn);
            }
        } else {
            for (TreeNode pn : recipeToNodes.getOrDefault(parent.recipe(), List.of())) {
                grid.deselect(pn);
                indeterminate.add(pn);
            }
        }
        updateParentState(parent);
    }
}
