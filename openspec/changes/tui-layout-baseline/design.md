## Context

atunko's TUI is built on `tamboui-toolkit`'s declarative DSL: each screen is a `public final class` with a private constructor and a static `render(...)` returning an `Element`, composed as `column(dock().top(...).center(...).bottom(...))`. `TuiController` (1521 lines) holds all screen state and all key dispatch.

Nothing connects those 11 classes. There is no base type, no interface, no registry — so "every screen has a header, a status bar and reachable help" is a convention held only by whoever wrote each screen, and it has already failed in measurable ways (header heights 3/1/computed, help on 3 of 10 screens, `README.md:106` documenting an unbound key).

Two TamboUI consumers were read as prior art. **Maven Pilot** (`abstract class ToolPanel`, 8 subclasses) and **Apache Camel's TUI** (`interface MonitorTab`) both solve this with a per-view contract that *includes the view's own hints and help*. Both compose at the `tamboui-tui` level (`Layout`/`Frame`) rather than the Toolkit DSL, so their layout code does not port — but the contract shape, the frame geometry and the footer treatment are API-independent and do.

Constraint: `atunko-tui` may depend on `atunko-core` but must not move UI logic into it. `BindingSets`, `Actions` and `KeyTrigger` are already on the classpath via `tamboui-toolkit` → `tamboui-tui`, verified in `tamboui-tui-0.4.0.jar`.

## Goals / Non-Goals

**Goals:**

- One place that decides what a screen looks like, so consistency is structural rather than remembered.
- One place that declares what a key does, so the footer, the help screen and the docs cannot disagree with the code.
- Focus that works: `Tab`/`Shift-Tab` cycling between panes, with visible indication.
- Overlays that overlay, and that do not eat the keystroke dismissing them.
- No new dependency; no change to what any screen actually *does*.

**Non-Goals:**

- vim/emacs bindings. Explicitly excluded by the owner. `j`/`k` are removed, not preserved behind a flag.
- Moving to the low-level `Layout`/`Frame` API. atunko stays on the Toolkit DSL; Pilot is a design reference, not a code source.
- Redesigning screen *content* — which fields the detail pane shows, what the run dialog offers.
- Mouse behaviour beyond keeping what exists working.
- Theming/colour changes beyond the focus indicator the focus model requires.

## Decisions

### 1. An interface with a shell renderer, not an abstract base

`TuiView` declares `title()`, `status()`, `keyHints()`, `helpSections()` and `renderContent()`. A separate `TuiShell` renders the frame and calls into the view.

Pilot uses an abstract class, but its `ToolPanel` also owns event dispatch, overlay state and 873 lines of shared behaviour — it is a framework. atunko's views are currently *static* renderers, and an interface migrates them without forcing each one to become a stateful object first. Camel's `MonitorTab` is the closer precedent.

*Alternative — abstract base:* would let the shell inherit rather than compose, but it forces instantiation of every view and a bigger first step, and Java's single inheritance costs flexibility later for nothing gained here.

### 2. The frame is fixed, and the details pane is the only variable

```
header   length(3)     title, tabs with counts, contextual indicators
content  fill()        the view's own element
details  percentage(30) or length(0) when collapsed or an overlay is active
footer   length(2)     status row, key-hint row
```

Pilot's proportions (3 / fill / 25% / 3) are the starting point. atunko's footer is 2 rather than 3 rows: Pilot's third row is a blank spacer, which is a style choice rather than information.

Collapsing details to `length(0)` when an overlay is active is taken directly from Pilot — it is what lets an overlay use the full frame without a separate layout path.

### 3. Bindings live in one registry, and the footer, help and docs are generated from it

`Bindings` holds `BindingSets.standard()` extended with atunko actions; each entry carries a key trigger, an action and a **description**. `keyHints()` and `helpSections()` read that registry, and a test renders the docs' key tables from it.

This is the part with no precedent among the 15 TamboUI consumers surveyed — all of them hardcode footer strings, and the framework gap (tamboui#168) is open. It is borrowed from Textual and Bubble Tea's `help` bubble, and is therefore the riskiest item. It is scoped last so the rest lands regardless.

### 4. Tabs address screens; sort becomes a status indicator

`tabs(NAME, TAGS, RECENT)` currently renders a sort control with the tab affordance, which reads as navigation and is not. Tabs move to addressing screens, with live counts in labels as Pilot does (`Declared: 42 (3 unused)`). Sort moves to the status row as a labelled indicator (`sort:name`), joining the existing `src:` and `fav:` indicators.

*Alternative — leave sort on the tabs and add a second bar:* two tab-like bars in a 3-row header is worse than either alone.

### 5. Focus: the shell owns traversal

Views declare focusable regions by id; the shell installs `Tab`/`Shift-Tab` traversal and renders the focus indicator. Today every screen marks only the root column focusable, which is why `Tab` does nothing anywhere — that is a shell concern, not something each view should re-solve.

### 6. Migration is incremental behind the contract

The contract and shell land first with `BrowserView` migrated; remaining views migrate one per task. Each is independently verifiable with the existing Pilot harness, so a half-migrated tree still builds and runs.

## Risks / Trade-offs

- **Touches every screen; large diff** → the contract lands first and views migrate individually, so review is per-view and a partial tree is always working.
- **Removing `j`/`k` breaks muscle memory for existing users** → owner's explicit decision; the removal is called out as breaking in the proposal and in the release notes. Note 4 of 15 surveyed consumers do ship vim keys, so this is a product choice, not a correctness one.
- **Generating docs from the registry is novel** (no TamboUI consumer does it; tamboui#168 open) → sequenced last, and the fallback is a test that *asserts* the hand-written tables match the registry rather than generating them.
- **Tabs-for-navigation changes a learned affordance** → the sort control does not disappear, it moves to the status row beside the filters it belongs with.
- **`TuiController` is 1521 lines and holds all dispatch** → this change moves key dispatch out but deliberately does not otherwise split the controller; doing both at once would make the diff unreviewable.
- **Pilot harness tests assert on rendered text** → frame geometry changes will churn existing snapshots; expected, and the churn is the evidence the frame actually changed.

## Migration Plan

No data or config migration. User-visible behaviour changes in three ways: `j`/`k` stop working, tabs address screens instead of sort order, and help is reachable everywhere. All three are documented in `README.md` and `docs/antora/.../tui.adoc`, which are regenerated from the binding registry.

Rollback is per-view: an unmigrated view still renders through its own `render()` until its migration task lands.

## Open Questions

None blocking. One to settle during implementation: whether the details pane should be bottom (Pilot) or remain right-hand (atunko today) in the browser. The frame supports either; bottom matches Pilot and gives long descriptions more width, right-hand preserves the current list/detail reading order. Resolve with a Pilot-harness screenshot of both before choosing.
