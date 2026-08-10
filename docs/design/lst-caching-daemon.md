# LST Caching — Daemon Approach

Follows from [#11 feat: LST caching](https://github.com/atunko-dev/atunko/issues/11) and
supersedes the serialization-based design in `docs/design/lst-caching-serialization-superseded.md`.

## Why the original design failed

`docs/design/lst-caching-serialization-superseded.md` assumed OpenRewrite LSTs could be round-tripped via Jackson polymorphic
JSON (`@JsonTypeInfo(use = Id.CLASS)` on `Tree`). Implementation proved this wrong for
all five supported subtypes:

| Subtype | Failure |
|---------|---------|
| `J.CompilationUnit` | `StackOverflowError` — `NameTree.names()` creates infinite recursion |
| `Yaml.Documents` | Same infinite recursion |
| `Json.Document` | Same infinite recursion |
| `Xml.Document` | No `@JsonCreator`; constructor parameter names absent from bytecode |
| `Properties.File` | Same missing creator |

`ObjectMappers.propertyBasedMapper()` (internal OpenRewrite API) has the same
failures. LST disk serialization exists only in Moderne's closed-source CLI (`-ast.jar`).
There is no open-source supported path for Jackson-based LST round-trip.

Binary serializers (Kryo, Apache Fury) would likely work but add a maintenance-heavy
dependency whose format stability across OpenRewrite upgrades is unverified. Last Kryo
release: November 2024 (18 months old as of writing). Fury is newer but less
battle-tested.

## The daemon idea

Keep parsed `List<SourceFile>` in memory in a long-lived JVM process. No serialization
required. File watching triggers selective re-parsing only for changed files; build-file
changes trigger a full re-scan + re-parse.

```
atunko-daemon (long-lived JVM, one per project root)
  ├─ ProjectInfo      ← from last scan (GradleProjectScanner or MavenProjectScanner)
  ├─ List<SourceFile> ← from last parse (ProjectSourceParser)
  ├─ WatchService     ← watches src/** and build files
  └─ local socket     ← listens for client connections

atunko-cli   ─── connect ──▶ daemon ──▶ RecipeExecutionEngine ──▶ result
```

On each client request:
1. Has a source file changed? → re-parse that file, replace in the list.
2. Has a build file changed? (`build.gradle`, `pom.xml`, etc.) → re-run
   `ProjectScannerFactory.detect(projectDir).scan(projectDir)` → new `ProjectInfo`
   → full re-parse.
3. Nothing changed? → use cached `List<SourceFile>` directly.

First run: full scan + parse (same cost as today). Every subsequent run: near-zero
parse cost.

## Maven and Gradle

Both are already supported via `ProjectScannerFactory` / `ProjectScanner`. The daemon
is build-system-agnostic after the initial scan:

| Point | Gradle | Maven |
|-------|--------|-------|
| Initial scan | Gradle Tooling API | Maven POM read |
| Build-file change detection | `*.gradle`, `*.gradle.kts`, `settings.*` | `pom.xml` (all modules) |
| Re-scan on change | New Tooling API call | New POM read |
| Source file invalidation | WatchService (agnostic) | WatchService (agnostic) |
| LST management | Agnostic | Agnostic |

The Gradle Tooling API is used on-demand (initial start + re-scan), not kept open
permanently. This is the same pattern as today; the daemon just makes re-scans rare.

## Key insight: TUI and Web already benefit

`TuiController` and `WebUiCommand` are long-lived processes that stay running between
recipe executions. They already get in-process caching for free if the parsed sources are
stored across runs within the same session. The daemon is only strictly needed for the
CLI (`atunko run`) which exits between invocations.

This suggests a phased approach:
1. **Phase 1 (in-process):** cache parsed sources within `TuiController` and
   `WebUiCommand` sessions. Zero new infrastructure. Delivers the benefit for TUI/Web
   users immediately.
2. **Phase 2 (daemon):** add the daemon for CLI use cases.

## Phase 1 implementation (this PR)

Since this plan was written, main gained the pieces Phase 1 slots into:
`ProjectSourceParser.parseWithCapabilities()` returns a `ParsedSources` record
(sources + `SourceCapability` set), sessions are a list of `ProjectEntry`
(projectDir + `ProjectInfo`) held by `SessionHolder`, and the TUI, Web UI, and
`WorkspaceExecutionEngine` each call the parser directly per execution.

Phase 1 adds a `ParsedSourcesCache` in `atunko-core` in front of those call sites:

- **Keyed per project directory** — each `ProjectEntry` of a workspace caches and
  invalidates independently (`CORE_0018.2`).
- **Invalidation by fingerprint, not WatchService.** On each lookup the cache walks the
  project's source/resource dirs and build files and fingerprints (relative path, size,
  mtime) tuples. Any difference → re-parse (`CORE_0018.1`). A walk is orders of
  magnitude cheaper than a parse and avoids WatchService lifecycle/portability issues;
  the WatchService approach remains the daemon's (Phase 2) concern.
