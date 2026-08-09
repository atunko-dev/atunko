## Context

Issue #19 (stacked base of this change) gave `EnvironmentProvider` the ability to load
user recipe *jars* next to the bundled classpath loader and to track the recipe names
they contribute, so `RecipeDiscoveryService` classifies each recipe's `RecipeSource`
honestly (`USER` exactly when a user loader contributed it). `RecipeSourceFilter`
provides one shared filtering implementation with TUI cycling. Recipe option editing
with validation already shipped with issue #17 (`RecipeOptionsView`).

What is missing from issue #10: loading *plain YAML* recipe files (no jar packaging),
favorites, and recently-used tracking. There is no config-dir concept in the codebase
yet; run configs live under the project dir (`atunko/runs`). YAML persistence uses
Jackson's YAML dataformat (`RunConfigService` pattern).

## Goals / Non-Goals

**Goals**

- A user can drop a declarative recipe YAML in `~/.config/atunko/recipes/` or pass
  `--recipes-file` and see the recipe in the catalog, classified `USER`, runnable like
  any other recipe.
- A malformed recipe YAML never takes down startup — clear warning, file skipped,
  everything else loads.
- Favorites and recently-used are core services with file persistence, so any UI can
  reuse them; the TUI surfaces both with minimal new chrome.

**Non-Goals**

- Web UI surfacing of favorites/recent/custom recipes (separate change).
- CLI exposure beyond `--recipes-file` (no `--favorites`, no `--sort recent`
  semantics change; `RECENT` degrades to name order without recent context).
- Authoring/editing recipe YAML *inside* the TUI.

## Decisions

1. **Recipe YAML files load through `YamlResourceLoader`, per file, inside
   `EnvironmentProvider`.** Each file gets its own
   `YamlResourceLoader(in, uri, props, classLoader, List.of(bundled))` with the bundled
   `ClasspathScanningLoader` as dependency loader, so declarative user recipes can
   reference bundled recipes — exactly the jar pattern from #19. The names each loader
   contributes join `userRecipeNames()`, so #19's classification and filtering apply
   unchanged. *Alternative considered*: a separate service building a second
   Environment — rejected, discovery must remain one environment and one
   classification mechanism.

2. **Per-file failure isolation with warnings.** Construction *and* descriptor listing
   of each YAML loader run inside a try/catch; any failure records a warning naming
   the file and the cause, and the file is skipped before it ever reaches the
   environment builder. `EnvironmentProvider.loadWarnings()` exposes the warnings;
   CLI commands print them to stderr, `tui` prints them before the alternate screen
   starts. A malformed *jar* keeps #19's behaviour (fail fast) — jars are built
   artifacts, YAML files are hand-edited and deserve leniency.

3. **Config dir: XDG conventions with a test override.** `ConfigDirs.configDir()`
   resolves, in order: system property `atunko.config.dir` (the test/override hook),
   `$XDG_CONFIG_HOME/atunko`, `~/.config/atunko`. `UserRecipeFiles.discover(dir)`
   lists `*.yml`/`*.yaml` sorted by filename; commands add the auto-discovered files
   to any `--recipes-file` values. Services additionally take explicit `Path`
   constructor arguments, so tests never touch the real home directory.

4. **Favorites: names-only YAML, cached in memory, persisted on toggle.**
   `FavoritesService` (default file `ConfigDirs` → `favorites.yml`, `Path` ctor for
   tests) loads lazily once, `toggle(name)` flips membership in memory and writes the
   file (creating parent dirs). A missing or malformed file reads as "no favorites"
   with a logged warning — favorites must never block startup. `FavoritesFilter
   { ALL, FAVORITES }` mirrors `RecipeSourceFilter` (`matches`/`next`) so the TUI
   filter cycles the same way `u` does.

5. **Recently used: newest-first list of (name, ISO-8601 timestamp), capped at 20.**
   `RecentRecipesService.record(names)` moves re-run recipes to the front (dedupe),
   stamps them via an injectable `Clock`, truncates to 20, and persists to
   `recent.yml`. Timestamps are stored as ISO-8601 strings — no extra Jackson module.
   Recording happens where execution is triggered (TUI run — both dry-run and real
   run execute recipes); the service owns all logic.

6. **Recent surfacing: a third `SortOrder`.** `SortOrder.RECENT` with a
   `comparator(List<String> recentFirst)` overload: recipes named in the recent list
   sort first in recency order, everything else falls back to name order. The
   existing no-arg `comparator()` degrades to name order, so the CLI's `--sort`
   option keeps working mechanically without new semantics. The TUI `s` key cycles
   name → tags → recent via `TuiController.cycleSortOrder()`; the header tabs gain a
   third tab. *Alternative considered*: a "recent" marker column — rejected, a sort
   order answers "what did I run last time?" directly and reuses existing chrome.

7. **TUI keys**: `f` toggles favorite on the highlighted recipe, `F` cycles the
   favorites filter — both free in the browser (the run dialog's `f`/`F` flatten
   bindings are a different screen). The favorite marker is a ` *` suffix after the
   recipe display name (a styled element, no prefix-column realignment), the status
   bar shows `fav:<filter>` next to `src:<filter>`, and Esc/clear-all resets the
   favorites filter like it resets the source filter.

8. **`TuiController` gets the services injected** (new trailing constructor
   parameters with defaults preserving every existing constructor), keeping all
   business logic in core and letting state tests point the services at `@TempDir`
   files.

## Risks / Trade-offs

- [Loading user recipe YAML executes only declarative recipes, but they can reference
  imperative bundled ones] → same trust model as #19 jars; files are user-authored in
  the user's own config dir or passed explicitly.
- [Favorites/recent files are read from the real config dir in production default
  constructors] → tests always inject `@TempDir` paths or set `atunko.config.dir`.
- [`--sort recent` in the CLI sorts by name] → documented degradation; honest without
  recent context, and CLI surfacing is explicitly out of scope.
- [Concurrent atunko processes could race on favorites/recent writes] → last-writer
  wins on tiny advisory files; acceptable for a local dev tool.

## Migration Plan

Single PR stacked on #19's branch (PR #80), refs #10. No flags; rollback = revert.

## Open Questions

- None blocking.
