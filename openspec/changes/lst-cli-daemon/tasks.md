# Tasks

## 1. Module and protocol scaffolding

- [x] 1.1 Add `atunko-daemon` module (`java-library`, package
      `io.github.atunkodev.daemon`) depending on `atunko-core` only; register in
      `settings.gradle` and make `atunko-cli` depend on it
- [x] 1.2 Define the wire protocol records — `HelloRequest` (token, client version),
      `ExecuteRequest` (recipe names, options, dry-run), `ExecuteResponse` (results,
      diagnostics), `StatusResponse`, `StopRequest` — as newline-delimited JSON over the
      existing Jackson mapper (`YamlMappers`/a new `JsonMappers` sibling)
- [x] 1.3 Write `ProtocolCodecTest` — every message type round-trips through the codec

## 2. Daemon registry

- [x] 2.1 Write `DaemonRegistryTest` — write/read an entry; entry file is created with
      owner-only permissions; a stale entry (dead PID) is detected and removed
- [x] 2.2 Implement `DaemonRegistry` over
      `${XDG_STATE_HOME:-~/.local/state}/atunko/daemons/<hash-of-root>.yaml` holding port,
      PID, resolved project root, atunko version, token and last-used timestamp
- [x] 2.3 Write `DaemonRegistryTest.evictsLeastRecentlyUsedIdleDaemon` — at the pool limit a
      further registration stops the LRU idle daemon; a busy daemon is never evicted
      [SVC_CORE_0023.1]
- [x] 2.4 Implement the bounded pool with LRU eviction (default max 3, property
      `atunko.daemon.max`)
- [x] 2.5 Add `@Requirements({"atunko:CORE_0023.1"})` to the eviction implementation and
      `@SVCs({"SVC_CORE_0023.1"})` to the test from 2.3

## 3. Daemon server

- [x] 3.1 Write `DaemonServerTest.reusesParsedSourcesAcrossRequests` — two executions for an
      unchanged project parse once [SVC_CORE_0023]
- [x] 3.2 Implement `DaemonServer`: bind `127.0.0.1:0`, own one `ParsedSourcesCache`, hold the
      project's `ProjectEntry` explicitly (not via `SessionHolder`), serialize requests per
      project, execute through the existing engine
- [x] 3.3 Add `@Requirements({"atunko:CORE_0023"})` to `DaemonServer` and
      `@SVCs({"SVC_CORE_0023"})` to the test from 3.1
- [x] 3.4 Write `DaemonServerTest.refusesRequestWithWrongToken` — a bad token is refused, no
      execution runs, connection closes [SVC_CORE_0023.3]
- [x] 3.5 Implement token check on the `hello` handshake; add
      `@Requirements({"atunko:CORE_0023.3"})` and `@SVCs({"SVC_CORE_0023.3"})`
- [x] 3.6 Write `DaemonServerTest.exitsAfterIdleTimeout` with a short configured timeout —
      daemon exits and deregisters [SVC_CORE_0023.2]
- [x] 3.7 Implement idle-timeout shutdown (default 30 min, property
      `atunko.daemon.idle-timeout`); add `@Requirements({"atunko:CORE_0023.2"})` and
      `@SVCs({"SVC_CORE_0023.2"})`

## 4. Daemon client

- [x] 4.1 Write `DaemonClientTest.replacesDaemonOnVersionMismatch` — an entry recorded by a
      different version is stopped and replaced [SVC_CORE_0023.4]
- [x] 4.2 Implement `DaemonClient`: resolve project root, read/validate the registry entry,
      probe liveness, auto-start a daemon when absent, perform the handshake
- [x] 4.3 Add `@Requirements({"atunko:CORE_0023.4"})` to the version check and
      `@SVCs({"SVC_CORE_0023.4"})` to the test from 4.1
- [x] 4.4 Print a one-line stderr notice on auto-start naming the daemon and
      `atunko daemon stop`

## 5. CLI integration

- [x] 5.1 Write `RunCommandDaemonTest.daemonRunMatchesInProcessRun` — same changed files and
      exit status with and without the daemon [SVC_CLI_0009]
- [x] 5.2 Route `RunCommand` through `DaemonClient`; add
      `@Requirements({"atunko:CLI_0009"})` and `@SVCs({"SVC_CLI_0009"})`
- [x] 5.3 Write `RunCommandDaemonTest.fallsBackWhenDaemonUnreachable` — correct result plus a
      warning naming the reason [SVC_CLI_0009.1]
- [x] 5.4 Implement fallback to in-process execution on any daemon failure; add
      `@Requirements({"atunko:CLI_0009.1"})` and `@SVCs({"SVC_CLI_0009.1"})`
- [x] 5.5 Write `RunCommandDaemonTest.noDaemonFlagStartsNoDaemon` [SVC_CLI_0009.2]
- [x] 5.6 Add `--no-daemon` and the `atunko.daemon.disabled` property; add
      `@Requirements({"atunko:CLI_0009.2"})` and `@SVCs({"SVC_CLI_0009.2"})`
- [x] 5.7 Write `DaemonCommandTest` — `status` lists root, port, PID and idle time; `stop`
      and `stop --all` terminate daemons and clear entries [SVC_CLI_0009.3]
- [x] 5.8 Implement `DaemonCommand` (`status`, `stop [--all]`) and register it on `App`; add
      `@Requirements({"atunko:CLI_0009.3"})` and `@SVCs({"SVC_CLI_0009.3"})`

## 6. Docs

- [x] 6.1 Document the daemon in `docs/antora/modules/ROOT/pages/cli.adoc` — `atunko daemon
      status|stop`, `--no-daemon`, the `atunko.daemon.*` properties, and that `run`
      auto-starts a daemon
- [x] 6.2 Record the four resolved design questions in `docs/design/lst-caching-daemon.md`,
      replacing its "Open questions" section

## 7. Wrap-up

- [x] 7.1 `./gradlew spotlessApply build` green
- [x] 7.2 `openspec validate --all --strict` passes
- [x] 7.3 `reqstool status local -p docs/reqstool` — CORE_0023 and CLI_0009 families covered
- [ ] 7.4 PR referencing #11
