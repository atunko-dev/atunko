# Atunko - OpenRewrite TUI/CLI Tool - PoC Plan & Roadmap

> **atunkọ** (Yoruba) = "the act of rewriting/rebuilding" — from *atun* (again) + *kọ* (to write/build)

## Context

OpenRewrite is a powerful open-source code transformation engine, but its best UX is locked behind
Moderne's commercial paywall (the `mod` CLI, DevCenter dashboards, multi-repo orchestration).
"OpenRewrite" is a registered trademark of Moderne, Inc. The existing open-source experience is
limited to Maven/Gradle plugin tasks (`rewriteRun`, `rewriteDryRun`, `rewriteDiscover`) which
provide no interactivity, no recipe browsing, and minimal output formatting.

**Pain points this tool addresses:**
1. Recipe discovery is awful — `rewriteDiscover` dumps 2000+ recipes as a wall of text
2. Zero interactivity — must edit build.gradle, run task, undo edit
3. No "try before you buy" — no quick way to test a recipe on your project
4. Recipe composition is manual — no tooling for building composite recipes
5. No way to save and share recipe configurations across teams/projects

**Goal:** Build `atunko`, an open-source TUI + CLI tool providing a rich, interactive developer
experience for OpenRewrite — recipe discovery, browsing, configuration, execution, and saveable
run configurations — all from the terminal.

**Engineering practices:** Use reqstool (SSOT for requirements + SVCs with scenarios) and OpenSpec
(spec-driven development linking to reqstool) for the atunko project itself.

---

## Architecture Overview

```
atunko/
├── app/                            # Application module (TUI + CLI entry point)
│   └── src/main/java/
│       └── dev/atunko/
│           ├── cli/                # Picocli commands
│           ├── tui/                # TamboUI views/screens
│           └── App.java            # Main entry point
├── core/                           # Core engine module (no UI dependencies)
│   └── src/main/java/
│       └── dev/atunko/core/
│           ├── engine/             # OpenRewrite integration
│           ├── recipe/             # Recipe discovery, search, metadata
│           ├── project/            # Project scanning, classpath resolution
│           ├── config/             # Saveable run configurations
│           └── result/             # Result processing
├── docs/
│   └── reqstool/                   # Requirements traceability (top-level system)
│       ├── requirements.yml        # System-level reqs (CLI_xxxx, CORE_xxxx, TUI_xxxx)
│       ├── software_verification_cases.yml  # SVCs with GIVEN/WHEN/THEN scenarios
│       └── reqstool_config.yml
├── openspec/                       # OpenSpec spec-driven development
│   ├── project.md                  # Project conventions, links to reqstool as SSOT
│   ├── AGENTS.md                   # AI assistant instructions
│   └── specs/                      # Capabilities — link to reqstool, add design context
├── build.gradle                # Root build
└── settings.gradle
```

### Key Design Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Name | **atunko** | Yoruba for "rewriting". Available on GitHub org, Maven Central, .dev/.io domains |
| License | Apache 2.0 | Matches OpenRewrite, reqstool, Picocli. Patent grant. Java ecosystem standard |
| Build tool | Gradle (Groovy DSL) | Java 25, familiar, good OpenRewrite support |
| CLI framework | Picocli | Industry standard, TamboUI integration, GraalVM ready |
| TUI framework | TamboUI | Modern Java TUI, CSS styling, Picocli integration, MIT license |
| Core UI | TUI-first | Most differentiated, developer-native, CLI comes free via Picocli |
| Web UI | Vaadin (future Phase 3) | Separate module, added later |
| Spec workflow | OpenSpec | Spec-driven dev with AI assistants, links to reqstool (no duplication) |
| Requirements | reqstool (SSOT) | Requirements + SVCs with GIVEN/WHEN/THEN scenarios |
| Gradle integration | Gradle Tooling API | Pure Java API, no subprocess. Gets classpath + source dirs + modules |
| Maven integration | Maven Embedder (Phase 2) | Pure Java API. Classpath + source dirs + modules |
| Diffing | Delegate to git | No built-in diff viewer. Use `git diff` / `git stash`. Focus on running recipes |
| Docs | AsciiDoc / Antora | README.adoc with Yoruba etymology |

---

## Build Tool Integration Matrix

