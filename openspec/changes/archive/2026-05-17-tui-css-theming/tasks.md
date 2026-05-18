## 1. reqstool — Requirements & SVCs

- [x] 1.1 Add TUI_0001.18 (TUI — CSS Theming) to `docs/reqstool/requirements.yml` as a sub-requirement of TUI_0001
- [x] 1.2 Add SVC_TUI_0001.18 (dark theme: TUI renders with dark theme styles from CSS when no theme is specified) to SVCs
- [x] 1.3 Add SVC_TUI_0001.18.1 (light theme: TUI renders with light theme styles from CSS when `--theme light` is specified) to SVCs
- [x] 1.4 Add SVC_TUI_0001.18.2 (user CSS file: TUI loads user-provided CSS file via `--css-file` flag, replacing bundled theme) to SVCs
- [x] 1.5 Add SVC_TUI_0001.18.3 (XDG user CSS: TUI loads `~/.config/atunko/theme.tcss` when present and no `--css-file` is specified) to SVCs
- [x] 1.6 Add SVC_TUI_0001.18.4 (theme flag: `--theme dark|light` selects bundled theme) to SVCs
- [x] 1.7 Update `atunko-tui/docs/reqstool` filter to include TUI_0001.18 and all new SVC IDs

## 2. Bundled TCSS Themes

- [x] 2.1 Create `atunko-tui/src/main/resources/themes/dark.tcss` — extract all current inline styles into CSS rules using semantic classes
- [x] 2.2 Create `atunko-tui/src/main/resources/themes/light.tcss` — light variant with inverted/appropriate colors

## 3. View Refactoring — Inline Style Extraction

- [x] 3.1 Add semantic `.cssClass()` calls to all elements in `BrowserView` and remove inline style calls
- [x] 3.2 Add semantic `.cssClass()` calls to all elements in `DetailView` and remove inline style calls
- [x] 3.3 Add semantic `.cssClass()` calls to all elements in `TagBrowserView` and remove inline style calls
- [x] 3.4 Add semantic `.cssClass()` calls to all elements in `ConfirmRunView` and remove inline style calls
- [x] 3.5 Add semantic `.cssClass()` calls to all elements in `ExecutionResultsView` and remove inline style calls
- [x] 3.6 Add semantic `.cssClass()` calls to all elements in `HelpOverlay` and remove inline style calls
- [x] 3.7 Add semantic `.cssClass()` calls to `RecipeListRenderer` and remove inline style calls

## 4. StyleEngine Integration

- [x] 4.1 Create `StyleEngine` in `AtunkoTui` constructor — load bundled theme or user CSS based on resolution order
- [x] 4.2 Pass `StyleEngine` to `ToolkitRunner` via builder API
- [x] 4.3 Add `--theme` and `--css-file` Picocli `@Option` fields to `TuiCommand`
- [x] 4.4 Pass theme options from `TuiCommand` to `AtunkoTui` (constructor parameter or config record)

## 5. Tests

- [x] 5.1 Write unit test: default theme is dark when no flags specified, annotated `@SVCs({"atunko:SVC_TUI_0001.18"})`
- [x] 5.2 Write unit test: `--theme light` selects light theme, annotated `@SVCs({"atunko:SVC_TUI_0001.18.1", "atunko:SVC_TUI_0001.18.4"})`
- [x] 5.3 Write unit test: `--css-file` loads user CSS and replaces bundled theme, annotated `@SVCs({"atunko:SVC_TUI_0001.18.2"})`
- [x] 5.4 Write unit test: XDG config file auto-loaded when present, annotated `@SVCs({"atunko:SVC_TUI_0001.18.3"})`
- [x] 5.5 Write unit test: `--css-file` takes priority over XDG config file
- [x] 5.6 Write unit test: user CSS replaces (not layers on) bundled theme

## 6. Build & Verification

- [x] 6.1 Run `./gradlew spotlessApply` to fix formatting
- [x] 6.2 Run `./gradlew build` — all checks pass (Spotless, Checkstyle, Error Prone, tests)
- [x] 6.3 Run `openspec validate --all --strict` to verify spec integrity
- [ ] 6.4 Visual verification: run TUI with dark theme, light theme, and custom CSS file
