# atunko

**atunkọ** (Yoruba) — _the act of rewriting/rebuilding_, from *atun* (again) + *kọ* (to write/build)

An open-source CLI + TUI tool for [OpenRewrite](https://docs.openrewrite.org) — recipe discovery,
browsing, configuration, execution, and saveable run configurations — all from the terminal.

> **Note:** "OpenRewrite" is a registered trademark of Moderne, Inc. This project is not affiliated
> with or endorsed by Moderne.

## Requirements

- Java 25+

## Installation

### Download the fat JAR

Download `atunko.jar` from the [releases page](https://github.com/jimisola/atunko/releases), then:

```bash
java -jar atunko.jar --help
```

### Build from source

```bash
git clone https://github.com/jimisola/atunko.git
cd atunko
./gradlew :app:shadowJar
```

The fat JAR is produced at `app/build/libs/atunko.jar`.

## Usage

### Interactive TUI

```bash
# Launch the interactive recipe browser (default command)
java -jar app/build/libs/atunko.jar tui

# Launch with debug logging to a file
java -jar app/build/libs/atunko.jar tui --log-file tui.log
```

Options: `--log-file <path>` — write TUI debug output to a file (useful for troubleshooting).

Key bindings:

| Key | Action |
|-----|--------|
| `↑`/`↓` | Navigate recipe list |
| `Enter` | Open recipe detail |
| `Space` | Toggle selection |
| `a` | Cycle selection (none → all → none) |
| `r` | Open run dialog with selected recipes |
| `t` | Open tag browser |
| `/` | Search recipes |
| `→`/`e` | Expand composite recipe |
| `←`/`c` | Collapse composite recipe |
| `s` | Toggle sort order |
| `Esc` | Reset filters / go back |
| `q` | Quit |

**Run dialog** (`r` from browser):

| Key | Action |
|-----|--------|
| `↑`/`↓` | Navigate run list |
| `Space`/`Enter` | Toggle individual recipe |
| `a` | Cycle selection (all/none) |
| `+`/`-` | Reorder recipes (move up/down) |
| `→`/`e` | Expand composite recipe |
| `←`/`c` | Collapse composite recipe |
| `f` | Flatten composite (replace with sub-recipes) |
| `r` | Run selected recipes |
| `d` | Dry-run (preview changes) |
| `Esc`/`q` | Back |

**Composite recipes:** Recipes containing sub-recipes are marked with `▶` in the
browser. Expand them to see their sub-recipes, or flatten them in the run dialog
to run sub-recipes individually.

### List recipes

```bash
# List all available recipes
java -jar app/build/libs/atunko.jar list

# List with sorting and output format
java -jar app/build/libs/atunko.jar list --sort tags --format table
```

Options: `--sort <name|tags>`, `--format <text|table>`.

### Search recipes

```bash
# Search for recipes by keyword
java -jar app/build/libs/atunko.jar search "spring boot"
java -jar app/build/libs/atunko.jar search "unused imports"

# Search in specific fields
java -jar app/build/libs/atunko.jar search "spring" --field name --format table
```

Options: `--field <name|description|tags|all>`, `--sort <name|tags>`, `--format <text|table>`.

### Run a recipe

```bash
# Run a recipe against a project
java -jar app/build/libs/atunko.jar run -r org.openrewrite.java.RemoveUnusedImports --project-dir /path/to/project
```

Options:

- `-r`, `--recipe` — Fully qualified recipe name (required)
- `--project-dir` — Path to the project directory (required). Uses the Gradle Tooling
  API to resolve source dirs, resource dirs, test dirs, and compile classpath for each
  module. Supports `build.gradle`, `build.gradle.kts`, and multi-module projects.

### Development shortcut

During development you can run commands directly via Gradle:

```bash
./gradlew :app:run                           # Launch TUI (default)
./gradlew :app:run --args="list"             # List all recipes
./gradlew :app:run --args="search 'spring'"  # Search recipes
./gradlew :app:run --args="run -r org.openrewrite.java.RemoveUnusedImports --project-dir ."
```

## Architecture

```
atunko/
├── app/     # CLI (Picocli) + TUI (TamboUI) entry point
├── core/    # Core engine — recipe discovery, execution, project scanning
└── docs/    # Requirements traceability (reqstool)
```

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for build instructions, code quality setup, and development workflow.

## License

Apache License 2.0. See [LICENSE](LICENSE).
