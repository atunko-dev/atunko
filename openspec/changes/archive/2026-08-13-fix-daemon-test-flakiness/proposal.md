## Why

The daemon spawn tests fail intermittently on CI (issue #91) with a bare
`NoSuchElementException`. The tests assert on registry side-effects
(`registry.find(...).orElseThrow()`, `registry.list()).hasSize(2)`) while discarding the
`Attempt.fallbackReason()` that would say what went wrong, so every failure hid its own cause.

Making the tests assert the daemon's actual contract first — `fallbackReason()` is null, before
any registry lookup — reproduced the failure locally and disproved the standing theory. The
daemon **served the request**; its registry entry was gone anyway. The cause is a race in the
registry itself:

- `DaemonRegistry.write` truncates and rewrites the entry file in place, so a concurrent read can
  observe a partially written file.
- `DaemonRegistry.read` treats an unparseable file as corruption and **deletes** it.
- Both processes write that file moments apart after every request: the daemon touches its own
  entry when it finishes handling one, and the client touches it when it gets the reply.

So a read timed inside the write window does not merely fail — it destroys the entry of a live,
healthy daemon. In production that orphans the daemon: it keeps running and holding LSTs, no
registry entry names it, the next run starts a second one, and the retention bound cannot count
what it cannot see.

## What Changes

- `DaemonRegistry.write` becomes atomic: write a temporary file in the registry directory, then
  rename it over the entry. A reader then sees either the old entry or the new one, never a
  half-written one. **This is the root-cause fix.**
- The spawn tests stop discarding evidence: every registry assertion is preceded by the daemon
  contract check — `fallbackReason()` is null / the run reported no `Daemon unavailable` — and
  failure messages carry the fallback reason plus the daemon log tail. This is what located the
  bug, and it is what will locate the next one.
- `DaemonClient` stops blaming the startup timeout for every failed launch: the launcher also
  gives up when the child exits early, and the timeout wording — which issue #91's original
  diagnosis was built on — asserts a cause it never observed.
- `DaemonLauncher` gains a configurable maximum heap for spawned daemons
  (`atunko.daemon.max-heap` → `-Xmx`), used by the daemon test suites. Unset keeps today's
  behaviour. This began as a mitigation for the superseded theory and is retained on its own
  merit: an unbounded daemon claims a quarter of physical memory, which is the wrong default to
  leave unconfigurable on a shared machine.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `lst-cli-daemon`: adds CORE_0023.5 (configurable maximum heap for spawned daemons, verified by
  SVC_CORE_0023.5) and CORE_0023.6 (registry updates are atomic — a concurrent reader never
  observes or deletes a partially written entry, verified by SVC_CORE_0023.6).

## Impact

- `atunko-daemon`: `DaemonRegistry.write` (atomic rename), `DaemonLauncher` (heap flag),
  `DaemonClient` (launch-failure wording), new `DaemonDiagnostics` test fixture, tests.
- `atunko-cli`: `DaemonCommandTest` diagnostics; consumes the new daemon test fixtures.
- `docs/reqstool`: CORE_0023.5/.6 and their SVCs.
- `docs/antora`: documents `atunko.daemon.max-heap`.
- Production behaviour changes only in that registry writes no longer corrupt concurrent reads.
