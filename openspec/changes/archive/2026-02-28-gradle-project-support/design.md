## Context

The recipe execution engine (CORE_0003) accepts `List<SourceFile>` but there's no way to obtain
parsed source files from a real project. `JavaParser` needs compile classpath JARs for accurate
type resolution, and the tool needs to know where source files live. The Gradle Tooling API
(`org.gradle:gradle-tooling-api:9.3.1`) is already a dependency.

## Goals / Non-Goals

**Goals:**
- Connect to a Gradle project and resolve compile classpath JAR paths
- Resolve main source directories
- Return structured `ProjectInfo` with classpath and source dirs
- Keep the scanner decoupled from recipe execution

**Non-Goals:**
- Multi-module Gradle project support (scan one module at a time for now)
- Test classpath or test source directories
- Resource directories
- Build script execution or task running
- Caching or connection pooling

## Decisions

### 1. Use `IdeaProject` model via Tooling API

**Decision**: Use `GradleConnector` to connect to the project, then fetch the `IdeaProject` model
which provides dependencies, content roots, and Java language settings.

**Rationale**: The `IdeaProject` model exposes classpath dependencies (via `IdeaSingleEntryLibraryDependency`)
and source directories (via `IdeaContentRoot`). The `EclipseProject` model is an alternative but
`IdeaProject` provides a more complete dependency view.

**Alternatives considered**:
- `EclipseProject` model — also viable but `IdeaProject` is more commonly used in tooling integrations
- Running `gradle dependencies` and parsing output — fragile, slow, not type-safe

### 2. New `GradleProjectScanner` class in `core.project` package

**Decision**: Create `GradleProjectScanner` with a `scan(Path projectDir)` method that returns
a `ProjectInfo` record.

**Rationale**: The `project` package is already scaffolded. A single `scan` method matches the
interface — give it a directory, get back project metadata. The `ProjectInfo` record holds
classpath paths and source dirs.

### 3. `ProjectInfo` record with classpath and source dirs

**Decision**: `ProjectInfo(List<Path> classpath, List<Path> sourceDirs)` — simple, immutable.

**Rationale**: These are the two things consumers need: classpath for `JavaParser`, source dirs
to find files to parse. Can be extended later with Java version, resource dirs, etc.

### 4. Test with the atunko project itself as fixture

**Decision**: Use the atunko project's `core` module as the test fixture — it's a real Gradle
project that's guaranteed to exist when tests run.

**Rationale**: Creating a synthetic Gradle fixture project is complex (needs wrapper, build files,
dependencies). The current project is a working Gradle project. The test can connect to `.` or
a known subproject path. This approach is simpler and more realistic.

**Trade-off**: Test depends on the project's own structure, but this is acceptable for a PoC.

## Risks / Trade-offs

- **Tooling API startup time**: Connecting to a Gradle project spawns a daemon if one isn't running.
  This can take several seconds. → Acceptable for PoC; tests will be slower than unit tests.
- **Gradle daemon version**: The Tooling API uses the Gradle wrapper version of the target project.
  If the target project uses a very old Gradle version, some APIs may not be available.
  → For now, assume modern Gradle (6+).
- **Single-module only**: Multi-module scanning (selecting which module to scan) is deferred.
  The first `IdeaModule` returned is used. → Good enough for single-module projects and testing.
