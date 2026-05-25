## Why

The TUI browser displays composite recipes with a static "Composite: N sub-recipes" label but gives no feedback about how many of those sub-recipes are currently selected by the user. The Web UI already has this capability (`WEB_0001.15`/`WEB_0001.16`), but the logic lives in `atunko-web` — violating the shared-implementation principle that core logic must reside in `atunko-core`.

## What Changes

- Extract `RecipeCoverageUtils` logic from `atunko-web` into a new `RecipeCoverageService` (or static utility) in `atunko-core`.
- The Web UI refactors to delegate to the core utility (no behaviour change).
- The TUI detail panel upgrades the composite label from "Composite: N sub-recipes" to "Composite: X/N covered" when sub-recipes are selected.
- The TUI recipe-list rows for composite recipes show a coverage fraction indicator (e.g. `[3/5]`) when at least one sub-recipe is selected.

## Capabilities

### New Capabilities
- `tui-coverage-indicators`: TUI displays per-composite sub-recipe coverage counts (selected / total) in both the list row and the detail panel, backed by shared core logic.

### Modified Capabilities
- (none — existing web coverage behaviour is unchanged; only its implementation moves to core)

## Impact

- **atunko-core**: new class `RecipeCoverageUtils` (or package-level utility) in `io.github.atunkodev.core.recipe`
- **atunko-web**: `RecipeCoverageUtils` refactored to delegate to the core class; `@Requirements` annotations updated to reference new CORE requirement IDs
- **atunko-tui**: `BrowserView` updated to call core utility and render coverage fraction in list rows and detail panel
- **reqstool**: new `CORE_0001.xx` requirement(s) for the shared coverage computation; new `TUI_0001.xx` requirement(s) for the TUI indicator display
- No API/protocol changes; no new dependencies
