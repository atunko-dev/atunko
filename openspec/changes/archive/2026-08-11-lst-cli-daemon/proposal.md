## Why

`ParsedSourcesCache` (CORE_0018) removed re-parsing from the hot path, but only *within a
process*. The TUI and Web UI are long-lived and benefit; `atunko run` exits after every
invocation, so the CLI still pays the full scan-and-parse cost — 10–60 s on a 500-file
project — on every single run. That is the workflow issue #11 was filed for: try a recipe,
tweak options, re-run.

On-disk serialization, the original answer, was proven infeasible — OpenRewrite LSTs cannot
be round-tripped through Jackson for any supported `SourceFile` subtype (see
`docs/design/lst-caching-serialization-superseded.md`). Keeping the parsed sources alive in a
long-lived JVM sidesteps serialization entirely, which is what
`docs/design/lst-caching-daemon.md` proposes as Phase 2.

## What Changes

- **New `atunko-daemon` process** — a long-lived JVM, one per project root, holding
  `ProjectInfo` + `ParsedSources` in an existing `ParsedSourcesCache` and executing recipes
  on behalf of short-lived CLI clients.
- **`atunko run` transparently uses the daemon.** It connects to a daemon for the project
  root, auto-starting one if none is running, and falls back to in-process execution if the
  daemon cannot be reached. Recipe output is unchanged either way.
- **New `atunko daemon` subcommands** — `status`, `stop`, `stop --all` for inspection and
  control.
- **New opt-out flag `--no-daemon`** on `run`, plus the `atunko.daemon.disabled` system
  property, for CI and troubleshooting.
- Registry of running daemons under `${XDG_STATE_HOME:-~/.local/state}/atunko/daemons/`,
  one file per daemon holding port, PID, project root and auth token.

The four questions left open in `docs/design/lst-caching-daemon.md` are decided here rather
than deferred:

1. **Lifecycle: auto-start**, Gradle-style, with explicit `daemon stop` and `--no-daemon`
   escapes. An opt-in daemon would leave the default CLI experience exactly as slow as it is
   today, which defeats the purpose of the change.
2. **Concurrency: one daemon per project root, at most 3 retained.** A fourth start evicts
   the least-recently-used idle daemon. Mirrors Gradle's default and bounds worst-case
   memory at roughly 3 × 200 MB.
3. **Idle timeout: 30 minutes**, configurable via `atunko.daemon.idle-timeout`. Gradle's 3 h
   is far too long for a tool invoked in bursts.
4. **Protocol: loopback TCP**, not Unix domain sockets. Windows support matters for CI
   agents, and it is what the Gradle daemon does. Bound to `127.0.0.1` and gated by a
   per-daemon token stored in the registry file with owner-only permissions, so another
   local user cannot drive the daemon.

Additionally, **partial re-parse is deliberately not implemented.** A change to any Java
source re-parses the whole project, because Java types resolve across files and splicing a
single re-parsed file back into the list is unsound — the same reasoning `ParsedSourcesCache`
already documents. The daemon's win is skipping the parse entirely when *nothing* changed,
which is the common case in the target workflow.

**Not breaking:** every existing CLI invocation keeps working, with identical output.

## Capabilities

### New Capabilities

- `lst-cli-daemon`: a long-lived per-project daemon that retains parsed sources across CLI
  invocations, with auto-start, idle expiry, a bounded daemon pool, loopback-token client
  protocol, `atunko daemon` control commands and transparent fallback to in-process
  execution (planned reqstool IDs: CORE_0023 + sub-requirements for the daemon server,
  registry, lifecycle and eviction; CLI_0009 + sub-requirements for client connection,
  auto-start, `--no-daemon` and the `daemon` subcommands).

### Modified Capabilities

<!-- none — `lst-in-process-cache` (CORE_0018) is reused unchanged as the daemon's cache;
     `cli-recipe-execution` keeps its existing requirements since observable run behaviour
     and output are unchanged. -->

## Impact

- **New module** `atunko-daemon` (server + protocol), depending on `atunko-core` only — no
  UI dependencies, consistent with the core-shared-logic principle.
- **`atunko-cli`**: `RunCommand` gains client dispatch and `--no-daemon`; new
  `DaemonCommand` with `status`/`stop` subcommands; `App` registers it.
- **`atunko-core`**: `ParsedSourcesCache` and `SessionHolder` are reused as-is. A small
  extraction may be needed so the daemon can hold session state per connection rather than
  in `SessionHolder`'s statics.
- **Docs**: `docs/antora/modules/ROOT/pages/cli.adoc` gains a daemon section (new commands
  and flags are user-facing); `docs/design/lst-caching-daemon.md` updated to record the four
  decisions.
- **Dependencies**: none added — JDK sockets and the existing Jackson YAML mapper for the
  registry files.
- **Security**: a local listening socket is new attack surface; bound to loopback, token
  gated, registry files created `rw-------`.
