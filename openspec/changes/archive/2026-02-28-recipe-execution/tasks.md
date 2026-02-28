## 1. Model classes

- [x] 1.1 Create `FileChange` record in `core.engine` with path, before, and after content
- [x] 1.2 Create `ExecutionResult` record in `core.engine` with list of file changes

## 2. Test fixture

- [x] 2.1 Create test fixture at `core/src/test/resources/fixtures/java-with-unused-imports/` with a Java file containing unused imports

## 3. Tests (TDD — write tests first)

- [x] 3.1 Add test: execute `RemoveUnusedImports` recipe removes unused imports from parsed sources (SVC_CORE_0003)
- [x] 3.2 Add test: execute with unknown recipe name throws or returns error
- [x] 3.3 Add test: execute with no matching changes returns empty result

## 4. Implementation

- [x] 4.1 Create `RecipeExecutionEngine` in `core.engine` with `execute(String recipeName, List<SourceFile> sources)` method
- [x] 4.2 Add `@Requirements({"CORE_0003"})` annotation to the execute method

## 5. Quality

- [x] 5.1 Run `./gradlew spotlessApply` to fix formatting
- [x] 5.2 Run `./gradlew build` to verify full build passes (Spotless + Checkstyle + Error Prone)
