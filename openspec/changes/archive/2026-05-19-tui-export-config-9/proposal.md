## Why

Users can already export run configurations as Maven/Gradle plugin snippets from the Web UI and CLI, but the TUI has no export path — closing that gap means TUI-only users can copy a ready-to-paste build-file snippet without switching tools.

## What Changes

- Add a **TUI export overlay** that displays a Maven or Gradle snippet for the current run configuration, with a format toggle (Gradle/Maven) and a mode toggle (Minimal/Full).
- Wire an `e` keybinding on the **ConfirmRunView** (pre-run confirmation screen) to open the overlay — the `RunConfig` is fully assembled there.
- The overlay reads from `ConfigExportService` (CORE_0009) already in `atunko-core`; no new core logic is required.

## Capabilities

### New Capabilities

- `tui-config-export`: Export the current run configuration to a Maven or Gradle plugin snippet from within the TUI, via a full-screen overlay launched from the confirm-run screen.

### Modified Capabilities

_(none — new sub-requirements under TUI_0001 cover the confirm-run keymap extension)_

## Impact

- **atunko-tui**: new `ExportConfigView.java`; minor changes to `ConfirmRunView.java` (key handler + help text)
- **atunko-core**: no changes — `ConfigExportService` and `RunConfig` are already sufficient
- **Dependencies**: no new dependencies
- **reqstool**: new TUI sub-requirements under `TUI_0001` referencing `CORE_0009`
