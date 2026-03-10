# feat: Web UI module (Vaadin) — Issue #13

## Context

Add a browser-based UI as an alternative to the TUI for recipe browsing and search. The web UI is **local-only** — started via `atunko webui`, runs an embedded Jetty server, no auth, no deployment. Uses **Vaadin standalone (no Spring Boot)** via `vaadin-boot` to keep the dependency footprint lean and aligned with the rest of the codebase.

**Scope**: Full module scaffold with basic recipe browser — enough to launch `atunko webui` and browse/search recipes in the browser.

## Architecture

```
atunko-cli ──→ atunko-web ──→ atunko-core
           ──→ atunko-tui ──→ atunko-core
```

`atunko-web` is parallel to `atunko-tui` — different UI, same core engine.

## Implementation Steps

### 1. reqstool — Add WEB_* requirements and SVCs

**Modify**: `docs/reqstool/requirements.yml`
- Add `WEB_0001` (Web UI Launch), `WEB_0001.1` (Recipe Grid), `WEB_0001.2` (Search Filter), `WEB_0001.3` (Port Option)
- Add `- path: ../../atunko-web/docs/reqstool` to `implementations.local`

**Modify**: `docs/reqstool/software_verification_cases.yml`
- Add `SVC_WEB_0001`, `SVC_WEB_0001.1`, `SVC_WEB_0001.2`, `SVC_WEB_0001.3`

### 2. OpenSpec — Add spec

**Create**: `openspec/specs/web-ui-launch/spec.md`
- Follow existing `tui-launch/spec.md` pattern — reference WEB_* requirements and SVC_WEB_* scenarios

### 3. Gradle module setup

**Modify**: `settings.gradle` — add `'atunko-web'`

**Modify**: `gradle/libs.versions.toml` — add:
```toml
vaadin = "24.6.3"
vaadin-boot = "13.6"

vaadin-bom = { module = "com.vaadin:vaadin-bom", version.ref = "vaadin" }
vaadin-core = { module = "com.vaadin:vaadin-core" }
vaadin-boot = { module = "com.github.mvysny.vaadin-boot:vaadin-boot", version.ref = "vaadin-boot" }

[plugins]
vaadin = { id = "com.vaadin", version.ref = "vaadin" }
```

**Modify**: `build.gradle` (root) — add Vaadin plugin with `apply false`:
```groovy
id 'com.vaadin' version '24.6.3' apply false
```

**Create**: `atunko-web/build.gradle`
- `java-library` plugin + `com.vaadin` plugin
- `api project(':atunko-core')`, Picocli, Vaadin BOM + core, vaadin-boot

**Modify**: `atunko-cli/build.gradle` — add `implementation project(':atunko-web')`

### 4. reqstool subproject setup

**Create**: `atunko-web/docs/reqstool/`
- `reqstool_config.yml` — same pattern as atunko-tui
- `requirements.yml` — urn: `atunko-web`, filter: `ids == /WEB_.*/`
- `software_verification_cases.yml` — filter: `ids == /SVC_WEB_.*/`

### 5. Tests (TDD)

**Create**: `atunko-web/src/test/java/io/github/atunkodev/web/WebUiCommandTest.java`
- `@SVCs({"SVC_WEB_0001"})` — command instantiation
- `@SVCs({"SVC_WEB_0001.3"})` — default port is 8080

**Create**: `atunko-web/src/test/java/io/github/atunkodev/web/view/RecipeBrowserViewTest.java`
- `@SVCs({"SVC_WEB_0001.1"})` — no query returns all recipes
- `@SVCs({"SVC_WEB_0001.2"})` — search filters by name, description, tags

### 6. Implementation

**Create**: `atunko-web/src/main/java/io/github/atunkodev/web/WebUiCommand.java`
- Picocli `@Command(name = "webui")` with `--port` option (default 8080)
- Constructor takes `RecipeDiscoveryService`
- `run()`: discovers recipes, stores in `RecipeHolder`, starts `VaadinBoot` on localhost

