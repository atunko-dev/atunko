# Proposal: Web UI Enhancements — Bulk Select, Sorting, Run Order, Save/Load, Coverage, Included-In

## Why

The Web UI can browse and execute recipes, but lacks workflow features that make it
productive for real use. Users need to sort and bulk-select recipes efficiently, review
execution order before running, save/load named configurations, and understand which
recipes overlap via composite coverage indicators and reverse-lookup.

Issues: atunko-dev/atunko#36, atunko-dev/atunko#37, atunko-dev/atunko#38,
atunko-dev/atunko#39, atunko-dev/atunko#40, atunko-dev/atunko#41

## What Changes

- **Run order dialog** (#36) — mandatory confirmation dialog before every Dry Run / Execute,
  showing selected recipes in an ordered grid with move-up/down controls and a "flatten
  composites" toggle that recursively expands composite recipes into their constituents
- **Save/load named configs** (#37) — save current recipe selection to
  `atunko/runs/<name>.yaml`, load from a picker that lists available runs.
  Reuses core `RunConfigService`.
- **Sorting** (#38) — `Select<SortOrder>` dropdown to sort recipes by name (alphabetical)
  or by tags (group by first tag). Reuses core `SortOrder` enum.
- **Bulk select/deselect** (#39) — Select All (respects active filters) and Deselect All
  buttons in the status bar
- **Coverage indicators** (#40) — Lumo "covered" badge on recipes that are already included
  in a selected composite, preventing redundant selection
- **Included-in reverse lookup** (#41) — detail panel shows "Included in: X, Y" for recipes
  that appear in composite recipes' `recipeList()`

## Capabilities

### New Capabilities
- `web-run-order`: Mandatory run order dialog with reorder and flatten (WEB_0001.10)
- `web-save-load-config`: Save/load named run configurations (WEB_0001.11, WEB_0001.12)
- `web-sorting`: Sort recipes by name or tags (WEB_0001.13)
- `web-bulk-select`: Select all / deselect all visible recipes (WEB_0001.14)
- `web-coverage-indicators`: Visual badge for recipes covered by selected composites (WEB_0001.15)
- `web-included-in`: Reverse lookup of composite membership in detail panel (WEB_0001.16)

### Modified Capabilities
_(none — existing execution and browsing unchanged)_

## Impact

- **New files**: `RunOrderDialog.java`, `RecipeCoverageUtils.java` (shared utility for
  #40 and #41), plus corresponding test files
- **Modified files**: `RecipeBrowserView.java` (all 6 features add fields/methods),
  `RecipeBrowserViewTest.java`, `requirements.yml`, `software_verification_cases.yml`
- **Reused core**: `SortOrder` (sorting), `RunConfigService` (save/load)
- **Core changes**: `RunConfig` evolves to structured format with `description` and
  per-recipe `options`; `RecipeEntry` record added; JSON Schema for validation
- **No breaking changes**: Existing UI behaviour preserved; run order dialog is additive
