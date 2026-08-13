## Context

See proposal.md — Why for the race and how it was found. The mechanics that matter here:

- `DaemonRegistry.write` used `Files.writeString`, which truncates then writes. `read` deletes
  any file it cannot parse. `DaemonServer` touches its own entry after serving a request and
  `DaemonClient` touches the same entry when the reply arrives, so two processes rewrite one file
  moments apart.
- `DaemonClient.execute` never throws for a daemon problem; it returns an `Attempt` whose
  `fallbackReason()` names the failure and points at the daemon log. `RunCommand` reports that on
  **stderr**, which `CommandLineFixture` captures.
- Daemon logs live at `<registry-dir>/<key>.log`, and the registry dir in both test classes is a
  JUnit `@TempDir` deleted when the test ends.

## Goals / Non-Goals

**Goals:**

- A concurrent reader can never observe, or delete, a partially written registry entry.
- A daemon test that fails names its cause in the test report.
- Daemon JVMs can be given a memory bound.

**Non-Goals:**

- No retry or quarantine of the spawn tests — masking the flake would have cost `SVC_CORE_0023`
  its automated verification and would have left the production race in place.
- No change to `DaemonClient`'s fallback semantics or to `registry.remove` on the unreachable
  path — both correct.
- No cross-host locking protocol. The race is a write-visibility problem, and an atomic rename
  solves it without introducing lock files or their stale-lock failure mode.

## Decisions

- **Atomic replace over in-place rewrite**: write a sibling temp file, then rename it onto the
  entry. `rename(2)` is atomic within a filesystem, so a reader sees the old entry or the new
  one. The temp file is created in the registry directory (same filesystem, so the rename cannot
  degrade to a copy) with a `.tmp` suffix (so `list()`, which reads only `.yaml`, ignores it),
  and permissions are restricted **before** the rename so the auth token is never briefly
  world-readable. `AtomicMoveNotSupportedException` falls back to a plain replace — still
  strictly better than a truncate.
  - Alternative — keeping in-place writes and making `read` tolerant (retry instead of delete) —
    rejected: it narrows the window rather than closing it, and leaves `write` producing states
    no reader should ever see.
  - Alternative — a lock file around read and write — rejected: heavier, and a crashed daemon
    leaves a stale lock that then needs its own recovery path.
- **Verify with a stress test, not a mocked filesystem**: one writer thread and one reader thread
  over the same entry, asserting no read comes back empty. Before the fix this reported 236 of
  300 reads destroying the entry, so it is not a probabilistic test in practice — the old code
  fails it overwhelmingly.
- **Heap bound as a system property** (`atunko.daemon.max-heap` → `-Xmx`), following the existing
  `atunko.daemon.*` pattern and forwarded to the child like the others. Verified by unit-testing
  command construction rather than by launching a JVM and reading `ProcessHandle.info()`:
  deterministic and instant.
- **Diagnostics in failure messages, not CI artifacts**, because `@TempDir` deletes the daemon
  logs before any upload step could collect them. Assertions are lazy (`withFailMessage` with a
  supplier) so green runs pay nothing, and every `orElseThrow` on a registry lookup is preceded
  by an assertion that can explain itself.

## Risks / Trade-offs

- [`ATOMIC_MOVE` unsupported on some network/virtualised filesystems] → falls back to a plain
  replace, which still writes a complete file before publishing it.
- [The stress test is timing-dependent, so it could in principle pass on broken code] → it fails
  the pre-fix implementation 236/300, and it runs in ~1s, so it is cheap to keep as a regression
  guard.
- [A leftover `.tmp` file if the process dies mid-write] → ignored by `list()` and by `find`,
  and the next successful write replaces the entry regardless.
- [1g in the test suites could be too small for the recipe environment] → both spawn suites run
  green under it locally; an OOM would land in the daemon log, which the new failure messages
  print.
