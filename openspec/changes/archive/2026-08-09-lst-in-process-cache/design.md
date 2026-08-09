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
`io.github.atunkodev.core.project` next to `ParsedSources`. Each composition root wires
exactly one instance: `AppServices.init` creates the Web UI's JVM-wide cache, and
`TuiCommand` creates the TUI session's cache and injects it into both `TuiController`
and `WorkspaceExecutionEngine`, so a project parsed by either is a hit for the other.
One-shot CLI runs construct a disabled cache — every project is visited once, so caching
could never hit and would only pin LSTs until process exit.

### 2. Fingerprint invalidation, not WatchService

On every `get`, walk `ProjectInfo.parseRoots()` — the single method both the parser and
the cache derive their file sets from, so they cannot drift — plus
`ProjectInfo.buildFiles()`, and build a fingerprint map of
`absolute path → (size, mtimeMillis, contentCrc)`. The CRC catches same-size edits whose
mtime is preserved or truncated away (mtime-preserving restores, coarse filesystem
timestamps). Classpath entries (dependency jars or class directories) are fingerprinted
too, by `(size, mtimeMillis)` only — a rebuilt jar changes type attribution like a source
edit does, but its bytes are not worth hashing. Equal map → cache hit; anything else
(including added/removed files) → full re-parse and store.

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

`ConcurrentHashMap.compute` gives per-key mutual exclusion: concurrent callers for the
same cold project share one parse instead of racing check-then-put into duplicates
(relevant for the Web UI, where the cache is JVM-wide across browser sessions). Entries
hold their `ParsedSources` via `SoftReference`, so under heap pressure the GC reclaims
cached LSTs and the next `get` re-parses instead of the JVM going out of memory.

## Non-goals

- Persistent/on-disk caching (proven infeasible — `PLAN_LST.md`).
- CLI daemon (Phase 2, tracked in `PLAN_LST_DAEMON.md`).
- Re-scanning `ProjectInfo` on build-file change — build-file edits invalidate the
  parse (they are fingerprinted), but the session's `ProjectInfo` (classpath, source
  dirs) still refreshes only per the existing scan lifecycle.
- Hard cache size limits: one `ParsedSources` per project of the current session is the
  natural bound, and the `SoftReference` entries make the GC the backstop for
  pathological sessions.
