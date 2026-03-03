## Why

Users need to save their recipe selections and options for repeatable execution. CORE_0007 requires the tool to persist the current recipe setup to a `.atunko.yml` file so it can be shared, version-controlled, and reloaded later (CORE_0008).

## What Changes

- Add `RunConfig` record to represent recipe names and options
- Add `RunConfigService` with a `save(RunConfig, Path)` method that writes `.atunko.yml`
- Use SnakeYAML for YAML serialization

## Capabilities

### New Capabilities
- `save-run-config`: Save current recipe setup to a portable `.atunko.yml` file (CORE_0007)

### Modified Capabilities

## Impact

- New classes in `core` module: `io.github.atunko.core.config.RunConfig`, `io.github.atunko.core.config.RunConfigService`
- New dependency: SnakeYAML (already transitive via OpenRewrite, but may need explicit declaration)
