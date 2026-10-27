## Why

The daemon tests fail intermittently on CI, always on dependency-bump PRs that do not touch
daemon code:

- `DaemonClientTest.stopShutsDownAndDeregisters` and `replacesDaemonOnVersionMismatch`, and
  `DaemonCommandTest.stopTerminatesTheDaemonForAProjectAndClearsItsEntry` / `stopAllTerminatesEveryDaemon`:
  the registry entry is still present after the client stopped the daemon.
- `RunCommandDaemonTest`, `DaemonLauncherTest` and `DaemonCommandTest`:
  `JUnitException at ArrayList.java:1604`. That is JUnit failing to close the test's extension
  context, with a `TempDirDeletionStrategy$DeletionException` / `DirectoryNotEmptyException` on the
  registry `@TempDir`; the `ArrayList` frame is only the `forEach` that runs the cleanup.

Both come from one cause in `DaemonServer.handle`: the daemon writes the reply first and updates
the registry afterwards (`registry.touch`, and on a stop also `registry.remove` from the serve
loop's `finally`). A client that has its reply therefore races the daemon:

- After a stop, the client removes the entry, then the daemon's late `touch` renames a fresh copy
  back into place. The test (or a launcher waiting for a replacement daemon of the new version)
  finds the stopped daemon's entry.
- After any request, a test that ends and lets JUnit delete the registry directory while the
  daemon is still in `touch` has a new `.yaml` appear mid-deletion.

In production the same ordering lets a stopped daemon's late write resurrect its entry, and its
late `remove` can delete the entry of the replacement daemon started for the same root.

## What Changes

- `DaemonServer` builds the reply, finishes its registry update (touch, or deregister on stop),
  and only then writes the reply. Deregistration happens at most once, so the serve loop's
  `finally` no longer touches the registry after a stop was acknowledged. **Root-cause fix.**
- Tests that spawn daemon JVMs stop them through a shared `DaemonProcesses.stopAll` fixture that
  waits for each process to exit before `@TempDir` cleanup. `DaemonCommandTest` already did this
  privately; `RunCommandDaemonTest` and `DaemonLauncherTest` only sent the signal.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `lst-cli-daemon`: adds CORE_0023.7 (registry settled before the daemon replies), verified by
  SVC_CORE_0023.7.

## Impact

- `atunko-daemon`: `DaemonServer.handle`/`serve`, new `DaemonProcesses` test fixture, tests.
- `atunko-cli`: `DaemonCommandTest`, `RunCommandDaemonTest` teardown.
- `docs/reqstool`: CORE_0023.7 and SVC_CORE_0023.7.
- No user-facing behaviour or configuration changes, so no Antora update.
