## Why

The TUI shipped after the shell refactor (#87) renders its header wrong and cannot be quit with `q`. Both are visible on
first launch: the word `atunko` appears twice in the header band, and pressing `q` does nothing, leaving `Ctrl-C` as the
only way out.

Both were reproduced headlessly through the existing Pilot harness:

- The shell draws `view.title(...)` (`TuiShell.renderHeader`) **and** `BrowserView.renderHeaderExtras` draws its own
  pre-shell title label — the label was never removed when the shell took ownership of the title. On top of the
  duplicate, nothing in the 3-row header lines up: the `.screen-title` background stretches the full header height while
  the tab bar text sits on the first row and the rounded search box occupies all three.
- `ToolkitApp.quit()` is a no-op unless the base class's private `runner` field is set, and only `ToolkitApp.run()` sets
  it. `AtunkoTui` overrides `run()` to build its own `ToolkitRunner` (it must, to pass the TCSS `StyleEngine`), so the
  field stays `null` and `requestQuit()` silently does nothing. The headless harness has the same gap, which is why no
  test caught it.

## What Changes

- Remove the duplicate title from `BrowserView`'s header extras — the shell owns the title.
- Lay the header band out so title, tab bar and the screen's own header control share one aligned row: the header row is
  wrapped in a rounded panel matching the `Recipes`/`Detail` panels, with single-line (unbordered) text inputs inside it.
  `TagBrowserView`'s filter input and `BrowserView`'s save-config input drop their `.rounded()` accordingly.
- `AtunkoTui` holds the `ToolkitRunner` it creates so `requestQuit()` actually quits; the headless harness binds the
  runner the same way, so `q` is testable.
- Regression tests: the screen title appears exactly once in the header band, and `q` ends the run loop.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `tui-layout-baseline`: adds TUI_0009.8 — header composition (title rendered once, header controls aligned on one row
  within the fixed header height).
- `tui-launch`: adds TUI_0001.28 — the TUI exits on the quit key from any screen.

## Impact

- `atunko-tui`: `TuiShell`, `BrowserView`, `TagBrowserView`, `AtunkoTui`, `PilotTestSupport`, theme stylesheets if the
  header panel needs a class.
- `docs/reqstool/requirements.yml` and `software_verification_cases.yml`: two new sub-requirements and their SVCs.
- No CLI, core or user-facing flag changes; `docs/antora/` is untouched (no documented behaviour changes — `q:quit` is
  already documented, it simply did not work).
