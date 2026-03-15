## Context

The TUI currently tracks selected recipes as a flat `Set<String>` (recipe names) in `TuiController` and `RecipeListState`. Selection is toggled independently for each recipe via `toggleSelection()`, which simply adds/removes the name from the set. There is no awareness of parent/child relationships at selection time.

`RecipeInfo.isComposite()` and `recipeList()` are already available in core, and `TuiController` already uses them for expand/collapse browsing (TUI_0001.13) and coverage indicators (TUI_0001.16). The cascade logic can leverage these without any core changes.

The `RecipeListRenderer` currently renders a recipe row as selected or unselected based on set membership. It needs to support a third state (indeterminate/partial) for composite recipes whose children are only partly selected.

## Goals / Non-Goals

**Goals:**
- Selecting a composite recipe in the browser auto-selects all its transitive sub-recipes
- Deselecting a composite deselects all its transitive sub-recipes
- When all sub-recipes of a composite are explicitly selected, the composite is auto-selected (cascade-up)
- When some but not all sub-recipes are selected, the composite shows an indeterminate visual indicator
- The cascade applies at toggle time (not lazily computed on render)

**Non-Goals:**
- Cascade selection in the Run Dialog (TUI_0001.14) — selection semantics there are different (ordered execution list)
- Deep cascade for more than one level of nesting (only direct parent-child propagation at toggle time; full transitive closure is handled recursively)
- Changing how `cycleSelection` works — cycle operates on visible rows and stays independent

## Decisions

### 1. Tri-state tracked explicitly, not derived

**Decision**: Add a `Set<String> partialRecipes` alongside `selectedRecipes` in `RecipeListState` to track composites whose children are partially selected.

**Rationale**: Deriving indeterminate state on every render (`RecipeListRenderer`) would require walking the full recipe tree per composite row. Instead, maintain it incrementally at toggle time alongside `selectedRecipes`. This keeps rendering O(1) per row.

**Alternative considered**: Compute indeterminate state in renderer by comparing composite's children against `selectedRecipes` — simpler but O(n) per composite row on every render pass.

### 2. Cascade logic lives in `RecipeListState.toggleSelection()`

**Decision**: Extend `RecipeListState.toggleSelection()` to perform cascade propagation and update `partialRecipes` after any selection change.

**Rationale**: `RecipeListState` already holds `selectedRecipes` and has access to the recipe source (top-level recipes). Centralising cascade logic here keeps `TuiController.toggleSelection()` unchanged and makes the state self-consistent.

**Alternative considered**: Implementing cascade in `TuiController.toggleSelection()` — this would require TuiController to re-expose recipe lookup internals and duplicate the logic if `RecipeListState` is used for run-dialog selection too.

### 3. Cascade is transitive (recursive)

**Decision**: When selecting/deselecting a composite, recursively collect all transitive sub-recipe names (not just direct children) and add/remove them all at once, then recompute `partialRecipes` for all ancestors.

**Rationale**: Recipes can nest multiple levels deep. Handling only one level would leave deeper sub-recipes unselected when a top-level composite is toggled.

### 4. `partialRecipes` exposed as read-only set; renderer queries it

**Decision**: Expose `RecipeListState.partialRecipes()` as an immutable `Set<String>` and pass it to `RecipeListRenderer` alongside `selectedRecipes`. Renderer uses `~` or `[~]` prefix for indeterminate composites.

**Rationale**: Follows the existing pattern where `selectedRecipes` and `expandedRecipes` are passed to the renderer as read-only state — no behaviour leaks into the view layer.

### 5. No changes to Run Dialog cascade behaviour

**Decision**: `RecipeListState` in the run dialog does NOT cascade. Run dialog uses a separate `RecipeListState` instance where selection semantics are per-recipe toggle.

**Rationale**: In the run dialog, individual recipe toggling is the intended UX — the user is managing an execution plan, not picking from a catalogue.

## Risks / Trade-offs

- **Cycle in cascade-up**: After cascade-down selects all children, cascade-up immediately fires and marks the composite as selected. This recursive call chain must be guarded to avoid infinite loops. Mitigation: cascade-up only fires when all children of a composite are in `selectedRecipes`; it does not recurse further up (the parent of that composite will be re-evaluated naturally on the same tick).
- **Performance with large composites**: Recursively collecting all transitive sub-recipes is O(n) for composite size. Acceptable for typical recipe catalogs (hundreds, not millions). No mitigation needed.
- **`partialRecipes` drift**: If the recipe source changes (e.g., filter applied), `partialRecipes` could become stale. Mitigation: recompute `partialRecipes` whenever `selectedRecipes` is mutated (all write paths go through the same helper).

## Migration Plan

Purely additive change inside `atunko-tui`. No data migration or config changes needed. No public API changes. The run dialog is explicitly excluded from cascade behaviour, so existing run dialog tests are unaffected.
