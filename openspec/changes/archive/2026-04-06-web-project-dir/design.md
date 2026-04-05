## Context

Both TUI and Web UI need to scan a user-supplied project directory at startup. Previously TUI
hardwired `GradleProjectScanner` — no Maven support, no sharing with Web UI. The fix unifies
everything in `atunko-core` so both UIs consume the same scanning infrastructure.

## Goals / Non-Goals

**Goals:**
- Single `ProjectScanner` interface in core; both scanners implement it
- `ProjectScannerFactory.detect()` selects the right scanner by inspecting build files
- `SessionHolder` in core stores `projectDir` + `ProjectInfo` once at startup; both UIs read from it
- `--project-dir` option on `WebUiCommand` (TUI already had it)

**Non-Goals:**
- Lazy/deferred scanning (deferred to future issue — startup-time scanning for now)
- Multi-module project selection
- Caching or connection pooling

## Decisions

### 1. `ProjectScanner` interface in `atunko-core`

**Decision:** Extract a `ProjectScanner` interface with `ProjectInfo scan(Path projectDir)`.
Both `GradleProjectScanner` and `MavenProjectScanner` implement it.

**Rationale:** Decouples callers from concrete scanner types. Enables `ProjectScannerFactory`
to return the right implementation without callers knowing which one.

### 2. `ProjectScannerFactory.detect()` throws on unknown build system

**Decision:** If neither `pom.xml` nor any Gradle build file is found, throw
`IllegalArgumentException` with a descriptive message. No silent default.

**Rationale:** Silently defaulting to Gradle would hide misconfigured projects.
Fail fast with a clear error is safer and easier to debug.

### 3. `SessionHolder` lives in `atunko-core`

**Decision:** New `SessionHolder` class in `io.github.atunkodev.core.project`.

**Rationale:** Both `atunko-tui` and `atunko-web` depend on `atunko-core`. Placing
`SessionHolder` in core means both UIs share the same state carrier without duplication.
The static singleton pattern mirrors `RecipeHolder` (which stays in `atunko-web` for
Vaadin-specific recipe state).

**Trade-off:** Static mutable state in a "pure" library is a compromise — accepted because
this is a single-process CLI application with a single startup lifecycle.

### 4. Scanning at startup, not lazily

**Decision:** `TuiCommand.run()` and `WebUiCommand.run()` call `ProjectScannerFactory.detect()`
immediately before launching the UI.

**Rationale:** Simpler code path, predictable failure mode (scan fails before UI starts, not
during recipe execution). Lazy loading is filed as a separate issue.

### 5. TUI scanner injection removed

**Decision:** Remove `GradleProjectScanner` from `TuiCommand` and `TuiController` constructors.
`TuiController.runSelectedRecipes()` reads `SessionHolder.getProjectInfo()` instead.

**Rationale:** The factory + SessionHolder pattern eliminates the need for injection.
`TuiController` retains the null-fallback path for tests that don't populate `SessionHolder`.

### 6. Requirement IDs renamed (pre-1.0.0 restructure)

**Decision:** Old `CORE_0004` (Gradle) → `CORE_GRADLE_0001`; old `CORE_0005` (Maven) →
`CORE_MAVEN_0001`. New generic `CORE_0004` for build system auto-detection.
`CORE_0005` left vacant.

**Rationale:** `CORE_0004` as a generic parent is more readable. Typed sub-requirements
with descriptive prefixes (`CORE_GRADLE_0001`, `CORE_MAVEN_0001`) are clearer than
numeric suffixes. Pre-1.0.0 restructuring is permitted by reqstool conventions.
