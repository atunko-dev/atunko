## Context

The atunko project has a fully scaffolded Gradle multi-module build but zero Java code.
CORE_0001 (Recipe Discovery) is the first requirement to implement — all other features
(search, execution, CLI commands, TUI views) depend on the ability to discover recipes.

OpenRewrite provides `Environment.builder().scanRuntimeClasspath().build()` which scans
for all recipes declared via `META-INF/rewrite/*.yml` files on the classpath. The core
module already declares dependencies on several OpenRewrite recipe modules (migrate-java,
spring, static-analysis, testing-frameworks).

## Goals / Non-Goals

**Goals:**
- Discover all OpenRewrite recipes available on the runtime classpath
- Return structured metadata (name, display name, description, tags) for each recipe
- Provide a clean service API that other components (search, CLI, TUI) can consume
- Link implementation to CORE_0001 and tests to SVC_CORE_0001 via reqstool annotations

**Non-Goals:**
- Recipe search/filtering (CORE_0002 — separate change)
- Recipe execution (CORE_0003 — separate change)
- Scanning external classpaths or project dependencies (CORE_0004/CORE_0005)
- Caching or lazy loading of the recipe environment

## Decisions

**1. Use `Environment.builder().scanRuntimeClasspath()` for discovery**

OpenRewrite's `Environment` is the standard entry point for recipe discovery. It scans
`META-INF/rewrite/*.yml` declarative recipe files and service-loaded `Recipe` classes.

*Alternative considered*: Manual classpath scanning with ServiceLoader — rejected because
OpenRewrite already handles this correctly including declarative YAML recipes.

**2. Immutable `RecipeInfo` record for recipe metadata**

A Java record provides immutable value semantics, automatic `equals`/`hashCode`/`toString`,
and keeps the API clean. Fields: `name`, `displayName`, `description`, `tags`.

*Alternative considered*: Exposing OpenRewrite's `Recipe` directly — rejected because it
couples consumers to OpenRewrite internals and the `Recipe` object carries more state than
needed for discovery/browsing.

**3. `RecipeDiscoveryService` as stateless service**

A simple class with a `discoverAll()` method that creates an `Environment`, scans, and
maps results to `RecipeInfo`. Stateless — each call creates a fresh environment.

*Alternative considered*: Caching the environment — deferred. Premature optimization for
the first implementation. Can be added later if startup time is an issue.

## Risks / Trade-offs

- [Classpath scanning is slow] → Acceptable for now; caching is a future optimization.
  The first call may take a few seconds depending on classpath size.
- [OpenRewrite API changes] → Mitigated by the `RecipeInfo` abstraction layer. Internal
  API changes only affect `RecipeDiscoveryService`, not consumers.
- [Some recipes may lack metadata] → `displayName`, `description`, and `tags` may be
  empty for some recipes. The service will return whatever OpenRewrite provides without
  filtering.
