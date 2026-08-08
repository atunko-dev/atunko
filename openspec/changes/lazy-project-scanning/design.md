## Context

Startup today (single-project mode): `TuiCommand.run()` / `WebUiCommand.run()` →
`ProjectScannerFactory.detect(dir).scan(dir)` → `SessionHolder.init(dir, projectInfo)` →
launch UI. The scan is the expensive step (Gradle Tooling API connection / `mvn
dependency:build-classpath`), and its result (`ProjectInfo`: classpath + source dirs) is
consumed only by `ProjectSourceParser` on the execution path. `SessionHolder` is a static
holder shared by TUI and Web. Stage 2 of `recipe-applicability` (PR #76) already seeds
pre-scan applicability badges via `SourceCapabilityHints.forProjectDir(dir)` — a cheap
file-existence check, independent of the scan.

## Goals / Non-Goals

**Goals**

- TUI and Web launch immediately for `--project-dir` (and default-dir) sessions.
- The scan runs exactly once, on first recipe execution, memoized and thread-safe
  (Web executes on background threads).
- Scan failure is reported at execution time in the UI without crashing the session;
  a subsequent run retries the scan.

**Non-Goals**

- `--workspace` mode stays eager: `WorkspaceScanner.scan` both discovers projects and
  scans them, and the workspace UI needs the project list at startup. Splitting
  discovery from scanning is a separate change.
- No background/speculative scanning at idle time (keep the model simple; can be
  layered on later).
- No change to `ProjectScanner` implementations.

## Decisions

1. **Laziness lives in `SessionHolder`** (core, shared by both UIs — the issue's
   "fix should be in atunko-core"). New static state: a pending project dir plus a
   memoized scan. API sketch:
   - `initLazy(Path dir)` — records the dir, clears entries; `getProjectDir()`
     returns it immediately (existing accessor semantics preserved).
   - `ensureScanned()` — synchronized double-checked memoization: if entries are
     empty and a pending dir is set, run `ProjectScannerFactory.detect(dir).scan(dir)`
     and populate entries; rethrows scanner exceptions without caching them, so the
     next call retries.
   - Existing eager `init(...)`/`initWorkspace(...)` remain for workspace mode and
     tests.
   *Alternative considered*: a `Supplier<ProjectInfo>` injected per UI — rejected,
   duplicates the trigger logic in TUI and Web and breaks the static-holder pattern
   the rest of the code assumes.

2. **Trigger point = the execution path.** TUI: `TuiController`'s run flow calls
   `SessionHolder.ensureScanned()` before parsing; the existing execution progress
   UI covers the scan duration (message e.g. "Scanning project…" if cheaply
   feasible; otherwise the standard running state). Web: the background execution
   thread calls `ensureScanned()` before parsing, inside the existing progress
   dialog; failures surface through the existing error path (notification/dialog)
   instead of a startup stack trace.

3. **Startup validation kept cheap but real**: `ProjectScannerFactory.detect(dir)`
   (file-existence checks only) still runs at startup so a directory with no build
   files fails fast with the current clear error; only `.scan()` is deferred.
   *Alternative considered*: deferring detection too — worse UX for an obvious
   user error, saves nothing.

4. **Views tolerate an unscanned session.** Anything that reads
   `SessionHolder.getProjectInfo()` before the first run must handle `null`
   (audit call sites; today it is only the execution path and project-info
   display, which shows the directory path pre-scan).

## Risks / Trade-offs

- [First run gets slower — scan cost moves into it] → covered by existing progress
  UI; net time is unchanged and startup is instant, which is the point of #44.
- [Concurrent Web executions racing the first scan] → `ensureScanned()` is
  synchronized and memoized; losers block briefly then reuse the result.
- [A view silently assumes scanned state] → call-site audit of
  `SessionHolder.getProjectInfo()`/`getProjectEntries()` is an explicit task; Pilot
  test asserts the TUI browses recipes pre-scan.

## Migration Plan

Single PR stacked on `feat/recipe-applicability-stage2` (#76); merges after it.
No flags; rollback = revert.

## Open Questions

- None blocking.
