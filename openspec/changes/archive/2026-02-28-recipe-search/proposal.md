## Why

Users need to find specific recipes among potentially hundreds of available OpenRewrite recipes.
Without search/filtering, they must scroll through the entire catalog manually. CORE_0002 adds
keyword-based search across recipe name, description, and tags to make recipe discovery practical.

## What Changes

- Add search/filtering capability to the core engine that matches recipes by keyword against
  name, description, and tags
- Support case-insensitive partial matching so users can find recipes with simple queries
  like "spring boot" or "junit"

## Capabilities

### New Capabilities

### Modified Capabilities
- `recipe-discovery`: Adding recipe search/filtering (CORE_0002) — users can search the
  discovered recipe catalog by keyword, matching against name, description, and tags

## Impact

- **Code**: Modifications to existing classes in `core/src/main/java/io/github/atunko/core/recipe/`
- **Tests**: New test methods in `core/src/test/java/io/github/atunko/core/recipe/` linked to SVC_CORE_0002
- **Dependencies**: No new dependencies — uses existing OpenRewrite recipe metadata
- **APIs**: No external API changes — internal core service enhancement
