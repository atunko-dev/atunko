## Why

Users currently need to clone the repo and build from source to try atunko. JBang enables
zero-install distribution — `jbang atunko@atunko-dev tui` downloads, caches, and runs
atunko with no setup. This significantly lowers the barrier to adoption.

Issue: [#18](https://github.com/atunko-dev/atunko/issues/18)

## What Changes

- Create a new `atunko-dev/jbang-catalog` GitHub repo with a `jbang-catalog.json` file
- Define an `atunko` alias using GAV (Maven coordinates) for clean dependency resolution
- Publish atunko to Maven Central (blocked on TamboUI 0.2.0 release)
- Add JBang installation instructions to the README

## Capabilities

### New Capabilities
- `jbang-distribution`: JBang catalog and alias configuration for zero-install distribution of atunko

### Modified Capabilities

_None — this is a new distribution channel, no existing specs change._

## Impact

- **New repo**: `atunko-dev/jbang-catalog` (single JSON file, public)
- **Maven Central**: atunko modules published as artifacts (prerequisite: TamboUI 0.2.0 release)
- **README**: New "Installation" section documenting JBang usage
- **No code changes**: This is purely distribution infrastructure
