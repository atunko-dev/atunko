## Why

When `atunko` is invoked without subcommands, it currently prints usage text. The CLI
(`list`, `search`, `run`) is implemented, but the primary interaction mode — an interactive
TUI for recipe browsing, selection, configuration, and execution — is missing. This is the
core differentiator from the existing `rewriteDiscover`/`rewriteRun` Gradle tasks: a rich,
interactive terminal experience (CLI_0001).

## What Changes

- Replace the default "print usage" behavior with TUI launch when no subcommand is given
- Add interactive recipe browser with scrollable table, detail panel, and search filtering
- Add recipe detail view, multi-select, sorting, and option configuration
- Add dry-run preview, recipe execution with results display, and run config save
- Add tag browser for filtering recipes by tag
- Add keyboard navigation and shortcuts for all interactions
- CLI subcommands (`list`, `search`, `run`) remain unchanged

## Capabilities

### New Capabilities
- `tui-launch`: Interactive TUI application — recipe browsing, search, selection,
  configuration, execution, tag browsing, and keyboard navigation
  (CLI_0001, CLI_0001.1–CLI_0001.12)

### Modified Capabilities
_(none — CLI subcommands are unchanged)_

## Impact

- **Code**: New `io.github.atunkodev.tui` package in `app` module with TUI controller,
  views, and screen management. `App.run()` changes from printing usage to launching TUI.
  `ServiceFactory` updated to wire TUI dependencies.
- **Dependencies**: TamboUI (`tamboui-toolkit`, `tamboui-jline3-backend`, `tamboui-picocli`)
  already on classpath — no new dependencies needed.
- **Reused services**: `RecipeDiscoveryService`, `RecipeExecutionEngine`, `ChangeApplier`,
  `JavaSourceParser`, `RunConfigService` from `core` module.
- **No breaking changes**: CLI subcommands continue to work exactly as before.
