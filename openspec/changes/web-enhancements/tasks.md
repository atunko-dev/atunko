## 1. Shared Utility — RecipeCoverageUtils

- [x] 1.1 Create `RecipeCoverageUtils` with `computeCovered(Set<RecipeInfo>)` and `buildReverseIndex(List<RecipeInfo>)` — `@SVCs({"atunko:SVC_WEB_0001.24", "atunko:SVC_WEB_0001.26"})`
- [x] 1.2 Create `RecipeCoverageUtilsTest` — `@SVCs({"atunko:SVC_WEB_0001.25", "atunko:SVC_WEB_0001.27"})`

## 2. Included-In Reverse Lookup (#41)

- [x] 2.1 Add `childToParents` field and build reverse index in `RecipeBrowserView` constructor
- [x] 2.2 Extend `selectForDetail()` to show "Included in: X, Y" when recipe has parents — `@Requirements({"atunko:WEB_0001.16"})`
- [x] 2.3 Add `RecipeBrowserViewTest` tests for included-in — `@SVCs({"atunko:SVC_WEB_0001.26", "atunko:SVC_WEB_0001.27"})`

## 3. Coverage Indicators (#40)

- [x] 3.1 Add `coveredRecipes` field and `updateCoveredRecipes()` method
- [x] 3.2 Replace `addHierarchyColumn` with `addComponentHierarchyColumn` showing "covered" badge — `@Requirements({"atunko:WEB_0001.15"})`
- [x] 3.3 Call `updateCoveredRecipes()` from selection listener after cascade update
- [x] 3.4 Add `RecipeBrowserViewTest` tests for coverage badges — `@SVCs({"atunko:SVC_WEB_0001.24", "atunko:SVC_WEB_0001.25"})`

## 4. Run Order Dialog (#36)

- [x] 4.1 Create `RunOrderDialog` with ordered grid, move up/down, flatten toggle — `@Requirements({"atunko:WEB_0001.10"})`
- [x] 4.2 Implement `flatten()` static method — recursive composite expansion with dedup
- [x] 4.3 Refactor `runRecipes()` → open `RunOrderDialog` first, extract `executeRecipes(List, boolean)`
- [x] 4.4 Create `RunOrderDialogTest` — `@SVCs({"atunko:SVC_WEB_0001.14", "atunko:SVC_WEB_0001.15", "atunko:SVC_WEB_0001.16"})`

## 5. Sorting (#38)

- [x] 5.1 Add `currentSortOrder` field and `Select<SortOrder>` dropdown in `buildSearchBar()` — `@Requirements({"atunko:WEB_0001.13"})`
- [x] 5.2 Sort filtered list in `applyFilters()` using `currentSortOrder.comparator()`
- [x] 5.3 Add testability hook `applySortOrder(SortOrder)`
- [x] 5.4 Add `RecipeBrowserViewTest` tests for sorting — `@SVCs({"atunko:SVC_WEB_0001.20", "atunko:SVC_WEB_0001.21"})`

## 6. Bulk Select/Deselect (#39)

- [x] 6.1 Add Select All and Deselect All buttons in `buildStatusBar()` — `@Requirements({"atunko:WEB_0001.14"})`
- [x] 6.2 Implement `selectAllVisible()` wrapped in `inCascadeUpdate` — iterates filtered recipes
- [x] 6.3 Implement `deselectAll()` wrapped in `inCascadeUpdate`
- [x] 6.4 Add `RecipeBrowserViewTest` tests — `@SVCs({"atunko:SVC_WEB_0001.22", "atunko:SVC_WEB_0001.23"})`

## 7. Core — Run Config Format Evolution (#37)

- [x] 7.1 Create `RecipeEntry` record: `name`, `options` (Map<String, Object>, nullable), `exclude` (List<String>, nullable) in `atunko-core`
- [x] 7.2 Update `RunConfig` record: add `description` (String, nullable), change `recipes` from `List<String>` to `List<RecipeEntry>`
- [x] 7.3 Create JSON Schema `run.schema.json` for the run config format
- [x] 7.4 Schema validation deferred — JSON Schema file provides IDE autocomplete; Jackson record structure validates at deserialization time
- [x] 7.5 Update `RunConfigServiceTest` for new format — `@SVCs({"atunko:SVC_CORE_0007", "atunko:SVC_CORE_0008"})`

## 8. Web UI — Save/Load Named Runs (#37)

- [x] 8.1 Add Save and Load buttons in `buildStatusBar()` — `@Requirements({"atunko:WEB_0001.11", "atunko:WEB_0001.12"})`
- [x] 8.2 Implement `saveConfig()` — name prompt dialog, write `atunko/runs/<name>.yaml`
- [x] 8.3 Implement `loadConfig()` — scan `atunko/runs/*.yaml`, picker dialog, resolve names to recipes, select
- [x] 8.4 Build `Map<String, RecipeInfo>` recursive name-to-recipe lookup
- [x] 8.5 Add `RecipeBrowserViewTest` tests — `@SVCs({"atunko:SVC_WEB_0001.17", "atunko:SVC_WEB_0001.18", "atunko:SVC_WEB_0001.19"})`

## 9. reqstool

- [x] 9.1 Add WEB_0001.10 through WEB_0001.16 to `docs/reqstool/requirements.yml`
- [x] 9.2 Add SVC_WEB_0001.14 through SVC_WEB_0001.27 to `docs/reqstool/software_verification_cases.yml`

## 10. Quality

- [x] 10.1 Run `./gradlew spotlessApply`
- [x] 10.2 Run `./gradlew build` — all tests green
- [x] 10.3 Run `reqstool status local -p docs/reqstool`
- [x] 10.4 Run `openspec validate --all --strict`
