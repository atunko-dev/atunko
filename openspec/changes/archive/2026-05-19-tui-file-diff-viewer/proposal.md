## Why

After execution or dry-run, the TUI showed only a flat list of changed file paths with no way to inspect what actually changed in each file. Users had to leave the TUI and open files manually to understand the impact of a recipe run. This closes the gap with the web UI's `DiffDialog` and completes TUI parity for recipe execution results (tracker #33, issue #34).

## What Changes

- `ExecutionResultsView` becomes navigable: `j`/`k` highlights files in the changed-files list; `Enter` opens a diff view for the highlighted file
- New `FileDiffView` screen shows side-by-side before/after content using `markupTextArea` panels with line numbers and scrollbar (TamboUI 0.3.0 feature)
- `Screen.FILE_DIFF` enum value added; `Esc`/`q` in `FileDiffView` returns to the results list (not the browser)
- `TuiController` gains `selectedFileIndex`, `moveFileDown()`, `moveFileUp()`, `openFileDiff()`, `returnFromFileDiff()` — index resets to 0 on each new execution result
- `openFileDiff()` is a no-op when no changes exist (defensive guard)
- `@Requirements(TUI_0001.8/9)` added to all new public controller methods

## Capabilities

### New Capabilities

- `tui-file-diff`: Navigate the execution results file list and view per-file before/after diff panels in the TUI

### Modified Capabilities

- `recipe-execution`: `openFileDiff()` SHALL be a no-op when no changes exist (guard added; previously always navigated)

## Impact

- `atunko-tui`: `TuiController`, `Screen`, `AtunkoTui`, `ExecutionResultsView`, new `FileDiffView`
- `atunko-tui` tests: 8 new unit tests for file index state and screen transitions
- Requires TamboUI ≥ 0.3.0 (`markupTextArea` with line numbers/scrollbar)
- No core or CLI changes
