## Context

The CLI is fully implemented with `list`, `search`, and `run` subcommands. When `atunko` is
invoked without subcommands, `App.run()` prints usage text. CLI_0001 requires launching an
interactive TUI instead. The core module provides all necessary services (`RecipeDiscoveryService`,
`RecipeExecutionEngine`, `RunConfigService`, `JavaSourceParser`, `ChangeApplier`). TamboUI
dependencies are already on the classpath.

## Goals / Non-Goals

**Goals:**
- Launch TUI when no subcommand is given (CLI_0001)
- Implement all 12 sub-capabilities (CLI_0001.1–CLI_0001.12) as TUI screens and interactions
- Reuse existing core services — no new core module changes
- Keep CLI subcommands working unchanged
- Follow MVC pattern: controller (state) + views (pure rendering) + event handling

**Non-Goals:**
- CSS theming (future work — use inline styling for now)
- Mouse support (keyboard-only in this phase)
- GraalVM native image (TamboUI + OpenRewrite may not be compatible)
- Web UI (Phase 3, separate module)
- Maven project scanning (Phase 2)
- RecipeDescriptor option editing at the OpenRewrite level (recipe options will be
  display-only until the core module supports option mutation)

## Decisions

### 1. MVC architecture with TuiController as state owner

**Choice**: Single `TuiController` class owns all TUI state (current screen, recipe list,
selection set, search query, sort order). View methods are pure functions of state that
return `Element` trees. Event handlers update controller state and trigger re-render.

**Rationale**: TamboUI is immediate-mode — `render()` is called each frame and builds the
element tree from current state. A controller holding all state keeps views stateless and
testable. Alternatives considered:
- Per-screen state objects — more complex, no benefit for this scope
- Redux-style store — over-engineering for a TUI app

### 2. Screen-based navigation via enum

**Choice**: `Screen` enum (`BROWSER`, `DETAIL`, `TAG_BROWSER`, `RUN_CONFIG`, `EXECUTION_RESULTS`)
with `TuiController.currentScreen` field. `render()` dispatches to the appropriate view method
based on current screen.

**Rationale**: TamboUI has no built-in screen/navigation system. An enum + switch is the
simplest approach that allows type-safe navigation. A back-stack list is unnecessary since
all screens except BROWSER are reachable from BROWSER and return to it.

### 3. Extend ToolkitApp, wire via ServiceFactory

**Choice**: `AtunkoTui extends ToolkitApp` is the TUI entry point. `ServiceFactory` creates
it with the shared `RecipeDiscoveryService`, `RecipeExecutionEngine`, etc. `App.run()` creates
and starts `AtunkoTui` instead of printing usage.

**Rationale**: `ToolkitApp` provides the event loop and rendering lifecycle. Using `ServiceFactory`
for wiring keeps the existing Picocli factory pattern and avoids duplicating service construction.
Alternative: `TuiCommand extends TuiCommand` with Picocli integration — rejected because the
TUI is the default behavior, not a subcommand.

### 4. Reuse SortOrder enum from cli package

**Choice**: Move `SortOrder` from `io.github.atunkodev.cli` to a shared location, or simply
reference it from the TUI package. Since both CLI and TUI are in the `app` module, cross-package
reference within the same module is acceptable.

**Rationale**: `SortOrder` already implements NAME and TAGS comparators. Duplicating this logic
would violate DRY. Moving to a shared package (e.g., `io.github.atunkodev.shared`) is an option
but may be premature — cross-package reference is simpler.

### 5. Controller unit tests only (no TUI rendering tests)

**Choice**: Test `TuiController` state transitions in unit tests. Do not test TamboUI
rendering or widget behavior.

**Rationale**: TamboUI does not publish a test fixtures JAR. Rendering tests would require
mocking the entire TUI framework, which is brittle and low-value. Controller tests verify
all business logic (filtering, sorting, selection, screen transitions) without UI coupling.

### 6. TamboUI widget selection

**Choice**:
- `ListElement` for recipe browser (supports selection, scrolling, custom item rendering)
- `Panel` for detail panel (bordered container with title)
- `TextInputElement` for search filter (single-line input with cursor)
- `DockElement` for screen layout (top=header, center=content, bottom=status bar)
- `Row`/`Column` for sub-layouts
- `TabsElement` for sort order toggle

**Rationale**: These are the standard TamboUI widgets for this type of application. `ListElement`
is preferred over `TableElement` for the recipe browser because it allows richer per-item
rendering (name + description + tags) with built-in selection support.

## Risks / Trade-offs

**[TamboUI is 0.2.0-SNAPSHOT]** → API may change between snapshots. Mitigation: pin to a
specific snapshot timestamp if instability occurs. The TUI layer is isolated in its own
package, limiting blast radius.

**[Recipe options are display-only]** → CLI_0001.7 (Recipe Options) will show options but
editing requires core module changes to support option mutation on `RecipeDescriptor`.
Mitigation: implement as read-only display now, add editing in a follow-up change.

**[No TUI rendering tests]** → Rendering bugs won't be caught by automated tests.
Mitigation: controller unit tests cover all state logic. Manual testing covers visual
correctness. This is acceptable for a PoC.

**[Large recipe list performance]** → OpenRewrite discovers 2000+ recipes. Mitigation:
TamboUI's `ListElement` handles virtualized scrolling. Search filtering reduces the
visible set. Lazy loading is not needed — discovery is already fast (in-memory scan).
