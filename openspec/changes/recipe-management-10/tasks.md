# Tasks

## 1. reqstool

- [ ] 1.1 Add requirements CORE_0020 (custom recipe YAML loading) with children
      CORE_0020.1 (explicit recipe YAML files via --recipes-file), CORE_0020.2
      (auto-discovery from the config dir), CORE_0020.3 (malformed YAML
      resilience); CORE_0021 (favorites persistence) with child CORE_0021.1
      (favorites filter); CORE_0022 (recently used tracking) with child
      CORE_0022.1 (recent-first sort order); TUI_0007 (favorites in the TUI)
      with child TUI_0007.1 (favorite marker + indication); TUI_0008 (recent in
      the TUI) to `docs/reqstool/requirements.yml`
- [ ] 1.2 Add SVCs SVC_CORE_0020(.1/.2/.3), SVC_CORE_0021(.1), SVC_CORE_0022(.1),
      SVC_TUI_0007(.1), SVC_TUI_0008 to
      `docs/reqstool/software_verification_cases.yml`

## 2. Core: custom recipe YAML loading

- [ ] 2.1 Add `ConfigDirs` (XDG config dir resolution: `atunko.config.dir`
      property override, `$XDG_CONFIG_HOME/atunko`, `~/.config/atunko`) and
      `UserRecipeFiles.discover(dir)` listing `*.yml`/`*.yaml` recipe files
- [ ] 2.2 `EnvironmentProvider`: accept user recipe YAML files, load each via
      `YamlResourceLoader` with the bundled loader as dependency loader, track
      contributed names as user recipes, isolate per-file failures, and expose
      `loadWarnings()`
- [ ] 2.3 Add `@Requirements({"atunko:CORE_0020..."})` on the implementing code
- [ ] 2.4 Tests: YAML recipe discovered and classified USER, auto-discovery
      listing, config dir override, malformed YAML skipped with a warning while
      the valid file still loads (@TempDir fixtures)
- [ ] 2.5 Add `@SVCs({"atunko:SVC_CORE_0020..."})` on the tests from 2.4

## 3. Core: favorites and recently used

- [ ] 3.1 Add `FavoritesService` (toggle/isFavorite/favorites, persisted to
      `favorites.yml`, missing/malformed file reads as empty) and
      `FavoritesFilter { ALL, FAVORITES }` with `matches`/`next`
- [ ] 3.2 Add `RecentRecipesService` (`record(names)` newest-first with ISO-8601
      timestamps, dedupe, cap 20, persisted to `recent.yml`) and
      `SortOrder.RECENT` with a `comparator(recentFirst)` overload
- [ ] 3.3 Add `@Requirements({"atunko:CORE_0021..."})` and
      `@Requirements({"atunko:CORE_0022..."})` on the implementing code
- [ ] 3.4 Tests: favorite toggle persistence and reload, malformed favorites
      file resilience, filter cycling; recent recording order/dedupe/cap and
      persistence, recent-first comparator (@TempDir files)
- [ ] 3.5 Add `@SVCs({"atunko:SVC_CORE_0021..."})` and
      `@SVCs({"atunko:SVC_CORE_0022..."})` on the tests from 3.4

## 4. CLI

- [ ] 4.1 `ListCommand` and `SearchCommand`: repeatable `--recipes-file <path>`,
      auto-discovered config-dir recipe files added to the discovery
      environment, load warnings printed to stderr
- [ ] 4.2 Add `@Requirements({"atunko:CORE_0020.1", "atunko:CORE_0020.2",
      "atunko:CORE_0020.3"})` on the implementing code
- [ ] 4.3 Tests: `--recipes-file` with a fixture YAML lists the user recipe under
      `--source user`; a malformed file warns on stderr and the command still
      succeeds
- [ ] 4.4 Add `@SVCs({"atunko:SVC_CORE_0020.1", "atunko:SVC_CORE_0020.3"})` on
      the tests from 4.3

## 5. TUI

- [ ] 5.1 `TuiCommand`: `--recipes-file` option and warning pass-through;
      `TuiController`: injected favorites/recent services, `f` toggle favorite,
      `F` favorites filter cycling, `s` three-way sort cycle, recent recording
      in `runSelectedRecipes`, Esc reset of the favorites filter
- [ ] 5.2 `RecipeListRenderer`: ` *` favorite marker; `BrowserView`: `f`/`F`
      keys, third sort tab, `fav:<filter>` status indicator; `HelpOverlay`:
      document `f`, `F`, and the new sort cycle
- [ ] 5.3 Add `@Requirements({"atunko:TUI_0007", "atunko:TUI_0007.1",
      "atunko:TUI_0008"})` on the implementing code
- [ ] 5.4 Tests: `TuiControllerTest` favorites toggle/filter/persistence and
      recent-sort state tests (@TempDir services); Pilot headless test — `f`
      marks with `*`, `F` narrows to favorites, status bar and help updated
- [ ] 5.5 Add `@SVCs({"atunko:SVC_TUI_0007", "atunko:SVC_TUI_0007.1",
      "atunko:SVC_TUI_0008"})` on the tests from 5.4

## 6. Wrap-up

- [ ] 6.1 `./gradlew spotlessApply` then `./gradlew build` green;
      `openspec validate --all --strict` passes
- [ ] 6.2 Stacked PR `feat: recipe management — custom recipe YAML, favorites,
      recently used (#10)` with base `feat/recipe-source-toggle-19`, refs #10
