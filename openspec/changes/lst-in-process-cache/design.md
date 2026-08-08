# Design — lst-in-process-cache

## Context

`ProjectSourceParser.parseWithCapabilities(ProjectInfo)` is called on every recipe
execution by three call sites: `TuiController` (run path), `RecipeBrowserView`
(run + preview paths), and `WorkspaceExecutionEngine` (per project entry). TUI and Web
sessions are long-lived, so every execution after the first re-does work whose inputs
rarely changed. `PLAN_LST_DAEMON.md` (Phase 1) motivates caching the parse result in
memory for the lifetime of the session.

## Decisions

### 1. One cache class in core, in front of the parser

`ParsedSourcesCache` wraps a `ProjectSourceParser` and exposes
`ParsedSources get(ProjectEntry entry)`. Callers swap
`sourceParser.parseWithCapabilities(info)` for `cache.get(entry)`. The cache lives in
`io.github.atunkodev.core.project` next to `ParsedSources`. A single shared instance is
wired through `AppServices` so TUI and Web reuse it without new plumbing.

### 2. Fingerprint invalidation, not WatchService

On every `get`, walk the project's source/resource dirs (`allSourceAndResourceDirs()`,
falling back to `sourceDirs()` — the same roots the parser reads) plus
`ProjectInfo.buildFiles()`, and build a fingerprint map of
`absolute path → (size, mtimeMillis)`. Equal map → cache hit; anything else (including
added/removed files) → full re-parse and store.

Why not `WatchService`: lifecycle (threads, overflow events, per-dir registration on
every subtree) and platform quirks for a benefit the fingerprint walk already provides —
a directory walk is milliseconds against a parse that is tens of seconds. The daemon
(Phase 2) is where WatchService belongs.

### 3. Whole-project granularity

Any change re-parses the entire project. Per-file splicing is unsafe for Java
(cross-file type resolution) per the open questions in `PLAN_LST_DAEMON.md`; safe
whole-project re-parse keeps semantics identical to today's parse-per-run.

### 4. Keyed by project directory

Key = `ProjectEntry.projectDir()`. Workspace projects hit and invalidate independently
(CORE_0018.2). Entries also store the `ProjectInfo` used to parse; a differing
`ProjectInfo` for the same directory (e.g. after a re-scan) is treated as a miss.

### 5. Disable switch

`ParsedSourcesCache` takes an `enabled` flag; the `AppServices` wiring reads system
property `atunko.lst.cache.disabled` (default: enabled). Disabled means delegate to the
parser on every call — no fingerprinting, no stored entries (CORE_0018.3).

### 6. Thread safety

`ConcurrentHashMap` for the entry map; parse-and-store is not globally locked — two
concurrent misses on the same project may both parse and the last write wins, which is
correct (both results are equivalent) and matches the low-concurrency reality (one
execution at a time per UI session).

## Non-goals

- Persistent/on-disk caching (proven infeasible — `PLAN_LST.md`).
- CLI daemon (Phase 2, tracked in `PLAN_LST_DAEMON.md`).
- Re-scanning `ProjectInfo` on build-file change — build-file edits invalidate the
  parse (they are fingerprinted), but the session's `ProjectInfo` (classpath, source
  dirs) still refreshes only per the existing scan lifecycle.
- Cache size limits: one `ParsedSources` per project of the current session is the
  natural bound; sessions hold a handful of projects at most.
