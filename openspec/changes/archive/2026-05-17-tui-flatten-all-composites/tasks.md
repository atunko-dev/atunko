## 1. reqstool — Requirements & SVCs

- [x] 1.1 Add TUI_0001.14.1 (TUI — Run Dialog Flatten-All) to `docs/reqstool/requirements.yml`
      as a child of TUI_0001.14
- [x] 1.2 Add SVC_TUI_0001.14.1 to `docs/reqstool/software_verification_cases.yml`

## 2. Implementation (TUI_0001.14.1)

- [x] 2.1 Implement `flattenAllRunRecipes()` in `TuiController` with loop-until-stable algorithm
      using `LinkedHashSet` for deduplication
- [x] 2.2 Add `@Requirements({"atunko:TUI_0001.14.1"})` to `flattenAllRunRecipes()`
- [x] 2.3 Add `F` key handler in `ConfirmRunView` to call `flattenAllRunRecipes()`
- [x] 2.4 Update footer hint text to `f:flatten F:flatten-all ?:help`
- [x] 2.5 Update `HelpOverlay` CONFIRM_RUN section to document both flatten keys

## 3. Tests (SVC_TUI_0001.14.1)

- [x] 3.1 Write `flattenAllRunRecipesReplacesAllCompositesWithSubRecipes`
      annotated `@SVCs({"atunko:SVC_TUI_0001.14.1"})`
- [x] 3.2 Write `flattenAllRunRecipesHandlesNestedComposites`
      annotated `@SVCs({"atunko:SVC_TUI_0001.14.1"})`
- [x] 3.3 Write `flattenAllRunRecipesAlreadyFlatListIsUnchanged`
      annotated `@SVCs({"atunko:SVC_TUI_0001.14.1"})`
- [x] 3.4 Write `flattenAllRunRecipesMixedListFlattensOnlyComposites`
      annotated `@SVCs({"atunko:SVC_TUI_0001.14.1"})`
- [x] 3.5 Write `flattenAllRunRecipesDeduplicatesSharedLeaves`
      annotated `@SVCs({"atunko:SVC_TUI_0001.14.1"})`
- [x] 3.6 Write `flattenAllRunRecipesMultipleLeafListIsUnchanged`
      annotated `@SVCs({"atunko:SVC_TUI_0001.14.1"})`

## 4. Build & Verification

- [x] 4.1 Run `./gradlew spotlessApply` to fix formatting
- [x] 4.2 Run `./gradlew build` — all checks pass