- **Whole-project granularity.** A change re-parses the whole project — the safe answer
  to the partial re-parse accuracy question below, and the parse being cached at all is
  the win Phase 1 is after.
- **Disable switch** (`CORE_0018.3`) for tests and troubleshooting.

Requirements: `CORE_0018`–`CORE_0018.3` in `docs/reqstool/requirements.yml`
(renumbered from the draft's `CORE_0010`, which main assigned to workspace scanning).

## Decisions

These were the open questions; all four were resolved when Phase 2 was implemented
(OpenSpec change `lst-cli-daemon`, requirements CORE_0023 and CLI_0009).

1. **Daemon lifecycle — auto-start.** Gradle's model, with `atunko daemon stop`,
   `--no-daemon` and `atunko.daemon.disabled` as escapes. An opt-in daemon would leave
   the default `atunko run` exactly as slow as before, which is the thing this change
   exists to fix. The surprise is mitigated by printing a one-line notice on the first
   auto-start naming the daemon and how to stop it.

2. **Concurrent daemons — one per project root, at most 3.** Same default Gradle
   settled on; bounds worst-case memory at roughly 3 × 200 MB. A fourth start evicts the
   least recently used *idle* daemon; one serving a request is never evicted. Rejected:
   a single daemon serving every project, whose heap becomes the sum of every project
   ever touched and whose OOM takes down every project's cache at once.

3. **Idle timeout — 30 minutes**, configurable via `atunko.daemon.idle-timeout`.
   Gradle's 3 hours suits a tool invoked continuously; atunko is invoked in bursts, and
   holding 200 MB for hours after one run is a bad trade.

4. **Client protocol — loopback TCP with a per-daemon token.** Unix domain sockets are
   faster and permission-gated for free, but exclude Windows, which matters for CI
   agents — and local TCP is what the Gradle daemon uses. Loopback alone is not an
   authorization boundary on a shared host, so every request carries a 256-bit token
   stored in an owner-only registry file, compared in constant time.

5. **Partial re-parse — not implemented.** Any change re-parses the whole project. Java
   types resolve across files, so splicing one re-parsed file back into the list is only
   sound when nothing resolves against it, and establishing that reliably costs the type
   analysis the parse was going to do anyway. The daemon's win is the *unchanged* case,
   which is what a tweak-and-re-run loop actually produces.

## Deviation from this plan

Phase 1 assumed the CLI parsed through `ProjectSourceParser` and a `ParsedSourcesCache`.
It does not: `RunCommand`'s single-project path uses `JavaSourceParser`, which walks
`**/*.java` with no build-system scan. The daemon therefore parses through
`JavaSourceParser` too — parsing more thoroughly inside the daemon would make its results
differ from `--no-daemon`, and a daemon that changes your output is a far worse bug than
one that parses no better than the CLI already did.

The invalidation logic is still shared: `InputFingerprint` was extracted from
`ParsedSourcesCache` into `atunko-core`, so both caches answer "did the input change?"
with one implementation instead of two that can drift.