| Capability | Gradle: Tooling API | Maven: Embedder (Phase 2) |
|------------|-------------------|--------------------------|
| **Compile classpath** | `EclipseProject.getClasspath()` → JAR paths | `MavenProject.getCompileClasspathElements()` or `dependency:build-classpath` in-process |
| **Source directories** | `IdeaContentRoot.getSourceDirectories()` | `MavenProject.getCompileSourceRoots()` |
| **Test source dirs** | `IdeaContentRoot.getTestDirectories()` | `MavenProject.getTestCompileSourceRoots()` |
| **Resource dirs** | `IdeaContentRoot.getResourceDirectories()` | `MavenProject.getBuild().getResources()` |
| **Modules** | `EclipseProject.getChildren()` | `MavenProject.getModules()` |
| **Java version** | `EclipseProject.getJavaSourceSettings()` | POM properties `maven.compiler.source` |
| **Requires build tool installed?** | NO — auto-downloads Gradle distribution | NO — embedded in our JVM |
| **Pure Java API?** | YES | YES |
| **Subprocess needed?** | NO | NO |
| **Dependency** | `org.gradle:gradle-tooling-api:8.12` | `org.apache.maven:maven-embedder:3.9.9` + transport/connector |
| **Phase** | PoC (Phase 1) | Phase 2 |

### Gradle Tooling API approach (PoC):
```java
ProjectConnection conn = GradleConnector.newConnector()
    .forProjectDirectory(projectDir)
    .useBuildDistribution()  // respects project's gradle wrapper
    .connect();

// Classpath
EclipseProject eclipse = conn.getModel(EclipseProject.class);
List<Path> classpath = eclipse.getClasspath().stream()
    .map(dep -> dep.getFile().toPath()).toList();

// Source + resource dirs
IdeaProject idea = conn.getModel(IdeaProject.class);
for (IdeaModule module : idea.getModules()) {
    for (IdeaContentRoot root : module.getContentRoots()) {
        root.getSourceDirectories();      // main sources
        root.getTestDirectories();        // test sources
        root.getResourceDirectories();    // main resources
        root.getTestResourceDirectories(); // test resources
    }
}

// Java version
eclipse.getJavaSourceSettings().getSourceLanguageLevel();

// Multi-module
eclipse.getChildren(); // recursive sub-projects
```

---

## Phase 1: Foundation (PoC - MVP)

### 1.1 Project Setup

- [ ] Create GitHub repo (under your account or `atunko` org)
- [ ] Initialize Gradle multi-module project (`settings.gradle`, `build.gradle`)
- [ ] Configure Java 25 toolchain
- [ ] Add Apache 2.0 LICENSE file
- [ ] Add core dependencies:
  - `org.openrewrite.recipe:rewrite-recipe-bom` (version alignment)
  - `org.openrewrite:rewrite-core`, `rewrite-java`, `rewrite-java-21`
  - `org.openrewrite:rewrite-xml`, `rewrite-yaml`, `rewrite-json`, `rewrite-properties`
  - `org.openrewrite:rewrite-maven`, `rewrite-gradle`
  - `org.openrewrite.recipe:rewrite-migrate-java`, `rewrite-spring`,
    `rewrite-static-analysis`, `rewrite-testing-frameworks`
  - `info.picocli:picocli` + `picocli-codegen`
  - `org.gradle:gradle-tooling-api` (for project scanning)
  - TamboUI: `tamboui-core`, `tamboui-widgets`, `tamboui-toolkit`, `tamboui-tui`,
    `tamboui-css`, `tamboui-picocli`
  - `io.github.reqstool:reqstool-java-annotations` (compileOnly + annotationProcessor)
  - `io.github.reqstool:reqstool-java-gradle-plugin`

### 1.2 Claude Code Setup

- [ ] Create `CLAUDE.md` at project root with:
  - Project description, architecture, module structure
  - Build commands (`./gradlew build`, `./gradlew test`, etc.)
  - Code conventions (formatting, naming)
  - Reference to reqstool requirements as SSOT
  - Reference to OpenSpec for proposal workflow
  - Link to `docs/reqstool/requirements.yml` for requirement context
- [ ] Review and incorporate relevant practices from
  https://github.com/shanraisshan/claude-code-best-practice
- [ ] Set up any useful Claude Code hooks (pre-commit, test runners)

### 1.3 reqstool Setup (SSOT)

**reqstool is the Single Source of Truth** for both requirements AND behavioral scenarios.

