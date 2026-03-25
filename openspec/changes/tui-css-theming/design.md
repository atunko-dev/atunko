## Context

The TUI uses TamboUI's Toolkit DSL (`ToolkitApp`, `Element`, `Dock`, `Column`, etc.) with all styling applied inline via `.fg(Color.X)`, `.bg(Color.Y)`, `.bold()`, `.borderColor(Color.Z)`. The `tamboui-css` dependency is already declared but unused. Elements already have IDs (`#browser`, `#detail`, `#tag-browser`, `#confirm-run`, `#execution-results`) but no CSS classes.

TamboUI's CSS module (`tamboui-css`) provides a `StyleEngine` that loads TCSS stylesheets, resolves styles via type/ID/class/pseudo-class selectors, and integrates with `ToolkitRunner`. Elements with `.id()` and `.cssClass()` automatically participate in CSS styling.

## Goals / Non-Goals

**Goals:**
- Extract all inline styles from views into TCSS stylesheets
- Ship two bundled themes (dark, light) as classpath resources in the JAR
- Default to dark theme when no theme is specified
- Support `--theme dark|light` CLI flag on `tui` subcommand
- Support `--css-file <path>` CLI flag for user-provided CSS
- Auto-load `~/.config/atunko/theme.tcss` if present (lower priority than `--css-file`)
- User CSS replaces the bundled theme (no layering)
- Use semantic CSS classes across all views

**Non-Goals:**
- Auto-detecting terminal background color (unreliable across terminals)
- Runtime theme switching within a TUI session (restart required)
- Theming the Web UI (separate concern, Vaadin has its own theming)
- Partial/layered CSS (user CSS on top of bundled theme)

## Decisions

### 1. StyleEngine initialized in AtunkoTui, configured via TuiConfig/ToolkitRunner

**Decision**: Create and configure the `StyleEngine` in `AtunkoTui`'s constructor, passing it to the `ToolkitRunner` via the builder API.

**Rationale**: `AtunkoTui` extends `ToolkitApp` and is the natural place to own the style engine lifecycle. The `ToolkitRunner` builder integrates with `StyleEngine` natively.

### 2. Theme resolution order

**Decision**: Resolve the active theme in this order:
1. `--css-file <path>` → load that file as an inline stylesheet (replaces bundled)
2. `~/.config/atunko/theme.tcss` exists → load it (replaces bundled)
3. `--theme dark|light` → load the named bundled theme
4. No flags → load `dark.tcss` (default)

**Rationale**: User-provided CSS is the most explicit override. XDG config is a persistent preference. CLI flag is a session override. Default is dark (safe for most dev terminals).

### 3. Bundled themes as classpath resources

**Decision**: Ship themes at `themes/dark.tcss` and `themes/light.tcss` in `atunko-tui/src/main/resources/`. Load via `StyleEngine.loadStylesheet(name, classpathResource)`.

**Rationale**: Classpath resources are packaged in the shadow JAR and accessible without filesystem paths. Named stylesheets integrate with `StyleEngine`'s theme switching API.

### 4. Semantic CSS class vocabulary

**Decision**: Use the following semantic classes across all views:

| Class | Purpose |
|-------|---------|
| `.app` | Root element — overall background and default text color |
| `.screen-title` | View title bars (e.g., "Recipe Browser", "Tag Browser") |
| `.status-bar` | Footer help/status lines |
| `.panel` | Bordered content panels |
| `.list-item` | Individual items in lists |
| `.section-header` | Section labels within views |
| `.selected` | Selected state indicator |
| `.search-mode` | Search/filter mode title styling |
| `.recipe-name` | Recipe name text |
| `.recipe-description` | Recipe description text |
| `.tag` | Tag labels |
| `.coverage-indicator` | Coverage status markers |

**Rationale**: Semantic classes decouple visual style from view structure. Themes can restyle the entire app by targeting these classes without knowing view-specific layout. This is more maintainable than per-view selectors.

### 5. Full inline style extraction (Option A)

**Decision**: Remove ALL inline style calls (`.fg()`, `.bg()`, `.bold()`, `.borderColor()`, etc.) from view files. Views assign only structure (layout, content) and semantic classes. All visual styling comes from CSS.

**Rationale**: Partial extraction (keeping some inline styles as "defaults") creates confusion about which styles come from where and makes themes unreliable. Full extraction ensures themes have complete control.

### 6. Theme options passed from TuiCommand to AtunkoTui

**Decision**: Add `--theme` and `--css-file` as Picocli `@Option` fields on `TuiCommand`. Pass them to `AtunkoTui` via constructor or a config record.

**Rationale**: `TuiCommand` is the CLI entry point and already handles `--project-dir` and `--log-file`. Theme options are TUI-specific (not on the top-level `atunko` command).

## Risks / Trade-offs

- **Visual regression risk**: Extracting ~70+ inline styles and recreating them in TCSS is error-prone. A style missed in CSS will render with terminal defaults instead of the intended color. Mitigation: systematic extraction — map each view's inline styles to CSS rules, then visually verify.
- **TamboUI CSS property coverage**: Not all inline style methods may have CSS equivalents (e.g., `Color.indexed(236)` — need to verify TCSS supports indexed/256 colors). Mitigation: check TamboUI CSS property support during implementation; fall back to hex equivalents if indexed colors aren't supported.
- **User CSS learning curve**: Users need to know the semantic class names and supported TCSS properties to write custom themes. Mitigation: document the class vocabulary and ship well-commented bundled themes as examples.

## Migration Plan

Purely additive to `atunko-tui`. No changes to `atunko-core` or `atunko-cli`. No data migration. The visual appearance with the default dark theme should be identical to the current hardcoded styling.
