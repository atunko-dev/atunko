= atunko
:toc:

**atunkọ** (Yoruba) — _the act of rewriting/rebuilding_, from *atun* (again) + *kọ* (to write/build)

An open-source TUI + CLI tool providing a rich, interactive developer experience for
https://docs.openrewrite.org[OpenRewrite] — recipe discovery, browsing, configuration,
execution, and saveable run configurations — all from the terminal.

NOTE: "OpenRewrite" is a registered trademark of Moderne, Inc. This project is not affiliated
with or endorsed by Moderne.

== Quick Start

[source,bash]
----
# Requires Java 25+
./gradlew build

# Launch TUI
./gradlew :app:run

# Discover recipes
./gradlew :app:run --args="discover"

# Run a recipe
./gradlew :app:run --args="run -r org.openrewrite.java.RemoveUnusedImports --project-dir ."
----

== CLI Commands

=== `discover` — List and search recipes

[source,bash]
----
# List all available recipes
atunko discover

# Search for recipes by keyword
atunko discover --search "spring boot"
atunko discover --search "unused imports"
----

=== `run` — Execute a recipe

[source,bash]
----
# Run a recipe against a project
atunko run -r org.openrewrite.java.RemoveUnusedImports --project-dir .

# Run a Spring Boot migration recipe
atunko run -r org.openrewrite.java.spring.boot3.UpgradeSpringBoot_3_0 --project-dir /path/to/project
----

Options:

* `-r`, `--recipe` — Fully qualified recipe name (required)
* `--project-dir` — Path to the project directory (required)

== Architecture

[source]
----
atunko/
├── app/     # CLI (Picocli) + TUI (TamboUI) entry point
├── core/    # Core engine — recipe discovery, execution, project scanning
└── docs/    # reqstool requirements (SSOT), Antora documentation
----

== License

Apache License 2.0. See link:LICENSE[LICENSE].
