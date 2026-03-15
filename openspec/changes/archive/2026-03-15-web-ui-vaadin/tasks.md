## 1. reqstool — Add WEB_* Requirements and SVCs

- [x] 1.1 Add `WEB_0001` (Web UI Launch), `WEB_0001.1` (Recipe TreeGrid), `WEB_0001.2` (Search +
         Tag Filter), `WEB_0001.3` (Port Option), `WEB_0001.4` (Expand/Collapse), `WEB_0001.5`
         (Cascade Multi-Select), `WEB_0001.6` (Detail Panel) to `docs/reqstool/requirements.yml`
- [x] 1.2 Add `SVC_WEB_0001`, `SVC_WEB_0001.1` – `SVC_WEB_0001.6`
         to `docs/reqstool/software_verification_cases.yml`
- [x] 1.3 Add `- path: ../../atunko-web/docs/reqstool` to `implementations.local`
         in `docs/reqstool/requirements.yml`

## 2. Gradle Module Setup

- [x] 2.1 Add `'atunko-web'` to `settings.gradle`
- [x] 2.2 Add Vaadin and vaadin-boot to `gradle/libs.versions.toml`:
         - `vaadin = "24.7.4"`, `vaadin-boot = "13.3"`
         - `vaadin-bom`, `vaadin-core`, `vaadin-boot` library aliases
         - `karibu-testing` alias (`com.github.mvysny.kaributesting:karibu-testing-v24:2.2.4`)
         - `vaadin` plugin alias (`com.vaadin`, version `24.7.4`)
- [x] 2.3 Add `id 'com.vaadin' version '24.7.4' apply false` to root `build.gradle`
- [x] 2.4 Create `atunko-web/build.gradle` with:
         - `java-library` + `com.vaadin` plugins
         - `api project(':atunko-core')`, Picocli, Vaadin BOM + core, vaadin-boot dependencies
         - `testImplementation` karibu-testing-v24
         - Spotless + Checkstyle + Error Prone config (same pattern as atunko-tui)
- [x] 2.5 Add `implementation project(':atunko-web')` to `atunko-cli/build.gradle`

## 3. reqstool Subproject Setup

- [x] 3.1 Create `atunko-web/docs/reqstool/reqstool_config.yml` (same pattern as atunko-tui)
- [x] 3.2 Create `atunko-web/docs/reqstool/requirements.yml` with urn `atunko-web`,
         filter `ids == /WEB_.*/`
- [x] 3.3 Create `atunko-web/docs/reqstool/software_verification_cases.yml` with
         filter `ids == /SVC_WEB_.*/`

## 4. Tests (TDD)

- [x] 4.1 Create `atunko-web/src/test/java/io/github/atunkodev/web/WebUiCommandTest.java`:
         - `@SVCs({"SVC_WEB_0001"})` — `WebUiCommand` can be instantiated
         - `@SVCs({"SVC_WEB_0001.3"})` — default port is 8080
- [x] 4.2 Create `atunko-web/src/test/java/io/github/atunkodev/web/view/RecipeBrowserViewTest.java`
         using Karibu-Testing (`MockVaadin.setup()` / `UI.getCurrent()`):
         - `@SVCs({"SVC_WEB_0001.1"})` — empty search returns all recipes in TreeGrid
         - `@SVCs({"SVC_WEB_0001.2"})` — text search filters by name, description, or tags
         - `@SVCs({"SVC_WEB_0001.2"})` — tag multi-select filters with OR/ANY logic
         - `@SVCs({"SVC_WEB_0001.2"})` — text search and tag filter compose with AND
         - `@SVCs({"SVC_WEB_0001.4"})` — composite recipe rows are expandable/collapsible
         - `@SVCs({"SVC_WEB_0001.5"})` — checking parent checks all children (cascade down)
         - `@SVCs({"SVC_WEB_0001.5"})` — checking all children checks parent (cascade up)
         - `@SVCs({"SVC_WEB_0001.5"})` — unchecking one child puts parent into indeterminate
         - `@SVCs({"SVC_WEB_0001.6"})` — clicking a row updates the detail panel

## 5. Implementation

- [x] 5.1 Create `RecipeHolder` — static singleton, `init(List<RecipeDescriptor>)` + `getRecipes()`
- [x] 5.2 Create `WebUiCommand` — `@Command(name = "webui")`, `--port` option (default 8080),
         calls `RecipeHolder.init()` then `WebServer.start(port)`
- [x] 5.3 Create `CascadeSelectionHandler` — encapsulates cascade-both-ways checkbox logic for
         `TreeGrid<RecipeDescriptor>`: check parent → check children; check all children →
         check parent; uncheck child → parent indeterminate
- [x] 5.4 Create `RecipeBrowserView` — `@Route("")`:
         - `AppLayout` shell with "atunko" header title
         - `TextField` search bar with `ValueChangeMode.EAGER`
         - `SplitLayout` (horizontal): left = `TreeGrid`, right = detail panel
         - `TreeGrid<RecipeDescriptor>`: Name column (sortable, hierarchy column),
           Tags column with `MultiSelectComboBox` header filter (OR/ANY)
         - `CascadeSelectionHandler` wired to TreeGrid selection events
         - Detail panel: name, description, tag chips, recipe list (if composite), options
         - Status bar footer: "Showing X recipes" (updates on filter change)
- [x] 5.5 Register `WebUiCommand` as subcommand in `App.java`
- [x] 5.6 Update `ServiceFactory` to create `WebUiCommand` with `RecipeDiscoveryService`

## 6. Verification

- [x] 6.1 Run `./gradlew spotlessApply && ./gradlew build` — all tests pass, quality checks clean
- [ ] 6.2 Run `./gradlew :atunko-cli:run --args="webui"` — browser opens at http://localhost:8080,
         TreeGrid displays all recipes, composite recipes are expandable
- [ ] 6.3 Type in search bar — grid filters live by name/description/tags
- [ ] 6.4 Select tags in column header — grid filters with OR/ANY; combines with text search
- [ ] 6.5 Check a composite recipe — all children checked; uncheck one child — parent indeterminate
- [ ] 6.6 Click a recipe row — detail panel updates with description, tags, options
- [ ] 6.7 Run `./gradlew :atunko-cli:run --args="webui --port 9090"` — server starts on port 9090
- [ ] 6.8 Run `./gradlew :atunko-cli:run --args="tui"` — TUI still launches (unchanged)
- [ ] 6.9 Run `./gradlew :atunko-cli:run --args="list"` — CLI still works (unchanged)
