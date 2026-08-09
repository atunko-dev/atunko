# Tasks

## 1. reqstool

- [x] 1.1 Add child requirements CORE_0006.1 (repo detection), CORE_0006.2
      (pre-execution stash checkpoint that never loses working-tree changes),
      CORE_0006.3 (CLI --git-checkpoint wiring + graceful non-repo degradation)
      under the existing CORE_0006 in `docs/reqstool/requirements.yml`
- [x] 1.2 Add SVCs SVC_CORE_0006 and SVC_CORE_0006.1/.2/.3 to
      `docs/reqstool/software_verification_cases.yml`

## 2. Core: GitService

- [x] 2.1 Tests first: `GitServiceTest` with @TempDir real `git init` repos
      (skipped via Assumptions when the git binary is missing) — detection returns
      true inside a repo and false outside; checkpoint on a dirty repo creates a
      stash entry while the uncommitted changes stay in the working tree and
      `git stash apply <sha>` restores the checkpointed state; clean tree and
      non-repo yield an empty checkpoint without error
- [x] 2.2 Add `@SVCs({"atunko:SVC_CORE_0006"})` at class level and
      `@SVCs({"atunko:SVC_CORE_0006.1"})` / `@SVCs({"atunko:SVC_CORE_0006.2"})`
      on the test methods from 2.1
- [x] 2.3 Implement `io.github.atunkodev.core.git.GitService`
      (`isGitAvailable`, `isGitRepository`, `createCheckpoint`) and
      `GitCheckpoint` per design decisions 1-3 — `git stash create` +
      `git stash store` via ProcessBuilder, no JGit
- [x] 2.4 Add `@Requirements({"atunko:CORE_0006.1"})` on `isGitRepository` and
      `@Requirements({"atunko:CORE_0006.2"})` on `createCheckpoint`

## 3. CLI: --git-checkpoint

- [x] 3.1 Tests first: RunCommand with `--git-checkpoint` — in a non-repo project
      dir prints a clear "not a git repository" message and still executes the
      recipe; in a dirty git repo prints the stash SHA and restore command and a
      stash entry exists after the run
- [x] 3.2 Add `@SVCs({"atunko:SVC_CORE_0006.3"})` on the test methods from 3.1
- [x] 3.3 Implement the `--git-checkpoint` option on `RunCommand` per design
      decision 4 (checkpoint before execution, graceful degradation messages)
- [x] 3.4 Add `@Requirements({"atunko:CORE_0006.3"})` on the implementing method

## 4. Wrap-up

- [x] 4.1 `./gradlew spotlessApply` then `./gradlew build` green;
      `openspec validate --all --strict` passes
- [x] 4.2 PR `feat(core): git integration — repo detection and stash-based
      checkpoint (#8)`, refs #8
