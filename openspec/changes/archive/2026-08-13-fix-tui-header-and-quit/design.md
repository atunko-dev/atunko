## Context

The shell refactor (#87) moved title, status, key hints and help onto one view contract and gave every screen the same
frame. Two things did not survive the move:

- `BrowserView.renderHeaderExtras` still returns its pre-shell header — title label included — which the shell then
  draws *after* its own title. The header band therefore reads `atunko … tabs … atunko … search box`, and because the
  band is three rows tall while the title and tab bar are one row each, the `.screen-title` background paints a
  three-row block and the rounded search input is the only thing that fills the band.
- `AtunkoTui.requestQuit()` calls the inherited `ToolkitApp.quit()`, which forwards to a **private** `runner` field that
  only `ToolkitApp.run()` assigns. `AtunkoTui` overrides `run()` — it has to, in order to pass the TCSS `StyleEngine` to
  `ToolkitRunner.builder()` — so the field is never set and `quit()` returns without doing anything. `ToolkitApp`
  exposes a `protected ToolkitRunner runner()` getter but no setter, so the subclass cannot fill the base field.

The headless `PilotTestSupport` harness builds its own runner and passes `app::render` too, so it reproduces the quit
bug rather than catching it.

## Goals / Non-Goals

**Goals:**

- The screen title appears exactly once in the header, on every screen.
- The header band reads as one aligned row: title badge, tab bar, and the screen's own header control.
- `q` quits from any screen, in the real app and under the Pilot harness.
- Regression coverage for both, through the existing headless harness.

**Non-Goals:**

- Changing `TuiShell.HEADER_HEIGHT`, the footer, or the content/details geometry — TUI_0009.1 stands as is.
- Wiring `AtunkoBindings.bindings()` into `ToolkitRunner` (the registry is still hint/help-only). Key handling stays
  where it is; that is a separate concern from this fix.
- Reworking search behaviour, only how the search field is drawn.

## Decisions

**Header: one bordered band instead of loose elements in a 3-row strip.**
The header row (title badge, tab bar, header extras) is wrapped in a rounded panel, which is exactly three rows —
border, content, border — so it fills `HEADER_HEIGHT` by construction and its single content row aligns everything on
it. It also matches the rounded cyan `Recipes`/`Detail` panels below, so the frame reads as one design.

Alternatives considered:

- *Vertically centre the title/tab row against the 3-row rounded search box* — keeps the search box prominent, but
  leaves the header as a floating box on an otherwise empty band, and the `.screen-title` block still needs a
  height-limiting wrapper.
- *Shrink the header to one row and drop the search field into the status area* — most compact, but changes
  `HEADER_HEIGHT` and the frame geometry that TUI_0009.1 pins, and hides the search box that TUI_0001.3 relies on.

Consequence: header text inputs must be single-line, so `BrowserView`'s search and save-config inputs and
`TagBrowserView`'s tag filter drop `.rounded()`. They keep their placeholders, which is what identifies them.

**Quit: `AtunkoTui` keeps its own reference to the runner it creates.**
`requestQuit()` calls `quit()` on that reference. The alternative — dropping the `run()` override so the base class
manages the runner — is not available, because the base builds a runner without a `StyleEngine` and the whole TUI is
themed through TCSS.

So that the same path is exercised headlessly, the reference is settable: `PilotTestSupport` binds the runner it built
before starting the render loop. Without that, a `q`-quits test would assert on a code path the real app does not use.

## Risks / Trade-offs

- **The unbordered inputs are less obviously "fields"** → they keep placeholder text (`Search recipes...`,
  `Filter tags...`), and the browser's search field keeps its always-visible cursor, so the affordance survives.
- **Pilot tests assert on exact header rows and will need updating** → intended; the assertions are the regression
  tests for this change, and `AtunkoTuiFramePilotTest` already indexes the header by `TuiShell.HEADER_HEIGHT` rather
  than hard-coded rows.
- **A settable runner is a seam that only tests use** → it is the same seam `ToolkitApp` already has (its own `run()`
  assigns the private field); making it visible is what lets the quit path be tested at all.
