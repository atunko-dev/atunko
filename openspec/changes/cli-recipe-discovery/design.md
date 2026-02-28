## Context

The core module already provides `RecipeDiscoveryService` with `discoverAll()` and `search(query)`. The CLI layer needs to expose this via Picocli subcommands.

## Goals / Non-Goals

**Goals:**
- `atunko discover` lists all available recipes (name + description)
- `atunko discover --search <query>` filters recipes by keyword
- Output is human-readable text to stdout

**Non-Goals:**
- JSON/structured output (Phase 2)
- Pagination (not needed for Phase 1)
- TUI integration (CLI_0001, separate)

## Decisions

### Root App command with subcommands

**Rationale:** Picocli's `@Command` with `subcommands` provides clean CLI structure. `App` is the top-level command (handles `--help`, `--version`). `DiscoverCommand` is a subcommand.

### Output format

Each recipe printed as: `<name> - <description>` (one per line). Simple, grep-friendly. Count shown at the end.

### Picocli CommandLine execution

**Rationale:** Use `new CommandLine(new App()).execute(args)` in `main()` for proper exit code handling.

## Risks / Trade-offs

- **TamboUI integration deferred**: CLI_0001 (TUI Launch) is not part of this change. The `App` command will be the entry point but TUI launch is handled separately.
