## Context

TamboUI 0.2.0-SNAPSHOT provides a full mouse-event model:

- `dev.tamboui.tui.event.MouseEvent` — fields: `x` (column), `y` (row), `kind`
  (`MouseEventKind` enum), `button` (`LEFT`, `RIGHT`, `MIDDLE`, `NONE`), `modifiers`.
- `dev.tamboui.tui.event.MouseEventKind` — values: `PRESS`, `RELEASE`, `DRAG`, `MOVE`,
  `SCROLL_UP`, `SCROLL_DOWN`.
- `dev.tamboui.toolkit.event.MouseEventHandler` — functional interface
  `EventResult handle(MouseEvent e)`.
- `StyledElement.onMouseEvent(MouseEventHandler)` — registers a handler on any element.
- `TuiConfig.builder().mouseCapture(true).build()` — enables OS-level mouse capture in the
  terminal backend.

Currently `AtunkoTui.configure()` returns `TuiConfig.defaults()` (or a logging variant),
neither of which enables mouse capture.

## Goals / Non-Goals

**Goals:**
- Enable mouse capture once in `AtunkoTui.configure()`.
- Add `onMouseEvent()` handlers to the root focusable element of each view.
- Support: scroll-to-navigate lists, left-click-to-highlight, right-click-to-toggle-select.
- Keep all state mutations on `TuiController`; views remain stateless.
- Tested: unit tests for new controller helpers; view mouse-handler unit tests for the
  BrowserView list (scroll up/down, click-to-highlight, right-click-to-select).

**Non-Goals:**
- Drag-and-drop reordering.
- Hover tooltips.
- Pixel-precise hit detection (row-offset calculation is approximate).
- Mouse support inside text-input widgets (TamboUI handles those internally).

## Decisions

### 1. Enable mouseCapture in configure()

`AtunkoTui.configure()` currently has two branches (with/without logFile).  
**Decision:** Both branches set `.mouseCapture(true)`.

```java
return TuiConfig.builder()
    .mouseCapture(true)
    .errorHandler(...)   // only if logFile != null
    .build();
```

### 2. Mouse handler on the root focusable column

Each view already has a `.onKeyEvent(handler)` on its root
`column(dock()...).id("x").focusable()` element.  
**Decision:** Chain `.onMouseEvent(mouseHandler)` on the same element — consistent with
the TamboUI canonical pattern and keeps all input handling in one place per screen.

### 3. Coordinate-to-list-index mapping

`MouseEvent.y` is a terminal row (0-based from the top of the screen). A list rendered
inside a `dock().center()` region starts after a fixed header height. Rather than
hard-coding layout constants in every view, a small utility
`TuiController.mouseRowToIndex(int mouseY, int headerRows, int rowCount)` converts a y
coordinate to a list index:

```java
public int mouseRowToIndex(int mouseY, int headerRows, int rowCount) {
    int idx = mouseY - headerRows;
    if (idx < 0 || idx >= rowCount) return -1;
    return idx;
}
```

Header heights are small constants defined per-view (typically 1–3 rows for the header
bar).

### 4. Scroll events map directly to moveUp/moveDown

`SCROLL_UP` → `controller.moveUp()` (or the view-specific equivalent).
`SCROLL_DOWN` → `controller.moveDown()`.

This is the established pattern from TamboUI's own list/tree scroll examples.

### 5. Click interactions

- `PRESS + LEFT`: compute index from `event.y`, call `setHighlightedIndex(idx)`.
- `PRESS + RIGHT`: compute index, highlight it, then toggle selection.

Double-click detection is deferred to a follow-up (requires timing state); not in scope.

### 6. Views in scope

| View | Scroll | Click | Notes |
|---|---|---|---|
| BrowserView | moveUp/Down | highlight + right-click select | main list only |
| ConfirmRunView | moveRunHighlight | highlight run list | right-click toggles |
| ExecutionResultsView | moveFileUp/Down | — | file list |
| TagBrowserView | tag list | tag highlight | |
| LoadConfigView | config list | config highlight | |
| DetailView | no-op | — | no list |
| FileDiffView | no-op | — | no list |
| RecipeOptionsView | moveOptionHighlight | — | |
| ExportConfigView | no-op | — | no list |
