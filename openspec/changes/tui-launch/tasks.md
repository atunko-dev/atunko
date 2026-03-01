## 1. TUI Controller Foundation

- [ ] 1.1 Write TuiController tests for initial state and recipe loading (SVC_CLI_0001)
- [ ] 1.2 Implement TuiController with Screen enum, state fields, and recipe loading from RecipeDiscoveryService
- [ ] 1.3 Write test for recipe table state — controller exposes filtered/sorted recipe list (SVC_CLI_0001.1)
- [ ] 1.4 Implement recipe list state in TuiController — discoverAll on start, expose as list

## 2. Search and Sort

- [ ] 2.1 Write test for search filtering — setting query filters the recipe list (SVC_CLI_0001.3)
- [ ] 2.2 Implement search filter in TuiController — query field, filtered list recomputed on change
- [ ] 2.3 Write test for sort order — toggling sort reorders the recipe list (SVC_CLI_0001.6)
- [ ] 2.4 Implement sort order in TuiController — reuse SortOrder enum from cli package

## 3. Selection and Navigation

- [ ] 3.1 Write test for recipe highlighting — highlighted index tracks current recipe (SVC_CLI_0001.2)
- [ ] 3.2 Implement highlighted recipe state in TuiController — index tracking, detail info exposed
- [ ] 3.3 Write test for multi-select — toggling selection adds/removes recipes (SVC_CLI_0001.5)
- [ ] 3.4 Implement multi-select in TuiController — Set of selected recipe names
- [ ] 3.5 Write test for screen navigation — selecting a recipe switches to DETAIL screen (SVC_CLI_0001.4)
- [ ] 3.6 Implement screen navigation — currentScreen field, methods to navigate between screens
- [ ] 3.7 Write test for keyboard navigation — focus changes and actions trigger state updates (SVC_CLI_0001.12)
- [ ] 3.8 Implement keyboard action methods in TuiController — up/down, enter, escape, tab, shortcuts

## 4. Recipe Options and Tag Browser

- [ ] 4.1 Write test for recipe options display — selected recipe exposes its option descriptors (SVC_CLI_0001.7)
- [ ] 4.2 Implement recipe options state in TuiController — load options from RecipeDescriptor
- [ ] 4.3 Write test for tag browser — listing all tags and filtering by tag (SVC_CLI_0001.11)
- [ ] 4.4 Implement tag browser state in TuiController — collect tags from all recipes, filter by selected tag

## 5. Execution

- [ ] 5.1 Write test for dry-run preview — running dry-run populates preview results without applying (SVC_CLI_0001.8)
- [ ] 5.2 Implement dry-run in TuiController — call RecipeExecutionEngine, store results, do not apply
- [ ] 5.3 Write test for recipe execution — executing applies changes and stores results (SVC_CLI_0001.9)
- [ ] 5.4 Implement execution in TuiController — call RecipeExecutionEngine + ChangeApplier, store results

## 6. Save Run Configuration

- [ ] 6.1 Write test for save run config — selected recipes are persisted to .atunko.yml (SVC_CLI_0001.10)
- [ ] 6.2 Implement save in TuiController — delegate to RunConfigService with selected recipe names

## 7. TUI Views and Wiring

- [ ] 7.1 Create AtunkoTui (extends ToolkitApp) — render() dispatches to view methods based on currentScreen
- [ ] 7.2 Implement BrowserView — DockElement with search input, recipe list, detail panel, status bar
- [ ] 7.3 Implement DetailView — full-screen recipe detail with options display
- [ ] 7.4 Implement TagBrowserView — tag list with recipe filtering
- [ ] 7.5 Implement ExecutionResultsView — file change list with before/after preview
- [ ] 7.6 Wire event handlers — keyboard events update TuiController state
- [ ] 7.7 Update ServiceFactory to create AtunkoTui with all dependencies
- [ ] 7.8 Update App.run() to launch AtunkoTui instead of printing usage

## 8. Verification

- [ ] 8.1 Run `./gradlew build` — all tests pass, Spotless + Checkstyle + Error Prone clean
- [ ] 8.2 Run `./gradlew :app:run` — TUI launches (not usage text)
- [ ] 8.3 Run `./gradlew :app:run --args="list"` — CLI still works
- [ ] 8.4 Run `openspec validate --all --strict` — specs valid
