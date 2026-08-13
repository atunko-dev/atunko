## 1. reqstool traceability

- [x] 1.1 Add requirement `CORE_0023.5` (Core — Daemon JVM Memory Bound, references CORE_0023) to `docs/reqstool/requirements.yml` after CORE_0023.4
- [x] 1.2 Add `SVC_CORE_0023.5` (automated-test, GIVEN/WHEN/THEN for the max-heap property) to `docs/reqstool/software_verification_cases.yml` after SVC_CORE_0023.4

## 2. Daemon heap bound (TDD)

- [x] 2.1 Add `DaemonLauncherTest` unit test: with `atunko.daemon.max-heap=1g` set, the constructed child command contains `-Xmx1g` and forwards the property; without it, no `-Xmx` is added — annotate `@SVCs({"atunko:SVC_CORE_0023.5"})` (fails first)
- [x] 2.2 Implement in `DaemonLauncher`: `MAX_HEAP_PROPERTY = "atunko.daemon.max-heap"`, map it to `-Xmx<value>` in `command(...)`, forward it to the child, make `command(...)` package-visible for the test — annotate `@Requirements({"atunko:CORE_0023.5"})`
- [x] 2.3 Set `atunko.daemon.max-heap=1g` in the `setUp`/`tearDown` of `DaemonLauncherTest` and `DaemonCommandTest`

## 3. Spawn-test diagnostics

- [x] 3.1 `DaemonLauncherTest`: add a lazy diagnostics helper (fallback reason + tail of every `*.log` in the registry dir); in `stopTerminatesTheSpawnedProcess` capture the `Attempt` and assert `fallbackReason()` is null before the `orElseThrow`, and attach diagnostics to the fallback-reason and log-file assertions in the other two spawn tests
- [x] 3.2 `DaemonCommandTest`: before each registry-size assertion, assert the captured run stderr (where `run` reports fallback) does not contain `Daemon unavailable`, with a failure message including the full stderr and daemon log tails
- [x] 3.3 `DaemonClient`: stop reporting "did not start within the startup timeout" when the launcher gives up — the launcher also gives up on a child that exited early, and naming a timeout it did not observe is what sent issue #91 after the wrong cause

## 4. Registry write race (root cause)

- [x] 4.1 Add requirement `CORE_0023.6` (Core — Atomic Registry Updates) and `SVC_CORE_0023.6` to the reqstool files
- [x] 4.2 Add `CORE_0023.6` to the `lst-cli-daemon` delta spec
- [x] 4.3 Add `DaemonRegistryTest.aConcurrentReaderNeverDestroysALiveEntry` — one writer thread and one reader thread over the same entry, asserting no read comes back empty — annotate `@SVCs({"atunko:SVC_CORE_0023.6"})` (fails first: 236/300 reads destroyed the entry)
- [x] 4.4 Make `DaemonRegistry.write` atomic: write a sibling `.tmp`, restrict permissions, then rename onto the entry, falling back to a plain replace where `ATOMIC_MOVE` is unsupported — annotate `@Requirements({"atunko:CORE_0023.6"})`
- [x] 4.5 Replace the bare `registry.find(...).orElseThrow()` calls in `DaemonLauncherTest` with an asserting helper, so a missing entry explains itself instead of throwing `NoSuchElementException`

## 5. Docs

- [x] 5.1 Document `atunko.daemon.max-heap` in the Antora daemon/CLI docs alongside the existing daemon properties

## 6. Verification

- [x] 6.1 Run `./gradlew spotlessApply` then `./gradlew build` (green, including both spawn-test suites)
- [x] 6.2 Run `openspec validate --all --strict` (passes)
