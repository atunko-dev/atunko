## Why

The TUI currently hardcodes ~70+ color values inline across 6 view files (`BrowserView`, `DetailView`, `TagBrowserView`, `ConfirmRunView`, `ExecutionResultsView`, `HelpOverlay`). This makes visual consistency fragile, theming impossible, and customization a source-code change. TamboUI provides a CSS module (`tamboui-css`) with a `StyleEngine` that supports TCSS stylesheets, semantic selectors, and runtime theme switching — the dependency is already declared but unused.

Adding CSS theming extracts all inline styles into TCSS files, ships two bundled themes (dark and light), and gives users the option to supply their own CSS file for full customization.

## What Changes

- All inline color/style values are removed from TUI view files
- Elements gain semantic CSS classes (`.screen-title`, `.status-bar`, `.panel`, `.selected`, etc.)
- A `StyleEngine` is initialized in `AtunkoTui` and loaded with the active theme
- Two bundled TCSS themes ship in the JAR: `dark.tcss` (default) and `light.tcss`
- `TuiCommand` gains `--theme dark|light` and `--css-file <path>` Picocli options
- User CSS from `~/.config/atunko/theme.tcss` is auto-loaded if present (unless `--css-file` overrides)
- User-provided CSS replaces the bundled theme entirely (no layering)

## Capabilities

### New Capabilities

- `tui-css-theming`: CSS-based theming for the TUI — semantic style classes, bundled dark/light themes, user-provided CSS file support, theme selection via `--theme` flag

### Modified Capabilities

- `tui-launch`: The TUI bootstrap changes — `AtunkoTui` initializes a `StyleEngine` and passes it to the `ToolkitRunner`. Views no longer contain inline styles; all styling is resolved via CSS.

## Impact

- `atunko-tui`: All 6 view files (inline style removal + CSS class assignment), `AtunkoTui` (StyleEngine setup), `TuiCommand` (new CLI options), new TCSS resource files
- `atunko-core`: No changes
- `atunko-cli`: No changes
- No breaking behavioral changes — the TUI looks the same (dark theme is the default), but styling is now externalized
