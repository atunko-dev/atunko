## Why

The core module already provided `RunConfigService` for saving and loading run
configurations (CORE_0007 / CORE_0008), and the Web UI exposed both via WEB_0001.11 /
WEB_0001.12. The TUI only had the save capability (TUI_0001.10) defined — the UI
implementation and the load workflow were missing, leaving TUI users without a way to
persist or restore their recipe selections.

## What Changes

- Add save workflow to `BrowserView`: press `S` to enter save-mode with an inline text
  input, Enter to confirm, Esc to cancel
- Add load workflow: press `L` to open a new `LOAD_CONFIG` screen (`LoadConfigView`)
  listing `atunko/runs/*.yml` files, navigate with j/k, confirm with Enter
- Extend `TuiController` with save/load state machine methods:
  `saveRunConfig`, `loadRunConfig`, `listRunConfigs`, `openLoadConfig`,
  `confirmLoadConfig`, `enterSaveConfigMode`, `exitSaveConfigMode`,
  `setSaveConfigName`, `confirmSaveConfig`
- New `LoadConfigView` screen
- Update `HelpOverlay` BROWSER section to document `S` and `L` keys

## Capabilities

### New Capabilities

- `tui-load-run-config`: Browse and load saved run configurations from the TUI (TUI_0001.19)

### Modified Capabilities

- `tui-save-run-config`: TUI save workflow implemented with inline text input (TUI_0001.10)

## Impact

- `atunko-tui`: `TuiController.java` (+93 lines), `BrowserView.java` (+47 lines),
  `HelpOverlay.java` (+2 lines), `Screen.java` (+1 enum value), `AtunkoTui.java` (+2 lines),
  `LoadConfigView.java` (new, 86 lines), `TuiControllerTest.java` (+181 lines of new tests)
- Reuses `RunConfigService` from `atunko-core` — no core changes needed
