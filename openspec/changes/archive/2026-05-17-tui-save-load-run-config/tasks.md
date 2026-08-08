## 1. reqstool — Requirements & SVCs

- [x] 1.1 Add TUI_0001.19 (TUI — Load Run Config) to `docs/reqstool/requirements.yml`
- [x] 1.2 Add SVC_TUI_0001.19, SVC_TUI_0001.19.1, SVC_TUI_0001.19.2 to
      `docs/reqstool/software_verification_cases.yml`

## 2. Screen & Navigation

- [x] 2.1 Add `LOAD_CONFIG` value to `Screen` enum in `AtunkoTui` switch
- [x] 2.2 Create `LoadConfigView` — list files, j/k navigation, Enter/Esc handling

## 3. TuiController — Save workflow (TUI_0001.10)

- [x] 3.1 Implement `saveRunConfig(Path)` and annotate `@Requirements({"atunko:TUI_0001.10"})`
- [x] 3.2 Implement `enterSaveConfigMode()`, `exitSaveConfigMode()`, `setSaveConfigName()`,
      `confirmSaveConfig()`
- [x] 3.3 Add `S` key handler in `BrowserView` to enter save-mode with inline text input

## 4. TuiController — Load workflow (TUI_0001.19)

- [x] 4.1 Implement `loadRunConfig(RunConfig)` and annotate `@Requirements({"atunko:TUI_0001.19"})`
- [x] 4.2 Implement `listRunConfigs()` and annotate `@Requirements({"atunko:TUI_0001.19"})`
- [x] 4.3 Implement `openLoadConfig()`, `confirmLoadConfig()`, `moveLoadConfigDown()`,
      `moveLoadConfigUp()`
- [x] 4.4 Add `L` key handler in `BrowserView` to call `openLoadConfig()`

## 5. Help Overlay

- [x] 5.1 Update `HelpOverlay` BROWSER section to document `S: Save run config` and
      `L: Load run config`

## 6. Tests — Save (SVC_TUI_0001.10)

- [x] 6.1 Write `saveRunConfigPersistsSelectedRecipes` annotated `@SVCs({"atunko:SVC_TUI_0001.10"})`
- [x] 6.2 Write `confirmSaveConfigWritesFileUnderProjectDir`
      annotated `@SVCs({"atunko:SVC_TUI_0001.10"})`
- [x] 6.3 Write `confirmSaveConfigWithBlankNameExitsWithoutSaving`
      annotated `@SVCs({"atunko:SVC_TUI_0001.10"})`
- [x] 6.4 Write `enterSaveConfigModeSetsModeFlag` annotated `@SVCs({"atunko:SVC_TUI_0001.10"})`
- [x] 6.5 Write `exitSaveConfigModeClearsFlag` annotated `@SVCs({"atunko:SVC_TUI_0001.10"})`

## 7. Tests — Load (SVC_TUI_0001.19 / 19.1 / 19.2)

- [x] 7.1 Write `loadRunConfigRestoresRecipeSelection`
      annotated `@SVCs({"atunko:SVC_TUI_0001.19"})`
- [x] 7.2 Write `loadRunConfigReplacesExistingSelection`
      annotated `@SVCs({"atunko:SVC_TUI_0001.19.1"})`
- [x] 7.3 Write `listRunConfigsReturnsEmptyWhenDirectoryAbsent`
      annotated `@SVCs({"atunko:SVC_TUI_0001.19.2"})`
- [x] 7.4 Write `listRunConfigsReturnsYmlFiles`
      annotated `@SVCs({"atunko:SVC_TUI_0001.19.2"})`
- [x] 7.5 Write `openLoadConfigSetsScreenAndPopulatesFileList`
      annotated `@SVCs({"atunko:SVC_TUI_0001.19"})`
- [x] 7.6 Write `confirmLoadConfigLoadsFileAndReturnsToBrowser`
      annotated `@SVCs({"atunko:SVC_TUI_0001.19"})`

## 8. Build & Verification

- [x] 8.1 Run `./gradlew spotlessApply` to fix formatting
- [x] 8.2 Run `./gradlew build` — all checks pass
