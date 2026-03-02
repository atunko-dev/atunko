# Contributing to atunko

## Build Commands

```bash
./gradlew build                       # Full build (Spotless + Checkstyle + Error Prone + tests)
./gradlew test                        # All tests
./gradlew :atunko-core:test            # Core module tests only
./gradlew :atunko-cli:test            # CLI module tests only
./gradlew :atunko-tui:test            # TUI module tests only
./gradlew :atunko-cli:run             # Launch TUI (default)
./gradlew :atunko-cli:run --args="list"  # Run a CLI command
./gradlew :atunko-cli:shadowJar       # Build the fat JAR
./gradlew spotlessApply               # Auto-fix formatting
./gradlew spotlessCheck               # Check formatting (CI mode)
./gradlew checkstyleMain              # Run Checkstyle on main sources
./gradlew checkstyleTest              # Run Checkstyle on test sources
```

## Code Quality

Three layers of automated checks run on every build:

1. **Spotless** + **Palantir Java Format** — code formatting (4-space indent, import ordering, Javadoc formatting)
   - `spotlessCheck` in CI (fails on violations), `spotlessApply` locally (auto-fix)
2. **Checkstyle** — style and convention enforcement (naming, imports, structure)
   - Google Java Style base, 120-char lines
   - Config: `gradle/checkstyle/checkstyle.xml`
3. **Error Prone** — compile-time bug detection (runs as a javac plugin)

**Before committing:** always run `./gradlew spotlessApply` then `./gradlew build`.

## Development Workflow

- **TDD**: Write tests first, then implement to make them pass
- **reqstool** is the single source of truth for requirements and verification cases
  - Requirements: `docs/reqstool/requirements.yml`
  - SVCs: `docs/reqstool/software_verification_cases.yml`
- Tests use `@SVCs` annotations from reqstool to link test methods to verification cases

## Project Structure

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
└── openspec/     # Spec-driven development (links to reqstool)
```

- Java 25, Gradle 9.x (Groovy DSL)
- Package root: `io.github.atunkodev` (cli), `io.github.atunkodev.tui` (tui), `io.github.atunkodev.core` (core)
- `java-library` plugin in core and tui (exposes API via `api` configuration)
- `application` plugin in cli (main class: `io.github.atunkodev.App`)

## Commit Conventions

All commits follow [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <description>
```

Types: `feat`, `fix`, `build`, `chore`, `ci`, `docs`, `perf`, `refactor`, `revert`, `style`, `test`

## Key Dependencies

- **OpenRewrite** (`rewrite-recipe-bom:3.25.0`) — code transformation engine
- **Picocli** (`4.7.7`) — CLI framework
- **TamboUI** (`0.2.0-SNAPSHOT`) — TUI framework
- **Gradle Tooling API** (`9.3.1`) — project scanning
- **reqstool annotations** (`1.0.0`) — requirements traceability
