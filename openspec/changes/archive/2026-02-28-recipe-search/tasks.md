## 1. Tests (TDD — write tests first)

- [x] 1.1 Add test: `search` with matching keyword returns filtered recipes (SVC_CORE_0002)
- [x] 1.2 Add test: `search` is case-insensitive (SVC_CORE_0002)
- [x] 1.3 Add test: `search` matches against name, displayName, description, and tags (SVC_CORE_0002)
- [x] 1.4 Add test: `search` with non-matching keyword returns empty list (SVC_CORE_0002)
- [x] 1.5 Add test: `search` with blank/null query returns all recipes (SVC_CORE_0002)

## 2. Implementation

- [x] 2.1 Add `search(String query)` method to `RecipeDiscoveryService`
- [x] 2.2 Add `@Requirements({"CORE_0002"})` annotation to the class or method
- [x] 2.3 Verify all tests pass with `./gradlew :core:test`

## 3. Quality

- [x] 3.1 Run `./gradlew spotlessApply` to fix formatting
- [x] 3.2 Run `./gradlew build` to verify full build passes (Spotless + Checkstyle + Error Prone)
