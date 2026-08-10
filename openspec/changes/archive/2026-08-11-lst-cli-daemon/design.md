## Context

`ParsedSourcesCache` (CORE_0018) caches `ParsedSources` per project directory, keyed on a
fingerprint of the parser's inputs, and holds them through a `SoftReference`. It is created
per process, so only long-lived processes benefit: `TuiController` and `WebUiCommand` reuse
it across executions, while `atunko run` builds one, uses it once, and exits.

Serialization was ruled out before this design: `docs/design/lst-caching-serialization-superseded.md`
records that `J.CompilationUnit`, `Yaml.Documents` and `Json.Document` blow the stack through
`NameTree.names()` recursion under Jackson, and `Xml.Document` / `Properties.File` have no
usable creator. Keeping LSTs in a live JVM is therefore the only open-source path to
cross-invocation reuse.

Constraints: `atunko-core` must stay UI-free; the daemon must not change observable `run`
output; a background process that outlives the user's command is a support burden, so its
failure modes must degrade to today's behaviour rather than to an error.

## Goals / Non-Goals

**Goals:**

- `atunko run` against an unchanged project skips scan and parse entirely on the second and
  subsequent invocations.
- The speedup is on by default — no flag, no setup step.
- Any daemon failure (not running, unreachable, version mismatch, crash mid-request) falls
  back to in-process execution with identical output.
- Bounded, predictable resource use: bounded daemon count, bounded idle lifetime, explicit
  `stop`.
- No new third-party dependency.

**Non-Goals:**

- Partial / per-file re-parse. Any Java change re-parses the project (see Decisions).
- Sharing a daemon between the CLI and the TUI/Web UI. Those already cache in-process.
- Cross-machine or multi-user daemons. Loopback and single-owner only.
- Persisting anything to disk. The registry holds coordination metadata, never LSTs.
- Workspace (`--workspace`) sessions. Single project root only in this change.

## Decisions

### 1. Lifecycle: auto-start, with escapes

`atunko run` starts a daemon when none is registered for the project root, then proceeds
through it. Gradle's model.

*Alternative — explicit `atunko daemon start`:* predictable and never surprises the user with
a background JVM, but it means the default `atunko run` stays exactly as slow as it is today,
and the feature only helps people who read the docs. The change exists to fix the default
path, so auto-start wins.

*Mitigating the surprise:* the first auto-start prints one line to stderr naming the daemon
and how to stop it; `atunko daemon status` lists everything running; `--no-daemon` and
`atunko.daemon.disabled` opt out entirely.

### 2. One daemon per project root, at most 3

Keyed by the real (symlink-resolved) project root. Each daemon holds roughly the parsed size
of one project, 50–200 MB. Retaining 3 bounds this at a few hundred MB — the same default
Gradle settled on. Starting a fourth evicts the least-recently-used *idle* daemon; a daemon
currently serving a request is never evicted.

*Alternative — one daemon serving all projects:* a single JVM's heap becomes the sum of every
project ever touched, and one bad project's OOM takes down everyone's cache. Per-root
isolation is worth the extra processes.

### 3. Idle timeout: 30 minutes

Each daemon exits after 30 minutes without a request, configurable through
`atunko.daemon.idle-timeout` (ISO-8601 duration or seconds). Gradle's 3 hours suits a tool
invoked continuously all day; atunko is invoked in bursts, and holding 200 MB for hours after
a single run is a bad trade.

### 4. Protocol: loopback TCP with a token

The daemon binds `127.0.0.1:0` (ephemeral port) and writes port, PID, project root, atunko
version and a 256-bit random token to its registry file at
`${XDG_STATE_HOME:-~/.local/state}/atunko/daemons/<hash-of-root>.yaml`, created with
owner-only permissions. Every request carries the token; a mismatch is refused and the
connection closed.

*Alternative — Unix domain sockets:* faster and filesystem-permission-gated for free, but
excludes Windows, which matters for CI agents and is the reason the Gradle daemon uses local
TCP too. Loopback TCP alone is not an authorization boundary on a multi-user host — any local
user can connect to `127.0.0.1` — hence the token.

Messages are newline-delimited JSON over the socket via the existing Jackson mapper: a
`hello` handshake carrying token and client version, then `execute` / `status` / `stop`.
Version mismatch between client and daemon makes the client stop that daemon and start a
fresh one, so an upgraded atunko never talks to a stale JVM holding LSTs parsed by the old
OpenRewrite.

### 5. Whole-project re-parse only

On each request the daemon re-fingerprints through `ParsedSourcesCache`, which already
re-parses everything when anything changed. Splicing one re-parsed Java file into a cached
list is only sound when nothing else resolves against it — false for any signature or type
change — and detecting that reliably means the type analysis the parse was going to do
anyway. Non-Java sources have no cross-file resolution and could be spliced safely, but they
are also the cheap ones to parse, so the complexity buys little.

The daemon's value is the *unchanged* case, which is what a tweak-options-and-re-run loop
actually produces.

### 6. Reuse `ParsedSourcesCache`, not a new cache

The daemon owns one `ParsedSourcesCache` and calls it per request. Fingerprint-on-request
also removes the need for a `WatchService`: no watch registration lifecycle, no platform
differences, no missed events on network filesystems, and correctness does not depend on
having observed an event. The walk is orders of magnitude cheaper than the parse it guards.

### 7. Session state moves off statics for the daemon

`SessionHolder` holds session state in statics, which is fine for one-session processes but
wrong for a daemon serving requests for one root over time. The daemon holds its own
`ProjectEntry` and passes it explicitly to the execution engine; `SessionHolder` is left
untouched for the TUI/Web/in-process paths.

## Risks / Trade-offs

- **A background JVM outlives the user's command and surprises them** → one-line stderr
  notice on auto-start, `atunko daemon status`, 30-minute idle exit, `--no-daemon`.
- **Stale daemon serves LSTs parsed by a different atunko/OpenRewrite version** → version is
  part of the handshake; mismatch stops the daemon and starts a fresh one.
- **Registry file points at a dead PID (crash, kill, reboot)** → client probes the socket
  and checks liveness before use; a stale entry is deleted and a new daemon started.
- **Another local user connects to the loopback port** → per-daemon random token, registry
  file `rw-------`. Not a defence against the same user, which is out of scope.
- **Daemon holds memory the user wants back** → soft-referenced LSTs are reclaimed under
  pressure by the existing cache; `atunko daemon stop --all` is immediate.
- **Fallback path masks a broken daemon, so users silently lose the speedup** → fallback logs
  the reason at warn level rather than failing silently.
- **Fingerprint walk on a very large project is itself slow** → still far below parse cost,
  and the walk is the same one `ParsedSourcesCache` already performs today in the TUI.
- **Concurrent `atunko run` in the same root** → the daemon serializes requests per project;
  `ParsedSourcesCache` already shares a single parse between concurrent callers.

## Migration Plan

Additive. No existing flag, command or output changes; the daemon is a new fast path with the
current behaviour as its fallback. Rollback is `--no-daemon` / `atunko.daemon.disabled=true`
per invocation, or removing the `atunko-daemon` module.

## Open Questions

None — the four questions carried in `docs/design/lst-caching-daemon.md` are resolved above
and that document is updated to record the decisions.
