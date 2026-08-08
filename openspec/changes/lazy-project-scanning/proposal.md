## Why

`TuiCommand.run()` and `WebUiCommand.run()` call `ProjectScannerFactory.detect(dir).scan(dir)`
synchronously at startup (issue #44). The scan connects to the Gradle Tooling API / invokes
Maven, which can take several seconds when no daemon is running — blocking the UI from
launching even though the project model is not needed until a recipe actually executes.

## What Changes

- `--project-dir` sessions initialise lazily: startup records the project directory only;
  the build-system scan runs on first recipe execution (memoized, thread-safe).
- Scan errors move from startup to execution time and are surfaced in the TUI and Web UI
  at the point of execution instead of killing the process at launch.
- Pre-scan UI state stays honest: recipe applicability badges already seed from
  `SourceCapabilityHints` (no scan needed); project-dependent info displays the directory
  path rather than scanned metadata until the first run.
- `--workspace` sessions keep eager scanning (out of scope, see design non-goals).

## Capabilities

### New Capabilities

- `lazy-project-scanning`: lazy session initialisation in core with scan-at-first-execution
  and execution-time error surfacing in TUI and Web UI (planned reqstool IDs: CORE_0017
  + sub-requirements, TUI_0005, WEB_0004).

### Modified Capabilities

<!-- none — startup flags, recipe browsing, and execution semantics are unchanged;
     only the moment the scan happens changes, which is new behaviour under the new
     capability rather than a change to existing requirements -->

## Impact

- `atunko-core`: `SessionHolder` gains lazy single-project initialisation
  (`initLazy(Path)` + memoized `ensureScanned()`); `AppServices` untouched.
- `atunko-tui`: `TuiCommand` stops scanning at startup; `TuiController` triggers the
  scan on the run path and surfaces scan failures in the UI; Pilot e2e coverage.
- `atunko-web`: `WebUiCommand` stops scanning at startup; execution path triggers the
  scan in its existing background thread and reports failures via notification/dialog.
- Stacked on `feat/recipe-applicability-stage2` (PR #76) — reuses `SourceCapabilityHints`
  for pre-scan badges and the `parseWithCapabilities` run path.
- reqstool: new requirements CORE_0017, TUI_0005, WEB_0004 with SVCs.
