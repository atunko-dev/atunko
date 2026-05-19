## Context

`App` is the Picocli root command in `atunko-cli`. When invoked with no arguments, Picocli
calls `App.run()`. Previously this printed usage. The TUI (`atunko tui`) is the primary
interface; printing usage as the default is a poor UX.

Picocli 4.7.7 (the version on the classpath) does not expose `setDefaultCommandName()` —
that method does not exist. The only available delegation path from `App.run()` is via
`spec.commandLine().getSubcommands().get("tui").execute()`.

## Goals / Non-Goals

**Goals:**
- `atunko` (no args) launches the TUI, identical to `atunko tui`
- `atunko --help` still prints root-level usage (unchanged — Picocli handles `--help` before `run()`)

**Non-Goals:**
- Propagating the TUI's exit code from `App.run()` (Picocli `Runnable.run()` is void; TUI exits with 0 on normal close)
- Mouse support, sorting, or any other TUI feature

## Decisions

**Delegate via `getSubcommands().get("tui").execute()` instead of `setDefaultCommandName()`**
Picocli 4.7.7 does not have `setDefaultCommandName`. Calling the subcommand's `execute()`
from `App.run()` is the idiomatic fallback: Picocli has already wired the factory and all
dependencies, so the subcommand executes with full context.

**Test verifies registration, not execution**
Launching TUI in a headless test environment is not possible (requires a real TTY). The
test instead asserts that "tui" is registered in `cmd.getSubcommands()`, which is the
necessary precondition for the delegation to succeed at runtime.

## Risks / Trade-offs

- **Exit code not propagated**: If TUI exits non-zero, `App.run()` still returns normally
  and Picocli reports exit code 0. Acceptable — TUI closes cleanly on `q`/`Esc` with code 0.
- **Headless test gap**: The delegation path itself is not exercised under test. The
  registration check is a proxy. → Mitigation: manual smoke test (`./gradlew :atunko-cli:run`).
