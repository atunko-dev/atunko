## Context

Recipe discovery today: `EnvironmentProvider` lazily builds one OpenRewrite `Environment`
via `Environment.builder().scanRuntimeClasspath().build()`; `RecipeDiscoveryService` maps
its `RecipeDescriptor`s to `RecipeInfo` records. There is no mechanism for loading recipes
from anywhere but atunko's own classpath, so today every recipe is — honestly — bundled.
`RecipeDescriptor` carries no reliable origin information (imperative recipes report a
placeholder source URI), so origin must be tracked at load time, per resource loader.

## Goals / Non-Goals

**Goals**

- An honest per-recipe source tag: `USER` exactly when the recipe was contributed by a
  user-supplied recipe jar, `BUNDLED` otherwise.
- A real way to supply user recipes on the command line (`--recipe-jar`), so the tag and
  the filter are meaningful rather than decorative.
- One filtering implementation in core, reused by CLI and TUI.

**Non-Goals**

- Web UI source toggle (explicitly out of scope for this change).
- Discovering recipes from the *target project's* dependency classpath automatically —
  that requires the (possibly deferred) project scan and is a separate change; the
  `RecipeSource` enum leaves room for it.
- Adding recipes manually inside the TUI (mentioned in the issue, separate change).

## Decisions

1. **Origin is tracked per resource loader, not inferred from descriptors.**
   `EnvironmentProvider` builds the bundled `ClasspathScanningLoader` (runtime classpath,
   exactly what `scanRuntimeClasspath()` does) plus one `ClasspathScanningLoader` per user
   jar (loaded through a `URLClassLoader` over that jar, with the bundled loader as
   dependency loader so declarative user recipes can reference bundled ones). The names
   contributed by the user loaders form `userRecipeNames()`. *Alternative considered*:
   classifying by `RecipeDescriptor.getSource()` — rejected, imperative recipes report a
   placeholder URI, which would make the tag dishonest.

2. **`RecipeSource` lives on `RecipeInfo` as a new trailing record component**, populated
   by `RecipeDiscoveryService.toRecipeInfo` (`USER` iff the descriptor's name was
   contributed by a user jar). All existing constructors are preserved and default to
   `BUNDLED` — the same backward-compatibility pattern used when `recipeList` and
   `options` were added. Sub-recipes are classified individually by the same rule, so a
   user composite wrapping bundled recipes shows its sub-recipes as bundled — honest.

3. **Filtering is a core enum, `RecipeSourceFilter { ALL, BUNDLED, USER }`**, with
   `matches(RecipeSource)` and `next()` (cycling ALL → BUNDLED → USER → ALL for the TUI).
   `RecipeDiscoveryService` gains `discoverAll(filter)` and `search(query, fields,
   filter)` overloads filtering top-level recipes; the TUI reuses `matches` inside its
   existing `filterRecipes()` chain. Filtering applies to top-level recipes only, like
   the existing tag and search filters.

4. **CLI**: `--source` is a `RecipeSourceFilter` option (default `ALL`; picocli's
   case-insensitive enum handling gives `bundled|user|all`). `--recipe-jar` is repeatable
   on `list`, `search`, and `tui`. Commands keep their injected shared
   `RecipeDiscoveryService`; when `--recipe-jar` is present they build a private
   `RecipeDiscoveryService(new EnvironmentProvider(jars))` instead, because the shared
   provider's environment must not be polluted per-invocation.

5. **TUI**: single-handler pattern preserved — `BrowserView`'s existing key handler gains
   a `u` binding calling `TuiController.cycleSourceFilter()`; the status bar shows
   `src:<filter>`; `HelpOverlay.BROWSER_HELP` documents the key. Cycling resets the
   highlight, like the tag filter does.

6. **Naming**: the new core enum `RecipeSource` coexists with the unrelated functional
   interface `TuiController.RecipeListState.RecipeSource` (a recipe-list supplier).
   `TuiController` only ever touches the enum through `RecipeSourceFilter.matches`, so no
   ambiguous reference arises; renaming the inner interface is left out to keep the diff
   focused.

## Risks / Trade-offs

- [Loading arbitrary user jars executes third-party code during scanning] → inherent to
  OpenRewrite recipe jars (same trust model as running the recipe); jars are only loaded
  when the user explicitly passes `--recipe-jar`.
- [`--source user` without `--recipe-jar` is always empty] → correct and honest; the CLI
  simply prints "No recipes found."
- [Serialized JSON output gains a `source` field] → additive; existing consumers keep
  working.

## Migration Plan

Single PR, closes #19. No flags; rollback = revert.

## Open Questions

- None blocking.
