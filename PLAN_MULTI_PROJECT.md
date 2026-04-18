# Plan: Multi-Project Support (#12)

Run recipes across multiple projects in a single session. Scan a parent directory for
projects, apply recipes to all discovered projects, and aggregate results.

Tracks: atunko-dev/atunko#12 (Phase 2, PLAN.md §2.4)

## Goals

- Point atunko at a parent directory and have it discover every Gradle/Maven project
  underneath it.
- Execute the same recipe (or a saved run config) against all discovered projects in one
  session.
- Aggregate execution results per-project and in total, with clear per-project success/failure
  reporting.
- Persist multi-project selection alongside recipe selection in `.atunko.yml`.

## Non-goals

- Cross-project refactorings (recipes that need to see sources from multiple projects
  simultaneously) — each project is scanned and executed independently.
- Parallel builds across projects in v1 — sequential execution first; parallelism is a
  follow-up.
- Workspace-level Git operations (branching, committing) — users drive VCS themselves.

## Current state

- `SessionHolder` holds a single `projectDir` + `ProjectInfo` (static singleton), set once
  at startup. See `atunko-core/src/main/java/io/github/atunkodev/core/project/SessionHolder.java`.
- `ProjectScannerFactory` picks a Gradle or Maven scanner for a single directory.
- `RecipeExecutionEngine.execute(recipeName, sources)` operates on one source set.
- `RunConfig` has no concept of multiple projects.
- TUI and Web UI both assume a single active project throughout navigation and execution.

## Design

### 1. Core: workspace abstraction

Introduce `Workspace` and `WorkspaceScanner` in `atunko-core`:

- `Workspace(Path root, List<ProjectInfo> projects)` — discovered projects under a root.
- `WorkspaceScanner.scan(Path root, ScanOptions opts)` — walks the directory tree,
  delegating each candidate to `ProjectScannerFactory`. Skips `build/`, `target/`,
  `node_modules/`, `.git/`, hidden dirs by default; configurable depth limit.
- Returns a mixed list (Gradle + Maven projects can coexist in one workspace).

Extend `SessionHolder` (or introduce `WorkspaceHolder` and keep `SessionHolder` for
single-project use) to carry either a single `ProjectInfo` or a `Workspace`. Prefer a
single `SessionHolder` that always exposes a `List<ProjectInfo>` — a one-project session
is just a workspace of size 1. This collapses the two code paths.

### 2. Core: multi-project execution

Add a `WorkspaceExecutionEngine` (or extend `RecipeExecutionEngine`) that:

- Iterates projects sequentially.
- Parses sources per-project via existing `ProjectSourceParser`.
- Runs the recipe and collects an `ExecutionResult` per project.
- Returns `WorkspaceExecutionResult(List<ProjectExecutionResult>)` where
  `ProjectExecutionResult = (ProjectInfo, ExecutionResult | Failure)`.

Failures in one project must not abort the rest. Capture exceptions per-project.

### 3. Config: `.atunko.yml` v2

Bump `RunConfig.CURRENT_VERSION` to 2 (keep loader back-compat for v1):

```yaml
version: 2
description: "..."
workspace:           # optional; absent => single-project (legacy) mode
  root: "./services"
  include: ["**/*"]   # optional glob filters
  exclude: ["build/**"]
recipes:
  - name: ...
```

v1 configs continue to load; `workspace` omitted means current behaviour.

### 4. CLI

- `atunko --workspace <dir>` flag (alternative to positional project dir) triggers scan.
- `atunko list --workspace <dir>` — list discovered projects.
- `atunko run <recipe> --workspace <dir>` — run recipe against all projects, print a
  per-project summary table + totals.
- Exit code: non-zero if any project failed.

### 5. TUI

- New "Workspace" view when launched with `--workspace`: tree/list of discovered projects,
  each toggleable (included/excluded from this run).
- Run screen aggregates per-project progress (N of M projects, current project name).
- Results screen: collapsible per-project sections; top-level totals.
- Single-project launch path unchanged.

### 6. Web UI

- Project dir picker gains a "Scan workspace" toggle.
- Project selector becomes a multi-select (checkbox column) when a workspace is loaded.
- Execution dialog shows per-project progress; diff viewer scopes to one project at a
  time (dropdown to switch).
- Aggregate results table with per-project counts and drill-down.

## Impact map

| Area                 | Files / packages touched                                                  |
|----------------------|---------------------------------------------------------------------------|
| Core — scanning      | `core.project.WorkspaceScanner` (new), `ProjectScannerFactory`            |
| Core — session       | `core.project.SessionHolder` (extend or split)                            |
| Core — execution     | `core.engine.RecipeExecutionEngine`, new `WorkspaceExecutionEngine`       |
| Core — config        | `core.config.RunConfig`, `RunConfigService` (v2 schema + migration)       |
| CLI                  | `atunko-cli` Picocli commands (`--workspace` flag, `run`/`list`)          |
| TUI                  | `atunko-tui.view` (new Workspace view, updates to Run/Results views)      |
| Web                  | `atunko-web` project-dir + selector + execution + diff views              |
| Specs                | openspec: new `workspace-support` change; spec deltas for CLI/TUI/Web     |
| Requirements         | reqstool: new `CORE_XXXX` / `CLI_XXXX` / `TUI_XXXX` / `WEB_XXXX` reqs+SVCs |

## Phased delivery

1. **Core workspace scanner + model** (no UI wiring). Unit tests on fixture workspaces
   under `atunko-core/src/test/resources/workspaces/`.
2. **Core workspace execution engine** + per-project aggregation. Integration tests.
3. **`.atunko.yml` v2** with back-compat loader + round-trip tests.
4. **CLI `--workspace`** support.
5. **TUI workspace view** (selection + run + aggregated results).
6. **Web UI workspace** support (multi-select, aggregated results, scoped diff).
7. **Docs + openspec archive** once all phases land.

Each phase is a separate PR against `main`; this PR only lands the plan.

## Open questions

- Should workspace scanning follow symlinks? (Default: no.)
- How deep to walk by default? (Proposal: unlimited, but skip known build output dirs.)
- Does a Gradle multi-project build (root `settings.gradle` with subprojects) count as
  one project or many? (Proposal: one — respect Gradle's own project model; don't descend
  into its subprojects as separate atunko projects.)
- Run config: should `workspace.include/exclude` be glob-based or explicit project-name
  lists? (Proposal: both — globs filter discovery, explicit list pins selection.)
- Parallelism: worth a flag in v1 or defer? (Proposal: defer; sequential is simpler and
  OpenRewrite already parses a lot per project.)

## Risks

- `SessionHolder` is a static singleton used widely — changing its shape is a wide
  blast radius. Mitigation: keep the single-project getters working (wrapper returning
  first project) during migration.
- OpenRewrite's `InMemoryLargeSourceSet` is the only option in 8.x — combining sources
  across projects would blow up memory. Per-project execution sidesteps this but means
  cross-project recipes are out of scope for v1 (acceptable — see non-goals).
- Web UI state management becomes more complex with multi-select + per-project diff
  scoping; plan dedicated Vaadin review before merging phase 6.
