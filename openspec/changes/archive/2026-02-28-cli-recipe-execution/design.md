## Context

The core module provides `RecipeExecutionEngine.execute(recipeName, sources)` and project scanners. The CLI layer needs to expose recipe execution via `atunko run -r <recipe>`.

## Goals / Non-Goals

**Goals:**
- `atunko run -r <recipe> --project-dir <path>` executes a recipe against a project
- Reports changed files to stdout
- Returns exit code 0 on success

**Non-Goals:**
- Dry-run mode (Phase 2)
- Running from config file via CLI (separate from CORE_0008 load)
- Automatic project type detection (user specifies project dir, scanner auto-detects)

## Decisions

### Minimal RunCommand for Phase 1

**Rationale:** The run command invokes `RecipeExecutionEngine` directly. For Phase 1, it parses Java source files from the project directory (using `JavaParser`) and reports results. Full project scanning integration (Gradle/Maven classpath resolution) will be wired in Phase 2.

### Output format

Each changed file printed with its path. Count of changes shown at the end.

## Risks / Trade-offs

- **No project scanning in Phase 1**: The run command parses Java files directly without classpath resolution. This limits recipe accuracy for recipes that need type information. Acceptable for Phase 1 PoC.
