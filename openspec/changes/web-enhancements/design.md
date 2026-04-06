# Design: Web UI Enhancements

## Key Decisions

1. **Single branch, single PR** — all 6 features on `feat/web-enhancements`, closing
   #36–#41 together. They touch mostly orthogonal parts of `RecipeBrowserView` and share
   the `RecipeCoverageUtils` utility.

2. **Shared `RecipeCoverageUtils`** — pure static utility (no Vaadin deps) providing:
   - `computeCovered(Set<RecipeInfo>)` for coverage badges (#40)
   - `buildReverseIndex(List<RecipeInfo>)` for included-in lookup (#41)
   Handles cycles via visited sets, same pattern as `CascadeSelectionHandler`.

3. **Run order dialog is mandatory** — every Dry Run / Execute click shows `RunOrderDialog`
   first. Current `runRecipes(dryRun)` is split: dialog opening + `executeRecipes(ordered, dryRun)`
   callback. The dialog receives the selected set, orders alphabetically by default, and
   returns the user-confirmed ordered list.

4. **Flatten is recursive dedup** — `RunOrderDialog.flatten()` walks `recipeList()` recursively,
   collects leaves into a `LinkedHashSet` (preserves order, deduplicates).

5. **Named runs via directory convention** — saves to `projectDir/atunko/runs/<name>.yaml`.
   Load scans `atunko/runs/` for `*.yaml` files, shows picker dialog. No magic default —
   all runs are named equally.

6. **Richer run config format** — `RunConfig` evolves from flat recipe name list to structured
   format with `description`, per-recipe `options`, and per-recipe `exclude`:
   ```yaml
   version: 1
   description: Spring Boot 3.5 migration
   recipes:
     - name: org.openrewrite.java.spring.boot3.UpgradeSpringBoot_3_5
       options:
         newVersion: 3.5.0
       exclude:
         - org.openrewrite.java.spring.boot3.SomeSubRecipe
     - name: org.openrewrite.java.migrate.UpgradeToJava21
   ```
   Core changes: `RunConfig` record gains `description` field, `recipes` changes from
   `List<String>` to `List<RecipeEntry>` where
   `RecipeEntry(String name, Map<String, Object> options, List<String> exclude)`.
   Both `options` and `exclude` are nullable/optional per entry. `Map<String, Object>` lets
   Jackson deserialize YAML native types naturally (strings, booleans, integers) without
   forcing everything through string coercion. Storing composites with excludes (rather
   than flattening) preserves user intent — bumping the OpenRewrite BOM automatically picks
   up new sub-recipes without the run config going stale.
   `RunConfigService` unchanged (Jackson handles the new structure automatically).

7. **JSON Schema for run config** — `run.schema.json` validates the YAML format. Shipped in
   the project and can be referenced by IDEs for autocomplete.
   `RunConfigService.load()` validates against the schema before deserializing.

9. **Sort reuses core `SortOrder`** — `SortOrder.NAME` and `SortOrder.TAGS` with their
   `comparator()` method. Applied in `applyFilters()` after filtering, before tree building.

10. **Bulk select wraps in `inCascadeUpdate`** — prevents N selection listener firings during
    bulk operation. Iterates `getVisibleRecipes()` (filtered list) for Select All.

11. **Coverage badge via `addComponentHierarchyColumn`** — replaces text hierarchy column
   with a component renderer. Shows `Span` name + Lumo `badge contrast small` "covered"
   when `coveredRecipes.contains(node.recipe())`. Recomputed on selection change.

## Implementation Order

Sequential to minimise conflicts in `RecipeBrowserView.java`:

1. `RecipeCoverageUtils` (shared utility, no conflicts)
2. #41 included-in (touches only `selectForDetail()`)
3. #40 coverage (touches `buildTreeGrid()` + selection listener)
4. #36 run order dialog (new class + `runRecipes()` refactor)
5. #38 sorting (touches `buildSearchBar()` + `applyFilters()`)
6. #39 bulk select (touches `buildStatusBar()`)
7. #37 save/load (touches `buildStatusBar()`, new dialogs)
