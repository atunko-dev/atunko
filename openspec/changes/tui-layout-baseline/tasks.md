# Tasks

## 1. View contract and shell

- [x] 1.1 Write `TuiShellTest.everyViewSuppliesTitleStatusHintsAndHelp` — reflectively asserts
      every `TuiView` implementation supplies all four, and that no view builds its own
      `dock()` frame [SVC_TUI_0009]
- [x] 1.2 Add `TuiView` interface — `title()`, `status()`, `keyHints()`, `helpSections()`,
      `renderContent(TuiController)`
- [x] 1.3 Add `TuiShell.render(TuiView, TuiController)` composing the shared frame:
      header `length(3)`, content `fill()`, details `percentage(30)`/`length(0)`,
      footer `length(2)`
- [x] 1.4 Add `@Requirements({"atunko:TUI_0009"})` to `TuiView`/`TuiShell` and
      `@SVCs({"SVC_TUI_0009"})` to the test from 1.1

## 2. Frame geometry

- [x] 2.1 Write `AtunkoTuiFramePilotTest.frameGeometryIsIdenticalAcrossScreens` — header and
      footer occupy the same rows and content starts at the same row after navigating
      [SVC_TUI_0009.1]
- [x] 2.2 Migrate `BrowserView` onto `TuiView`/`TuiShell` as the reference implementation
- [x] 2.3 Add `@Requirements({"atunko:TUI_0009.1"})` to the shell's frame construction and
      `@SVCs({"SVC_TUI_0009.1"})` to the test from 2.1

## 3. Status bar

- [x] 3.1 Write `AtunkoTuiFooterPilotTest` — state and hints on separate rows, keys styled
      distinctly from labels, hints change with mode [SVC_TUI_0009.2]
- [x] 3.2 Implement the two-row footer in `TuiShell`, rendering `keyHints()` as
      key/label span pairs and selecting the hint set from the active mode
- [x] 3.3 Add `@Requirements({"atunko:TUI_0009.2"})` to the footer renderer and
      `@SVCs({"SVC_TUI_0009.2"})` to the test from 3.1

## 4. Binding registry

- [x] 4.1 Write `BindingsTest` — every registry entry has a description; rendering the TUI
      after changing a description updates both the footer hint and the help entry; no
      screen matches a key outside the registry [SVC_TUI_0009.6]
- [x] 4.2 Add `Bindings` built on `BindingSets.standard().toBuilder()` with atunko actions,
      each carrying trigger, action and description
- [x] 4.3 Install the registry via `ToolkitRunner.Builder.bindings(...)` and replace the 65
      `isChar(...)` matches with `event.matches(ACTION)` across the views
- [x] 4.4 Remove the `j`/`k` vim bindings (14 `isChar('j')`/`isChar('k')` call sites)
- [x] 4.5 Add `@Requirements({"atunko:TUI_0009.6"})` to `Bindings` and
      `@SVCs({"SVC_TUI_0009.6"})` to the test from 4.1

## 5. Overlays

- [ ] 5.1 Write `AtunkoTuiOverlayPilotTest` — underlying content still present behind the
      overlay, footer shows the overlay's hints, and the dismissing key also performs its
      action on the screen beneath [SVC_TUI_0009.4]
- [ ] 5.2 Render help and diff as overlays over the content region instead of replacing it,
      collapsing the details region while active
- [ ] 5.3 Stop swallowing the dismissing keystroke in `BrowserView.handleKeyEvent`
- [ ] 5.4 Add `@Requirements({"atunko:TUI_0009.4"})` to the overlay renderer and
      `@SVCs({"SVC_TUI_0009.4"})` to the test from 5.1

- [ ] 5.5 Write `AtunkoTuiHelpPilotTest.helpOpensOnEveryScreen` — parameterised over every
      screen [SVC_TUI_0009.7]
- [ ] 5.6 Route help through `TuiShell` so it is available wherever a `TuiView` renders, and
      populate it from the opening screen's `helpSections()`
- [ ] 5.7 Add `@Requirements({"atunko:TUI_0009.7"})` and `@SVCs({"SVC_TUI_0009.7"})`

## 6. Focus traversal

- [ ] 6.1 Write `AtunkoTuiFocusPilotTest` — Tab moves focus to details and Shift-Tab back,
      focus is indicated, and an oversized details region scrolls while focused
      [SVC_TUI_0009.5]
- [ ] 6.2 Let views declare focusable regions by id; implement Tab/Shift-Tab traversal and
      the focus indicator in `TuiShell`
- [ ] 6.3 Make the details region scrollable
- [ ] 6.4 Add a focus rule to both `.tcss` themes
- [ ] 6.5 Add `@Requirements({"atunko:TUI_0009.5"})` to the traversal implementation and
      `@SVCs({"SVC_TUI_0009.5"})` to the test from 6.1

## 7. Tabs

- [ ] 7.1 Write `AtunkoTuiTabsPilotTest` — tabs name screens, active screen indicated, live
      counts present, sort shown as a status indicator [SVC_TUI_0009.3]
- [ ] 7.2 Replace `tabs(SortOrder…)` in the header with screen tabs carrying live counts
- [ ] 7.3 Move sort order to the status row beside `src:` and `fav:`
- [ ] 7.4 Add `@Requirements({"atunko:TUI_0009.3"})` and `@SVCs({"SVC_TUI_0009.3"})`

## 8. Migrate remaining views

- [ ] 8.1 Migrate `DetailView` onto the contract (drops its computed header height)
- [ ] 8.2 Migrate `ConfirmRunView`
- [ ] 8.3 Migrate `ExecutionResultsView`
- [ ] 8.4 Migrate `TagBrowserView`
- [ ] 8.5 Migrate `RecipeOptionsView`
- [ ] 8.6 Migrate `LoadConfigView` and `ExportConfigView`
- [ ] 8.7 Migrate `FileDiffView`, rendering the diff as an overlay rather than a screen
- [ ] 8.8 Resolve the open question — details pane bottom vs right-hand — by comparing
      Pilot-harness screenshots of both, and record the choice in `design.md`

## 9. Docs

- [ ] 9.1 Regenerate the key tables in `README.md` from the binding registry, fixing the
      existing drift (`c` documented but unbound; `a` documented as cycle)
- [ ] 9.2 Update `docs/antora/modules/ROOT/pages/tui.adoc` — frame, tabs-as-navigation,
      focus traversal, help everywhere, and the removal of `j`/`k`
- [ ] 9.3 Add a test asserting the documented key tables match the registry, so they cannot
      drift again

## 10. Wrap-up

- [ ] 10.1 `./gradlew spotlessApply build` green
- [ ] 10.2 `openspec validate --all --strict` passes
- [ ] 10.3 `reqstool status local -p docs/reqstool` — TUI_0009 family covered
- [ ] 10.4 PR referencing #87
