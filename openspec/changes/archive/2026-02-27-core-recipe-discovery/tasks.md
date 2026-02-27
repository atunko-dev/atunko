## 1. Test First (SVC_CORE_0001)

- [x] 1.1 Create `RecipeDiscoveryServiceTest` in `core/src/test/java/dev/atunko/core/recipe/` annotated with `@SVCs({"SVC_CORE_0001"})`
- [x] 1.2 Write test: discovering recipes returns a non-empty list with name and description
- [x] 1.3 Write test: discovered recipes include known recipes from classpath modules (e.g., `org.openrewrite.staticanalysis.RemoveUnusedImports`)
- [x] 1.4 Verify tests compile but fail (red phase)

## 2. Core Implementation (CORE_0001)

- [x] 2.1 Create `RecipeInfo` record in `dev.atunko.core.recipe` with fields: `name`, `displayName`, `description`, `tags`
- [x] 2.2 Create `RecipeDiscoveryService` in `dev.atunko.core.recipe` with `discoverAll()` method
- [x] 2.3 Implement `discoverAll()` using `Environment.builder().scanRuntimeClasspath().build()` mapping to `RecipeInfo`
- [x] 2.4 Annotate `RecipeDiscoveryService` with `@Requirements({"CORE_0001"})`

## 3. Verify

- [x] 3.1 Run `./gradlew :core:test` — all tests pass (green phase)
- [x] 3.2 Run `./gradlew build` — full build passes
- [x] 3.3 Run `reqstool status local -p core/docs/reqstool` — CORE_0001 shows implementation and test coverage
