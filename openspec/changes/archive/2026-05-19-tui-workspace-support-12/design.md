## Context

Core (CORE_0010/0011/0012), CLI (CLI_0005), and Web UI (WEB_0002) already support workspace mode. The TUI subcommand (`atunko tui`) is the only surface without it. All necessary engine APIs (`WorkspaceScanner`, `WorkspaceExecutionEngine`, `WorkspaceExecutionResult`, `SessionHolder.initWorkspace()`) are already in `atunko-core`. The TUI only needs wiring and display changes.

Current single-project flow: `TuiCommand` → `SessionHolder.init(dir, info)` → `TuiController` executes via `RecipeExecutionEngine` → `ExecutionResultsView` shows changed files.

## Goals / Non-Goals

**Goals:**
- Add `--workspace <path>` option to `TuiCommand` (mirrors CLI/Web)
- Route TUI execution through `WorkspaceExecutionEngine` when workspace mode is active
- Show all workspace projects in `ConfirmRunView` before execution
- Show per-project PASS/FAIL results table after execution (`WORKSPACE_RESULTS` screen)
- Store run configs under `<workspaceRoot>/atunko/runs/` in workspace mode

**Non-Goals:**
- Per-project recipe browsing / project-switching screen (not in CLI or Web MVP)
- Partial project selection (run against a subset of workspace projects)
- Parallel project execution (serial, same as `WorkspaceExecutionEngine`)

## Decisions

### D1 — No new project-selector screen
Single recipe browser that works across all workspace projects, same as in CLI/Web. The ConfirmRunView summarises which projects will be targeted; users see the full list before executing.

**Alternatives considered:** A dedicated `PROJECT_SELECTOR` screen before `BROWSER`. Rejected — adds complexity with no clear benefit for the MVP; CLI and Web don't have one either.

### D2 — `WORKSPACE_RESULTS` screen is a new `Screen` enum value, not a flag on `EXECUTION_RESULTS`
Clean separation keeps `ExecutionResultsView` render logic readable: it inspects `controller.lastWorkspaceResult()` (non-null → workspace table) vs `controller.lastExecutionResult()` (non-null → single-project list). Both paths live in `ExecutionResultsView`; `AtunkoTui` routes `WORKSPACE_RESULTS` there.

**Alternatives considered:** Reuse `EXECUTION_RESULTS` with a mode flag. Rejected — complicates view dispatch and the `Screen` enum already encodes screen identity.

### D3 — `TuiController` is the workspace/single-project router
`TuiController.runSelectedRecipes(boolean dryRun)` checks `SessionHolder.getWorkspaceRoot() != null`. If true, it builds a `Workspace` from `SessionHolder.getProjectEntries()` and delegates to `WorkspaceExecutionEngine`; result stored as `lastWorkspaceResult`. Otherwise, existing single-project path is unchanged.

### D4 — Config directory follows `SessionHolder.getWorkspaceRoot()`
`TuiController.runsDir()` returns `workspaceRoot.resolve("atunko/runs")` when workspace mode is active, `projectDir.resolve("atunko/runs")` otherwise. This matches `CORE_0012` (workspace run-config block) and how CLI handles it.

### D5 — `TuiCommand` option group: `--workspace` mutually exclusive with `--project-dir`
Picocli `@ArgGroup(exclusive = true)` — same pattern as `RunCommand`. Default: single-project mode with `--project-dir` defaulting to `.`.

## Risks / Trade-offs

- [Risk] `WorkspaceExecutionEngine` runs projects serially — large workspaces may feel slow in the TUI. → Mitigation: out of scope; match existing CLI/Web behaviour.
- [Risk] `ConfirmRunView` "Project" line currently uses `controller.projectDir()` — if workspace mode isn't checked, a NullPointerException could surface. → Mitigation: guard in `ConfirmRunView` before reading `projectDir()`.
- [Risk] `ExecutionResultsView` currently assumes a flat `List<Path>` of changed files. Workspace results have a different shape. → Mitigation: `controller.lastWorkspaceResult()` accessor added; view checks at render time.

## Migration Plan

No migration needed. The `--workspace` option is additive; existing `--project-dir` invocations are unaffected.
