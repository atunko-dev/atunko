## Context

The CLI has fully working `list`, `search`, `run`, and `tui` subcommands. The `atunko-tui`
module provides the TUI via `TuiCommand`. The `atunko-core` module provides all engine services.
The goal is to add a parallel `atunko-web` module that provides a browser-based recipe browser
accessible via `atunko webui`, reusing `RecipeDiscoveryService` from core with no TUI dependency.

## Goals / Non-Goals

**Goals:**
- Launch embedded Jetty server when `atunko webui` is invoked (WEB_0001)
- Display all discovered recipes in a hierarchical, sortable tree grid (WEB_0001.1)
- Filter recipes by name/description via text search; by tags via multi-select (WEB_0001.2)
- Expand/collapse composite recipes (recipeList) in the tree grid (WEB_0001.4)
- Multi-select recipes with cascade-both-ways checkbox selection (WEB_0001.5)
- Show recipe detail (description, tags, options, recipe list) in right pane (WEB_0001.6)
- Accept `--port` option (default 8080) (WEB_0001.3)
- Reuse `RecipeDiscoveryService` from core — no new core module changes
- Keep all existing subcommands and TUI working unchanged

**Non-Goals:**
- Recipe execution from the Web UI (follow-up change)
- Run configuration management in the Web UI (follow-up change)
- Authentication or remote access (local-only, no auth)
- Dark mode / custom theming (use Vaadin defaults)
- GraalVM native image (Vaadin is not compatible)
- Maven project scanning (Phase 2)

## Wireframe

```
┌─ AppLayout ──────────────────────────────────────────────────────────────────┐
│  atunko                                                                      │
├──────────────────────────────────────────────────────────────────────────────┤
│  [🔍 Search recipes...                                                    ]  │
│ ┌──────────────────────────────────────┬────────────────────────────────┐   │
│ │ ☐  Name ↕           │ [Tags...    ▼] │  MigrateToJava21               │   │
│ ├──────────────────────────────────────┤                                │   │
│ │ ☐  AddApacheHeader   │ java          │  Description                   │   │
│ │ ▶ ☐  UpgradeSpring   │ spring        │  Upgrades the project to       │   │
│ │ ▼ ☑  MigrateToJava21 │ java          │  Java 21 LTS.                  │   │
│ │      ☑  UpdateSyntax │ java          │                                │   │
│ │      ☑  RemoveLegacy │ java          │  Tags                          │   │
│ │ ☐  ChangePackage     │ java          │  java  migration               │   │
│ └──────────────────────────────────────┴────────────────────────────────┘   │
│  Showing 2,341 recipes                                                       │
└──────────────────────────────────────────────────────────────────────────────┘
```

`▶` = collapsed composite recipe (has recipeList), `▼` = expanded. Leaf nodes have no arrow.
Tag filter (`[Tags... ▼]`) is a `MultiSelectComboBox` in the Tags column header.

## Decisions

### 1. Vaadin standalone with vaadin-boot (no Spring Boot)

**Choice**: Use `vaadin-boot 13.3` to embed Jetty directly. No Spring Boot, no Spring context.

**Rationale**: atunko is a lean CLI tool — introducing Spring Boot would triple the dependency
footprint and startup time. `vaadin-boot` provides the same embedded Jetty lifecycle with a
minimal API. Consistent with the vaadin-boot pattern for standalone desktop/CLI Vaadin apps.

### 2. atunko-web as a separate Gradle module (parallel to atunko-tui)

**Choice**: New `atunko-web/` module with `java-library` plugin. Dependency:
`atunko-web` → `atunko-core`. `atunko-cli` adds `implementation project(':atunko-web')`.

**Rationale**: Mirrors the `atunko-tui` structure. Keeps Vaadin dependencies isolated from
TUI and core modules.

### 3. WebUiCommand as Picocli subcommand, wired via ServiceFactory

**Choice**: `WebUiCommand implements Callable<Integer>` with `@Command(name = "webui")` and
`@Option(names = {"--port"}, defaultValue = "8080")`. Registered in `App.java` alongside
`tui`, `list`, `search`, `run`. `ServiceFactory` creates `WebUiCommand` with
`RecipeDiscoveryService`.

