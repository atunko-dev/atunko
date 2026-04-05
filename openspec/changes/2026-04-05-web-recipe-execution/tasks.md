## 1. Core — AppServices

- [x] 1.1 Create `AppServices` in `atunko-core/src/main/java/.../core/`
- [x] 1.2 Add `@Requirements({"atunko:WEB_0001.8"})` to `AppServices.init()`
- [x] 1.3 Create `AppServicesTest` with 5 tests — `@SVCs({"atunko:SVC_WEB_0001.12"})`

## 2. Web UI — Wiring

- [x] 2.1 Update `WebUiCommand` constructor: add `engine`, `sourceParser`, `changeApplier` params
- [x] 2.2 Call `AppServices.init()` in `WebUiCommand.run()`
- [x] 2.3 Update `ServiceFactory` to pass engine/parser/applier to `WebUiCommand`
- [x] 2.4 Update `WebUiCommandTest` to use new 4-param constructor

## 3. Web UI — Execution UI

- [x] 3.1 Add Dry Run and Execute buttons to `RecipeBrowserView`
- [x] 3.2 Implement `runRecipes(dryRun)` — parse, execute, apply/skip, show dialog
- [x] 3.3 Show `ExecutionResult` in a Vaadin `Dialog` with per-file changes
- [x] 3.4 Add `getDryRunButton()` and `getExecuteButton()` testability hooks

## 4. reqstool

- [x] 4.1 Add `WEB_0001.8` and `WEB_0001.9` to `docs/reqstool/requirements.yml`
- [x] 4.2 Add `SVC_WEB_0001.12` and `SVC_WEB_0001.13` to SVCs file

## 5. Tests

- [x] 5.1 Add 5 `RecipeBrowserViewTest` tests for buttons/execution — `@SVCs({"atunko:SVC_WEB_0001.13"})`
- [x] 5.2 Reset `AppServices` and `SessionHolder` in `@BeforeEach`

## 6. Quality

- [ ] 6.1 Run `./gradlew spotlessApply`
- [ ] 6.2 Run `./gradlew build` — all tests green
- [ ] 6.3 Run `reqstool status local -p docs/reqstool`
- [ ] 6.4 Run `openspec validate --all --strict`
