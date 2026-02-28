## 1. Model classes

- [x] 1.1 Create `ProjectInfo` record in `core.project` with classpath and source dirs

## 2. Tests (TDD — write tests first)

- [x] 2.1 Add test: scan Gradle project returns non-empty classpath (SVC_CORE_0004)
- [x] 2.2 Add test: scan Gradle project returns source directories (SVC_CORE_0004)
- [x] 2.3 Add test: scan non-existent directory throws or returns error

## 3. Implementation

- [x] 3.1 Create `GradleProjectScanner` in `core.project` with `scan(Path projectDir)` method
- [x] 3.2 Add `@Requirements({"CORE_0004"})` annotation to the scan method

## 4. Quality

- [x] 4.1 Run `./gradlew spotlessApply` to fix formatting
- [x] 4.2 Run `./gradlew build` to verify full build passes
