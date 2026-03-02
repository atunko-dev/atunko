## Why

When a composite recipe like "Spring Boot 3.5" is selected, its sub-recipes (3.4,
Prometheus, etc.) appear independently in the recipe browser with no visual indication
that they are already covered. Users cannot tell which recipes will run as part of a
composite selection, leading to confusion about duplicate execution.

## What Changes

- Add visual indicators in the recipe list: `[✓]` for sub-recipes of an expanded
  selected composite, `[≈]` for top-level recipes covered by a selected composite
- Dim covered-but-unselected recipes to show they are included implicitly
- Show "Included in: <composite names>" in the detail panel for covered recipes
- Compute covered recipes recursively through nested composites
- Users can still explicitly select a covered recipe (it would run twice)

## Capabilities

### New Capabilities
- `recipe-coverage-indicators`: Visual indication of recipes covered by selected
  composites (TUI_0001.16)

### Modified Capabilities
_(none)_

## Impact

- **Code**: New `coveredRecipes()` and `includedIn()` methods on `TuiController`.
  Updated `RecipeListRenderer` to accept and display coverage state. Updated
  `BrowserView` and `DetailView` to show "Included in" info.
- **Dependencies**: None.
- **No breaking changes**: Existing selection behavior is preserved.
