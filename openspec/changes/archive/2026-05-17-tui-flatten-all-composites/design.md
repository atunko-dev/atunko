## Context

The TUI run dialog (`ConfirmRunView`) already had a single-composite flatten (`f` key) backed
by `TuiController.flattenRunRecipe()`. Flatten-all is a superset: keep flattening until the
run order contains only leaf recipes.

## Goals / Non-Goals

**Goals:**
- Single keypress (`F`) to recursively flatten all composites in the run order
- Deduplicate leaves that appear in more than one composite (preserve first-seen order)
- Update help text and help overlay to document both flatten modes

**Non-Goals:**
- Auto-flatten on dialog open
- Undo/redo

## Decisions

### Loop-until-stable algorithm

**Rationale:** Nested composites (a composite whose sub-recipe is itself composite) require
multiple passes. A `while (changed)` loop is the simplest correct approach; in practice
recipe nesting rarely exceeds 2–3 levels so performance is not a concern.

### `LinkedHashSet` for deduplication

**Rationale:** Preserves insertion order while eliminating duplicate leaf names that arise
when two composites share a sub-recipe. A plain `List` + `contains` check would also work
but is O(n²) on large lists; `LinkedHashSet` is O(n).

### `F` key binding (uppercase)

**Rationale:** Consistent with the existing convention of `f` for single-flatten, uppercase
for the "more powerful" variant (also consistent with `F` for "flatten-all" in the Web UI
run order dialog).

## Risks / Trade-offs

- If a composite's sub-recipe is not in `allRecipes` (unlikely but possible with partial
  classpath scans), `findRecipeDeep` returns empty and the name is kept as-is rather than
  crashing. Acceptable conservative behaviour.
