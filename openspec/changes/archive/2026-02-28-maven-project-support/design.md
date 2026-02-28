## Context

The tool supports Gradle project scanning via `GradleProjectScanner` (CORE_0004). Maven is the other major Java build tool and needs equivalent support. The scanner must resolve compile classpath JARs and source directories, returning a `ProjectInfo` — the same contract as `GradleProjectScanner`.

## Goals / Non-Goals

**Goals:**
- Resolve Maven compile classpath and source directories via `MavenProjectScanner`
- Reuse existing `ProjectInfo` record
- Support Maven Wrapper (`mvnw`) when present, fall back to system `mvn`

**Non-Goals:**
- Embedding a full Maven resolver (too complex for this use case)
- Supporting multi-module Maven reactors (single module for Phase 1)
- Resolving test-scoped dependencies

## Decisions

### Use ProcessBuilder to invoke `mvn dependency:build-classpath`

**Rationale:** This mirrors the pattern of requiring the build tool to be installed (same as Gradle Tooling API requiring Gradle). ProcessBuilder has zero additional dependencies, and `dependency:build-classpath` uses Maven's own resolution (respects settings.xml, mirrors, profiles). Maven Invoker (`maven-invoker`) was considered but is a thin wrapper that still spawns `mvn` externally — adds a dependency with little value for a single goal invocation.

### Convention-based source directory detection

**Rationale:** Maven projects follow `src/main/java` by default. We resolve source directories by checking the conventional path rather than parsing the POM's `<sourceDirectory>` element. This handles 95%+ of projects and avoids XML parsing complexity.

### Prefer `./mvnw` over system `mvn`

**Rationale:** Maven Wrapper ensures the correct Maven version for the project. Check for `mvnw` (or `mvnw.cmd` on Windows) first, fall back to `mvn` on PATH.

## Risks / Trade-offs

- **Requires Maven on PATH**: If no `mvnw` and no system `mvn`, scanning fails. → Throw clear exception with message.
- **External process overhead**: ~2-5 seconds per invocation. → Acceptable for a project scanning tool; can cache later.
- **Non-standard source dirs**: Projects overriding `<sourceDirectory>` won't be detected. → Acceptable for Phase 1; can parse POM later.
