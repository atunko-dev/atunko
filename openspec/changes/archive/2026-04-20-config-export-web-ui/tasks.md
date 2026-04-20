## 1. Reqstool Traceability

- [x] 1.1 Add requirement `WEB_0001.17` to `docs/reqstool/requirements.yml`
- [x] 1.2 Add SVCs `SVC_WEB_0001.28`–`SVC_WEB_0001.31` to `docs/reqstool/software_verification_cases.yml`
- [x] 1.3 Add SVCs `SVC_WEB_0001.32`–`SVC_WEB_0001.34` to `docs/reqstool/software_verification_cases.yml`

## 2. ExportConfigDialog — initial (done)

- [x] 2.1 Create `ExportConfigDialog.java` extending `Dialog` with `ExportFormat` enum, `RadioButtonGroup`, `TextArea`, copy `Button`
- [x] 2.2 Implement `updateSnippet()` — builds `RunConfig` from selected recipes, calls `ConfigExportService`, sets `TextArea` value
- [x] 2.3 Implement empty-selection guard — disable controls, show "No recipes selected." message
- [x] 2.4 Implement copy-to-clipboard via `UI.getCurrent().getPage().executeJs` + `Notification`
- [x] 2.5 Add package-private testability getters: `getFormatSelector()`, `getSnippetArea()`, `getCopyButton()`

## 3. RecipeBrowserView Integration (done)

- [x] 3.1 Add `exportButton` field with `VaadinIcon.DOWNLOAD_ALT`, `LUMO_SMALL + LUMO_PRIMARY` variants
- [x] 3.2 Insert `exportButton` between `loadButton` and `dryRunButton` in `buildStatusBar()`; wire click listener to `openExportDialog()`
- [x] 3.3 Add `openExportDialog()` method (null-guard on `cascadeHandler`, open `ExportConfigDialog`)
- [x] 3.4 Annotate `openExportDialog()` with `@Requirements({"atunko:WEB_0001.17"})`
- [x] 3.5 Add `getExportButton()` testability getter

## 4. Minimal/Full Mode — ConfigExportService

- [x] 4.1 Add `ExportMode { MINIMAL, FULL }` enum to `ConfigExportService`
- [x] 4.2 Add `exportToGradle(RunConfig, ExportMode)` — FULL emits `plugins {}`, `repositories {}`, `rewrite {}` (Groovy DSL)
- [x] 4.3 Add `exportToMaven(RunConfig, ExportMode)` — FULL emits complete `pom.xml` with GAV `io.github.atunkodev:atunko-rewrite:0.1.0-SNAPSHOT`
- [x] 4.4 Keep existing no-arg methods as MINIMAL delegates (backward compat)

## 5. Minimal/Full Mode — CLI

- [x] 5.1 Add `--full` flag to `ConfigExportSubcommand` (default false)
- [x] 5.2 Pass `ExportMode.FULL` or `ExportMode.MINIMAL` based on flag when calling `exportToGradle` / `exportToMaven`

## 6. Minimal/Full Mode — ExportConfigDialog

- [x] 6.1 Add `RadioButtonGroup<ExportMode> modeSelector` field (MINIMAL/FULL, default MINIMAL)
- [x] 6.2 Wire `modeSelector` value-change listener to call `updateSnippet()`
- [x] 6.3 Pass `modeSelector.getValue()` as `ExportMode` argument in `updateSnippet()`
- [x] 6.4 Add `getModeSelector()` package-private testability getter
- [x] 6.5 Disable `modeSelector` in empty-selection guard

## 7. Tests — Minimal/Full Mode

- [x] 7.1 `ConfigExportServiceTest`: full Gradle includes `plugins {`, `repositories {`, `rewrite {` (`SVC_WEB_0001.33`)
- [x] 7.2 `ConfigExportServiceTest`: full Maven includes `<?xml`, `<groupId>io.github.atunkodev</groupId>`, recipes (`SVC_WEB_0001.34`)
- [x] 7.3 `ConfigExportServiceTest`: existing no-arg methods still return minimal output (regression)
- [x] 7.4 `ConfigExportCommandTest`: `--full --gradle` outputs standalone build.gradle (`SVC_WEB_0001.33`)
- [x] 7.5 `ConfigExportCommandTest`: `--full --maven` outputs full pom.xml (`SVC_WEB_0001.34`)
- [x] 7.6 `ExportConfigDialogTest`: switching mode updates snippet (`SVC_WEB_0001.32`)
- [x] 7.7 `ExportConfigDialogTest`: Full Gradle mode includes `plugins {` block (`SVC_WEB_0001.33`)
- [x] 7.8 `ExportConfigDialogTest`: Full Maven mode includes `<?xml` declaration (`SVC_WEB_0001.34`)

## 8. Build & Verify

- [x] 8.1 Run `./gradlew spotlessApply`
- [x] 8.2 Run `./gradlew build` — all tests pass
- [x] 8.3 Run `./gradlew spotlessApply` (post mode changes)
- [x] 8.4 Run `./gradlew build` — all tests pass (post mode changes)
