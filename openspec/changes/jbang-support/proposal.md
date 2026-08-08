## Why

Trying atunko today requires cloning the repo and building from source (no releases are
published yet, and none are installable). Issue #18 asks for JBang distribution so users
can run `jbang atunko@atunko-dev tui` with zero install — JBang resolves the artifact and
manages the Java version, which lowers the adoption barrier dramatically.

## What Changes

- Add a JBang catalog (`jbang-catalog.json`) at the repo root defining an `atunko` alias
  that runs the released shadow JAR — enabling `jbang atunko@atunko-dev/atunko <args>`.
- Release workflow additionally uploads a stable-named asset (`atunko.jar`) so the alias
  can use the permanent `releases/latest/download/atunko.jar` URL and never needs
  updating per release.
- README documents JBang usage (tui, discover/search, run examples from the issue).
- Follow-up (org-level, outside this repo's PR): create `atunko-dev/jbang-catalog` with
  the same catalog so the shorter `jbang atunko@atunko-dev` form works.

## Capabilities

### New Capabilities

- `jbang-distribution`: zero-install execution of atunko via a JBang catalog alias
  backed by the released shadow JAR (planned reqstool ID: CLI_0006).

### Modified Capabilities

<!-- none — no runtime behaviour changes; packaging/distribution only -->

## Impact

- New file `jbang-catalog.json` (repo root).
- `.github/workflows/release.yml`: upload an additional stable-named `atunko.jar` asset.
- `README.md`: JBang quick-start section.
- No code changes; the shadow JAR (main class `io.github.atunkodev.App`) is already the
  distribution artifact.
- reqstool: new requirement CLI_0006 with SVCs.
- Note: the alias resolves only once the first non-prerelease GitHub release exists.
