# Proposal: Web UI Recipe Execution and Dry-Run Preview

## Why

The Web UI is currently a read-only recipe browser. Users need to be able to execute
selected recipes against a target project directly from the browser, including a dry-run
preview mode that shows the diff before applying changes to disk.

Issue: atunko-dev/atunko#34

## What Changes

- **AppServices** — new shared singleton in `atunko-core` holding `RecipeExecutionEngine`,
  `ProjectSourceParser`, and `ChangeApplier`; initialised at startup by both TUI and Web UI
- **WebUiCommand** — extended with `engine`, `sourceParser`, and `changeApplier` constructor
  params; calls `AppServices.init()` in `run()` after `SessionHolder.init()`
- **RecipeBrowserView** — adds Dry Run and Execute buttons; `runRecipes(dryRun)` method
  orchestrates parsing → execution → result dialog
- **ExecutionResultDialog** — inline dialog showing per-file changes from `ExecutionResult`

## Capabilities

- New: `web-recipe-execution` — Dry Run and Execute for selected recipes (WEB_0001.9)
- New: `web-execution-services` — AppServices shared singleton (WEB_0001.8)
