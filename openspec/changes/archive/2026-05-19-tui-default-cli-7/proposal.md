## Why

Running `atunko` without a subcommand printed the help/usage text — not useful as a default
UX since the TUI is the primary interface. Issue #7 requires the bare `atunko` invocation
to launch the TUI directly, the same as `atunko tui`.

## What Changes

- `App.run()` delegates to the registered "tui" subcommand via
  `spec.commandLine().getSubcommands().get("tui").execute()` instead of printing usage
- `@Requirements({"atunko:CLI_0001"})` added to `App.run()`
- `AppTest.noArgsPrintsUsage` replaced by `tuiSubcommandIsRegistered` — verifies "tui" is
  the registered subcommand the default path delegates to (TUI cannot be launched headlessly)

## Capabilities

### New Capabilities

- `cli-default-tui`: Invoking `atunko` without arguments launches the interactive TUI

## Impact

- `atunko-cli`: `App` — one-line change to `run()`; new `CLI_0001` requirement added
- `atunko-cli` tests: `AppTest` — one test replaced
- `docs/reqstool/requirements.yml`: new `CLI_0001`
- `docs/reqstool/software_verification_cases.yml`: new `SVC_CLI_0001`
- No core, TUI, or web changes
