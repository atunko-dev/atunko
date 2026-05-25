## Why

Users can browse, search, and select OpenRewrite recipes in the TUI, but they cannot view or set recipe options (parameters) before running. This means any recipe that requires configuration (e.g. a target Java version, a package name to rename, a flag to enable/disable behaviour) cannot be meaningfully used from the TUI — users must fall back to editing YAML by hand.

## What Changes

- **Core**: Extend `RecipeInfo` to carry `List<RecipeOptionInfo>` (name, type, displayName, description, example, valid values, required flag, current value). Populate from `RecipeDescriptor.getOptions()` in `RecipeDiscoveryService`.
- **Core**: Extend `RecipeEntry` so its `options` map is the single source of truth for configured values. (Already exists as `@Nullable Map<String, Object> options` — no structural change needed, just usage discipline.)
- **TUI**: Add a new `RecipeOptionsView` full-screen overlay, reachable from the recipe list (browse/search screen) and from the confirm-run screen. Shows all options for the focused recipe with their type, requirement, default/example, and current value.
- **TUI**: Allow users to navigate options (j/k), press Enter to edit a value (inline text input for string/integer/boolean toggle), and save/discard changes. Edited values are stored on `TuiController` keyed by recipe name.
- **TUI**: Apply stored option values when assembling a `RunConfig` (in `buildRunConfig()`), so they flow through to execution and export.

## Capabilities

### New Capabilities

- `tui-recipe-options`: TUI overlay for viewing and editing recipe options/parameters before execution.
- `recipe-option-metadata`: Core capability to capture and expose `OptionDescriptor` data from OpenRewrite as part of `RecipeInfo`.

### Modified Capabilities

- `tui-launch`: Key binding added to open options overlay from recipe list screen (new key `o`).
- `tui-config-export`: Export must include configured option values in the emitted snippet (already flows through `RecipeEntry.options` — verify and annotate).

## Impact

- `atunko-core`: `RecipeInfo` record gains `List<RecipeOptionInfo>` field; new `RecipeOptionInfo` record added; `RecipeDiscoveryService.toRecipeInfo()` updated; `RunConfig`/`RecipeEntry` usage unchanged.
- `atunko-tui`: New `RecipeOptionsView.java`; `TuiController` gains option-value storage and retrieval; `RecipeListView` and `ConfirmRunView` get `o` key binding; `buildRunConfig()` merges stored options into `RecipeEntry`.
- No new dependencies; no breaking API changes.
