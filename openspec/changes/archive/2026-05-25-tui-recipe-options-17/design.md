## Context

OpenRewrite recipes can declare typed options (`OptionDescriptor`: name, type, displayName, description, example, valid values, required flag, current value). These are available via `RecipeDescriptor.getOptions()` at discovery time. Currently `RecipeInfo` discards this metadata, and `RecipeEntry.options` (a `Map<String, Object>`) exists in the config model but is never populated from the TUI — users must edit YAML directly to configure recipe options.

The TUI already has patterns for full-screen overlays (`ExportConfigView`, `FileDiffView`) and inline text input (`BrowserView` search). The `TuiController` holds all mutable state; views are stateless renderers.

## Goals / Non-Goals

**Goals:**
- Expose option metadata (name, type, description, required, example/valid values, current value) in a dedicated TUI overlay
- Allow editing string, integer/long, and boolean option values in the overlay
- Store edited values on `TuiController` keyed by recipe name; merge them into `RecipeEntry.options` when building `RunConfig`
- Reach the overlay with key `o` from both the recipe list (browse/search screen) and the confirm-run screen
- Tested with unit tests following the existing pattern (controller state tests + view render tests)

**Non-Goals:**
- Validating values against `valid` lists at runtime (display only)
- Multi-value / list-type option support beyond string representation
- Persisting option values across TUI sessions (they are in-memory; users can save via run-config save)
- Mouse support

## Decisions

### 1. Extend `RecipeInfo` with `List<RecipeOptionInfo>` (new core record)

**Decision:** Add a new `RecipeOptionInfo` record in `atunko-core` mirroring the useful fields from `OptionDescriptor` (name, type, displayName, description, example, valid, required, defaultValue). Populate it in `RecipeDiscoveryService.toRecipeInfo()` from `descriptor.getOptions()`.

**Why not use `OptionDescriptor` directly?** `OptionDescriptor` is an OpenRewrite type — exposing it in `RecipeInfo` would leak a transitive dependency into callers (web UI, tests) that don't otherwise need it. A project-owned record decouples the model.

**Alternative:** Store options as a `Map<String, String>` — rejected; loses type and required information needed to render the UI properly.

### 2. Option value storage on `TuiController` as `Map<String, Map<String, Object>>`

**Decision:** `TuiController` holds `Map<String, Map<String, Object>> recipeOptions` keyed by recipe name, then option name. Methods: `getRecipeOptions(name)`, `setRecipeOption(name, optionName, value)`, `clearRecipeOption(name, optionName)`.

**Why not on `RecipeListState`?** Options span both browse and confirm-run contexts; controller-level state is consistent with how `selectedRecipes` and `exportFormat` are managed.

### 3. Full-screen overlay `RecipeOptionsView`, same pattern as `ExportConfigView`

**Decision:** `RecipeOptionsView.render(controller, recipe)` returns a full-screen `column(dock()...)` element. It replaces the screen's content when `controller.isShowOptions()` is true.

Layout:
```
┌─ Recipe Options ────────────────── <recipeName> ─┐
│ [idx/total] <displayName>  [REQUIRED] <type>      │  ← option row (highlighted)
│ Description: ...                                  │
│ Example: ...  Valid: [a, b, c]                    │
│ Value: [______________]                            │  ← editable field
│ ...                                               │
├───────────────────────────────────────────────────┤
│  j/k:navigate  Enter:edit  Del:clear  Esc:close   │
└───────────────────────────────────────────────────┘
```

Navigation: `j`/`k` move between options. `Enter` activates inline editing (character-by-character via `Toolkit.handleTextInputKey`). Boolean options cycle `true`/`false`/`(unset)` on `Enter` without text input. `Delete`/`Backspace` (when not editing) clears the stored value. `Esc` closes (if editing, first Esc cancels edit; second Esc closes overlay).

**Why not a floating popup?** TamboUI has no modal primitive — the full-screen overlay is the established pattern.

### 4. `buildRunConfig()` merges stored options into `RecipeEntry`

`TuiController.buildRunConfig()` already assembles `RecipeEntry` from `runOrder` filtered by `selectedRecipes`. It will additionally pass the stored options map (null if empty, to keep `@JsonInclude NON_NULL` behaviour).

### 5. Key binding: `o` from both screens

`o` is unused in `BrowserView`/`RecipeListView` and `ConfirmRunView`. It opens the options overlay for the currently highlighted recipe. If the recipe has no options, show a "No configurable options" message in the overlay rather than doing nothing.

## Risks / Trade-offs

- **`RecipeInfo` record change is additive but breaks any direct construction in tests** → All existing test constructions use the 4- or 5-arg constructor; adding a 6th field requires a new constructor or updating call sites. Mitigate: add a convenience constructor that defaults `options` to `List.of()`.
- **Options metadata depends on a loaded OpenRewrite environment** → Integration tests use fixture projects; unit tests can use `RecipeInfo` directly with hand-crafted `RecipeOptionInfo` instances. No new infrastructure needed.
- **Inline text editing in TamboUI** → `Toolkit.handleTextInputKey` is already used in `BrowserView`. Same approach applies here; state held in the controller during edit.
- **Type coercion** — option values stored as `Object`; for execution they must match the recipe's expected type. We store `Integer`/`Long`/`Boolean` parsed from the edited string; if parsing fails, store as `String` and let OpenRewrite report the error at run time.

## Open Questions

- Should we show options for sub-recipes within a composite, or only for top-level recipes? **Initial cut:** top-level only (sub-recipe options are rarely user-facing). Can expand later.
- Should clearing all options for a recipe remove the key from the map entirely (to avoid noise in serialised run config)? **Yes** — `buildRunConfig()` should omit null/empty options maps.
