## 1. reqstool — Requirements and SVCs

- [x] 1.1 Add CORE_0001.2 (Recipe Option Discovery) to `docs/reqstool/requirements.yml`
- [x] 1.2 Add CORE_0003.2 (Recipe Execution with Options) to `docs/reqstool/requirements.yml`
- [x] 1.3 Add CORE_0007.1 (Save Run Config with Options) to `docs/reqstool/requirements.yml`
- [x] 1.4 Add CORE_0008.1 (Load Run Config with Options) to `docs/reqstool/requirements.yml`
- [x] 1.5 Add SVC_CORE_0001.2, SVC_CORE_0003.2, SVC_CORE_0007.1, SVC_CORE_0008.1 to `docs/reqstool/software_verification_cases.yml`

## 2. Core — RecipeInfo wraps RecipeDescriptor

- [x] 2.1 Convert `RecipeInfo` from field-extracting record to descriptor-wrapping record with delegation methods and `options()` accessor
- [x] 2.2 Add `RecipeInfo.of()` static factory methods for test ergonomics
- [x] 2.3 Simplify `RecipeDiscoveryService.toRecipeInfo()` to wrap descriptor directly
- [x] 2.4 Write `RecipeInfoTest` — verify options from wrapped descriptor, verify `of()` factory (SVC_CORE_0001.2)

## 3. Core — RunConfig with option values

- [x] 3.1 Create `RecipeConfig` record with `name` and `options` fields
- [x] 3.2 Update `RunConfig` to use `List<RecipeConfig>`, add `recipeNames()` convenience method
- [x] 3.3 Write tests for RunConfig save/load round-trip with option values (SVC_CORE_0007.1, SVC_CORE_0008.1)

## 4. Core — JSON Schema for .atunko.yml

- [x] 4.1 Create `docs/schemas/atunko-run-config.schema.json`

## 5. Core — Execution with options

- [x] 5.1 Add `execute(String, Map<String, Object>, List<SourceFile>)` overload to `RecipeExecutionEngine`
- [x] 5.2 Delegate existing `execute(String, List<SourceFile>)` to new overload with empty options
- [x] 5.3 Write test for execution with option values applied (SVC_CORE_0003.2)

## 6. TUI — Display options in DetailView

- [x] 6.1 Add options section to `DetailView.renderRecipeDetail()` showing name, type, required, description
- [x] 6.2 Update `TuiController.saveRunConfig()` to create `RecipeConfig` entries
- [x] 6.3 Update fallback `RecipeInfo` construction in `TuiController` line 585 to use `RecipeInfo.of()`

## 7. Web UI — Display options in detail panel

- [x] 7.1 Add options section to `RecipeBrowserView` detail panel showing name, type, required, description

## 8. Test fixture migration

- [x] 8.1 Update all `new RecipeInfo(...)` to `RecipeInfo.of(...)` in `TuiControllerTest` (~14 instances)
- [x] 8.2 Update all `new RecipeInfo(...)` to `RecipeInfo.of(...)` in `AtunkoTuiTest` (1 instance)
- [x] 8.3 Update all `new RecipeInfo(...)` to `RecipeInfo.of(...)` in `RecipeBrowserViewTest` (~6 instances)
- [x] 8.4 Update `TuiControllerTest.saveRunConfig_persistsSelectedRecipes` for new `RunConfig` shape

## 9. Build verification

- [x] 9.1 Run `./gradlew spotlessApply && ./gradlew build` — all tests pass
- [x] 9.2 Launch TUI and verify options display for a recipe with options (e.g., ChangeMethodName)
