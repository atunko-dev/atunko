## Why

The TUI is the primary interface of atunko, yet it is the only part of the system without
closed-loop automated verification: `TuiControllerTest` and the view tests cover state
transitions and handler logic, but nothing exercises the real render/event pipeline —
key routing, focus, element IDs, mouse dispatch, and what actually appears on screen.
The archived `tui-default-cli-7` change even recorded "TUI cannot be launched headlessly"
as a testing limitation.

TamboUI 0.3.0 (the version already pinned in `gradle/libs.versions.toml`) ships headless
Pilot testing as published test fixtures: an in-memory `TestBackend` the app renders
against, and `ToolkitPilot` to drive it (`press`, `click`, `resize`, element lookup by
ID). All three fixture artifacts (`tamboui-toolkit`, `tamboui-tui`, `tamboui-core`) are
published on Maven Central for 0.3.0.

## What Changes

- **`atunko-tui/build.gradle`**: add `testImplementation(testFixtures(...))` for
  `dev.tamboui:tamboui-toolkit`, `dev.tamboui:tamboui-tui`, and `dev.tamboui:tamboui-core`
  (versions from the existing TamboUI BOM). The tui/core fixtures must be declared
  explicitly — the toolkit fixtures pull them at runtime only, but tests compile against
  `Pilot` and `TestBackend` types that live in them.
- **New test harness** `PilotTestSupport` (test sources only): builds a `TuiController`
  with stub recipes (same fixtures as `TuiControllerTest`), wraps it in `AtunkoTui`, and
  runs `app::render` on a headless `TestBackend` via `ToolkitRunner.builder()` — with the
  production TCSS style engine and a post-render processor that captures each frame's
  text for `screen()` assertions (TamboUI's own `ToolkitTestRunner` records no frame
  content and cannot carry a style engine — see design.md).
- **New end-to-end tests** in `atunko-tui/src/test`: headless Pilot tests covering
  launch/first render, keyboard navigation, search filtering, selection → confirm-run
  flow, help overlay, and mouse scroll/click through the real event pipeline.
- **`docs/reqstool`**: new requirement `TUI_0003` (TUI is verifiable headlessly) with SVCs
  `SVC_TUI_0003` … `SVC_TUI_0003.5`; tests annotated with `@SVCs`.
- **No main-source changes**: `AtunkoTui.render()` and `configure()` are `protected` and
  the tests live in the same package (`io.github.atunkodev.tui`), so no API is widened.

## Capabilities

### New Capabilities

- `tui-headless-testing`: The TUI can be launched against a headless virtual-terminal
  backend and driven programmatically (keys, mouse, resize) with assertions on both
  controller state and rendered frame content.

## Impact

- `atunko-tui`: `build.gradle` (three test-fixtures dependencies), new test classes only
- `docs/reqstool/requirements.yml`: new `TUI_0003`
- `docs/reqstool/software_verification_cases.yml`: new `SVC_TUI_0003` family
- No changes to `atunko-core`, `atunko-cli`, `atunko-web`, or any main sources
- Existing `TuiControllerTest` / view tests are untouched; Pilot tests are an additional
  end-to-end layer above them
