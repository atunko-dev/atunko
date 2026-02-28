## 1. Tests (TDD — SVC_CORE_0007)

- [x] 1.1 Write `RunConfigServiceTest.save_writesYamlFile` — asserts .atunko.yml is created with correct recipe list
- [x] 1.2 Write `RunConfigServiceTest.save_withEmptyRecipes_writesEmptyList` — asserts empty recipes list is handled
- [x] 1.3 Write `RunConfigServiceTest.save_overwritesExistingFile` — asserts existing file is replaced

## 2. Implementation (CORE_0007)

- [x] 2.1 Create `RunConfig` record with `List<String> recipes` field
- [x] 2.2 Implement `RunConfigService.save(RunConfig, Path)` using Jackson YAML
- [x] 2.3 Add `@Requirements({"CORE_0007"})` to save method, `@SVCs({"SVC_CORE_0007"})` to tests

## 3. Verify

- [x] 3.1 Run `./gradlew :core:test` and confirm all tests pass
- [x] 3.2 Run `./gradlew build` and confirm full build passes
