## 1. Tests (TDD — SVC_CORE_0008)

- [ ] 1.1 Write `RunConfigServiceTest.load_readsYamlFile` — save then load produces equivalent RunConfig
- [ ] 1.2 Write `RunConfigServiceTest.load_nonExistentFile_throws` — asserts IOException for missing file
- [ ] 1.3 Write `RunConfigServiceTest.load_invalidYaml_throws` — asserts exception for malformed YAML

## 2. Implementation (CORE_0008)

- [ ] 2.1 Implement `RunConfigService.load(Path)` using Jackson YAML deserialization
- [ ] 2.2 Add `@Requirements({"CORE_0008"})` to load method, `@SVCs({"SVC_CORE_0008"})` to tests

## 3. Verify

- [ ] 3.1 Run `./gradlew :core:test` and confirm all tests pass
- [ ] 3.2 Run `./gradlew build` and confirm full build passes
