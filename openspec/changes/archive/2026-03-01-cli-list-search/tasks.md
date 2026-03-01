## 1. Tests — List (TDD: SVC_CLI_0002, SVC_CLI_0002.1–.4)

- [x] 1.1 Write `ListCommandTest.list_displaysRecipesAsText` (SVC_CLI_0002.1)
- [x] 1.2 Write `ListCommandTest.list_displaysRecipesAsJson` (SVC_CLI_0002.2)
- [x] 1.3 Write `ListCommandTest.list_sortsByNameByDefault` (SVC_CLI_0002.3)
- [x] 1.4 Write `ListCommandTest.list_sortsByTags` (SVC_CLI_0002.4)

## 2. Tests — Search (TDD: SVC_CLI_0004, SVC_CLI_0004.1–.5)

- [x] 2.1 Write `SearchCommandTest.search_displaysMatchingRecipesAsText` (SVC_CLI_0004.1)
- [x] 2.2 Write `SearchCommandTest.search_showsMessageWhenNoResults` (SVC_CLI_0004.1)
- [x] 2.3 Write `SearchCommandTest.search_displaysMatchingRecipesAsJson` (SVC_CLI_0004.2)
- [x] 2.4 Write `SearchCommandTest.search_sortsByName` (SVC_CLI_0004.3)
- [x] 2.5 Write `SearchCommandTest.search_sortsByTags` (SVC_CLI_0004.4)
- [x] 2.6 Write `SearchCommandTest.search_filtersByField` (SVC_CLI_0004.5)

## 3. Implementation (CLI_0002, CLI_0004)

- [x] 3.1 Create `RecipeField` enum in core
- [x] 3.2 Add `search(query, fields)` overload to `RecipeDiscoveryService`
- [x] 3.3 Create `OutputFormat` and `SortOrder` enums in app
- [x] 3.4 Create `ListCommand` with `--format` and `--sort` options
- [x] 3.5 Create `SearchCommand` with `--format`, `--sort`, and `--field` options
- [x] 3.6 Update `App.java` subcommand registration
- [x] 3.7 Update `ServiceFactory.java` wiring
- [x] 3.8 Delete `DiscoverCommand` and `DiscoverCommandTest`
- [x] 3.9 Add `@Requirements` and `@SVCs` annotations

## 4. Verify

- [x] 4.1 Run `./gradlew build` and confirm all tests pass
- [x] 4.2 Run `openspec validate --all --strict` and confirm specs valid