**Rationale**: Same pattern as `TuiCommand`. Keeps CLI wiring in one place.

### 4. AppLayout + SplitLayout for two-pane UI

**Choice**: `AppLayout` for the page shell (header with "atunko" title). `SplitLayout`
(horizontal) divides the content area: left pane = `TreeGrid`, right pane = detail panel.
Status bar ("Showing X recipes") is a `Span` in a footer div below the `SplitLayout`.

**Rationale**: `AppLayout` is the standard Vaadin shell component. `SplitLayout` gives a
resizable divider between list and detail — same UX pattern as the TUI's browser/detail
split. Status bar at bottom is conventional for data grids.

### 5. TreeGrid for hierarchical recipe display with cascade-both-ways selection

**Choice**: `TreeGrid<RecipeDescriptor>` with `SelectionMode.MULTI`. Recipes with a non-empty
`recipeList()` are expandable — their children are the sub-recipes. Checkbox selection uses
cascade-both-ways logic:
- Checking a parent checks all its children
- Checking all children auto-checks the parent
- Unchecking one child puts the parent into indeterminate state (☒)

**Rationale**: OpenRewrite recipes can be composite (recipeList). Exposing this hierarchy
directly in the grid matches what users see in the TUI tag browser and recipe detail. Plain
`Grid` cannot represent hierarchy. `TreeGrid` is the standard Vaadin component for this.
Cascade-both-ways is the most ergonomic selection model for hierarchical items (select a
composite recipe → select all its children automatically).

**Note**: Cascade-both-ways selection is also the desired pattern for the TUI — to be
backported as a separate follow-up change.

### 6. Tags column with MultiSelectComboBox header filter (OR/ANY logic)

**Choice**: Tags column header contains a `MultiSelectComboBox<String>` populated with all
distinct tags across all recipes. Selecting tags filters the grid to recipes that have AT
LEAST ONE of the selected tags (OR/ANY). Tag filter and text search compose with AND: both
must match for a recipe to be visible.

**Rationale**: `MultiSelectComboBox` is the standard Vaadin component for multi-value
selection with search. OR/ANY logic is more permissive and useful for exploration ("show me
all java OR spring recipes"). AND would over-narrow results for broad tag sets.

### 7. RecipeHolder as application-scoped singleton for recipe data

**Choice**: `RecipeHolder` is a plain singleton (not a Vaadin CDI bean) initialized before
`WebServer.start()`. `RecipeBrowserView` reads from `RecipeHolder.getInstance()`.

**Rationale**: vaadin-boot without Spring Boot has no CDI. Static singleton is the simplest
pattern for passing data from the CLI command into the Vaadin UI.

### 8. Karibu-Testing for server-side UI tests (no browser required)

**Choice**: `karibu-testing-v24:2.2.4` for unit testing Vaadin views. Tests instantiate
`RecipeBrowserView` directly in JUnit 5, interact with the `TreeGrid` and `MultiSelectComboBox`
server-side, and assert on displayed data — no browser or servlet container needed.

**Rationale**: Karibu-Testing is by the same author as vaadin-boot (mvysny) — compatible and
purpose-built for this setup. 5–60ms per test vs seconds for browser-based tests. Allows
TDD on the view layer, covering filter logic, tree expansion, and selection behavior.
Vaadin TestBench (browser-based) is commercial and overkill for this scope.

## Risks / Trade-offs

**[Cascade selection is custom logic]** → Vaadin `TreeGrid` does not implement cascade-both-ways
out of the box. Selection listeners must manually check/uncheck children and update parent
state. Mitigation: encapsulate in a `CascadeSelectionHandler` helper class, unit-tested with
Karibu.

**[No CDI in vaadin-boot]** → Static singleton for `RecipeHolder` is a minor code smell but
acceptable for a local-only single-user tool.

**[Large recipe list in browser]** → Vaadin TreeGrid handles in-memory data sets of 2000+
items. Grid handles pagination natively. Lazy loading is not needed.

**[Vaadin Gradle plugin downloads Node.js on first build]** → Takes a few minutes, cached
after. Production mode (`-Pvaadin.productionMode`) bundles frontend — needed for shadow JAR
distribution.
