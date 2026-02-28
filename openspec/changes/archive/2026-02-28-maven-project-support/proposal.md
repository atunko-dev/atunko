## Why

The tool needs to support Maven projects in addition to Gradle. CORE_0005 requires resolving classpaths and source directories from Maven projects so OpenRewrite recipes can be executed against them.

## What Changes

- Add `MavenProjectScanner` that resolves compile classpath JARs and source directories from a Maven project
- Follows the same `ProjectInfo` contract as `GradleProjectScanner`
- Uses Maven Invoker to run `dependency:build-classpath` for classpath resolution and convention-based source directory detection

## Capabilities

### New Capabilities
- `maven-project-support`: Resolve classpaths and source directories from Maven projects (CORE_0005)

### Modified Capabilities

## Impact

- New class in `core` module: `io.github.atunko.core.project.MavenProjectScanner`
- New dependency: `org.apache.maven.shared:maven-invoker` for running Maven goals
- Reuses existing `ProjectInfo` record from CORE_0004