`docs/reqstool/requirements.yml` — system-level:
```yaml
metadata:
  urn: atunko
  variant: system
  title: Atunko System Requirements
  url: https://github.com/atunko/atunko

requirements:
  # CLI requirements
  - id: CLI_0001
    title: TUI Launch
    significance: shall
    description: The tool shall launch an interactive TUI when invoked without subcommands
    categories: [interaction-capability]
    revision: 0.1.0

  - id: CLI_0002
    title: CLI Recipe Discovery
    significance: shall
    description: The tool shall list and search available recipes via CLI subcommand
    categories: [functional-suitability]
    revision: 0.1.0

  - id: CLI_0003
    title: CLI Recipe Execution
    significance: shall
    description: The tool shall execute recipes via CLI subcommand
    categories: [functional-suitability]
    revision: 0.1.0

  # Core requirements
  - id: CORE_0001
    title: Recipe Discovery
    significance: shall
    description: The core engine shall discover all OpenRewrite recipes from the classpath
    categories: [functional-suitability]
    revision: 0.1.0

  - id: CORE_0002
    title: Recipe Search
    significance: shall
    description: The core engine shall support searching recipes by name, description, and tags
    categories: [functional-suitability]
    revision: 0.1.0

  - id: CORE_0003
    title: Recipe Execution
    significance: shall
    description: The core engine shall execute selected OpenRewrite recipes against a target project
    categories: [functional-suitability]
    revision: 0.1.0

  - id: CORE_0004
    title: Gradle Project Support
    significance: shall
    description: The core engine shall resolve classpaths and source dirs for Gradle projects via Tooling API
    categories: [functional-suitability]
    revision: 0.1.0

  - id: CORE_0005
    title: Maven Project Support
    significance: shall
    description: The core engine shall resolve classpaths and source dirs for Maven projects
    categories: [functional-suitability]
    revision: 0.1.0

  - id: CORE_0006
    title: Git Integration
    significance: should
    description: The core engine should detect git repos and support stash-based versioning
    categories: [functional-suitability, safety]
    revision: 0.1.0

  - id: CORE_0007
    title: Save Run Configuration
    significance: shall
    description: The tool shall save current recipe setup to a portable YAML file (.atunko.yml)
    categories: [functional-suitability]
    revision: 0.1.0

  - id: CORE_0008
    title: Load Run Configuration
    significance: shall
    description: The tool shall load and execute from a saved configuration file
    categories: [functional-suitability]
    revision: 0.1.0

  - id: CORE_0009
    title: Export Configuration
    significance: should
    description: The tool should export run configs to Maven/Gradle plugin format
    categories: [compatibility]
    revision: 0.1.0
```

`docs/reqstool/software_verification_cases.yml` — **SVCs with GIVEN/WHEN/THEN scenarios**:
```yaml
cases:
  - id: SVC_CORE_0001
    requirement_ids: ["CORE_0001"]
    title: Discover all recipes from classpath
    verification: automated-test
    description: |
      GIVEN the tool has recipe modules on its classpath
      WHEN the RecipeDiscoveryService scans the classpath
      THEN all recipes from all modules are returned with name and description
    revision: "0.1.0"

  - id: SVC_CORE_0002
    requirement_ids: ["CORE_0002"]
    title: Search recipes by keyword
    verification: automated-test
    description: |
      GIVEN the recipe catalog contains recipes
      WHEN the user searches for "spring boot"
      THEN only recipes matching "spring boot" in name or description are returned
    revision: "0.1.0"

  - id: SVC_CORE_0003
    requirement_ids: ["CORE_0003"]
    title: Execute recipe against project
    verification: automated-test
    description: |
      GIVEN a Gradle project with Java sources
      WHEN the user runs RemoveUnusedImports recipe
      THEN unused imports are removed from all Java files
    revision: "0.1.0"

  - id: SVC_CORE_0004
    requirement_ids: ["CORE_0004"]
    title: Resolve Gradle classpath via Tooling API
    verification: automated-test
    description: |
      GIVEN a Gradle project with dependencies
      WHEN the GradleToolingApiResolver connects to the project
      THEN compile classpath JAR paths and source directories are returned
    revision: "0.1.0"

  - id: SVC_CORE_0007
    requirement_ids: ["CORE_0007"]
    title: Save run configuration
    verification: automated-test
    description: |
      GIVEN the user has selected recipes and options
      WHEN they invoke config save
      THEN a .atunko.yml file is written with the recipe setup
    revision: "0.1.0"

  - id: SVC_CORE_0008
    requirement_ids: ["CORE_0008"]
    title: Load and execute from saved config
    verification: automated-test
    description: |
      GIVEN a valid .atunko.yml file exists
      WHEN the user runs with --config .atunko.yml
      THEN the saved recipes are executed with the saved options
    revision: "0.1.0"

  - id: SVC_CLI_0001
    requirement_ids: ["CLI_0001"]
    title: Launch TUI by default
    verification: manual-test
    description: |
      GIVEN the user runs `atunko` with no subcommand
      WHEN the application starts
      THEN the TamboUI main dashboard is displayed
    instructions: "Run `atunko` and verify the TUI dashboard appears"
    revision: "0.1.0"
```

