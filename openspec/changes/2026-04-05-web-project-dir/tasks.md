## 1. Core — Interface and Factory

- [x] 1.1 Create `ProjectScanner` interface in `atunko-core/src/main/java/.../project/`
- [x] 1.2 Add `implements ProjectScanner` to `GradleProjectScanner`
- [x] 1.3 Add `@Requirements({"atunko:CORE_GRADLE_0001"})` to `GradleProjectScanner.scan()`
- [x] 1.4 Add `implements ProjectScanner` to `MavenProjectScanner`
- [x] 1.5 Add `@Requirements({"atunko:CORE_MAVEN_0001"})` to `MavenProjectScanner.scan()`
- [x] 1.6 Create `ProjectScannerFactory` with `detect(Path projectDir)` — throws on unknown build system
- [x] 1.7 Add `@Requirements({"atunko:CORE_0004"})` to `ProjectScannerFactory.detect()`
- [x] 1.8 Create `SessionHolder` in `atunko-core/src/main/java/.../project/`
- [x] 1.9 Add `@Requirements({"atunko:CORE_0004"})` to `SessionHolder.init()`

## 2. Core — Tests

- [x] 2.1 Create `ProjectScannerFactoryTest` with `@SVCs({"atunko:SVC_CORE_0004"})` at class level
- [x] 2.2 Add test: `detect_settingsGradle_returnsGradleScanner` — `@SVCs({"atunko:SVC_CORE_0004.1"})`
- [x] 2.3 Add test: `detect_buildGradleKts_returnsGradleScanner` — `@SVCs({"atunko:SVC_CORE_0004.1"})`
- [x] 2.4 Add test: `detect_settingsGradleKts_returnsGradleScanner` — `@SVCs({"atunko:SVC_CORE_0004.1"})`
- [x] 2.5 Add test: `detect_pomXml_returnsMavenScanner` — `@SVCs({"atunko:SVC_CORE_0004.2"})`
- [x] 2.6 Add test: `detect_noBuildFiles_throwsIllegalArgument` — `@SVCs({"atunko:SVC_CORE_0004.3"})`
- [x] 2.7 Update `GradleProjectScannerTest`: `@SVCs` → `SVC_CORE_GRADLE_0001`
- [x] 2.8 Update `MavenProjectScannerTest`: `@SVCs` → `SVC_CORE_MAVEN_0001`
- [x] 2.9 Create `SessionHolderTest` with 5 tests — `@SVCs({"atunko:SVC_CORE_0004.4"})`

## 3. TUI Refactor

- [x] 3.1 Remove `GradleProjectScanner` field and constructor param from `TuiCommand`
- [x] 3.2 Add `SessionHolder.init(projectDir, ProjectScannerFactory.detect(projectDir).scan(projectDir))` to `TuiCommand.run()`
- [x] 3.3 Remove `GradleProjectScanner` field and constructor param from `TuiController`
- [x] 3.4 Update `TuiController.runSelectedRecipes()` to read `SessionHolder.getProjectInfo()` instead of scanning
- [x] 3.5 Remove `new GradleProjectScanner()` from `ServiceFactory`

## 4. Web UI

- [x] 4.1 Revert `RecipeHolder` to single-arg `init(List<RecipeInfo>)` — no `projectDir`
- [x] 4.2 Update `WebUiCommand.run()`: call factory + `SessionHolder.init()` after `RecipeHolder.init()`
- [x] 4.3 Update `RecipeBrowserViewTest.setupView()`: `RecipeHolder.init(recipes)` single-arg

## 5. reqstool

- [x] 5.1 Update `docs/reqstool/requirements.yml`: rename `CORE_0004`/`CORE_0005`, add new generic `CORE_0004`
- [x] 5.2 Update `docs/reqstool/software_verification_cases.yml`: rename SVCs, add `SVC_CORE_0004` + children

## 6. OpenSpec

- [x] 6.1 Create `openspec/changes/2026-04-05-web-project-dir/` with proposal, design, specs, tasks
- [x] 6.2 Update `archive/2026-02-28-gradle-project-support` spec: ID references
- [x] 6.3 Update `archive/2026-02-28-maven-project-support` spec: ID references

## 7. Quality

- [ ] 7.1 Run `./gradlew spotlessApply`
- [ ] 7.2 Run `./gradlew build` — all tests green
- [ ] 7.3 Run `reqstool status local -p docs/reqstool`
- [ ] 7.4 Run `openspec validate --all --strict`
