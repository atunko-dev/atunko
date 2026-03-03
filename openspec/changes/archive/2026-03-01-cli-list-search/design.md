## Context

The core module provides `RecipeDiscoveryService` with `discoverAll()` and `search(query)`. The CLI layer currently exposes this via a single `DiscoverCommand`. This change splits the CLI into two focused subcommands.

## Goals / Non-Goals

**Goals:**
- `atunko list` lists all available recipes with `--format` and `--sort` options
- `atunko search <query>` filters recipes by keyword with `--format`, `--sort`, and `--field` options
- JSON output includes all RecipeInfo fields (name, displayName, description, tags)
- Sort by name (alphabetical) or by tags (first tag, then name within group)
- Field filter narrows which recipe fields the query matches against

**Non-Goals:**
- Pagination (not needed for Phase 1)
- TUI integration (CLI_0001, separate)

## Decisions

### Separate list and search commands

**Rationale:** `list` always shows all recipes. `search` always requires a query. Clear intent, no ambiguity about whether `--search` is optional.

### Shared enums for format and sort

**Rationale:** `OutputFormat` and `SortOrder` enums are reused by both commands. Avoids duplication.

### RecipeField enum in core

**Rationale:** Field filtering is a core search concern, not just CLI. Adding `search(query, fields)` overload keeps the core module reusable.

### Jackson ObjectMapper for JSON

**Rationale:** Jackson is already on the classpath via OpenRewrite BOM. No new dependency needed.

## Risks / Trade-offs

- **Breaking change**: `atunko discover` is removed. Acceptable pre-1.0.0.
