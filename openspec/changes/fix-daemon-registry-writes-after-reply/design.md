## Context

See proposal.md for the failures and the race. The daemon is single-threaded, so the only
concurrency is between the daemon and whoever holds its reply: the client process, the test
thread, or JUnit's `@TempDir` cleanup.

## Goals / Non-Goals

**Goals:**

- Once a client has a reply, the daemon has no registry write pending for that request.
- Once a stop is acknowledged, the daemon never writes the registry again.

**Non-Goals:**

- No retries, longer sleeps, or `@Disabled` in the tests.
- No change to `DaemonClient.stop`. Its own `registry.remove` stays for the path where the daemon
  does not answer and is killed instead.
- Not making `stop` wait for the daemon process to exit: the in-process tests run the daemon in
  the test JVM, and the CLI does not need it once the registry is settled.

## Decisions

- **Settle the registry, then reply.** `handle` computes the reply, updates `lastRequestMillis`,
  touches the entry (or deregisters when the request stopped the daemon), then writes. The touch
  timestamp moves earlier by the time it takes to write the reply, which nothing depends on.
  - Alternative: the client waits for the daemon to exit after a stop. Rejected: it does not cover
    the touch after an ordinary request, and an in-process daemon never exits.
- **Deregister once.** An `AtomicBoolean` guards `registry.remove`, so the `finally` in `serve`
  removes the entry only on the idle-timeout and close paths, never after an acknowledged stop
  when the root may already belong to a replacement daemon.
- **Deterministic regression test.** `DaemonServerTest.finishesRegistryUpdatesBeforeReplying`
  uses a registry whose `touch`/`remove` are slow and asserts they had completed when each reply
  arrived. It fails on the old ordering every time rather than probabilistically.
- **Wait for spawned daemons in teardown.** Signalling without waiting leaves a JVM that may still
  be shutting down when JUnit deletes its directory. `DaemonCommandTest.destroyAndWait` moves to a
  shared test fixture used by all three spawning test classes.

## Risks / Trade-offs

- [A slow registry write now delays the reply] → it is one small file write and rename, the same
  work as before, only ordered earlier.
