## 1. Requirements & SVCs

- [x] 1.1 Add `TUI_0003` requirement to `docs/reqstool/requirements.yml` (TUI is
  launchable against a headless terminal backend and drivable programmatically for
  automated end-to-end verification)
- [x] 1.2 Add SVCs to `docs/reqstool/software_verification_cases.yml`:
  `SVC_TUI_0003` (headless launch renders browser view), `SVC_TUI_0003.1` (keyboard
  navigation), `SVC_TUI_0003.2` (search filtering), `SVC_TUI_0003.3` (selection →
  confirm-run flow), `SVC_TUI_0003.4` (mouse scroll/click via event pipeline),
  `SVC_TUI_0003.5` (help overlay + resize)

## 2. Build wiring

- [x] 2.1 Add to `atunko-tui/build.gradle`:
  `testImplementation testFixtures(libs.tamboui.toolkit)`,
  `testImplementation testFixtures(libs.tamboui.tui)`,
  `testImplementation testFixtures(libs.tamboui.core)` — versions via the existing
  `tamboui-bom` platform; `tamboui-core`/`tamboui-tui` catalog entries added to
  `gradle/libs.versions.toml`
- [x] 2.2 Verify resolution: `:atunko-tui:compileTestJava` compiles against
  `ToolkitPilot`, `Pilot`, and `TestBackend`

## 3. Test harness

- [x] 3.1 Create `PilotTestSupport` in `atunko-tui/src/test/java/io/github/atunkodev/tui/`
  — builds stub `RecipeInfo` fixtures, `TuiController`, `AtunkoTui`; runs `app::render`
  headlessly via `ToolkitRunner.builder()` with `TestBackend` (120×40), the production
  dark TCSS theme, and a `FrameCapture` post-render processor; exposes `controller()`,
  `pilot()`, `screen()`, and `dispatch(Event)`; `launch()` waits for the first rendered
  frame

## 4. End-to-end Pilot tests (`AtunkoTuiPilotTest`)

- [x] 4.1 Headless launch: browser view renders; `screen()` contains stub recipe
  names, detail panel, and status bar — `@SVCs({"atunko:SVC_TUI_0003"})`
- [x] 4.2 Keyboard navigation: DOWN/j/k/UP move highlight; ENTER opens DETAIL (with
  rendered detail content); ESC returns to BROWSER — `@SVCs({"atunko:SVC_TUI_0003.1"})`
- [x] 4.3 Search: press `/` (header shows SEARCH), type a query key by key, list
  filters live (filtered-out recipe absent from frame); ENTER confirms; ESC clears —
  `@SVCs({"atunko:SVC_TUI_0003.2"})`
- [x] 4.4 Selection flow: SPACE toggles selection (rendered `[x]` marker + status-bar
  count), `r` reaches CONFIRM_RUN with run order rendered; ESC backs out without
  executing — `@SVCs({"atunko:SVC_TUI_0003.3"})`
- [x] 4.5 Mouse: scroll events move highlight both directions; `click(x, y)` on a row
  highlights it and the detail panel follows — through the real event pipeline.
  Right-click stays handler-level only (`BrowserViewMouseTest`): TamboUI's EventRouter
  never routes right-button presses — `@SVCs({"atunko:SVC_TUI_0003.4"})`
- [x] 4.6 Help overlay: `?` shows overlay content in the frame, ESC dismisses it
  (content absent again); `resize(80, 24)` keeps the TUI responsive —
  `@SVCs({"atunko:SVC_TUI_0003.5"})`

## 5. Quality

- [x] 5.1 Run `./gradlew spotlessApply` and fix any formatting issues
- [x] 5.2 Run `./gradlew build` — no Checkstyle or Error Prone violations
- [x] 5.3 Run `./gradlew :atunko-tui:test` — all tests (existing + new) pass headlessly
