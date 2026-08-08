## Why

atunko currently operates on one project at a time. Teams running organisation-wide migrations need to apply the same recipe across many related projects in a single session — today they must repeat the workflow manually for each project.

## What Changes

- New core workspace model: `Workspace`, `WorkspaceScanner`, `ProjectEntry` — discovers Gradle and Maven projects under a root directory, handling multi-project Gradle builds and Maven multi-module aggregators as single units.
- New `WorkspaceExecutionEngine` — runs a recipe sequentially across all discovered projects, collecting per-project results; one project failing does not abort the rest.
- CLI `--workspace <dir>` flag on `run` and `list` commands — scans the given directory and operates across all discovered projects; prints a per-project summary table; exits non-zero if any project failed.
- `.atunko.yml` extended with an optional `workspace:` block (`root`, `include`, `exclude`) — no version bump; the block is simply absent for single-project configs.
- Web UI workspace support — "Scan workspace" toggle on the project-dir picker, multi-select project list, per-project execution progress, aggregate results table, and scoped diff viewer.
- `.atunkoignore` marker file convention — placing this file in a directory prevents `WorkspaceScanner` from descending into it.

TUI workspace view is out of scope for this change and tracked separately in atunko-dev/atunko#33.

## Capabilities

### New Capabilities

- `workspace-scanning`: Discover Gradle and Maven projects under a root directory, skip build output dirs and `.atunkoignore`-marked dirs, handle Gradle multi-project builds and Maven multi-module aggregators.
- `workspace-execution`: Execute a recipe across all projects in a workspace sequentially; aggregate per-project results with isolated failure handling.
- `workspace-run-config`: `.atunko.yml` extended with an optional `workspace:` block (root path and glob filters); absent block means single-project mode.
- `web-workspace-ui`: Web UI surfaces for workspace scanning, project multi-select, per-project execution progress, aggregate results, and scoped diff viewing.
- `cli-workspace`: `--workspace <dir>` flag on `run` and `list` Picocli commands; per-project summary table output; non-zero exit if any project fails.

### Modified Capabilities

- `build-system-detection`: `ProjectScannerFactory` extended to support workspace-level scanning context (subproject exclusion for Gradle multi-project and Maven multi-module roots).
- `load-run-config`: Loader updated to read optional `workspace:` block when present.
- `save-run-config`: Save updated to include `workspace:` block when a workspace is configured.

## Impact

- **atunko-core**: New `core.project` types (`WorkspaceScanner`, `Workspace`, `ProjectEntry`); new `core.engine.WorkspaceExecutionEngine`; `RunConfig`/`RunConfigService` extended with optional `workspace:` block.
- **atunko-cli**: `run` and `list` Picocli commands gain `--workspace` option.
- **atunko-web**: Project-dir component, recipe selector, execution dialog, results view, diff viewer.
- **SessionHolder**: Migrated to always expose `List<ProjectEntry>` — single-project session is a workspace of size 1.
- **No atunko-tui changes** in this scope.
- **No new external dependencies** — Gradle Tooling API already on classpath; workspace scan uses lightweight file-based detection only.
