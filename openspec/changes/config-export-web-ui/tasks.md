## 1. Reqstool Traceability

- [x] 1.1 Add requirement `WEB_0001.17` to `docs/reqstool/requirements.yml`
- [x] 1.2 Add SVCs `SVC_WEB_0001.28`–`SVC_WEB_0001.31` to `docs/reqstool/software_verification_cases.yml`

## 2. ExportConfigDialog (covers SVC_WEB_0001.29, .30, .31)

- [x] 2.1 Create `ExportConfigDialog.java` extending `Dialog` with `ExportFormat` enum, `RadioButtonGroup`, `TextArea`, copy `Button`
- [x] 2.2 Implement `updateSnippet()` — builds `RunConfig` from selected recipes, calls `ConfigExportService`, sets `TextArea` value
- [x] 2.3 Implement empty-selection guard — disable controls, show "No recipes selected." message
- [x] 2.4 Implement copy-to-clipboard via `UI.getCurrent().getPage().executeJs` + `Notification`
- [x] 2.5 Add package-private testability getters: `getFormatSelector()`, `getSnippetArea()`, `getCopyButton()`

## 3. RecipeBrowserView Integration (covers SVC_WEB_0001.28)

- [x] 3.1 Add `exportButton` field with `VaadinIcon.DOWNLOAD_ALT`, `LUMO_SMALL + LUMO_PRIMARY` variants
- [x] 3.2 Insert `exportButton` between `loadButton` and `dryRunButton` in `buildStatusBar()`; wire click listener to `openExportDialog()`
- [x] 3.3 Add `openExportDialog()` method (null-guard on `cascadeHandler`, open `ExportConfigDialog`)
- [x] 3.4 Annotate `openExportDialog()` with `@Requirements({"atunko:WEB_0001.17"})`
- [x] 3.5 Add `getExportButton()` testability getter

## 4. Tests

- [x] 4.1 Create `ExportConfigDialogTest.java` with MockVaadin setup/teardown
- [x] 4.2 Test: Gradle snippet contains `rewrite {` and recipe name (`SVC_WEB_0001.29`)
- [x] 4.3 Test: Maven snippet contains `<groupId>org.openrewrite.maven</groupId>` and recipe name (`SVC_WEB_0001.29`)
- [x] 4.4 Test: Switching format live-updates snippet (`SVC_WEB_0001.31`)
- [x] 4.5 Test: Empty selection disables snippet area and copy button (`SVC_WEB_0001.30`)
- [x] 4.6 Test: Empty selection shows "No recipes selected." (`SVC_WEB_0001.30`)
- [x] 4.7 Add to `RecipeBrowserViewTest`: export button exists in status bar (`SVC_WEB_0001.28`)
- [x] 4.8 Add to `RecipeBrowserViewTest`: export button has `lumo-small` and `lumo-primary` variants (`SVC_WEB_0001.28`)

## 5. Build & Verify

- [x] 5.1 Run `./gradlew spotlessApply`
- [x] 5.2 Run `./gradlew build` — all tests pass
