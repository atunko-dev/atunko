## Why

Users can discover and search recipes (CORE_0001, CORE_0002) but cannot actually run them.
Without execution, atunko is a recipe browser — not a recipe runner. CORE_0003 adds the ability
to execute a selected recipe against parsed source files and return the transformation results.

## What Changes

- Add a recipe execution engine to the core module that takes a recipe name, parsed source files,
  and an execution context, runs the recipe, and returns results (modified files, diffs, errors)
- Provide a simple API surface: execute a recipe by name against a list of source files
- Return structured results that callers (CLI, TUI) can use to display changes or write to disk

## Capabilities

### New Capabilities
- `recipe-execution`: Execute an OpenRewrite recipe against parsed source files and return
  transformation results (CORE_0003)

### Modified Capabilities

## Impact

- **Code**: New classes in `core/src/main/java/io/github/atunko/core/engine/`
- **Tests**: New test class with test fixture project in `core/src/test/resources/`
  linked to SVC_CORE_0003
- **Dependencies**: No new dependencies — uses existing OpenRewrite rewrite-core and rewrite-java
- **APIs**: New internal core API — no CLI/TUI changes in this change
