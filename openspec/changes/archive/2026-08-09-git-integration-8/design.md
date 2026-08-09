## Context

`atunko run --project-dir <dir>` (and `--workspace <dir>`) parses sources, executes the
recipe, and applies changes to disk via `ChangeApplier` — there is no dry-run mode on
this path, so every run mutates the working tree. Issue #8 asks for git integration:
detect whether the target is a git repository and offer a stash-based undo point before
changes are applied. The project already shells out to external tools elsewhere (Maven,
Gradle Tooling API); git is treated the same way.

## Goals / Non-Goals

**Goals**

- Detect whether a directory is inside a git working tree.
- Create an opt-in pre-execution safety checkpoint that can restore the pre-recipe
  state with a single git command — and that can **never lose the user's uncommitted
  changes**, under any failure mode.
- Graceful degradation everywhere: missing git binary, non-repo directory, clean
  working tree — clear message, run continues.

**Non-Goals**

- No TUI or Web UI integration (separate change; core API is designed so both can
  reuse it).
- No automatic restore/rollback after a failed run — undo stays a user decision
  (the restore command is printed).
- No JGit or other library dependency — the `git` CLI via `ProcessBuilder` is the
  whole integration surface.
- No per-project checkpointing inside a workspace run: the checkpoint targets the
  directory given on the command line (workspace roots are typically one repository;
  finer granularity can be layered on later).

## Decisions

1. **Checkpoint mechanism: `git stash create` + `git stash store` — never
   `git stash push`.** `git stash push` records the checkpoint but *removes* the
   changes from the working tree; restoring them requires an immediate
   `git stash apply`, leaving a window (crash, kill, apply conflict) in which the
   user's working tree has been silently reverted. `git stash create <msg>` instead
   builds the stash commit object *without touching the working tree or the ref
   log*, and `git stash store -m <msg> <sha>` then registers it in `refs/stash` so
   it shows up in `git stash list`. The working tree is never modified at any point,
   so there is no failure mode that loses or even temporarily hides user changes.
   *Trade-off (documented, accepted)*: unlike `push --include-untracked`,
   `stash create` snapshots tracked files only — untracked files are not part of
   the checkpoint. Recipes overwhelmingly modify tracked sources, and an untracked
   file is never reverted by `git stash apply` either, so nothing is destroyed; the
   limitation is only that brand-new untracked files are not captured in the undo
   point.
2. **Delegate to the `git` CLI via `ProcessBuilder`.** `git -C <dir> rev-parse
   --is-inside-work-tree` for detection; `git -C <dir> stash create/store` for the
   checkpoint. Stdout is captured, stderr discarded to a buffer for diagnostics,
   and a hard timeout guards against a hung git. An `IOException` (binary missing)
   is mapped to the same "unavailable" result as a non-zero exit — callers see
   booleans/Optionals, never exceptions, because every git failure must degrade
   gracefully rather than abort a recipe run.
3. **API shape** (`io.github.atunkodev.core.git`): `GitService` with
   `isGitAvailable()`, `isGitRepository(Path)`, and
   `createCheckpoint(Path, String message)` returning `Optional<GitCheckpoint>` —
   empty when there is nothing to checkpoint (clean tree, unborn HEAD) or when git
   is unusable. `GitCheckpoint(String stashSha, String message)` carries
   `restoreCommand()` = `git stash apply <sha>` — the SHA form is used because a
   `stash@{n}` index shifts as later stashes are pushed, while the SHA stays valid.
4. **CLI wiring**: `RunCommand` gains `--git-checkpoint`. Before execution it calls
   the service with message `atunko: pre-recipe <timestamp>` and prints either the
   created checkpoint (SHA + restore command) or why none was created (not a repo /
   no git / clean tree), then always proceeds with the run. Core stays UI-free; the
   messaging lives in the CLI.

## Risks / Trade-offs

- [Untracked files are not in the checkpoint] → accepted, see decision 1; nothing is
  ever deleted, the undo point just does not cover new files.
- [`git stash store` could fail after `create` succeeded] → the working tree was
  never touched, so nothing is lost; the checkpoint is simply reported as not
  created.
- [Hung or interactive git invocation] → timeout + process destroy; reported as
  unavailable, run continues.
