# Tasks

## 1. reqstool

- [x] 1.1 Add requirements CORE_0017 (lazy session initialisation), TUI_0005
      (TUI deferred scan + error surfacing), WEB_0004 (Web deferred scan + error
      surfacing) to `docs/reqstool/requirements.yml`
- [x] 1.2 Add SVCs SVC_CORE_0017(.1/.2), SVC_TUI_0005(.1), SVC_WEB_0004 to
      `docs/reqstool/software_verification_cases.yml`

## 2. Core: lazy SessionHolder

- [x] 2.1 Tests first: `SessionHolderTest` additions — `initLazy` records dir and
      leaves entries empty; `ensureScanned` populates entries exactly once
      (memoized, thread-safe under concurrent callers); scanner exception is
      rethrown and NOT cached (next call retries); eager `init`/`initWorkspace`
      unchanged
- [x] 2.2 Implement `SessionHolder.initLazy(Path)` + `ensureScanned()` per
      design decision 1 (startup still runs `ProjectScannerFactory.detect` for
      fail-fast validation, only `.scan()` deferred)
- [x] 2.3 Add `@Requirements({"atunko:CORE_0017"})` on the new methods and
      `@SVCs` on the tests from 2.1

## 3. TUI

- [x] 3.1 `TuiCommand`: replace startup scan with `detect(dir)` validation +
      `SessionHolder.initLazy(dir)`; `TuiController` run path calls
      `ensureScanned()` before parsing and surfaces scan failure in the UI
      (execution results/error state, not a crash)
- [x] 3.2 Pilot e2e tests: TUI launches and browses recipes with a project dir
      whose scan has not run; run on a valid project triggers scan and executes;
      scan failure (fixture with build file but broken model) shows an error and
      the session survives
- [x] 3.3 Add `@Requirements({"atunko:TUI_0005"})` on the implementing code and
      `@SVCs` on the tests from 3.2

## 4. Web

- [x] 4.1 `WebUiCommand`: replace startup scan with `detect(dir)` validation +
      `SessionHolder.initLazy(dir)`; execution background thread calls
      `ensureScanned()` inside the progress dialog and reports failure via the
      existing error notification path
- [x] 4.2 Karibu test: view opens with unscanned session; execution triggers
      scan; scan failure surfaces as notification and view stays usable
- [x] 4.3 Add `@Requirements({"atunko:WEB_0004"})` on the implementing code and
      `@SVCs` on the test from 4.2

## 5. Audit + wrap-up

- [x] 5.1 Audit all call sites of `SessionHolder.getProjectInfo()` /
      `getProjectEntries()` for pre-scan null/empty tolerance; fix any that
      assume a scanned session
- [x] 5.2 `./gradlew spotlessApply build` green; `openspec validate --all --strict`
      passes
- [x] 5.3 PR (stacked on feat/recipe-applicability-stage2), closes #44
