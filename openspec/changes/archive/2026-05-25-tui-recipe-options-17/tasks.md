## 1. Core — Recipe Option Metadata (CORE_0014, SVC_CORE_0014, SVC_CORE_0014.1)

- [x] 1.1 Add `RecipeOptionInfo` record to `atunko-core` (`name`, `type`, `displayName`, `description`, `example`, `valid`, `required`, `defaultValue`) — mirrors `OptionDescriptor` fields without leaking the OpenRewrite type
- [x] 1.2 Extend `RecipeInfo` with `List<RecipeOptionInfo> options()` field; add convenience constructors that default `options` to `List.of()` so existing call sites compile unchanged
- [x] 1.3 Update `RecipeDiscoveryService.toRecipeInfo()` to populate `options` from `descriptor.getOptions()`
- [x] 1.4 Add `docs/reqstool` entries: requirement `CORE_0014` (RecipeInfo exposes option metadata) and `CORE_0014.1` (RecipeOptionInfo captures name/type/displayName/description/example/valid/required/defaultValue); add matching SVCs `SVC_CORE_0014` and `SVC_CORE_0014.1`
- [x] 1.5 Write unit tests in `atunko-core` verifying `RecipeOptionInfo` fields and that `RecipeDiscoveryService` populates them; annotate with `@SVCs({"atunko:SVC_CORE_0014", "atunko:SVC_CORE_0014.1"})`

## 2. TUI — Controller State (TUI_0001.24–26, SVC_TUI_0001.24–26)

- [x] 2.1 Add `Map<String, Map<String, Object>> recipeOptions` field to `TuiController`; add `getRecipeOptions(String recipeName)`, `setRecipeOption(String recipeName, String optionName, Object value)`, `clearRecipeOption(String recipeName, String optionName)` methods; annotate with `@Requirements({"atunko:TUI_0001.25"})`
- [x] 2.2 Add `showOptions` boolean field and `focusedRecipeForOptions` string field to `TuiController`; add `isShowOptions()`, `openOptions(String recipeName)`, `closeOptions()`, `focusedRecipeForOptions()` — annotate with `@Requirements({"atunko:TUI_0001.24"})`
- [x] 2.3 Update `TuiController.buildRunConfig()` to merge stored option values into each `RecipeEntry`; omit null/empty option maps — annotate with `@Requirements({"atunko:TUI_0001.26"})`
- [x] 2.4 Add `docs/reqstool` entries: TUI_0001.24 (options overlay accessible via `o` key, shows all options for focused recipe), TUI_0001.25 (user can edit string/int/boolean option values; stored on controller), TUI_0001.26 (configured option values are applied when building RunConfig); add SVCs SVC_TUI_0001.24–26
- [x] 2.5 Write `TuiController` unit tests for option state: default empty, set/get, clear, buildRunConfig with options; annotate with `@SVCs({"atunko:SVC_TUI_0001.24", "atunko:SVC_TUI_0001.25", "atunko:SVC_TUI_0001.26"})`

## 3. TUI — RecipeOptionsView (TUI_0001.24, TUI_0001.25)

- [x] 3.1 Create `atunko-tui/.../view/RecipeOptionsView.java` — full-screen overlay following `ExportConfigView` pattern; renders all options for the focused recipe with name, type, required flag, description, example/valid values, and current stored value; highlight tracks focused option
- [x] 3.2 Key handler in `RecipeOptionsView`: `j`/`k` navigate options; `Enter` on string/int option activates inline text-editing mode (use `Toolkit.handleTextInputKey`); `Enter` on boolean cycles `true`/`false`/`(unset)`; first `Esc` while editing cancels edit; second `Esc` (or `Esc` when not editing) closes overlay; `Del`/`Backspace` (not editing) clears the stored value for the focused option
- [x] 3.3 Show "No configurable options" message body when `recipe.options()` is empty
- [x] 3.4 Wire `o` key in `BrowserView` / recipe-list screen: if a recipe is highlighted and `o` is pressed, call `controller.openOptions(recipe.name())` and return `HANDLED`
- [x] 3.5 Wire `o` key in `ConfirmRunView`: if `o` is pressed and recipes exist, call `controller.openOptions(highlighted recipe name)` and return `HANDLED`; add `controller.isShowOptions()` guard at top of `render()` to delegate to `RecipeOptionsView.render(controller)`
- [x] 3.6 Update footer/status-bar text in `BrowserView` and `ConfirmRunView` to include `o:options`
- [x] 3.7 Annotate `RecipeOptionsView` with `@Requirements({"atunko:TUI_0001.24", "atunko:TUI_0001.25"})`
- [x] 3.8 Write `RecipeOptionsView` unit tests: renders options list, highlights focused option, shows "no options" message, editing a value updates controller, clearing removes value, Esc closes overlay; annotate with `@SVCs({"atunko:SVC_TUI_0001.24", "atunko:SVC_TUI_0001.25"})`

## 4. Build and Quality

- [x] 4.1 Run `./gradlew spotlessApply` and `./gradlew build` — fix any formatting, Checkstyle, or Error Prone issues
- [x] 4.2 Run `./gradlew test` — confirm all tests pass
