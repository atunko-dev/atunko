## 1. Tests (TDD — SVC_CLI_0003)

- [x] 1.1 Write `RunCommandTest.run_withValidRecipe_reportsChanges` — asserts output reports changed files
- [x] 1.2 Write `RunCommandTest.run_withInvalidRecipe_reportsError` — asserts error for unknown recipe
- [x] 1.3 Write `RunCommandTest.run_withMissingRequiredOptions_fails` — asserts exit code for missing -r

## 2. Implementation (CLI_0003)

- [x] 2.1 Create `RunCommand` with `-r`/`--recipe` and `--project-dir` options
- [x] 2.2 Wire to `RecipeExecutionEngine` and report results to stdout
- [x] 2.3 Register `RunCommand` in `App` root command
- [x] 2.4 Add `@Requirements({"CLI_0003"})` and `@SVCs({"SVC_CLI_0003"})` annotations

## 3. Documentation

- [x] 3.1 Create Antora docs structure under `docs/antora/` with CLI usage page
- [x] 3.2 Update README.md with run command usage

## 4. Verify

- [x] 4.1 Run `./gradlew :app:test` and confirm all tests pass
- [x] 4.2 Run `./gradlew build` and confirm full build passes
