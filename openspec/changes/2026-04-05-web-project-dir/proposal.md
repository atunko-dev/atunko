## Why

Web UI needs a `--project-dir` option so recipe execution targets a user-specified project
rather than always defaulting to the current working directory. The TUI already had this
option (via `--project-dir` on `TuiCommand`) but was hardwired to `GradleProjectScanner`,
breaking Maven projects and duplicating build-system detection between the two UIs.

This change unifies project scanning under `atunko-core`:
- A `ProjectScanner` interface abstracts Gradle and Maven scanners
- `ProjectScannerFactory` auto-detects the build system and fails fast when nothing is recognized
- `SessionHolder` (in core) holds the scanned `projectDir` + `ProjectInfo` at startup, shared by
  both TUI and Web UI — no duplication

## What Changes

- `atunko-core`: new `ProjectScanner` interface, `ProjectScannerFactory`, `SessionHolder`
- `atunko-core`: `GradleProjectScanner` and `MavenProjectScanner` implement `ProjectScanner`
- Requirement rename: `CORE_0004` (Gradle) → `CORE_GRADLE_0001`; `CORE_0005` (Maven) →
  `CORE_MAVEN_0001`; new generic `CORE_0004` for build system auto-detection
- `atunko-tui`: scanner injection removed; `TuiCommand.run()` calls factory + `SessionHolder.init()`
- `atunko-web`: `WebUiCommand` gains `--project-dir`; calls factory + `SessionHolder.init()` at startup
- `RecipeHolder` reverted to recipes-only (no `projectDir`)

## Capabilities

### New Capabilities
- `build-system-detection`: Auto-detect Gradle vs Maven and select the appropriate scanner (CORE_0004)
- `web-project-dir`: `--project-dir` option for Web UI startup scanning (WEB_0001.7)

### Modified Capabilities
- `gradle-project-support`: requirement ID renamed CORE_0004 → CORE_GRADLE_0001
- `maven-project-support`: requirement ID renamed CORE_0005 → CORE_MAVEN_0001

## Impact

- **Code**: New classes in `atunko-core/src/main/java/.../project/`; TUI and Web UI updated
- **Tests**: New `ProjectScannerFactoryTest` in core; `WebUiCommandTest` covers WEB_0001.7
- **APIs**: No CLI/user-facing change beyond the new `--project-dir` option on `webui` subcommand
