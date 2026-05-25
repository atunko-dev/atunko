## 1. TuiController — File Index State

- [x] 1.1 Add `private int selectedFileIndex = 0` field to `TuiController`
- [x] 1.2 Add `selectedFileIndex()` getter with `@Requirements({"atunko:TUI_0001.8.1", "atunko:TUI_0001.8.2"})`
- [x] 1.3 Add `moveFileDown()` — increment, clamp to `executionResult.changes().size() - 1`; add `@Requirements({"atunko:TUI_0001.8.1"})`
- [x] 1.4 Add `moveFileUp()` — decrement, clamp to 0; add `@Requirements({"atunko:TUI_0001.8.1"})`
- [x] 1.5 Add `openFileDiff()` — set `currentScreen = Screen.FILE_DIFF` only when result is non-null and non-empty; add `@Requirements({"atunko:TUI_0001.8.2"})`
- [x] 1.6 Add `returnFromFileDiff()` — set `currentScreen = Screen.EXECUTION_RESULTS`; add `@Requirements({"atunko:TUI_0001.8.2"})`
- [x] 1.7 Reset `selectedFileIndex = 0` in `showDryRunResult()` and `showExecutionResult()`

## 2. Screen + Router

- [x] 2.1 Add `FILE_DIFF` to `Screen` enum
- [x] 2.2 Add `case FILE_DIFF -> FileDiffView.render(controller)` to `AtunkoTui.render()` switch

## 3. ExecutionResultsView — Navigable File List

- [x] 3.1 Add `.selected(controller.selectedFileIndex())` to the `list()` element
- [x] 3.2 Extract `handleKeyEvent()` from the inline lambda; derive `hasChanges` live from `controller.executionResult()` inside the method (do not capture from render scope)
- [x] 3.3 Add `j`/Down → `moveFileDown()`, `k`/Up → `moveFileUp()`, `Enter` → `openFileDiff()` key bindings (only when `hasChanges`)
- [x] 3.4 Update footer: show `↑↓/jk:navigate  Enter:diff  Esc/q:back` when changes exist, `Esc/q:back` otherwise

## 4. FileDiffView — New Screen

- [x] 4.1 Create `FileDiffView.java` in `atunko-tui/.../view/`
- [x] 4.2 Annotate class with `@Requirements({"atunko:TUI_0001.8.2"})`
- [x] 4.3 Read `FileChange` at `controller.selectedFileIndex()`; show guard text if empty/out-of-bounds
- [x] 4.4 Render header: title (dryrun-mode or success-mode styling) + spacer + filename
- [x] 4.5 Render center: two `markupTextArea` panels side-by-side — "Before" and "After", each with `.showLineNumbers().scrollbar().wrapWord().constraint(Constraint.fill())`; null-safe fallback to `"<empty>"`
- [x] 4.6 Key handler: `Esc`/`q` → `controller.returnFromFileDiff()`
- [x] 4.7 Wrap in `column(...).id("file-diff").focusable().onKeyEvent(handler)` per TamboUI canonical pattern

## 5. Tests

- [x] 5.1 `selectedFileIndexStartsAtZero` — initial state; add `@SVCs({"atunko:SVC_TUI_0001.8.1"})`
- [x] 5.2 `moveFileDownIncrementsIndex` — basic increment; add `@SVCs({"atunko:SVC_TUI_0001.8.1"})`
- [x] 5.3 `moveFileDownClampsAtLastIndex` — no overflow; add `@SVCs({"atunko:SVC_TUI_0001.8.1"})`
- [x] 5.4 `moveFileUpDecrementsIndex` — basic decrement; add `@SVCs({"atunko:SVC_TUI_0001.8.1"})`
- [x] 5.5 `moveFileUpClampsAtZero` — no underflow; add `@SVCs({"atunko:SVC_TUI_0001.8.1"})`
- [x] 5.6 `openFileDiffSwitchesToFileDiffScreen` — with non-empty result; add `@SVCs({"atunko:SVC_TUI_0001.8.2"})`
- [x] 5.7 `openFileDiffDoesNothingWhenNoResults` — no execution result set; add `@SVCs({"atunko:SVC_TUI_0001.8.4"})`
- [x] 5.8 `openFileDiffDoesNothingWhenEmptyChangesList` — empty result; add `@SVCs({"atunko:SVC_TUI_0001.8.4"})`
- [x] 5.9 `returnFromFileDiffGoesBackToExecutionResults` — not browser; add `@SVCs({"atunko:SVC_TUI_0001.8.3"})`
- [x] 5.10 `showDryRunResultResetsSelectedFileIndex` — index reset on new run; add `@SVCs({"atunko:SVC_TUI_0001.8.1"})`
- [x] 5.11 `showExecutionResultResetsSelectedFileIndex` — index reset on new run; add `@SVCs({"atunko:SVC_TUI_0001.8.1"})`

## 6. Quality

- [x] 6.1 Run `./gradlew spotlessApply && ./gradlew build` — all tests pass, no Spotless/Checkstyle violations
