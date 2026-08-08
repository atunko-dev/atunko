## 1. reqstool — Add Requirements and SVCs

- [x] 1.1 Add TUI_0001.21 to `docs/reqstool/requirements.yml` — export overlay accessible from confirm-run (`x` key)
- [x] 1.2 Add TUI_0001.22 to `docs/reqstool/requirements.yml` — format toggle (Gradle/Maven) inside overlay
- [x] 1.3 Add TUI_0001.23 to `docs/reqstool/requirements.yml` — mode toggle (Minimal/Full) inside overlay
- [x] 1.4 Add SVC_TUI_0001.21 to `docs/reqstool/software_verification_cases.yml` (covers TUI_0001.21)
- [x] 1.5 Add SVC_TUI_0001.22 to `docs/reqstool/software_verification_cases.yml` (covers TUI_0001.22)
- [x] 1.6 Add SVC_TUI_0001.23 to `docs/reqstool/software_verification_cases.yml` (covers TUI_0001.23)

## 2. TuiController — Export State

- [x] 2.1 Add `ExportFormat` enum (GRADLE, MAVEN) to `TuiController`
- [x] 2.2 Add `exportFormat` field (default GRADLE) and getter/setter to `TuiController`
- [x] 2.3 Add `exportMode` field (default MINIMAL, type `ConfigExportService.ExportMode`) and getter/setter to `TuiController`
- [x] 2.4 Add `showExport` boolean field and getter/setter to `TuiController`

## 3. Tests — TuiController Export State

- [x] 3.1 Unit test: default export state values (format=GRADLE, mode=MINIMAL, showExport=false)
- [x] 3.2 Unit test: toggling `exportFormat` cycles GRADLE↔MAVEN
- [x] 3.3 Unit test: toggling `exportMode` cycles MINIMAL↔FULL

## 4. ExportConfigView — New View

- [x] 4.1 Create `atunko-tui/src/main/java/io/github/atunkodev/tui/view/ExportConfigView.java`
- [x] 4.2 Render current snippet via `ConfigExportService` using `controller.exportFormat()` and `controller.exportMode()`
- [x] 4.3 Display snippet in `markupTextArea` with scrollbar (follow `FileDiffView` pattern)
- [x] 4.4 Header row: title + current format + mode indicators
- [x] 4.5 Footer status bar: `g:Gradle m:Maven Tab:Minimal/Full Esc:close`
- [x] 4.6 Key handler: `g` → GRADLE, `m` → MAVEN, `Tab` → toggle mode, `Esc` → `showExport=false`
- [x] 4.7 Annotate with `@Requirements({"atunko:TUI_0001.21", "atunko:TUI_0001.22", "atunko:TUI_0001.23"})`

## 5. ConfirmRunView — Wire Export

- [x] 5.1 Detect `controller.isShowExport()` and render `ExportConfigView.render(controller)` in place of normal content
- [x] 5.2 Add `x` key handler in `ConfirmRunView`: sets `showExport=true` (note: `e` was already bound to expand)
- [x] 5.3 Update footer status bar to include `x:export`

## 6. Tests — ExportConfigView / Integration

- [x] 6.1 Unit test: `ExportConfigView` renders Gradle snippet for a `RunConfig` with known recipes (covers SVC_TUI_0001.21)
- [x] 6.2 Unit test: format toggle updates rendered snippet to Maven (covers SVC_TUI_0001.22)
- [x] 6.3 Unit test: mode toggle switches between MINIMAL and FULL snippets (covers SVC_TUI_0001.23)

## 7. Build & Quality

- [x] 7.1 Run `./gradlew spotlessApply` — fix any formatting issues
- [x] 7.2 Run `./gradlew build` — confirm clean build, all tests pass
