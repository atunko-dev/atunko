## Why

Recipe discovery is the foundational capability of atunko — without it, nothing else works.
The core engine needs to scan the classpath for all available OpenRewrite recipes so that
users can browse, search, and eventually execute them. This is CORE_0001 and the first
requirement to implement.

## What Changes

- Add `RecipeDiscoveryService` to the core module that scans the runtime classpath using
  OpenRewrite's `Environment` API and returns recipe metadata
- Add `RecipeInfo` record to hold recipe metadata (name, display name, description, tags)
- Add tests linked to SVC_CORE_0001 verifying that recipes from all classpath modules
  are discovered with name and description

## Capabilities

### New Capabilities
- `recipe-discovery`: Core engine classpath scanning for OpenRewrite recipes, returning
  structured metadata (name, display name, description, tags) for all discovered recipes

### Modified Capabilities

## Impact

- **Code**: New classes in `core/src/main/java/dev/atunko/core/recipe/`
- **Tests**: New test class in `core/src/test/java/dev/atunko/core/recipe/`
- **Dependencies**: Uses existing OpenRewrite dependencies already declared in `core/build.gradle`
- **APIs**: No external API changes — this is an internal core service
