## Why

CLI_0002 requires the tool to list and search available recipes via a CLI subcommand. This enables headless/scripted usage of recipe discovery without the TUI.

## What Changes

- Add `DiscoverCommand` Picocli subcommand that lists all recipes
- Support `--search` option for keyword filtering
- Wire to existing `RecipeDiscoveryService` from core module
- Add root `App` command as the Picocli entry point

## Capabilities

### New Capabilities
- `cli-recipe-discovery`: List and search recipes via `atunko discover` CLI subcommand (CLI_0002)

### Modified Capabilities

## Impact

- New classes in `app` module: `io.github.atunko.App`, `io.github.atunko.cli.DiscoverCommand`
- Wires to `io.github.atunko.core.recipe.RecipeDiscoveryService`
