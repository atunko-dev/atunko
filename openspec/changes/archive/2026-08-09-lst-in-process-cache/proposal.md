## Why

Parsing dominates recipe-execution cost (typically 10–60 s for a mid-size Java project vs.
0.5–5 s for the recipe visit pass), and the TUI, Web UI, and workspace engine re-parse the
whole project on every execution (issue #11). The original fix — an on-disk serialized LST
cache (`PLAN_LST.md`) — proved infeasible: OpenRewrite LSTs cannot be round-tripped
through Jackson for any supported `SourceFile` subtype. `PLAN_LST_DAEMON.md` supersedes it
with a phased approach whose Phase 1 is an in-process cache: the TUI and Web UI are
long-lived processes, so keeping `ParsedSources` in memory across executions removes the
parse cost with zero new infrastructure.

## What Changes

- New `ParsedSourcesCache` in `atunko-core` caching `ParsedSources` per project directory.
- Invalidation by filesystem fingerprint: each lookup fingerprints (relative path, size,
  mtime) of all files under the project's source/resource dirs plus its build files; any
  difference triggers a full re-parse of that project.
- TUI (`TuiController`), Web UI (`RecipeBrowserView`), and `WorkspaceExecutionEngine`
  parse through the cache instead of calling `ProjectSourceParser` directly.
- Cache can be disabled (system property `atunko.lst.cache.disabled=true`), restoring
  parse-per-execution behaviour.
- CLI single-shot runs (`atunko run`) are unchanged — they exit between invocations;
  daemon support is Phase 2 (out of scope).

## Capabilities

### New Capabilities

- `lst-in-process-cache`: session-lifetime in-memory LST caching with fingerprint
  invalidation (reqstool IDs: CORE_0018 + sub-requirements).

### Modified Capabilities

<!-- none — execution semantics are unchanged; only redundant re-parses are skipped -->

## Impact

- `atunko-core`: new `ParsedSourcesCache` (+ tests); `AppServices` exposes a shared
  instance; `WorkspaceExecutionEngine` parses through it.
- `atunko-tui`: `TuiController` run path uses the cache.
- `atunko-web`: `RecipeBrowserView` run/preview paths use the cache.
- reqstool: new requirements CORE_0018–CORE_0018.3 with SVCs.
- Lands on PR #53 (`docs/plan-lst-caching`) together with the revised plan documents.
