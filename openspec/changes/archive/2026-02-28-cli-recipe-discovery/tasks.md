## 1. Tests (TDD — SVC_CLI_0002)

- [x] 1.1 Write `DiscoverCommandTest.discover_listsAllRecipes` — asserts output contains recipe names
- [x] 1.2 Write `DiscoverCommandTest.discover_withSearch_filtersRecipes` — asserts --search filters output
- [x] 1.3 Write `DiscoverCommandTest.discover_withNoResults_showsMessage` — asserts message when no matches

## 2. Implementation (CLI_0002)

- [x] 2.1 Create `App` root Picocli command with subcommand registration
- [x] 2.2 Create `DiscoverCommand` with `--search` option wired to `RecipeDiscoveryService`
- [x] 2.3 Add `@Requirements({"CLI_0002"})` and `@SVCs({"SVC_CLI_0002"})` annotations

## 3. Verify

- [x] 3.1 Run `./gradlew :app:test` and confirm all tests pass
- [x] 3.2 Run `./gradlew build` and confirm full build passes
