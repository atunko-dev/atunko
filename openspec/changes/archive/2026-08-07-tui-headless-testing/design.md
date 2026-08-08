## Context

TamboUI 0.3.0 publishes Pilot testing as Gradle test fixtures (verified against Maven
Central and the `v0.3.0` tag of tamboui/tamboui):

- `dev.tamboui.toolkit.app.ToolkitTestRunner` (toolkit fixtures) —
  `runTest(Supplier<Element>)` / `runTest(supplier, Size)` starts the app on a daemon
  thread against a headless `TestBackend` with a test-tuned `TuiConfig`
  (`rawMode(false)`, `alternateScreen(false)`, `mouseCapture(true)`, `noTick()`,
  10 ms poll). Exposes `pilot()`, `backend()`, `runner()`; `Closeable`.
- `dev.tamboui.toolkit.app.ToolkitPilot` (toolkit fixtures) — `press(char)`,
  `press(KeyCode)`, `press(String...)`, `click(x, y)`, `click("element-id")`,
  `mousePress/mouseRelease/mouseMove`, `resize(w, h)`, `pause()`, `quit()`,
  `findElement("id")`, `hasElement("id")`. By-ID lookup uses the ElementRegistry
  populated by the `.id("...")` calls the views already make.
- `dev.tamboui.terminal.TestBackend` (core fixtures) — the headless `Backend`
  implementation the runners render against. Note: its `draw()` is a no-op, so it
  records no frame content (see Implementation findings).
- `dev.tamboui.tui.pilot.Pilot` / `TestRunner` interfaces (tui fixtures).

Dependency metadata detail: the toolkit `testFixturesApiElements` variant exposes only the
toolkit fixtures; the tui/core fixtures arrive via `testFixturesRuntimeElements` only.
Tests compile against `Pilot` and `TestBackend`, so all three fixtures must be declared
as `testImplementation`.

## Goals / Non-Goals

**Goals:**

- Full closed-loop TUI verification: launch headlessly, drive with synthetic key/mouse
  events, assert on controller state and rendered output — runnable in CI and by agents
  via `./gradlew :atunko-tui:test`.
- Zero main-source changes; zero new production dependencies.
- Cover the primary user journeys end-to-end (browse, navigate, search, select, confirm
  run, help overlay, mouse).

**Non-Goals:**

- Recipe execution inside Pilot tests — execution is core's responsibility and already
  covered by `atunko-core` integration tests; e2e tests stop at the CONFIRM_RUN screen.
- Color/style assertions — frames are rendered with the production dark theme, but
  `screen()` extracts cell symbols only; assertions target text content and layout
  presence, not colors.
- Snapshot/golden-frame testing — start with substring/state assertions; goldens can be
  layered on `screen()` later if drift-detection is wanted.

## Implementation findings (verified against 0.3.0 behaviour)

Three facts discovered while implementing changed the design from the original sketch:

1. **`TestBackend.draw(DiffResult)` is a no-op in 0.3.0 (and still in 0.4.0).** Frame
   content never reaches the backend recorder; `rawOutput()` only captures `writeRaw`,
   which the diff renderer does not use. Frame-content assertions through `TestBackend`
   are therefore impossible — the harness captures frames itself via a
   `ToolkitPostRenderProcessor` instead (see Decision 1).
