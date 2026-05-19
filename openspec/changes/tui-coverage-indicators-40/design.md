## Context

The TUI shows composite recipes with a static "Composite: N sub-recipes" label. There is no indication of how many sub-recipes the user has currently selected. The Web UI already has this via `RecipeCoverageUtils` in `atunko-web`, but that class belongs in `atunko-core` per the shared-implementation principle.

`TUI_0001.16` (Recipe Coverage Indicators) and its SVCs (`SVC_TUI_0001.16`, `SVC_TUI_0001.16.1`, `SVC_TUI_0001.16.2`) already exist in reqstool. No new TUI requirements are needed; this change implements them.

## Goals / Non-Goals

**Goals:**
- Move `computeCovered` and `buildReverseIndex` logic into `atunko-core` as `CORE_0013`
- Refactor `atunko-web`'s `RecipeCoverageUtils` to delegate to the core utility
- Wire the core utility into the TUI `BrowserView` to display "Composite: X/N covered" in the detail panel and a coverage fraction in each composite list row

**Non-Goals:**
- Changing the visual style beyond the text label
- Adding new composite-level selection interaction (covered by TUI_0001.17, separate change)
- Changing web coverage UI behaviour

## Decisions

### 1. Core utility: static class vs. service

**Decision:** Rename `RecipeCoverageUtils` to a public final class in `io.github.atunkodev.core.recipe` — the same approach already used in `atunko-web`. No state is needed; pure functions over `RecipeInfo` lists.

**Alternatives considered:**
- Injectable `@Service` — unnecessary overhead for pure functions with no I/O or lifecycle
- Interface + impl — useful only if multiple strategies are expected; not the case here

### 2. Coverage fraction in the list row

**Decision:** Append `[X/N]` to the composite row label in `BrowserView.renderList()` only when `coveredCount > 0`, so unselected composites look exactly as before.

**Alternatives considered:**
- Always show `[0/N]` — noisy; most composites start uncovered
- Show only in the detail panel — hides information from the scannable list view

### 3. Detail panel wording

**Decision:** Change `"Composite: N sub-recipes"` to `"Composite: X/N covered"` (always) so the total count remains visible and the coverage count is contextually clear.

### 4. Web refactor scope

**Decision:** `RecipeCoverageUtils` in `atunko-web` is replaced by a thin delegation class that calls the core methods. The class stays in its current package so no import changes are required in other Web UI classes.

## Risks / Trade-offs

- [Cycle risk] `atunko-tui` already depends on `atunko-core`; adding coverage calls there is safe. `atunko-web` also depends on `atunko-core`. No new dependency edges introduced.
- [Naming collision] Both web and core will have a class named `RecipeCoverageUtils`. Resolved by putting the web class in a different package (`web.view`) and the core class in `core.recipe` — explicit imports eliminate ambiguity.
- [Performance] `computeCovered` traverses all sub-recipes on every render tick. This is acceptable for the recipe counts expected (<10k), but if it becomes a bottleneck it should be memoised in the controller layer (out of scope here).
