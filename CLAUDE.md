# atunko — Claude Code Project Instructions

**atunkọ** (Yoruba) — _the act of rewriting/rebuilding_, from *atun* (again) + *kọ* (to write/build)

An open-source TUI + CLI tool for OpenRewrite — recipe browsing, search, configuration,
execution, and saveable run configurations.

## Architecture

```
atunko/
├── atunko-cli/   # CLI entry point — Picocli commands, App main class, shadow JAR
│                 # Package: io.github.atunkodev, io.github.atunkodev.cli
├── atunko-tui/   # TUI module — TamboUI interactive interface
│                 # Package: io.github.atunkodev.tui, io.github.atunkodev.tui.view
├── atunko-core/  # Core engine module — no UI dependencies
│                 # Package: io.github.atunkodev.core.{engine,recipe,project,config,result}
├── docs/
│   └── reqstool/ # Requirements traceability (SSOT)
└── openspec/     # Spec-driven development (links to reqstool, no duplication)
```

**Dependency graph:** `atunko-cli` → `atunko-tui` → `atunko-core`

**Shared implementation principle:** Always implement shared logic in `atunko-core`. Both
`atunko-tui` and `atunko-web` must use core — never duplicate business logic between them.
If TUI and Web UI need the same behaviour, it belongs in core.

## Build Commands

```bash
./gradlew build                    # Full build (includes Spotless + Checkstyle + Error Prone)
./gradlew test                     # All tests
./gradlew :atunko-core:test         # Core module tests only
./gradlew :atunko-cli:test         # CLI module tests only
./gradlew :atunko-tui:test         # TUI module tests only
./gradlew :atunko-cli:run          # Launch TUI (default)
./gradlew :atunko-cli:run --args="list"  # Run CLI command
./gradlew spotlessApply            # Auto-fix formatting (Palantir Java Format)
./gradlew spotlessCheck            # Check formatting (CI mode — fails on violations)
./gradlew checkstyleMain           # Run Checkstyle on main source
./gradlew checkstyleTest           # Run Checkstyle on test source
```

## Code Quality

Three layers of automated quality checks run on every build:

- **Spotless** + **Palantir Java Format** — code formatting (4-space indent, import ordering, Javadoc formatting)
  - `spotlessCheck` in CI (fail on violations), `spotlessApply` locally (auto-fix)
- **Checkstyle** — style & convention enforcement (naming, imports, structure)
  - Google Java Style base, 120-char lines, formatting-overlap rules disabled
  - Config: `gradle/checkstyle/checkstyle.xml`, suppressions: `gradle/checkstyle/suppressions.xml`
- **Error Prone** — compile-time bug detection (runs as javac plugin)

**Before committing:** always run `./gradlew spotlessApply` then `./gradlew build`.

## Code Conventions

- Java 25, Gradle 9.3.1 (Groovy DSL)
- Package root: `io.github.atunkodev` (cli), `io.github.atunkodev.tui` (tui), `io.github.atunkodev.core` (core)
- Use `java-library` plugin in core and tui (exposes API via `api` configuration)
- Use `application` plugin in cli (main class: `io.github.atunkodev.App`)
- Conventional commits for all changes

## Key Dependencies

- **OpenRewrite** (`rewrite-recipe-bom:3.25.0`) — code transformation engine
- **Picocli** (`4.7.7`) — CLI framework
- **TamboUI** (`0.2.0-SNAPSHOT`) — TUI framework (snapshot from Sonatype)
  - Docs: https://tamboui.dev/docs/main
  - Canonical pattern: `column(dock()...).id("x").focusable().onKeyEvent(handler)` — wrap dock in column
  - DockElement extends StyledElement (not ContainerElement) — does NOT support focus/key events directly
  - Single handler per screen, not per widget — inner widgets should NOT be focusable
  - Character input: use `Toolkit.handleTextInputKey(state, event)` manually in the handler
  - GitHub discussions: https://github.com/tamboui/tamboui/discussions
- **Gradle Tooling API** (`9.3.1`) — project scanning (from Gradle's repo)
- **reqstool annotations** (`1.0.0`) — requirements traceability

## Web UI Conventions

- **Buttons**: Use `VaadinIcon` for all action buttons. Use `LUMO_SMALL` for all buttons;
  add `LUMO_PRIMARY` for the main action (e.g. Execute). Never use `LUMO_TERTIARY_INLINE`.
- **Progress**: Use a centered modal `Dialog` with an indeterminate `ProgressBar` for
  long-running operations. Run execution in a background thread with `UI.access()` push.
  Disable action buttons while running.
- **Icons**: Vaadin Icons (`VaadinIcon` enum, ~600 icons) — built-in, no extra dependency.
- **Diff display**: Uses diff2html (`@NpmPackage`) + java-diff-utils for GitHub-style
  side-by-side diff rendering in `DiffDialog`.

## Testing

- JUnit 5 + AssertJ
- Tests annotated with reqstool `@SVCs` to link to verification cases
- Test fixture projects in `atunko-core/src/test/resources/` for integration tests

## reqstool

Requirements and SVCs are tracked via the official reqstool-ai plugin (config: `.reqstool-ai.yaml`).
Use `/reqstool:add-req`, `/reqstool:add-svc`, `/reqstool:status` for traceability work.
