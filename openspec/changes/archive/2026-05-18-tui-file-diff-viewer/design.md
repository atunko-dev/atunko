## Context

The TUI execution result screen previously showed a static `list()` of changed file paths. The web UI has a `DiffDialog` (diff2html, side-by-side). In the TUI, a full diff algorithm is out of scope; instead, showing the before/after file content with line numbers in side-by-side `markupTextArea` panels gives the user enough context to understand recipe changes.

TamboUI 0.3.0's `markupTextArea` provides line numbers (`.showLineNumbers()`), a scrollbar (`.scrollbar()`), and the 0.3.0 list-scrolling bug fix — all needed for a usable diff view.

## Goals / Non-Goals

**Goals:**
- Navigate the file list in `ExecutionResultsView` with keyboard
- View per-file before/after content in a new `FileDiffView` screen
- Guard `openFileDiff()` so it is safe to call regardless of state

**Non-Goals:**
- Syntax highlighting (plain text only)
- Line-level diff algorithm (unified/side-by-side computed diff)
- Mouse support for scrolling the panels

## Decisions

**Decision 1: State lives in `TuiController`, not in the view**

`selectedFileIndex` is controller state, consistent with all other navigation state (`highlightedIndex`, `loadConfigHighlightIndex`, etc.). Views are stateless render functions.

**Decision 2: `returnFromFileDiff()` goes to `EXECUTION_RESULTS`, not `goBack()` → `BROWSER`**

`goBack()` always returns to `BROWSER`. A dedicated `returnFromFileDiff()` preserves the correct back-navigation semantics — the user came from the results list and should return there.

**Decision 3: Side-by-side layout using `markupTextArea`**

Two `markupTextArea` panels in a `row()`, each with `Constraint.fill()`, give equal horizontal space for before/after. `markupTextArea` was chosen over `list()` because it provides line numbers and a scrollbar widget. The content passed is the full file string, not pre-split lines — `markupTextArea` handles newlines internally.

**Decision 4: `openFileDiff()` is a no-op when no results/changes**

`ExecutionResultsView` only renders `Enter:diff` in the footer when `hasChanges` is true, so in practice the guard never fires from normal navigation. It exists so `openFileDiff()` is safe to call unconditionally (e.g., in tests or future callers).

**Decision 5: Derive `hasChanges` live inside the key handler**

The render-time `hasChanges` boolean was captured in a lambda closure, making it stale if execution results were replaced between renders. The handler now reads from `controller.executionResult()` directly at event time.

## Risks / Trade-offs

- [Risk] Large files (thousands of lines) may be slow to render in `markupTextArea` → **Mitigation**: OpenRewrite recipes typically change a bounded number of lines; full-file rendering is acceptable for the current scope. Can be revisited if performance issues surface.
- [Risk] No diff algorithm means unchanged lines take up screen space → **Mitigation**: Acceptable for v1; future enhancement could add unified diff rendering.
