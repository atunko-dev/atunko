## 1. TUI Controller Foundation

- [x] 1.1 Write TuiController tests for initial state and recipe loading (SVC_CLI_0001)
- [x] 1.2 Implement TuiController with Screen enum, state fields, and recipe loading from RecipeDiscoveryService
- [x] 1.3 Write test for recipe table state — controller exposes filtered/sorted recipe list (SVC_CLI_0001.1)
- [x] 1.4 Implement recipe list state in TuiController — discoverAll on start, expose as list

## 2. Search and Sort

- [x] 2.1 Write test for search filtering — setting query filters the recipe list (SVC_CLI_0001.3)
- [x] 2.2 Implement search filter in TuiController — query field, filtered list recomputed on change
- [x] 2.3 Write test for sort order — toggling sort reorders the recipe list (SVC_CLI_0001.6)
- [x] 2.4 Implement sort order in TuiController — reuse SortOrder enum from cli package

## 3. Selection and Navigation

- [x] 3.1 Write test for recipe highlighting — highlighted index tracks current recipe (SVC_CLI_0001.2)
- [x] 3.2 Implement highlighted recipe state in TuiController — index tracking, detail info exposed
- [x] 3.3 Write test for multi-select — toggling selection adds/removes recipes (SVC_CLI_0001.5)
- [x] 3.4 Implement multi-select in TuiController — Set of selected recipe names
- [x] 3.5 Write test for screen navigation — selecting a recipe switches to DETAIL screen (SVC_CLI_0001.4)
- [x] 3.6 Implement screen navigation — currentScreen field, methods to navigate between screens
- [x] 3.7 Write test for keyboard navigation — focus changes and actions trigger state updates (SVC_CLI_0001.12)
- [x] 3.8 Implement keyboard action methods in TuiController — up/down, enter, escape, tab, shortcuts

## 4. Recipe Options and Tag Browser

- [x] 4.1 Write test for recipe options display — selected recipe exposes its option descriptors (SVC_CLI_0001.7)
- [x] 4.2 Implement recipe options state in TuiController — load options from RecipeDescriptor
- [x] 4.3 Write test for tag browser — listing all tags and filtering by tag (SVC_CLI_0001.11)
- [x] 4.4 Implement tag browser state in TuiController — collect tags from all recipes, filter by selected tag

## 5. Execution

- [x] 5.1 Write test for dry-run preview — running dry-run populates preview results without applying (SVC_CLI_0001.8)
- [x] 5.2 Implement dry-run in TuiController — call RecipeExecutionEngine, store results, do not apply
- [x] 5.3 Write test for recipe execution — executing applies changes and stores results (SVC_CLI_0001.9)
- [x] 5.4 Implement execution in TuiController — call RecipeExecutionEngine + ChangeApplier, store results

## 6. Save Run Configuration

- [x] 6.1 Write test for save run config — selected recipes are persisted to .atunko.yml (SVC_CLI_0001.10)
- [x] 6.2 Implement save in TuiController — delegate to RunConfigService with selected recipe names

## 7. TUI Views and Wiring

- [x] 7.1 Create AtunkoTui (extends ToolkitApp) — render() dispatches to view methods based on currentScreen
- [x] 7.2 Implement BrowserView — DockElement with search input, recipe list, detail panel, status bar
- [x] 7.3 Implement DetailView — full-screen recipe detail with options display
- [x] 7.4 Implement TagBrowserView — tag list with recipe filtering
- [x] 7.5 Implement ExecutionResultsView — file change list with before/after preview
- [x] 7.6 Wire event handlers — keyboard events update TuiController state
- [x] 7.7 Create TuiCommand (Picocli @Command) that launches AtunkoTui
- [x] 7.8 Update ServiceFactory to create TuiCommand with all dependencies
- [x] 7.9 Register TuiCommand as subcommand in App (alongside list, search, run)

## 8. Verification

- [x] 8.1 Run `./gradlew build` — all tests pass, Spotless + Checkstyle + Error Prone clean
- [x] 8.2 Run `./gradlew :app:run --args="tui"` — TUI launches
- [x] 8.3 Run `./gradlew :app:run` — prints help (default unchanged)
- [x] 8.4 Run `./gradlew :app:run --args="list"` — CLI still works
- [x] 8.5 Run `openspec validate --all --strict` — specs valid
