## Why

CLI_0002 currently conflates listing and searching into a single `discover` command. Splitting into `list` and `search` as separate subcommands makes intent explicit and enables richer options per command. Both commands gain `--format text|json` and `--sort name|tags`. Search also gains `--field` to narrow which recipe fields the query matches against.

## What Changes

- Replace `DiscoverCommand` with `ListCommand` (lists all recipes)
- Add `SearchCommand` (searches by keyword with field filtering)
- Both commands support `--format text|json` and `--sort name|tags`
- Add `RecipeField` enum and field-filtered search in core

## Capabilities

### Modified Capabilities
- `cli-recipe-discovery`: Split `discover` into `list` and `search` with format, sort, and field options (CLI_0002, CLI_0004)

## Impact

- Breaking: `atunko discover` is removed, replaced by `atunko list` and `atunko search`
- Modified: `App.java`, `ServiceFactory.java`
- New: `ListCommand`, `SearchCommand`, `RecipeField`
- Deleted: `DiscoverCommand`, `DiscoverCommandTest`
