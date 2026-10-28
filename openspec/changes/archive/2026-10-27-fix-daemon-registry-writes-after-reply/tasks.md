## 1. reqstool traceability

- [x] 1.1 Add requirement `CORE_0023.7` (Core — Registry Settled Before Reply, references CORE_0023) to `docs/reqstool/requirements.yml`
- [x] 1.2 Add `SVC_CORE_0023.7` (automated-test) to `docs/reqstool/software_verification_cases.yml`
- [x] 1.3 Add `CORE_0023.7` to the `lst-cli-daemon` delta spec

## 2. Daemon ordering (root cause, TDD)

- [x] 2.1 Add `DaemonServerTest.finishesRegistryUpdatesBeforeReplying` with a slow registry; annotate `@SVCs({"atunko:SVC_CORE_0023.7"})` (fails first: no update had completed when the status reply arrived)
- [x] 2.2 `DaemonServer.handle`: build the reply, touch or deregister, then write the reply; annotate `@Requirements({"atunko:CORE_0023.7"})`
- [x] 2.3 `DaemonServer`: deregister at most once, so `serve`'s `finally` makes no registry update after an acknowledged stop

## 3. Test teardown

- [x] 3.1 Add `DaemonProcesses.stopAll(registryDir)` to the daemon test fixtures: destroy every registered daemon other than this JVM and wait for it to exit
- [x] 3.2 Use it in `DaemonLauncherTest`, `DaemonCommandTest` and `RunCommandDaemonTest`

## 4. Verification

- [x] 4.1 Reproduce before the fix and confirm after: repeated stop/version-mismatch runs, and repeated real-daemon runs with the old teardown
- [x] 4.2 Run `./gradlew spotlessApply` then `./gradlew build`
- [x] 4.3 Run `openspec validate --all --strict`