Each Gradle module (`app`, `core`) acts as a **microservice** variant importing from system-level.

### 1.4 OpenSpec Setup

OpenSpec links to reqstool — **no duplicated requirements or scenarios**.

```
openspec/
├── project.md                    # Conventions, states reqstool is SSOT
├── AGENTS.md                     # AI instructions, references reqstool + CLAUDE.md
└── specs/
    ├── recipe-discovery/
    │   └── spec.md               # Links CORE_0001/0002/SVC_CORE_0001/0002 + design context
    ├── recipe-execution/
    │   └── spec.md               # Links CORE_0003/0004 + design context
    ├── run-configuration/
    │   └── spec.md               # Links CORE_0007-0009 + .atunko.yml format design
    └── cli-interface/
        └── spec.md               # Links CLI_0001-0003 + command design
```

Example spec (design context only, no duplicated requirements/scenarios):
```markdown
# Recipe Discovery

## Requirements & Verification
- Requirements: CORE_0001, CORE_0002 → see `docs/reqstool/requirements.yml`
- Scenarios: SVC_CORE_0001, SVC_CORE_0002 → see `docs/reqstool/software_verification_cases.yml`

## Design
- Uses `Environment.builder().scanRuntimeClasspath()` from OpenRewrite
- Recipes indexed in-memory by name, description, tags for fast search
- Recipe options extracted via reflection on `@Option` annotations
- Grouped by source module (rewrite-spring, rewrite-testing-frameworks, etc.)

## Technical Notes
- Recipe catalog is built once at startup and cached
- Search uses simple substring matching for PoC (consider fuzzy matching later)
```

**OpenSpec CLI workflow** (standard proposal → apply → archive):
```bash
openspec init                              # Already done in setup
openspec list --specs                      # List current capabilities
openspec validate <change-id> --strict     # Validate a change proposal
openspec archive <change-id> --yes         # Archive completed change
```

For new features, AI assistants use the `/openspec:proposal` flow:
1. Create `changes/<change-id>/proposal.md` — WHY
2. Create spec deltas referencing NEW reqstool IDs — WHAT
3. Create `tasks.md` — STEPS
4. Validate, get approval, implement

### 1.5 Core Engine (`core` module)

**RecipeDiscoveryService** — Recipe browsing and search:
```
core/src/main/java/dev/atunko/core/recipe/
  RecipeDiscoveryService.java    # Scans classpath via Environment, discovers all recipes
  RecipeCatalog.java             # In-memory indexed catalog with search
  RecipeMetadata.java            # Name, description, tags, options, source module
```

**ProjectScanner** — Project analysis via Gradle Tooling API:
```
core/src/main/java/dev/atunko/core/project/
  ProjectScanner.java            # Interface for project analysis
  ProjectInfo.java               # Classpath, source dirs, resource dirs, modules, Java version
  GradleToolingApiScanner.java   # Gradle Tooling API implementation (pure Java, no subprocess)
```

**RecipeExecutionEngine** — Run recipes:
```
core/src/main/java/dev/atunko/core/engine/
  RecipeExecutionEngine.java     # Parses sources, runs recipes, writes results
  ExecutionProgress.java         # Progress callback interface
  ExecutionResult.java           # Structured results (files changed, counts)
```

**RunConfiguration** — Saveable recipe setups:
```
core/src/main/java/dev/atunko/core/config/
  RunConfiguration.java          # Model: recipe(s), options, paths, exclusions
  RunConfigurationStore.java     # Load/save from .atunko.yml files
```

### 1.6 CLI Commands (Picocli)

