## 1. reqstool (SSOT first)

- [x] 1.1 Add requirement TUI_0009.8 (TUI — Header Composition: the header band presents the screen title exactly once,
      alongside the tab bar and the screen's own header control, aligned on a single row within the fixed header height)
      to `docs/reqstool/requirements.yml`, referencing TUI_0009
- [x] 1.2 Add requirement TUI_0001.28 (TUI — Exit: the TUI exits when the quit key is pressed on any screen) to
      `docs/reqstool/requirements.yml`, referencing TUI_0001
- [x] 1.3 Add SVC_TUI_0009.8 and SVC_TUI_0001.28 to `docs/reqstool/software_verification_cases.yml` with GIVEN/WHEN/THEN
      and `verification: automated-test`
- [x] 1.4 Run `reqstool generate-json local` (or the project's equivalent) to confirm both IDs resolve

## 2. Quit (TUI_0001.28 / SVC_TUI_0001.28)

- [x] 2.1 Write the failing Pilot test: launching the browser and pressing `q` ends the render loop
      (`AtunkoTuiPilotTest` or a new quit test), with `@SVCs({"atunko:SVC_TUI_0001.28"})` on the test method
- [x] 2.2 Give `AtunkoTui` a field for the `ToolkitRunner` it builds in `run()`, and make `requestQuit()` quit through it
- [x] 2.3 Add the seam the harness needs to bind an externally built runner, and use it from `PilotTestSupport` before
      the render thread starts, so the test exercises the same path as the real app
- [x] 2.4 Add `@Requirements({"atunko:TUI_0001.28"})` to `AtunkoTui.requestQuit()`
- [x] 2.5 Confirm 2.1 passes and that `PilotTestSupport.close()` still shuts the harness down cleanly

## 3. Header composition (TUI_0009.8 / SVC_TUI_0009.8)

- [x] 3.1 Write the failing Pilot test: the screen title appears exactly once in the header band (browser and tag
      browser), with `@SVCs({"atunko:SVC_TUI_0009.8"})` on the test method
- [x] 3.2 Remove the duplicate `screen-title` label (and the now-redundant search-mode variant) from
      `BrowserView.renderHeaderExtras`/`renderHeader`, leaving the tag indicator and the search input
- [x] 3.3 Wrap the header row in `TuiShell.renderHeader` in a rounded panel sized to `HEADER_HEIGHT`, with the title
      badge, tab bar and header extras on its single content row; add a theme class in `dark.tcss`/`light.tcss` if the
      panel needs one
- [x] 3.4 Drop `.rounded()` from the header text inputs so they fit one row: `BrowserView` search, `BrowserView`
      save-config name, `TagBrowserView` tag filter
- [x] 3.5 Add `@Requirements({"atunko:TUI_0009.8"})` to `TuiShell.renderHeader`
- [x] 3.6 Confirm 3.1 passes and check the rendered header rows for each screen through the Pilot harness

## 4. Regression sweep

- [x] 4.1 Update the existing header/frame Pilot assertions (`AtunkoTuiFramePilotTest`, `AtunkoTuiTabsPilotTest`,
      `AtunkoTuiOverlayPilotTest`, `TuiShellTest`) to the new header, keeping them indexed by `TuiShell.HEADER_HEIGHT`
- [x] 4.2 Verify search mode, save-config mode and the tag browser still render and accept input with unbordered fields
- [x] 4.3 Run `./gradlew spotlessApply` then `./gradlew build`
- [x] 4.4 Run `openspec validate --all --strict`