2. **`EventRouter` routes only left-button presses to mouse handlers.** Right-click
   press events are dropped before reaching any view handler, so the browser's
   right-click-to-select binding is unreachable through the live pipeline (a latent
   app-level finding — the mouse-support unit tests call the handler directly and
   don't see this). Pilot tests cover scroll + left-click; right-click stays
   handler-level only.
3. **The first render is asynchronous.** `ToolkitTestRunner`'s fixed 50 ms startup
   grace is not enough on a cold JVM; events dispatched before the first render are
   mis-routed (e.g. arrow keys consumed by the unfocused `ListElement`). The harness
   polls for the first captured frame + registered root element (5 s deadline) before
   returning.

## Decisions

### 1. Custom harness on `ToolkitRunner.builder()`, not `ToolkitTestRunner`

`ToolkitTestRunner.runTest(...)` cannot carry a `StyleEngine` or post-render
processors. The `PilotTestSupport` harness instead replicates its test-tuned
`TuiConfig` (headless `TestBackend`, `rawMode(false)`, `noTick()`, 10 ms poll,
`mouseCapture(true)`) and builds the runner directly:

- `ToolkitRunner.builder().config(...).styleEngine(...).postRenderProcessor(...)` —
  rendering uses the production dark TCSS theme (the "unstyled tests" non-goal from the
  original sketch turned out to be avoidable);
- a `FrameCapture` post-render processor snapshots each frame's `Buffer` as plain text,
  exposed as `screen()` — per-frame content, so *absence* assertions work (impossible
  with a cumulative raw-output stream);
- `new ToolkitPilot(runner, backend)` provides the standard Pilot driving API;
- the runner runs `app::render` from a real `AtunkoTui` on a daemon thread, so the
  exact production screen-switching logic is under test. `render()` is `protected`;
  the test classes live in `io.github.atunkodev.tui`, so same-package access applies —
  no visibility widening.

The three test-fixtures dependencies are still required: `ToolkitPilot` (toolkit),
`Pilot` (tui), and `TestBackend` (core).

### 2. Stub recipe catalog, real controller

`new TuiController(recipes)` with the same in-memory `RecipeInfo` fixtures pattern used
by `TuiControllerTest` (fast, deterministic, no OpenRewrite environment scan). A shared
`PilotTestSupport` helper builds controller + app + runner to avoid per-test boilerplate:

```java
try (PilotTestSupport tui = PilotTestSupport.launch()) {
    tui.pilot().press(KeyCode.DOWN);
    assertThat(tui.controller().highlightedIndex()).isEqualTo(1);
    assertThat(tui.screen()).contains("Beta Recipe");
}
```

### 3. Assertion strategy: state first, frames second

Primary assertions on `TuiController` observable state (screen, highlight, selection,
search results) — stable across layout tweaks. Secondary `screen().contains(...)` /
`doesNotContain(...)` assertions prove the pipeline actually rendered the state (the
gap the unit tests cannot see). Pilot interactions already pause ~50 ms after each
dispatched event, letting the render loop process it before asserting.

### 4. Terminal size 120×40

Default 80×24 truncates recipe names in the browser table; 120×40 matches the
development terminal baseline and keeps `contains(...)` assertions reliable. One test
exercises `pilot.resize(...)` explicitly.

### 5. Mouse events go through the real event pipeline

`pilot.click(x, y)` and dispatched scroll events complement the hand-constructed
`MouseEvent` objects used in `BrowserViewMouseTest` — verifying capture, dispatch, and
the y-coordinate mapping end-to-end (the harness config sets `mouseCapture(true)`).
`Pilot` has no scroll method, so the harness exposes `dispatch(Event)` for scroll
events. Right-click stays handler-level only: TamboUI's `EventRouter` never routes
right-button presses (see Implementation findings).

### 6. Traceability: new `TUI_0003` requirement family

Headless verifiability is a system property worth tracking (it was previously recorded
as impossible). One new requirement `TUI_0003` with one SVC per e2e journey
(`SVC_TUI_0003` … `SVC_TUI_0003.5`); test methods annotated `@SVCs` accordingly.
Existing `TUI_0001.x` requirements/SVCs are untouched — the Pilot tests additionally
verify several of them end-to-end, but re-mapping SVC verification methods is deferred
to keep this change focused.

### 7. Flakiness guard

The runner thread is a daemon; `launch()` blocks until the first frame is captured and
the root element is registered (5 s deadline) — a fixed startup grace proved
insufficient on a cold JVM. Each pilot interaction pauses ~50 ms after dispatch. If CI
shows timing flakes, `pause(Duration)` is available as an escape hatch.
