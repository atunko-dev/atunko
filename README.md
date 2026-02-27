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

# CLI commands
./gradlew :app:run --args="discover"
./gradlew :app:run --args="discover --search 'spring boot'"
./gradlew :app:run --args="run -r org.openrewrite.java.RemoveUnusedImports"
----

== Architecture

[source]
----
atunko/
├── app/     # CLI (Picocli) + TUI (TamboUI) entry point
├── core/    # Core engine — recipe discovery, execution, project scanning
└── docs/    # reqstool requirements (SSOT)
----

== License

Apache License 2.0. See link:LICENSE[LICENSE].
