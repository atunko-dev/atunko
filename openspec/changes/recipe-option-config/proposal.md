## Why

OpenRewrite recipes can have configurable options (e.g., `ChangeMethodName` has `oldMethodName`, `newMethodName`), but atunko ignores them entirely. `RecipeInfo` drops option metadata during discovery, `RunConfig` stores only recipe names, and recipes are executed with defaults only. This means recipes that *require* options cannot be used, and configurable recipes cannot be tuned — a fundamental gap for a recipe management tool.

## What Changes

- **RecipeInfo wraps RecipeDescriptor** — instead of extracting fields, RecipeInfo delegates to the OpenRewrite descriptor, gaining access to options (and future descriptor fields) for free
- **Option metadata exposed** — `RecipeInfo.options()` returns `List<OptionDescriptor>` with name, type, description, required, example, and valid values
- **RunConfig stores option values** — recipes change from `List<String>` to `List<RecipeConfig>` where each entry has a name and an optional `Map<String, Object>` of configured values. **BREAKING**: `.atunko.yml` format changes from string list to object list
- **Execution applies options** — `RecipeExecutionEngine` uses `Recipe.withOptions(Map)` to apply configured values before running
- **TUI and Web UI display options** — detail panels show option metadata (name, type, required) for the highlighted recipe
- **JSON Schema for .atunko.yml** — enables IDE validation and completion

## Capabilities

### New Capabilities
- `recipe-option-config`: Recipe option discovery, configuration storage, and execution with configured values

### Modified Capabilities
- `recipe-discovery`: RecipeInfo now wraps RecipeDescriptor and exposes option metadata (CORE_0001)
- `recipe-execution`: Execution engine accepts and applies option values (CORE_0003)
- `save-run-config`: RunConfig format changes to include option values per recipe (CORE_0007)
- `load-run-config`: Loading config restores option values (CORE_0008)
- `tui-launch`: DetailView displays recipe options (TUI_0001.7)
- `web-ui-launch`: Detail panel displays recipe options (WEB_0001.6)

## Impact

- **atunko-core**: RecipeInfo.java (record → wrapper), RecipeDiscoveryService.java (simplified mapping), new RecipeConfig.java, RunConfig.java (schema change), RecipeExecutionEngine.java (options overload)
- **atunko-tui**: DetailView.java (options display), TuiController.java (saveRunConfig, fallback RecipeInfo)
- **atunko-web**: RecipeBrowserView.java (options in detail panel)
- **Tests**: All `new RecipeInfo(...)` calls migrate to `RecipeInfo.of(...)` factory across tui, web, and core test suites
- **New file**: `docs/schemas/atunko-run-config.schema.json`
- **reqstool**: New requirements CORE_0001.2, CORE_0003.2, CORE_0007.1, CORE_0008.1 and corresponding SVCs
