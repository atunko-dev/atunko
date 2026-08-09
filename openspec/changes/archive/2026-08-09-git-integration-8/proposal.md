## Why

Recipe execution rewrites source files in place. When the target project has uncommitted
work, a bad recipe run mixes its edits into the user's changes with no easy way back
(issue #8). Git already provides the primitives for a safety net — the tool only has to
use them: detect that the project is a git repository and record an undo point before
changes are applied.

## What Changes

- New core git integration (`io.github.atunkodev.core.git.GitService`): repo detection
  and a stash-based pre-execution checkpoint, both delegating to the `git` CLI via
  `ProcessBuilder` — no JGit dependency.
- The checkpoint uses `git stash create` + `git stash store`, which records a stash
  entry **without touching the working tree** — the user's uncommitted changes are never
  removed, even transiently (see design.md for the mechanism choice).
- `atunko run` gains a `--git-checkpoint` flag: before executing, a checkpoint is
  created and the stash SHA plus the restore command (`git stash apply <sha>`) are
  printed. Undo is one command away.
- Everything degrades gracefully: no git binary, not a git repository, or a clean
  working tree each print a clear message and the run continues without a checkpoint.
- TUI and Web UI integration are OUT of scope for this change (core + CLI only).

## Capabilities

### New Capabilities

- `git-integration`: repo detection and stash-based safety checkpoint in core, with
  CLI wiring via `atunko run --git-checkpoint` (reqstool IDs: CORE_0006 with new
  sub-requirements CORE_0006.1/.2/.3).

### Modified Capabilities

<!-- none — existing run semantics are unchanged; the checkpoint is opt-in via a new
     flag and adds behaviour before execution only -->

## Impact

- `atunko-core`: new package `io.github.atunkodev.core.git` with `GitService` and
  `GitCheckpoint`; no new dependencies (ProcessBuilder + git CLI).
- `atunko-cli`: `RunCommand` gains `--git-checkpoint` and prints checkpoint/restore
  info or a graceful degradation message.
- reqstool: child requirements CORE_0006.1/.2/.3 under the existing CORE_0006 with
  SVCs SVC_CORE_0006(.1/.2/.3).
