## 1. reqstool — Requirements & SVCs

- [x] 1.1 Add TUI_0001.17 (TUI — Cascade Multi-Select) to `docs/reqstool/requirements.yml` as a sub-requirement of TUI_0001
- [x] 1.2 Add SVC_TUI_0001.17 (cascade-down: selecting composite selects all transitive children) to `docs/reqstool/software_verification_cases.yml`
- [x] 1.3 Add SVC_TUI_0001.17.1 (cascade-up: selecting all children of a composite auto-selects the composite) to SVCs
- [x] 1.4 Add SVC_TUI_0001.17.2 (deselecting all children deselects the composite) to SVCs
- [x] 1.5 Add SVC_TUI_0001.17.3 (indeterminate visual state: partial child selection renders composite with indeterminate indicator) to SVCs
- [x] 1.6 Update `atunko-tui/docs/reqstool` filter to include TUI_0001.17 and all new SVC IDs

## 2. Core State — `RecipeListState`

- [x] 2.1 Add `partialRecipes` field (`Set<String>`) to `RecipeListState` (tracks composites with partial child selection)
- [x] 2.2 Implement `collectAllSubRecipeNames(RecipeInfo, Set<String>)` helper for transitive sub-recipe collection
- [x] 2.3 Implement `recomputePartialState()` helper that updates `partialRecipes` after any selection change by walking top-level recipes and checking each composite's children against `selectedRecipes`
- [x] 2.4 Refactor `toggleSelection()` in `RecipeListState` to:
  - If toggling ON a composite: add composite + all transitive children to `selectedRecipes`; call cascade-up for parent if applicable
  - If toggling OFF a composite: remove composite + all transitive children from `selectedRecipes`
  - If toggling a leaf: normal toggle, then call `recomputePartialState()` and cascade-up any fully/partially-selected parents
- [x] 2.5 Expose `partialRecipes()` as `Set<String>` (immutable copy) on `RecipeListState` (covers SVC_TUI_0001.17.3)
- [x] 2.6 Ensure `cycleSelection()` calls `recomputePartialState()` after bulk select/deselect

## 3. TUI Controller

- [x] 3.1 Annotate `TuiController.toggleSelection()` with `@Requirements({"atunko:TUI_0001.17"})` and verify cascade is delegated to `RecipeListState`
- [x] 3.2 Expose `partialRecipes()` accessor on `TuiController` (delegates to `browserState.partialRecipes()`) for use by renderer

## 4. Renderer — Indeterminate Visual State

- [x] 4.1 Update `RecipeListRenderer` to accept/query `partialRecipes` set alongside `selectedRecipes`
- [x] 4.2 Render composite rows with `[~]` (or equivalent TamboUI styled indicator) when in `partialRecipes` and not in `selectedRecipes` (covers SVC_TUI_0001.17.3)
- [x] 4.3 Verify `[x]` still used for fully selected, `[ ]` for unselected

## 5. Tests

- [x] 5.1 Write unit tests for `RecipeListState.toggleSelection()` cascade-down behaviour, annotated `@SVCs({"atunko:SVC_TUI_0001.17"})` (covers SVC_TUI_0001.17)
- [x] 5.2 Write unit test for cascade-up: selecting all children auto-selects parent, annotated `@SVCs({"atunko:SVC_TUI_0001.17.1"})` (covers SVC_TUI_0001.17.1)
- [x] 5.3 Write unit test for deselecting all children deselects parent, annotated `@SVCs({"atunko:SVC_TUI_0001.17.2"})` (covers SVC_TUI_0001.17.2)
- [x] 5.4 Write unit test for `partialRecipes()` state when only some children are selected, annotated `@SVCs({"atunko:SVC_TUI_0001.17.3"})` (covers SVC_TUI_0001.17.3)
- [x] 5.5 Verify run dialog `RecipeListState` is NOT affected by cascade (toggle in run dialog remains simple set add/remove)
- [x] 5.6 Verify `cycleSelection()` recomputes partial state correctly after bulk operations

## 6. Build & Verification

- [x] 6.1 Run `./gradlew spotlessApply` to fix formatting
- [x] 6.2 Run `./gradlew build` — all checks pass (Spotless, Checkstyle, Error Prone, tests)
- [x] 6.3 Run `openspec validate --all --strict` to verify spec integrity
