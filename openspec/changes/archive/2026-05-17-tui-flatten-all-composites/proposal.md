## Why

The TUI run dialog already supported flattening a single highlighted composite recipe
(press `f`). Users with multiple composites in their run order had to flatten them one
by one, which was tedious. A "flatten all" operation covers the common case of
wanting fully-resolved leaf recipes before executing.

## What Changes

- Add `flattenAllRunRecipes()` to `TuiController`: iterates the run order until no
  composites remain, deduplicating shared leaves via `LinkedHashSet`
- Bind the `F` (uppercase) key in `ConfirmRunView` to trigger flatten-all
- Update `HelpOverlay` CONFIRM_RUN section to document `f:flatten F:flatten-all`

## Capabilities

### New Capabilities

- `tui-flatten-all-composites`: Single-keystroke flatten of all composites in the run
  order, with shared-leaf deduplication (TUI_0001.14.1)

### Modified Capabilities

- `tui-launch`: The run dialog footer hint text and help overlay are updated to document
  both flatten modes

## Impact

- `atunko-tui`: `TuiController.java` (+51 lines), `ConfirmRunView.java` (+8 lines),
  `HelpOverlay.java` (+15 lines), `TuiControllerTest.java` (+100 lines of new tests)
- No breaking changes — the existing `f` flatten-one behaviour is unchanged
