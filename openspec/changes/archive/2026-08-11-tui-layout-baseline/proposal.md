## Why

The TUI reads as improvised, and issue #87 traces that to one structural cause: **there is no shared view contract.** atunko has 11 unrelated `public final class` views, each a private constructor plus a static `render()`. Nothing obliges a view to declare its title, its status, its key hints or its help, so nothing keeps them consistent — and they already are not:

- the header is `length(3)` in `BrowserView`, `length(1)` in every other view, and computed in `DetailView`, so the frame jumps on every navigation
- the status bar is one concatenated string in which no key is visually distinguishable from its label
- help is reachable from 3 of 10 screens, is not an overlay (it *replaces* the content), and swallows the keystroke that dismisses it
- `README.md` documents `c` for collapse, which is not bound in the browser — the docs have already drifted from the code

Both reference implementations solve this the same way, and both were read directly for this proposal. Maven Pilot has `abstract class ToolPanel` with eight subclasses, mandating `toolName()`, `status()`, `keyHints()` and `helpSections()`. Apache Camel's TUI has `interface MonitorTab`, mandating `description()`, `renderFooter(List<Span>)`, `getHelpText()`, `isOverlayActive()` and `isDetailFocused()`. In both, **help and key hints are required members of the view contract**, which is why theirs cannot drift.

## What Changes

- **A view contract** — every TUI screen declares its title, status, key hints and help sections, and is rendered inside one shared frame. This is the load-bearing change; the rest become cheap once it exists.
- **One frame for every screen** — fixed-height header, filling content, collapsible details pane, fixed-height footer. Ends the header jump.
- **A structured status bar** — a state row and a key-hint row, with each key emphasised against its label, and the hint set swapped by mode so it describes the mode the user is actually in.
- **Tabs become navigation.** Today `tabs(NAME, TAGS, RECENT)` spends the tab affordance on a *sort control* while atunko has 11 screens and no tab navigation at all. Tabs will address screens and carry live counts in their labels; sort moves to a labelled status-bar indicator.
- **Real overlays** for help and diff preview — drawn over the content rather than replacing it, with the footer swapping to the overlay's own hints, and the dismissing keystroke no longer swallowed.
- **A focusable, scrollable details pane**, so `Tab`/`Shift-Tab` finally cycle focus and long descriptions stop overflowing.
- **A central binding registry** built on `BindingSets.standard()`, replacing 65 hand-rolled `isChar(...)` calls, with the footer hints and the help screen generated from that one registry. **BREAKING (user-visible):** the `j`/`k` vim keys are removed, per the owner's constraint; arrows, `Tab`, `Enter`, `Esc` and letter mnemonics remain.

`BindingSets`, `Actions` and `KeyTrigger` are already on the runtime classpath transitively via `tamboui-toolkit` — verified in `tamboui-tui-0.4.0.jar` — so this adds no dependency.

## Capabilities

### New Capabilities

- `tui-layout-baseline`: a shared TUI view contract and frame — consistent header/content/details/footer geometry, a structured two-row status bar with emphasised and context-sensitive key hints, tab-based screen navigation with live counts, true overlays for help and diff, a focusable and scrollable details pane, and a single binding registry that also generates the footer hints and help (planned reqstool IDs: TUI_0009 + sub-requirements).

### Modified Capabilities

<!-- none — `tui-launch`, `tui-file-diff`, `tui-recipe-options`, `tui-cascade-selection` etc. keep their
     existing requirements. What a screen *does* is unchanged; this proposal changes how every screen is
     structured and how its keys are declared. -->

## Impact

- **`atunko-tui`**: a new view contract type plus a shared shell renderer; all 11 view classes migrate onto it. `TuiController` (1521 lines) loses its per-view key dispatch to the binding registry.
- **Tests**: the Pilot Toolkit tests are the model — the existing TamboUI Pilot harness (`AtunkoTui*PilotTest`) gains assertions on frame geometry, footer content and focus traversal.
- **Docs**: `README.md` and `docs/antora/modules/ROOT/pages/tui.adoc` key tables are regenerated from the binding registry's descriptions, which is what stops them drifting again.
- **No new dependencies.**
- **Risk**: this touches every screen. Landing it as one change is what makes the result consistent, but it means a large diff; the task list is therefore ordered so the contract and the shell land first and views migrate one at a time behind it.
