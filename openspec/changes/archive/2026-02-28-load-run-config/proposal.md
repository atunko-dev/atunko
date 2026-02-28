## Why

CORE_0008 requires loading and executing recipes from a saved `.atunko.yml` configuration file. This complements CORE_0007 (save) to enable repeatable recipe execution workflows.

## What Changes

- Add `RunConfigService.load(Path)` method that reads a `.atunko.yml` file and returns a `RunConfig`
- Uses Jackson YAML for deserialization (same as save)

## Capabilities

### New Capabilities
- `load-run-config`: Load and parse a saved `.atunko.yml` configuration file (CORE_0008)

### Modified Capabilities

## Impact

- Modified class in `core` module: `io.github.atunko.core.config.RunConfigService` (add `load` method)
- No new dependencies (Jackson YAML already available)
