## 1. reqstool — requirements and SVCs

- [x] 1.1 Add CORE_0010 / CORE_0010.1–5 requirements to `docs/reqstool/requirements.yml` (WorkspaceScanner, skip dirs, .atunkoignore, Gradle multi-project, Maven multi-module, ProjectEntry)
- [x] 1.2 Add CORE_0011 / CORE_0011.1 requirements to `docs/reqstool/requirements.yml` (WorkspaceExecutionEngine, per-project failure isolation)
- [x] 1.3 Add CORE_0012 requirement to `docs/reqstool/requirements.yml` (RunConfig optional workspace block)
- [x] 1.4 Add CORE_0007.1 / CORE_0008.1 requirements to `docs/reqstool/requirements.yml` (save/load workspace block when configured)
- [x] 1.5 Add CLI_0005 / CLI_0005.1–2 requirements to `docs/reqstool/requirements.yml` (`--workspace` flag on `run`/`list`, per-project summary table, non-zero exit on failure)
- [x] 1.6 Add WEB_0002 / WEB_0002.1–4 requirements to `docs/reqstool/requirements.yml` (Web UI workspace features)
- [x] 1.7 Add corresponding SVCs (SVC_CORE_0010–SVC_CORE_0012, SVC_CORE_0007.1, SVC_CORE_0008.1, SVC_CLI_0005–SVC_CLI_0005.2, SVC_WEB_0002–SVC_WEB_0002.4) to `docs/reqstool/software_verification_cases.yml`

## 2. Core — model types

- [x] 2.1 Create `ProjectEntry(Path projectDir, ProjectInfo info)` record in `atunko-core/src/main/java/io/github/atunkodev/core/project/` [SVC_CORE_0010.5]
- [x] 2.2 Migrate `SessionHolder` to expose `List<ProjectEntry>`; add backward-compat shim `getProjectInfo()` returning first entry's info [SVC_CORE_0010]
- [x] 2.3 Update `AppServices.init(...)` and all startup wiring in `atunko-cli` and `atunko-web` to use `List<ProjectEntry>` [SVC_CORE_0010]

## 3. Core — workspace scanner

- [x] 3.1 Create `WorkspaceScanner` in `atunko-core/src/main/java/io/github/atunkodev/core/project/` with top-down walk logic [SVC_CORE_0010]
- [x] 3.2 Implement skip list: `build/`, `target/`, `.gradle/`, `node_modules/`, `.git/`, hidden dirs [SVC_CORE_0010.1]
- [x] 3.3 Implement `.atunkoignore` marker file check [SVC_CORE_0010.2]
- [x] 3.4 Implement `settings.gradle[.kts]` regex parsing to extract and claim Gradle subproject dirs [SVC_CORE_0010.3]
- [x] 3.5 Implement `pom.xml` `<modules>` XML parsing to detect Maven multi-module roots and claim declared module dirs [SVC_CORE_0010.4]
- [x] 3.6 Create `Workspace(Path root, List<ProjectEntry> projects)` record [SVC_CORE_0010]
- [x] 3.7 Write unit tests for `WorkspaceScanner` using fixture workspaces under `atunko-core/src/test/resources/workspaces/` [SVC_CORE_0010–SVC_CORE_0010.5]
- [x] 3.8 Create workspace test fixtures: `workspace-flat/` (3 sibling Maven/Gradle projects), `workspace-gradle-multi/` (root + subprojects), `workspace-maven-multi/` (aggregator + modules), `workspace-atunkoignore/` (marker file exclusion) [SVC_CORE_0010.1–SVC_CORE_0010.4]

## 4. Core — workspace execution engine

- [x] 4.1 Create `WorkspaceExecutionEngine` in `atunko-core/src/main/java/io/github/atunkodev/core/engine/` [SVC_CORE_0011]
- [x] 4.2 Create `ProjectExecutionResult(ProjectEntry entry, ExecutionResult result, @Nullable Throwable failure)` record [SVC_CORE_0011.1]
- [x] 4.3 Create `WorkspaceExecutionResult(List<ProjectExecutionResult> results)` record with aggregate helpers (totalChanges, failureCount) [SVC_CORE_0011]
- [x] 4.4 Implement per-project failure isolation (catch per iteration, continue loop) [SVC_CORE_0011.1]
- [x] 4.5 Write integration tests for `WorkspaceExecutionEngine` using fixture workspaces [SVC_CORE_0011, SVC_CORE_0011.1]

## 5. Core — RunConfig v2

- [x] 5.1 Add `WorkspaceConfig(@Nullable String root, List<String> include, List<String> exclude)` record to `atunko-core/src/main/java/io/github/atunkodev/core/config/` [SVC_CORE_0012]
- [x] 5.2 Add `@Nullable WorkspaceConfig workspace` field to `RunConfig` (no version bump; absent = single-project mode) [SVC_CORE_0012]
- [x] 5.3 Update `RunConfigService` loader to deserialise optional `workspace:` block when present [SVC_CORE_0008.1]
- [x] 5.4 Update `RunConfigService` save to include `workspace:` block when non-null [SVC_CORE_0007.1]
- [x] 5.5 Write round-trip tests: config without workspace loads correctly; config with workspace saves and reloads [SVC_CORE_0012, SVC_CORE_0007.1, SVC_CORE_0008.1]

## 6. CLI — workspace support

- [x] 6.1 Add `--workspace <dir>` option to `run` Picocli command in `atunko-cli`; wire to `WorkspaceScanner` + `WorkspaceExecutionEngine` [SVC_CLI_0005]
- [x] 6.2 Add `--workspace <dir>` option to `list` Picocli command; print discovered project paths [SVC_CLI_0005]
- [x] 6.3 Print per-project summary table after `run --workspace` (project name, change count, pass/fail) [SVC_CLI_0005.1]
- [x] 6.4 Exit non-zero if any project failed during workspace run [SVC_CLI_0005.2]
- [x] 6.5 Write CLI integration tests for `run --workspace` and `list --workspace` using fixture workspaces [SVC_CLI_0005–SVC_CLI_0005.2]

## 7. Web UI — workspace support

- [x] 7.1 Add "Scan workspace" toggle to the project-dir picker component; wire to `WorkspaceScanner` [SVC_WEB_0002]
- [x] 7.2 Replace single-project display with multi-select project list (checkbox column) when workspace is loaded [SVC_WEB_0002]
- [x] 7.3 Persist workspace `root` path alongside project selection in session/run config [SVC_WEB_0002.4]
- [x] 7.4 Update execution dialog to show per-project progress (project name, N of M) using `WorkspaceExecutionEngine` [SVC_WEB_0002.1]
- [x] 7.5 Build aggregate results table: per-project row with change count, pass/fail status, drill-down [SVC_WEB_0002.2]
- [x] 7.6 Scope diff viewer to one project at a time; add project dropdown to switch context [SVC_WEB_0002.3]
- [ ] 7.7 Manual end-to-end test: scan a fixture workspace, execute recipe, verify per-project results and diff scoping [SVC_WEB_0002–SVC_WEB_0002.4]

## 8. Quality and sign-off

- [ ] 8.1 Run `./gradlew spotlessApply` and fix all formatting violations
- [ ] 8.2 Run `./gradlew build` — zero warnings, zero test failures
- [ ] 8.3 Validate OpenSpec artifacts: `openspec validate --all --strict`
- [ ] 8.4 Run `reqstool status` — all new requirement IDs annotated and SVCs linked to tests
