## 1. Requirements & SVCs

- [x] 1.1 Add `CLI_0001` requirement to `docs/reqstool/requirements.yml`
- [x] 1.2 Add `SVC_CLI_0001` to `docs/reqstool/software_verification_cases.yml`

## 2. App — Default Delegation

- [x] 2.1 Change `App.run()` to call `spec.commandLine().getSubcommands().get("tui").execute()`
- [x] 2.2 Add `@Requirements({"atunko:CLI_0001"})` to `App.run()`

## 3. Tests

- [x] 3.1 Replace `noArgsPrintsUsage` with `tuiSubcommandIsRegistered` — asserts `cmd.getSubcommands()` contains key `"tui"`; annotate with `@SVCs({"atunko:SVC_CLI_0001"})`

## 4. Quality

- [x] 4.1 Run `./gradlew spotlessApply && ./gradlew :atunko-cli:test` — all tests pass, no violations
