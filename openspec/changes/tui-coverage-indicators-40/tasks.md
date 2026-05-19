## 1. reqstool — Add CORE_0013 requirement and SVCs

- [ ] 1.1 Add `CORE_0013` requirement to `docs/reqstool/requirements.yml` (title: "Core — Recipe Coverage Computation", covering `computeCovered` and `buildReverseIndex`)
- [ ] 1.2 Add `CORE_0013.1` sub-requirement for `buildReverseIndex` (reverse-index computation)
- [ ] 1.3 Add `SVC_CORE_0013` and `SVC_CORE_0013.1` SVCs to `docs/reqstool/software_verification_cases.yml`

## 2. atunko-core — Extract coverage utility

- [ ] 2.1 Create `RecipeCoverageUtils` in `atunko-core/src/main/java/io/github/atunkodev/core/recipe/` with `computeCovered(Set<RecipeInfo>)` and `buildReverseIndex(List<RecipeInfo>)` (annotate with `@Requirements({"atunko:CORE_0013"})` / `@Requirements({"atunko:CORE_0013.1"})`)
- [ ] 2.2 Write unit tests for `computeCovered` and `buildReverseIndex` in `atunko-core/src/test/java/io/github/atunkodev/core/recipe/RecipeCoverageUtilsTest.java` (annotate with `@SVCs({"atunko:SVC_CORE_0013", "atunko:SVC_CORE_0013.1"})`)
- [ ] 2.3 Run `./gradlew :atunko-core:test` — all tests green

## 3. atunko-web — Refactor to delegate to core

- [ ] 3.1 Refactor `atunko-web/src/main/java/io/github/atunkodev/web/view/RecipeCoverageUtils.java` to delegate `computeCovered` and `buildReverseIndex` calls to `io.github.atunkodev.core.recipe.RecipeCoverageUtils`
- [ ] 3.2 Update `@Requirements` annotations on the web class methods to still reference `WEB_0001.15`/`WEB_0001.16` (the display requirements stay web-scoped)
- [ ] 3.3 Run `./gradlew :atunko-web:test` — all tests green

## 4. atunko-tui — Add coverage indicators to BrowserView

- [ ] 4.1 Wire `RecipeCoverageUtils.computeCovered` and `buildReverseIndex` into `BrowserView` (or `TuiController`) — compute on each render from `controller.selectedRecipes()` and `controller.allRecipes()`
- [ ] 4.2 Update composite list row label in `BrowserView.renderList()` to append `[X/N]` when `coveredCount > 0`
- [ ] 4.3 Update detail panel composite label from `"Composite: N sub-recipes"` to `"Composite: X/N covered"` (always show the fraction)
- [ ] 4.4 Add `@Requirements({"atunko:TUI_0001.16"})` annotation on the relevant render method(s)
- [ ] 4.5 Write TUI integration tests covering `SVC_TUI_0001.16`, `SVC_TUI_0001.16.1`, `SVC_TUI_0001.16.2` in `atunko-tui/src/test/java/`
- [ ] 4.6 Run `./gradlew :atunko-tui:test` — all tests green

## 5. Quality checks and commit

- [ ] 5.1 Run `./gradlew spotlessApply` to auto-fix formatting
- [ ] 5.2 Run `./gradlew build` — full build green (Spotless + Checkstyle + Error Prone + all tests)
- [ ] 5.3 Commit: `feat(tui): add coverage indicators for composite recipes (#40)` with DCO sign-off
