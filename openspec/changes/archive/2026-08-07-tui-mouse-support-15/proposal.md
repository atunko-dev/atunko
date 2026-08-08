## Why

Keyboard-only navigation is functional but slow for users who prefer pointing devices.
TamboUI 0.2.0-SNAPSHOT exposes a complete mouse-event model (`MouseEvent`, `MouseEventKind`,
`onMouseEvent()` on any `StyledElement`, and `TuiConfig.builder().mouseCapture(true)`).
Adding mouse support to the TUI allows users to scroll the recipe list, click to highlight
and select recipes, and scroll result/diff views — without changing any keyboard shortcuts.

## What Changes

- **TUI — AtunkoTui**: Enable `mouseCapture(true)` in `TuiConfig` so the terminal backend
  captures and routes mouse events.
- **TUI — Views**: Add `onMouseEvent()` handlers to the focusable root element of each
  view. Supported interactions per screen:
  - **BrowserView** (recipe list): scroll up/down moves the highlighted recipe; left-click
    on a row highlights that recipe; double-click (two rapid presses on same row) opens
    detail; right-click toggles selection.
  - **ConfirmRunView**: scroll moves run-list highlight; left-click highlights; right-click
    toggles selection.
  - **ExecutionResultsView**: scroll moves the file-list highlight.
  - **TagBrowserView**: scroll + click.
  - **LoadConfigView**: scroll + click.
  - **DetailView / FileDiffView / ExportConfigView / RecipeOptionsView**: scroll events
    passed through (no list to navigate, no-op or ignored).
- **TUI — TuiController**: Add `setHighlightByMouseY(int y, int listTopY, int rowHeight,
  int rowCount)` utility used by view mouse handlers to convert a terminal row coordinate
  into a list index.

## Capabilities

### New Capabilities

- `tui-mouse-support`: Enable mouse capture and handle scroll/click events in all TUI
  screens.

### Modified Capabilities

- `tui-launch`: `TuiConfig` gains `mouseCapture(true)`.

## Impact

- `atunko-tui`: `AtunkoTui.configure()` updated; all view files gain `onMouseEvent()`
  handlers on their root element.
- No new library dependencies — TamboUI 0.2.0-SNAPSHOT already ships the mouse API.
- No breaking changes to controller state API; all new controller helpers are additive.
