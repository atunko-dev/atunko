## Why

The TUI is the primary interaction mode, but some users prefer a browser-based UI — particularly
when running atunko on a remote machine or in an environment where terminal UIs are inconvenient.
A Web UI provides the same recipe browsing and search as the TUI, accessible via any browser,
with a familiar two-pane layout: recipe tree on the left, detail on the right (WEB_0001).

## What Changes

- Add new `atunko-web` Gradle module with Vaadin 24.7.4 + vaadin-boot 13.3 (no Spring Boot)
- Add `webui` subcommand (`atunko webui`) that starts an embedded Jetty server
- Implement recipe browser: hierarchical TreeGrid with expand/collapse for composite recipes,
  cascade-both-ways checkbox multi-select, text search, tag multi-select filter, detail panel,
  and status bar
- Wire `atunko-web` into CLI via `App.java` and `ServiceFactory`
- No changes to TUI, CLI subcommands, or core module

## Capabilities

### New Capabilities
- `web-ui-launch`: Browser-based recipe browser — launch embedded Jetty, display hierarchical
  recipe TreeGrid, expand/collapse composite recipes (recipeList), multi-select with
  cascade-both-ways checkboxes, text search, tag multi-select filter (OR/ANY), detail panel
  with description/tags/options/recipe list, configurable port via `--port`
  (WEB_0001, WEB_0001.1, WEB_0001.2, WEB_0001.3, WEB_0001.4, WEB_0001.5, WEB_0001.6)

### Modified Capabilities
_(none — TUI and CLI subcommands are unchanged)_

## Impact

- **New module**: `atunko-web/` — packages `io.github.atunkodev.web`, `io.github.atunkodev.web.view`
- **Dependencies**: Vaadin 24.7.4 (BOM) + vaadin-core, vaadin-boot 13.3, karibu-testing-v24 2.2.4 (test)
- **Reused services**: `RecipeDiscoveryService` from `atunko-core` — no new core changes
- **CLI wiring**: `WebUiCommand` registered as subcommand in `App.java`; `ServiceFactory` updated
- **Dependency graph**: `atunko-cli` → `atunko-web` → `atunko-core` (parallel to `atunko-tui`)
- **No breaking changes**: All existing subcommands and TUI continue to work unchanged

## Follow-up

- Cascade-both-ways checkbox selection to be backported to TUI (separate change)
- Recipe execution from Web UI (separate change)
- Run configuration management from Web UI (separate change)