**Create**: `atunko-web/src/main/java/io/github/atunkodev/web/RecipeHolder.java`
- Static volatile holder bridging CLI → Vaadin UI (set once before server start, read by views)

**Create**: `atunko-web/src/main/java/io/github/atunkodev/web/view/RecipeBrowserView.java`
- `@Route("")` — root URL
- `Grid<RecipeInfo>` with columns: displayName, description, tags
- `TextField` search with `ValueChangeMode.EAGER`
- `filterRecipes(List, String)` — public pure function for testability

### 7. CLI integration

**Modify**: `atunko-cli/src/main/java/io/github/atunkodev/App.java`
- Add `WebUiCommand.class` to `@Command(subcommands = {...})`

**Modify**: `atunko-cli/src/main/java/io/github/atunkodev/cli/ServiceFactory.java`
- Add `WebUiCommand` branch: `new WebUiCommand(discoveryService)`

### 8. Sync reqstool filters

Run `/reqstool:sync-filters` to ensure all subproject filters are up to date.

## Key Design Decisions

| Decision | Rationale |
|---|---|
| Vaadin standalone (no Spring Boot) | Local-only tool, no auth needed, keeps deps lean |
| `vaadin-boot` for embedded Jetty | Purpose-built for running Vaadin from `main()`, minimal config |
| `RecipeHolder` static bridge | Single-user local tool — no need for DI framework |
| `filterRecipes()` as pure function | Testable without Vaadin test harness, mirrors TuiController pattern |
| `java-library` (not `application`) | `atunko-cli` owns the shadow JAR; web module is a library |

## Files Summary

### New files
- `atunko-web/build.gradle`
- `atunko-web/docs/reqstool/{reqstool_config,requirements,software_verification_cases}.yml`
- `atunko-web/src/main/java/io/github/atunkodev/web/WebUiCommand.java`
- `atunko-web/src/main/java/io/github/atunkodev/web/RecipeHolder.java`
- `atunko-web/src/main/java/io/github/atunkodev/web/view/RecipeBrowserView.java`
- `atunko-web/src/test/java/io/github/atunkodev/web/WebUiCommandTest.java`
- `atunko-web/src/test/java/io/github/atunkodev/web/view/RecipeBrowserViewTest.java`
- `openspec/specs/web-ui-launch/spec.md`

### Modified files
- `settings.gradle`
- `build.gradle` (root)
- `gradle/libs.versions.toml`
- `atunko-cli/build.gradle`
- `atunko-cli/src/main/java/io/github/atunkodev/App.java`
- `atunko-cli/src/main/java/io/github/atunkodev/cli/ServiceFactory.java`
- `docs/reqstool/requirements.yml`
- `docs/reqstool/software_verification_cases.yml`

## Verification

1. `./gradlew spotlessApply && ./gradlew build` — all modules compile, tests pass
2. `./gradlew :atunko-cli:run --args="webui"` — server starts on localhost:8080
3. Open `http://localhost:8080` — recipe grid visible, search works
4. `./gradlew :atunko-cli:run --args="webui --port 9090"` — server starts on port 9090
5. `/reqstool:status` — WEB_* requirements and SVCs show as covered

## Notes

- Vaadin Gradle plugin downloads Node.js on first build (takes a few minutes, cached after)
- Versions locked: Vaadin `24.7.4`, vaadin-boot `13.3`, karibu-testing-v24 `2.2.4`
- Production mode (`-Pvaadin.productionMode`) bundles frontend — needed for shadow JAR distribution

## Follow-up: TUI Cascade Selection

The Web UI implements cascade-both-ways checkbox selection for the TreeGrid (checking a parent
checks all children; checking all children checks the parent; unchecking one child puts the
parent into indeterminate state). This same selection model should be backported to the TUI
as a separate follow-up change — TUI currently toggles recipes individually with no cascade.
