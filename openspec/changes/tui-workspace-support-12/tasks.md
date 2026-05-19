## 1. reqstool — Add TUI_0002 Requirements and SVCs

- [x] 1.1 Add TUI_0002 (TUI Workspace Support — top-level) requirement and SVC_TUI_0002 to `docs/reqstool/requirements.yml` and `docs/reqstool/software_verification_cases.yml`
- [x] 1.2 Add TUI_0002.1 (TUI — `--workspace` Option) requirement and SVC_TUI_0002.1 (covers `TuiCommand` option parsing and `SessionHolder.initWorkspace()` call)
- [x] 1.3 Add TUI_0002.2 (TUI — Run Dialog Shows All Workspace Projects) requirement and SVC_TUI_0002.2
- [x] 1.4 Add TUI_0002.3 (TUI — Workspace Execution via `WorkspaceExecutionEngine`) requirement and SVC_TUI_0002.3
- [x] 1.5 Add TUI_0002.4 (TUI — Aggregate Workspace Results Screen) requirement and SVC_TUI_0002.4
- [x] 1.6 Add TUI_0002.5 (TUI — Workspace Config Directory) requirement and SVC_TUI_0002.5

## 2. TuiCommand — `--workspace` Option

- [x] 2.1 Add `--workspace Path` option to `TuiCommand` in an `@ArgGroup(exclusive = true)` with `--project-dir` (covers TUI_0002.1 / SVC_TUI_0002.1)
- [x] 2.2 When `--workspace` is supplied, scan with `WorkspaceScanner.scan(workspaceDir)` and call `SessionHolder.initWorkspace(workspace.root(), workspace.projects())`
- [x] 2.3 Keep existing `--project-dir` / `SessionHolder.init()` path unchanged

## 3. TuiController — Workspace Mode

- [x] 3.1 Add `lastWorkspaceResult` field (`WorkspaceExecutionResult`) and accessor to `TuiController`
- [x] 3.2 Add `runsDir()` helper: returns `workspaceRoot/atunko/runs` when workspace mode active, `projectDir/atunko/runs` otherwise (covers TUI_0002.5 / SVC_TUI_0002.5)
- [x] 3.3 In `runSelectedRecipes(boolean dryRun)`: when `SessionHolder.getWorkspaceRoot() != null`, build `Workspace` from `SessionHolder.getProjectEntries()` and execute via `WorkspaceExecutionEngine`; store result in `lastWorkspaceResult` and navigate to `Screen.WORKSPACE_RESULTS` (covers TUI_0002.3 / SVC_TUI_0002.3)
- [x] 3.4 Replace hardcoded `projectDir.resolve("atunko/runs")` references in `listRunConfigs()` and `saveRunConfig()` with `runsDir()`
- [x] 3.5 Write unit tests for `TuiController` workspace execution path annotated with `@SVCs("SVC_TUI_0002.3")`

## 4. Screen Enum

- [x] 4.1 Add `WORKSPACE_RESULTS` to the `Screen` enum in `atunko-tui`

## 5. ConfirmRunView — Workspace Project List

- [x] 5.1 In `ConfirmRunView`, when `SessionHolder.getWorkspaceRoot() != null`, render a list of all `ProjectEntry` names instead of the single "Project: `<path>`" line (covers TUI_0002.2 / SVC_TUI_0002.2)
- [x] 5.2 Guard `controller.projectDir()` access in `ConfirmRunView` so it is only called in single-project mode

## 6. ExecutionResultsView — Workspace Results Table

- [x] 6.1 In `ExecutionResultsView`, check `controller.lastWorkspaceResult() != null`; if so, render a per-project table: project directory name | change count | PASS / FAIL (covers TUI_0002.4 / SVC_TUI_0002.4)
- [x] 6.2 Keep existing single-project flat file list path unchanged
- [x] 6.3 Write unit test for workspace results rendering annotated with `@SVCs("SVC_TUI_0002.4")`

## 7. AtunkoTui — Route WORKSPACE_RESULTS Screen

- [x] 7.1 In `AtunkoTui` screen dispatch, add a `case WORKSPACE_RESULTS` that renders `ExecutionResultsView` (workspace path)

## 8. Build & Quality

- [x] 8.1 Run `./gradlew spotlessApply` and fix any formatting issues
- [x] 8.2 Run `./gradlew build` and confirm zero errors / test failures