```bash
atunko                                     # Launches TUI (default)
atunko discover                            # List all available recipes
atunko discover --search "spring boot 3"   # Search recipes
atunko info <recipe-name>                  # Show recipe details and options
atunko run -r <recipe>                     # Run a recipe (applies changes)
atunko run -r <recipe> --dry-run           # Preview changes only (no writes)
atunko run --config .atunko.yml            # Run from saved configuration
atunko config save -r <recipe> [opts]      # Save current setup to .atunko.yml
atunko config show                         # Show current config
atunko config export --gradle              # Export to Gradle plugin format
atunko config export --maven               # Export to Maven plugin format
```

### 1.7 Git Integration

- **Before execution:** optionally `git stash push -m "atunko: pre-recipe"`
- **After execution:** user reviews with `git diff`, `git stash pop` to merge back
- **Undo:** `git checkout -- .` to discard all recipe changes

### 1.8 TUI Screens (TamboUI)

```
┌──[ atunko v0.1.0 ]───────────────────────────────────┐
│  Project: /path/to/project (Gradle, Java 17)          │
│  Git: clean (main branch)  │  Recipes: 1,847          │
│                                                        │
│  [D] Discover    [R] Run    [C] Configs    [Q] Quit   │
└────────────────────────────────────────────────────────┘
```

Recipe Browser → Recipe Execution → "Run `git diff` to review" → Save as config

---

## Phase 2: Enhanced Features

### 2.1 Maven Project Support
- Add `MavenEmbedderScanner` implementation (pure Java, no subprocess)
- `MavenProject.getCompileSourceRoots()`, `getCompileClasspathElements()`
- Auto-detect Maven vs Gradle

### 2.2 Recipe Management
- Custom recipe YAML authoring
- Recipe favorites / recently used
- Recipe option configuration with validation in TUI

### 2.3 LST Caching
- Serialize parsed LSTs to disk for faster re-runs

### 2.4 Multi-Project Support
- Run recipes across multiple projects

---

## Phase 3: Web UI (Future)

- Add `web` module with Vaadin + Spring Boot
- Reuse `core` module services
- Launch via `atunko ui`

---

## Technology Stack

| Component | Technology | Version | License |
|-----------|-----------|---------|---------|
| Language | Java | 25 | — |
| Build | Gradle (Groovy DSL) | 9.x | Apache 2.0 |
| CLI | Picocli | 4.7.x | Apache 2.0 |
| TUI | TamboUI | 0.2.x | MIT |
| Core engine | OpenRewrite | latest | Apache 2.0 |
| Gradle integration | Gradle Tooling API | 8.12 | Apache 2.0 |
| Spec workflow | OpenSpec | latest | Open source |
| Requirements | reqstool | latest | Apache 2.0 |
| Documentation | AsciiDoc / Antora | latest | — |
| Web UI (future) | Vaadin | 24.x | Apache 2.0 |

---

## PoC Implementation Order

1. **Project setup** — Gradle multi-module, Java 25, dependencies, LICENSE
2. **Claude Code setup** — `CLAUDE.md`, hooks, best practices
3. **reqstool requirements** (SSOT) — `requirements.yml` + `software_verification_cases.yml` with GIVEN/WHEN/THEN
4. **OpenSpec specs** — Link to reqstool IDs/SVCs, add design context only
5. **Core engine** — Recipe discovery, Gradle Tooling API scanner, recipe execution
6. **Run configuration** — Save/load/export portable `.atunko.yml` configs
7. **Git integration** — Detect git repos, optional stash before recipe runs
8. **Picocli CLI** — `discover`, `run`, `info`, `config` commands
9. **TamboUI TUI** — Recipe browser, execution screen, configuration manager

---

## Verification Plan

### Manual Testing
1. `atunko discover` → verify recipes listed
2. `atunko discover --search "junit"` → verify filtered results
3. `atunko info <recipe>` → verify details
4. `atunko run -r <recipe> -p <test-project>` → verify changes applied
5. `atunko config save` / `atunko run --config` → verify round-trip
6. Launch TUI (`atunko`) → browse, select, run, save config

### Automated Tests
- Unit tests for `RecipeDiscoveryService`, `RecipeCatalog`
- Unit tests for `GradleToolingApiScanner` against test fixture project
- Unit tests for `RunConfiguration` save/load/export
- Integration tests running actual recipes against test fixtures
- reqstool status check: `reqstool status local -p docs/reqstool`

### reqstool Gate
- All requirements have `@Requirements` annotations in code
- All SVCs have `@SVCs` annotations in tests
- `reqstool status` exits with code 0
