# Tasks

## 1. reqstool

- [ ] 1.1 Add requirements CORE_0019 (recipe source classification) with children
      CORE_0019.1 (user recipe jar loading), CORE_0019.2 (source filtering);
      CLI_0008 (--source on list/search) with child CLI_0008.1 (--recipe-jar);
      TUI_0006 (TUI source toggle) with child TUI_0006.1 (status bar + help
      indicator) to `docs/reqstool/requirements.yml`
- [ ] 1.2 Add SVCs SVC_CORE_0019(.1/.2), SVC_CLI_0008(.1), SVC_TUI_0006(.1) to
      `docs/reqstool/software_verification_cases.yml`

## 2. Core: source classification and filtering

- [ ] 2.1 Add `RecipeSource` enum (BUNDLED, USER) and `RecipeSourceFilter` enum
      (ALL, BUNDLED, USER) with `matches(RecipeSource)` and `next()` cycling
- [ ] 2.2 Add `source` component to `RecipeInfo` (trailing, defaulting to
      BUNDLED in all existing constructors — backward compatible)
- [ ] 2.3 `EnvironmentProvider`: accept user recipe jars, build the environment
      from the bundled classpath loader plus one loader per user jar, and expose
      `userRecipeNames()` / `isUserRecipe(name)`
- [ ] 2.4 `RecipeDiscoveryService`: classify each descriptor at discovery time;
      add `discoverAll(RecipeSourceFilter)` and
      `search(query, fields, RecipeSourceFilter)` overloads
- [ ] 2.5 Tests: classification of bundled vs user-jar recipes (fixture jar with
      a declarative recipe built in the test), filter overloads, RecipeInfo
      backward-compatible constructors
- [ ] 2.6 Add `@Requirements({"atunko:CORE_0019..."})` on the implementing code
      and `@SVCs({"atunko:SVC_CORE_0019..."})` on the tests from 2.5

## 3. CLI

- [ ] 3.1 `ListCommand` and `SearchCommand`: add `--source bundled|user|all`
      (default all) and repeatable `--recipe-jar <path>`; use a dedicated
      discovery service when jars are given
- [ ] 3.2 Tests: extend `ListCommandTest`/`SearchCommandTest` — `--source
      bundled` equals default, `--source user` empty without jars, and with a
      fixture `--recipe-jar` only the user recipe is listed
- [ ] 3.3 Add `@Requirements({"atunko:CLI_0008", "atunko:CLI_0008.1"})` on the
      implementing code and `@SVCs` on the tests from 3.2

## 4. TUI

- [ ] 4.1 `TuiController`: `sourceFilter()` state + `cycleSourceFilter()`,
      source filtering wired into `filterRecipes()`; `BrowserView`: `u` key in
      the single browse-mode handler, `src:<filter>` in the status bar;
      `HelpOverlay`: document `u`; `TuiCommand`: `--recipe-jar` option
- [ ] 4.2 Tests: `TuiControllerTest` cycle + filter state tests; Pilot headless
      test — pressing `u` cycles the filter, narrows the list, and updates the
      status bar
- [ ] 4.3 Add `@Requirements({"atunko:TUI_0006", "atunko:TUI_0006.1"})` on the
      implementing code and `@SVCs` on the tests from 4.2

## 5. Wrap-up

- [ ] 5.1 `./gradlew spotlessApply` then `./gradlew build` green;
      `openspec validate --all --strict` passes
- [ ] 5.2 PR `feat: recipe source toggling — bundled vs user recipes (#19)`,
      refs #19
