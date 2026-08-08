## Context

atunko's session model (`SessionHolder`, `AppServices`) is built around a single `ProjectInfo` set once at startup. `ProjectScannerFactory.detect(Path)` identifies one project directory; `RecipeExecutionEngine.execute(name, sources)` operates on one source set. Extending to multiple projects requires a workspace abstraction at the core layer with minimal changes to existing call sites.

Key existing constraints:
- `SessionHolder` is a static singleton referenced by both TUI and Web UI
- `ProjectSourceParser.parse(ProjectInfo)` is fully stateless — creates all objects fresh per call
- Gradle `GradleProjectScanner` already uses the Tooling API (`GradleConnector`, `IdeaProject`)
- `InMemoryLargeSourceSet` is the only option in OpenRewrite 8.x — cross-project source merging would blow memory; per-project execution sidesteps this

## Goals / Non-Goals

**Goals:**
- Discover all Gradle/Maven projects under a root directory in one scan
- Execute a recipe across all discovered projects sequentially with per-project result isolation
- Persist workspace config in `.atunko.yml` v2 (backward-compatible with v1)
- Expose workspace workflows in the Web UI and CLI

**Non-Goals:**
- TUI workspace view (tracked in atunko-dev/atunko#33)
- Parallel execution across projects (sequential first; parallelism is a follow-up)
- Cross-project recipes (each project scanned and executed independently)
- Workspace-level Git operations

## Decisions

### D1: `ProjectEntry` wrapper over adding `projectDir` to `ProjectInfo`

`ProjectInfo` is pure build metadata (classpath, source dirs). Adding a filesystem identity field would mix concerns. Instead, a minimal wrapper `ProjectEntry(Path projectDir, ProjectInfo info)` pairs identity with metadata at the boundary where both are needed.

**Alternative considered:** Add `projectDir` to `ProjectInfo` — rejected because every existing constructor and test instantiation would need updating for a concern that isn't intrinsic to build metadata.

### D2: `SessionHolder` migrated to `List<ProjectEntry>`

Single-project sessions become a workspace of size 1. Existing callers that need `ProjectInfo` get `SessionHolder.getProjectEntries().getFirst().info()` via a backward-compat helper, removing the need for two diverging code paths.

**Alternative considered:** Keep `SessionHolder` single-project, introduce a separate `WorkspaceHolder` — rejected because it doubles the session state surface and complicates all callers that need to check which mode is active.

### D3: Lightweight file-based scanning; Gradle Tooling API only on confirmed roots

`WorkspaceScanner` uses only file system checks during the directory walk — no Gradle/Maven process invocations. Gradle subproject exclusion is done by regex-parsing `settings.gradle[.kts]` to extract `include ':subproject'` declarations. Maven multi-module detection reads the `<modules>` block from `pom.xml`. The full Tooling API / Maven process runs only once `GradleProjectScanner.scan()` / `MavenProjectScanner.scan()` is called on a confirmed root.

**Why:** The Gradle Tooling API has a 1–3 s startup cost per project. Paying it during directory traversal would make workspace scanning prohibitively slow for large monorepos.

**Gradle edge case:** A `build.gradle[.kts]` without `settings.gradle` is treated as a standalone single-project build unless its directory was already claimed as a subproject by a parent `settings.gradle`. Walk is top-down so parents are always processed before children.

### D4: `.atunkoignore` marker file

Any directory containing `.atunkoignore` is skipped by `WorkspaceScanner` (not descended into). This is consistent with the `.gitignore` / `.openrewriteignore` convention users already know.

The scanner also skips `build/`, `target/`, `.gradle/`, `node_modules/`, `.git/`, and all hidden directories by default — not configurable in v1.

### D5: `WorkspaceExecutionEngine` as a new class (not extending `RecipeExecutionEngine`)

`RecipeExecutionEngine` operates on a single `List<SourceFile>`. `WorkspaceExecutionEngine` composes it: for each `ProjectEntry`, call `sourceParser.parse(entry.info())` then `engine.execute(name, sources)`. Failures are caught per-project and wrapped in `ProjectExecutionResult` — one project failing never aborts the loop.

`ProjectSourceParser` is fully stateless so no changes to `AppServices` are needed for sequential execution.

### D6: `.atunko.yml` — additive `workspace:` block, no version bump

```yaml
version: 1
description: "..."
workspace:           # optional; absent = single-project mode
  root: "./services"
  include: ["**/*"]  # optional glob filters
  exclude: []
recipes:
  - name: ...
```

No version bump. The project is pre-1.0 and the `workspace:` field is simply optional — existing configs that omit it continue to work unchanged via Jackson's `@JsonInclude(NON_NULL)` and `@Nullable` annotation on the field.

## Risks / Trade-offs

- **`SessionHolder` blast radius** → `List<ProjectEntry>` shape is a wide change. Mitigation: introduce `getFirstEntry()` / `getProjectInfo()` shims that return the first element; remove shims in a follow-up once all callers are updated.
- **`settings.gradle` regex parsing** → complex DSL edge cases (custom `projectDir` overrides, `includeBuild`) are not handled in v1. Mitigation: document limitation; worst case is an extra project discovered that should have been excluded — not data loss.
- **Maven `<modules>` XML parsing** → simple element extraction is sufficient; full Maven model not needed.
- **Web UI state complexity** → multi-select + per-project diff scoping adds considerable Vaadin state. Mitigation: dedicated review before merging Web UI phase.

## Migration Plan

1. Merge core phases (scanner, execution engine, config v2) — no UI changes, fully backward-compatible.
2. Merge Web UI phase — users opt in via "Scan workspace" toggle; single-project path unchanged.
3. TUI phase follows separately via atunko-dev/atunko#33.

No rollback complexity — `workspace:` block in config is optional; removing it restores single-project behaviour.

## Open Questions

- Should `WorkspaceScanner` follow symlinks? (Proposal: no — v1.)
- Should `workspace.include/exclude` support explicit project-name lists in addition to globs? (Proposal: globs only in v1; explicit lists in a later release.)
