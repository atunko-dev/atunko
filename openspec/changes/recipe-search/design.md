## Context

`RecipeDiscoveryService` currently provides `discoverAll()` which returns all available
OpenRewrite recipes. With hundreds of recipes on the classpath, users need a way to filter
results by keyword. CORE_0002 requires searching by name, description, and tags.

## Goals / Non-Goals

**Goals:**
- Add keyword-based search/filtering to the recipe discovery service
- Support case-insensitive partial matching across name, description, and tags
- Keep the API simple — a single search method with a query string

**Non-Goals:**
- Advanced query syntax (boolean operators, field-specific queries)
- Relevance ranking or scoring — results are unordered matches
- Fuzzy/typo-tolerant matching
- Pagination or result limiting

## Decisions

### 1. Add `search(String query)` method to `RecipeDiscoveryService`

**Decision**: Add search as a method on the existing service rather than creating a separate
`RecipeSearchService`.

**Rationale**: Search is a filter over discovery — it's the same responsibility (finding recipes),
just with a narrower scope. A separate service would need to depend on `RecipeDiscoveryService`
anyway, adding indirection without value at this stage.

**Alternatives considered**:
- Separate `RecipeSearchService` — unnecessary abstraction for a single filter method
- `Predicate`-based API (e.g., `discover(Predicate<RecipeInfo>)`) — more flexible but
  over-engineered for the current requirement; can refactor to this later if needed

### 2. Case-insensitive substring matching

**Decision**: Convert both query and target fields to lowercase, then check if any field
contains the query string.

**Rationale**: Simple, predictable behavior. Users searching "spring boot" expect to find
recipes regardless of casing. Substring matching covers the common case without complexity.

### 3. Match against name, description, and tags

**Decision**: A recipe matches if the query appears as a substring in any of: `name`,
`displayName`, `description`, or any entry in `tags`.

**Rationale**: Matching all text fields gives the best recall. Including `displayName`
alongside `name` is natural since both identify the recipe. Tags are included per CORE_0002.

## Risks / Trade-offs

- **Performance for large catalogs**: `discoverAll()` rescans the classpath on every call,
  and `search()` will do the same before filtering. For now this is acceptable — the recipe
  count is bounded and scanning is fast. → If performance becomes an issue, add caching later.
- **Empty query returns all**: A blank/null query will return all recipes (same as `discoverAll()`).
  This is intentional — it's the least surprising behavior.
