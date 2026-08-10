# atunko

**atunkọ** (Yoruba) — _the act of rewriting/rebuilding_, from *atun* (again) + *kọ* (to write/build)

An open-source CLI + TUI tool for [OpenRewrite](https://docs.openrewrite.org) — recipe discovery,
browsing, configuration, execution, and saveable run configurations — all from the terminal.

> **Note:** "OpenRewrite" is a registered trademark of Moderne, Inc. This project is not affiliated
> with or endorsed by Moderne.

## Requirements

- Java 25+

## Installation

### Run with JBang (zero install)

With [JBang](https://www.jbang.dev) installed, no download or build is needed — JBang fetches
the JAR and provisions a matching JDK:

```bash
# Launch the interactive recipe browser
jbang atunko@atunko-dev/atunko tui

# Search for recipes
jbang atunko@atunko-dev/atunko search "spring"

# Run a recipe against the current project
jbang atunko@atunko-dev/atunko run -r org.openrewrite.java.RemoveUnusedImports
```

The alias resolves the JAR from the rolling [snapshot pre-release](https://github.com/atunko-dev/atunko/releases/tag/snapshot)
(stable releases will take over once published). The shorter `jbang atunko@atunko-dev …` form works
as well, via the [`atunko-dev/jbang-catalog`](https://github.com/atunko-dev/jbang-catalog) repository.

> **Note:** JBang caches downloaded JARs. Since the snapshot URL is rolling, pass `--fresh` to
> pick up the newest build: `jbang --fresh atunko@atunko-dev/atunko tui`

### Download the fat JAR

Download `atunko.jar` from the [releases page](https://github.com/atunko-dev/atunko/releases), then:

```bash
java -jar atunko.jar --help
```

### Build from source

```bash
git clone https://github.com/jimisola/atunko.git
cd atunko
./gradlew :atunko-cli:shadowJar
```

The fat JAR is produced at `atunko-cli/build/libs/atunko-<version>.jar` — for example
`atunko-0.1.0-SNAPSHOT.jar`. (`atunko-cli/build/libs` also holds the thin
`atunko-cli-<version>.jar`, which is *not* runnable on its own.)

The usage examples below all invoke `atunko.jar`, matching the stable asset name published
on the releases page. To get the same name from a local build, copy it:

```bash
cp atunko-cli/build/libs/atunko-[0-9]*.jar atunko.jar
java -jar atunko.jar --help
```

## Usage

### Interactive TUI

```bash
# Launch the interactive recipe browser (default command)
java -jar atunko.jar tui

# Launch with light theme
java -jar atunko.jar tui --theme light

# Launch with a custom CSS theme file
java -jar atunko.jar tui --css-file ~/.config/atunko/mytheme.tcss

# Launch with debug logging to a file
java -jar atunko.jar tui --log-file tui.log
```

Options:

- `--theme <dark|light>` — select a bundled theme (default: `dark`)
- `--css-file <path>` — load a custom TCSS theme file (overrides `--theme` and the XDG default)
- `--log-file <path>` — write TUI debug output to a file (useful for troubleshooting)

**Theme resolution order:** `--css-file` → `~/.config/atunko/theme.tcss` (XDG) → `--theme` → `dark`

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
| `?` | Show help overlay |
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
| `?` | Show help overlay |
| `Esc`/`q` | Back |

**Composite recipes:** Recipes containing sub-recipes are marked with `▶` in the
browser. Expand them to see their sub-recipes, or flatten them in the run dialog
to run sub-recipes individually.

### List recipes

```bash
# List all available recipes
java -jar atunko.jar list

# List with sorting and output format
java -jar atunko.jar list --sort tags --format table
```

Options: `--sort <name|tags>`, `--format <text|table>`.

### Search recipes

```bash
# Search for recipes by keyword
java -jar atunko.jar search "spring boot"
java -jar atunko.jar search "unused imports"

# Search in specific fields
java -jar atunko.jar search "spring" --field name --format table
```

Options: `--field <name|description|tags|all>`, `--sort <name|tags>`, `--format <text|table>`.

### Run a recipe

```bash
# Run a recipe against a project
java -jar atunko.jar run -r org.openrewrite.java.RemoveUnusedImports --project-dir /path/to/project
```

Options:

- `-r`, `--recipe` — Fully qualified recipe name (required)
- `--project-dir` — Path to the project directory (required). Uses the Gradle Tooling
  API to resolve source dirs, resource dirs, test dirs, and compile classpath for each
  module. Supports `build.gradle`, `build.gradle.kts`, and multi-module projects.

### Web UI

A browser-based recipe browser with the same discovery, selection, execution and diff
review as the TUI:

```bash
# Start the web UI on http://localhost:8080 against the current directory
java -jar atunko.jar webui

# Pick a port and a project
java -jar atunko.jar webui --port 9090 --project-dir /path/to/project

# Scan a parent directory and work across every project underneath
java -jar atunko.jar webui --workspace /path/to/workspace
```

Options:

- `--port <port>` — port to listen on (default: `8080`)
- `--project-dir <path>` — project directory (default: current directory)
- `--workspace <path>` — workspace root; scans for all Gradle/Maven projects underneath.
  Takes precedence over `--project-dir` when both are given.

The server runs in the foreground — stop it with `Ctrl+C`.

### Development shortcut

During development you can run commands directly via Gradle:

```bash
./gradlew :atunko-cli:run                           # Launch TUI (default)
./gradlew :atunko-cli:run --args="list"             # List all recipes
./gradlew :atunko-cli:run --args="search 'spring'"  # Search recipes
./gradlew :atunko-cli:run --args="run -r org.openrewrite.java.RemoveUnusedImports --project-dir ."
```

## Architecture

```
atunko/
├── atunko-cli/   # CLI entry point — Picocli commands, App main class, shadow JAR
├── atunko-tui/   # TUI module — TamboUI interactive interface
├── atunko-web/   # Web UI module — Vaadin browser interface
├── atunko-core/  # Core engine — recipe discovery, execution, project scanning
├── docs/         # Antora user docs + requirements traceability (reqstool)
└── openspec/     # Spec-driven development artifacts
```

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for build instructions, code quality setup, and development workflow.

## License

Apache License 2.0. See [LICENSE](LICENSE).
