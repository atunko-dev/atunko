## Why

CLI_0003 requires the tool to execute recipes via a CLI subcommand. This enables headless/scripted recipe execution without the TUI.

## What Changes

- Add `RunCommand` Picocli subcommand that executes a recipe against a project
- Support `-r`/`--recipe` for specifying the recipe name
- Support `--project-dir` for specifying the project directory
- Wire to existing `RecipeExecutionEngine` from core module
- Register in `App` root command

## Capabilities

### New Capabilities
- `cli-recipe-execution`: Execute recipes via `atunko run` CLI subcommand (CLI_0003)

### Modified Capabilities

## Impact

- New class in `app` module: `io.github.atunko.cli.RunCommand`
- Modified class: `io.github.atunko.App` (register RunCommand subcommand)
- Wires to `io.github.atunko.core.engine.RecipeExecutionEngine`
