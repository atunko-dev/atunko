## Why

Recipe management (issue #10) is incomplete: users cannot author their own declarative
recipe YAML files without packaging them into a jar, cannot mark the recipes they care
about, and get no help re-finding the recipes they ran last time. Recipe option editing
with validation already shipped separately (issue #17), and recipe jars shipped with the
source toggle (issue #19) — this change builds on #19's source classification and covers
the remaining three capabilities.

## What Changes

- Custom recipe YAML authoring/loading: user-authored declarative OpenRewrite recipe
  YAML files join the discovery environment — supplied explicitly via a repeatable
  `--recipes-file <path>` option on `list`/`search`/`tui`, and auto-discovered from
  `~/.config/atunko/recipes/*.yml` (XDG config dir conventions, overridable for tests).
  Recipes from these files classify as `USER`, reusing #19's classification. A malformed
  YAML file never crashes startup: it is reported as a clear warning and skipped.
- Favorites: a recipe can be marked/unmarked as favorite, persisted to
  `~/.config/atunko/favorites.yml`. In the TUI, `f` toggles favorite on the highlighted
  recipe, favorites show a `*` marker in the list, and `F` cycles a favorites filter
  (all → favorites → all) mirroring #19's source filter.
- Recently used: recipe names and a timestamp are recorded on every execution in
  `~/.config/atunko/recent.yml` (capped at 20). The TUI surfaces them through a new
  `RECENT` sort order in the existing sort cycle (name → tags → recent).
- Web UI is out of scope. CLI exposure beyond `--recipes-file` is out of scope.

## Capabilities

### New Capabilities

- `recipe-management`: custom recipe YAML loading, favorites, and recently-used tracking
  in core, `--recipes-file` on `list`/`search`/`tui`, and favorites/recent surfacing in
  the TUI browser (planned reqstool IDs: CORE_0020 + sub-requirements, CORE_0021 +
  sub-requirement, CORE_0022 + sub-requirement, TUI_0007 + sub-requirement, TUI_0008).

### Modified Capabilities

<!-- none — discovery, browsing, and execution behaviour is unchanged when no recipe
     files exist, no favorites are marked, and the sort order stays at its default -->

## Impact

- `atunko-core`: `EnvironmentProvider` learns to load user recipe YAML files via
  OpenRewrite's `YamlResourceLoader` (with per-file failure isolation and warnings);
  new `ConfigDirs` (XDG config dir resolution, overridable), `UserRecipeFiles`
  (recipe-YAML auto-discovery), `FavoritesService` + `FavoritesFilter`, and
  `RecentRecipesService`; `SortOrder` gains `RECENT` with a recent-aware comparator.
- `atunko-cli`: `ListCommand` and `SearchCommand` gain repeatable `--recipes-file`;
  load warnings print to stderr.
- `atunko-tui`: `TuiCommand` gains `--recipes-file`; `TuiController` gains favorites
  state (`f`/`F`), recent recording on run, and a three-way sort cycle; `BrowserView`
  and `RecipeListRenderer` render the `*` favorite marker and the `fav:` status
  indicator; `HelpOverlay` documents the keys.
- reqstool: new requirements CORE_0020, CORE_0021, CORE_0022, TUI_0007, TUI_0008
  with SVCs.
