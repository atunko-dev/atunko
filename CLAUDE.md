# atunko — Claude Code Project Instructions

**atunkọ** (Yoruba) — _the act of rewriting/rebuilding_, from *atun* (again) + *kọ* (to write/build)

An open-source TUI + CLI tool for OpenRewrite — recipe browsing, search, configuration,
execution, and saveable run configurations.

## Architecture

```
atunko/
├── app/          # Application module — Picocli CLI + TamboUI TUI entry point
│                 # Package: io.github.atunkodev.cli, io.github.atunkodev.tui
├── core/         # Core engine module — no UI dependencies
│                 # Package: io.github.atunkodev.core.{engine,recipe,project,config,result}
├── docs/
│   └── reqstool/ # Requirements traceability (SSOT)
└── openspec/     # Spec-driven development (links to reqstool, no duplication)
```

## Build Commands

```bash
./gradlew build                    # Full build (includes Spotless + Checkstyle + Error Prone)
./gradlew test                     # All tests
./gradlew :core:test               # Core module tests only
./gradlew :app:test                # App module tests only
./gradlew :app:run                 # Launch TUI (default)
./gradlew :app:run --args="discover"  # Run CLI command
./gradlew spotlessApply            # Auto-fix formatting (Palantir Java Format)
./gradlew spotlessCheck            # Check formatting (CI mode — fails on violations)
./gradlew checkstyleMain           # Run Checkstyle on main source
./gradlew checkstyleTest           # Run Checkstyle on test source
```

## Development Approach

- **TDD**: Write tests first, then implement to make them pass
- **reqstool** is the SSOT for requirements and verification cases
  - Requirements: `docs/reqstool/requirements.yml`
  - SVCs: `docs/reqstool/software_verification_cases.yml`
- **OpenSpec** links to reqstool — no duplicated requirements or scenarios
  - Spec conventions: `.claude/reqstool-openspec-conventions.md`
- Tests use `@SVCs` annotations from reqstool to link test methods to verification cases

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
- Package root: `io.github.atunkodev` (app), `io.github.atunkodev.core` (core)
- Use `java-library` plugin in core (exposes API via `api` configuration)
- Use `application` plugin in app (main class: `io.github.atunkodev.App`)
- Conventional commits for all changes

## Key Dependencies

- **OpenRewrite** (`rewrite-recipe-bom:3.25.0`) — code transformation engine
- **Picocli** (`4.7.7`) — CLI framework
- **TamboUI** (`0.2.0-SNAPSHOT`) — TUI framework (snapshot from Sonatype)
- **Gradle Tooling API** (`9.3.1`) — project scanning (from Gradle's repo)
- **reqstool annotations** (`1.0.0`) — requirements traceability

## Testing

- JUnit 5 + AssertJ
- Tests annotated with reqstool `@SVCs` to link to verification cases
- Test fixture projects in `core/src/test/resources/` for integration tests

## reqstool

When working with reqstool, **always read `.claude/reqstool-conventions.md` first**.
