## Why

PR #52 shipped workspace (multi-project) support for core, CLI, and web UI, but the TUI subcommand was left out. Users who invoke `atunko tui` from a multi-project workspace cannot browse, select, and execute recipes across all projects the way `atunko run --workspace` and `atunko webui --workspace` already can.

## What Changes

- `TuiCommand` gains a `--workspace <path>` option (mutually exclusive with `--project-dir`) that scans the workspace and calls `SessionHolder.initWorkspace()`.
- `TuiController` gains workspace-mode awareness: detects multi-project state via `SessionHolder`, routes execution through `WorkspaceExecutionEngine`, and stores run configs under `<workspaceRoot>/atunko/runs/`.
- `Screen` enum gains a `WORKSPACE_RESULTS` entry for the post-execution workspace summary screen.
- `ConfirmRunView` shows all workspace projects when in workspace mode instead of the single "Project: `<path>`" line.
- `ExecutionResultsView` renders a per-project results table (project name | change count | PASS/FAIL) in workspace mode; single-project mode is unchanged.
- `AtunkoTui` routes the `WORKSPACE_RESULTS` screen to the updated `ExecutionResultsView`.
- New `TUI_0002` requirement family added to reqstool for TUI workspace support.

## Capabilities

### New Capabilities

- `tui-workspace-support`: TUI workspace mode — `--workspace` option, multi-project execution, per-project results screen.

### Modified Capabilities

- `tui-launch`: `TuiCommand` gains a new CLI option; no requirement-level behavior change to existing single-project flow.

## Impact

- **atunko-tui**: `TuiCommand`, `TuiController`, `Screen`, `ConfirmRunView`, `ExecutionResultsView`, `AtunkoTui` — all modified.
- **atunko-core**: no changes (all engine/session APIs already exist).
- **atunko-cli**: no changes (TuiCommand lives in atunko-tui; App.java is unchanged).
- **atunko-web**: no changes.
- **reqstool**: new `TUI_0002` requirements and SVCs added to `docs/reqstool/`.
- **Dependencies**: `WorkspaceExecutionEngine`, `WorkspaceScanner`, `WorkspaceExecutionResult`, `ProjectExecutionResult` — already in core, just wired into TUI.
