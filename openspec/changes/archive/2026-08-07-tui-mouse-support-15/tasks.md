## 1. Enable mouse capture in AtunkoTui

- [x] 1.1 Update `AtunkoTui.configure()` to call `.mouseCapture(true)` on
  `TuiConfig.builder()` in both branches (with/without logFile); annotate with
  `@Requirements({"atunko:TUI_0001.27"})`
- [x] 1.2 Add `docs/reqstool` entries: requirement `TUI_0001.27` (TUI enables mouse
  capture so scroll and click events are delivered to handlers); add SVC
  `SVC_TUI_0001.27` (mouse capture enabled — verified by integration that mouse events
  are handled)

## 2. TuiController — mouse helper

- [x] 2.1 Add `mouseRowToIndex(int mouseY, int headerRows, int rowCount)` to
  `TuiController` — returns list index or `-1` if out of range; annotate with
  `@Requirements({"atunko:TUI_0001.27"})`
- [x] 2.2 Write unit tests for `mouseRowToIndex` covering: in-range, below header,
  above list, above row count; annotate with `@SVCs({"atunko:SVC_TUI_0001.27"})`

## 3. BrowserView — mouse handler

- [x] 3.1 Add `onMouseEvent()` to the root `column` in `BrowserView.render()`: handle
  `SCROLL_UP` → `controller.moveUp()`, `SCROLL_DOWN` → `controller.moveDown()`,
  `PRESS + LEFT` → highlight by y-coordinate, `PRESS + RIGHT` → highlight then toggle
  selection; guard: ignore when `isSearchMode()` or `isSaveConfigMode()` or
  `isShowHelp()` or `isShowOptions()`
- [x] 3.2 Write unit tests for the BrowserView mouse handler: scroll moves highlight,
  left-click sets highlight by row, right-click toggles selection; annotate with
  `@SVCs({"atunko:SVC_TUI_0001.27"})`

## 4. ConfirmRunView — mouse handler

- [x] 4.1 Add `onMouseEvent()` to root `column` in `ConfirmRunView.render()`: scroll
  → `moveRunHighlightUp/Down`, left-click → highlight run list by y-coordinate,
  right-click → highlight then toggle run recipe; guard: ignore when `isShowHelp()`,
  `isShowOptions()`, or `isShowExport()`

## 5. ExecutionResultsView — mouse handler

- [x] 5.1 Add `onMouseEvent()` to root `column` in `ExecutionResultsView.render()`:
  `SCROLL_UP` → `moveFileUp`, `SCROLL_DOWN` → `moveFileDown`

## 6. TagBrowserView — mouse handler

- [x] 6.1 Add `onMouseEvent()` to root `column` in `TagBrowserView.render()`: scroll
  moves tag-list highlight; read the existing controller methods used for Up/Down
  navigation and wire scroll to those

## 7. LoadConfigView — mouse handler

- [x] 7.1 Add `onMouseEvent()` to root `column` in `LoadConfigView.render()`: scroll →
  `moveLoadConfigUp/Down`; left-click → highlight by y-coordinate

## 8. RecipeOptionsView — mouse handler

- [x] 8.1 Add `onMouseEvent()` to root `column` in `RecipeOptionsView.render()`: scroll
  → `moveOptionHighlightUp/Down` using current option count

## 9. Build and Quality

- [x] 9.1 Run `./gradlew spotlessApply` and fix any formatting issues
- [x] 9.2 Run `./gradlew build` — confirm no Checkstyle or Error Prone violations
- [x] 9.3 Run `./gradlew test` — confirm all tests pass
