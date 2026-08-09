## Why

All recipes are shown in one flat list regardless of where they came from (issue #19).
Teams that maintain their own recipe libraries cannot focus the list on their custom
recipes, and users cannot tell a recipe bundled with atunko apart from one they supplied
themselves.

## What Changes

- Every discovered recipe carries a source classification: `BUNDLED` (discovered from
  atunko's own runtime classpath) or `USER` (contributed by a recipe jar the user supplied
  on the command line).
- `list` and `search` accept repeatable `--recipe-jar <path>` options that add user recipe
  jars to the discovery environment, and a `--source bundled|user|all` filter
  (default `all`). The `tui` subcommand accepts `--recipe-jar` as well.
- The TUI browser gets a source toggle key (`u`) cycling All → Bundled → User, with the
  active filter shown in the status bar and the key listed in the help overlay.
- Web UI is out of scope for this change.

## Capabilities

### New Capabilities

- `recipe-source-toggle`: recipe source classification and filtering in core, `--source`
  and `--recipe-jar` on the CLI `list`/`search` commands, and a source toggle in the TUI
  browser (planned reqstool IDs: CORE_0019 + sub-requirements, CLI_0008 +
  sub-requirement, TUI_0006 + sub-requirement).

### Modified Capabilities

<!-- none — existing discovery, search, and browsing behaviour is unchanged when no
     user jars are supplied and the filter is at its default (all) -->

## Impact

- `atunko-core`: new `RecipeSource` and `RecipeSourceFilter` enums; `RecipeInfo` gains a
  `source` component (existing constructors keep working, defaulting to `BUNDLED`);
  `EnvironmentProvider` learns to load user recipe jars and report which recipe names they
  contributed; `RecipeDiscoveryService` classifies at discovery time and offers filtered
  `discoverAll`/`search` overloads.
- `atunko-cli`: `ListCommand` and `SearchCommand` gain `--source` and `--recipe-jar`.
- `atunko-tui`: `TuiController` gains source-filter state; `BrowserView` gains the `u`
  toggle key and the status-bar indicator; `HelpOverlay` lists the key; `TuiCommand`
  gains `--recipe-jar`.
- reqstool: new requirements CORE_0019, CLI_0008, TUI_0006 with SVCs.
