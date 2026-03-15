## Why

The TUI currently supports basic multi-select (TUI_0001.5) and composite recipe expand/collapse browsing (TUI_0001.13), but selecting a composite recipe does not automatically propagate selection to its sub-recipes. Users must manually select each sub-recipe, which is tedious and error-prone when working with large composite recipes. Adding cascade-both-ways selection logic brings the TUI to parity with the Web UI (WEB_0001.5) and makes composite recipe selection intuitive and efficient.

## What Changes

- Selecting a composite recipe in the TUI auto-selects all its sub-recipes (cascade down)
- When all sub-recipes of a composite are explicitly selected, the composite is auto-selected (cascade up)
- Deselecting one or more (but not all) sub-recipes of a composite puts the composite in an **indeterminate** (partial) visual state
- Deselecting a composite recipe deselects all its sub-recipes
- The recipe list renderer reflects these three checkbox states: selected, unselected, indeterminate

## Capabilities

### New Capabilities

- `tui-cascade-selection`: Cascade-both-ways checkbox selection for composite recipes in the TUI browser — selecting a parent selects all children, selecting all children selects the parent, partial child selection yields indeterminate parent state

### Modified Capabilities

- `tui-launch`: The TUI browser's multi-select behavior changes — composite recipes now participate in cascade selection rather than independent toggle. The selection state model gains an indeterminate state.

## Impact

- `atunko-tui`: `TuiController` selection logic, `BrowserView` key handling, `RecipeListRenderer` visual state
- `atunko-core`: No changes expected — `RecipeInfo.isComposite()` and `recipeList` already provide the needed structure
- No breaking API changes — purely behavioral and visual enhancement within the TUI
