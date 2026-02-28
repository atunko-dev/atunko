## Why

The recipe execution engine (CORE_0003) accepts pre-parsed `SourceFile` objects but has no way to
obtain them from a real project. To run recipes against actual Gradle projects, the tool needs to
resolve the compile classpath (for accurate Java parsing) and locate source directories. CORE_0004
bridges this gap using the Gradle Tooling API.

## What Changes

- Add a project scanner that connects to a Gradle project via the Tooling API and resolves:
  - Compile classpath JAR paths (needed by `JavaParser` for type resolution)
  - Source directory paths (the files to parse and transform)
- Return a structured `ProjectInfo` record with classpath and source dirs
- Keep this decoupled from recipe execution — the scanner provides input data,
  the execution engine consumes it

## Capabilities

### New Capabilities
- `gradle-project-support`: Resolve classpaths and source directories from Gradle projects
  using the Tooling API (CORE_0004)

### Modified Capabilities

## Impact

- **Code**: New classes in `core/src/main/java/io/github/atunko/core/project/`
- **Tests**: New test class with a Gradle test fixture project in `core/src/test/resources/`
  linked to SVC_CORE_0004
- **Dependencies**: No new dependencies — uses existing `org.gradle:gradle-tooling-api:9.3.1`
- **APIs**: New internal core API — no CLI/TUI changes in this change
