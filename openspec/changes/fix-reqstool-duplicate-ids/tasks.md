## 1. Restore CLI_0009.3

- [x] 1.1 Add `categories: [functional-suitability]` and `revision: 0.1.0` to `CLI_0009.3` in `docs/reqstool/requirements.yml`
- [x] 1.2 Add the `revision` field `SVC_CLI_0009.3` is missing in `docs/reqstool/software_verification_cases.yml` — same truncation, same commit, surfaced only once the requirements error stopped masking it
- [x] 1.3 Confirm `reqstool validate local -p docs/reqstool` passes

## 2. Renumber the file-diff requirements

- [x] 2.1 In `docs/reqstool/requirements.yml`, change the file-diff family's ids: `TUI_0001.19` (Results File Navigation) → `TUI_0001.29`, `TUI_0001.20` (File Diff View) → `TUI_0001.30`, leaving the load-config/markdown entries untouched
- [x] 2.2 In `docs/reqstool/software_verification_cases.yml`, renumber the file-diff SVCs and their `requirement_ids`: `SVC_TUI_0001.19` → `.29`, `SVC_TUI_0001.19.1` → `.29.1`, `SVC_TUI_0001.20` → `.30`, `SVC_TUI_0001.20.1` → `.30.1`, `SVC_TUI_0001.20.2` → `.30.2`
- [x] 2.3 Verify no duplicate ids remain in either reqstool file

## 3. Repoint the annotations

- [x] 3.1 `FileDiffView.java`: `TUI_0001.20` → `TUI_0001.30`
- [x] 3.2 `TuiController.java` lines ~1023–1048 (file diff navigation and open/return): `.19` → `.29`, `.20` → `.30`; leave lines ~1197/1204 (load run config) on `.19`
- [x] 3.3 `TuiControllerTest.java` file-diff block (~1418–1542, including the section comment): `SVC_TUI_0001.19` → `.29`, `SVC_TUI_0001.19.1` → `.29.1`, `SVC_TUI_0001.20` → `.30`, `.20.1` → `.30.1`, `.20.2` → `.30.2`; leave the load-config block (~529–666) untouched
- [x] 3.4 Confirm `DetailView.java` and `DetailViewTest.java` are unchanged — they belong to the markdown family that keeps `.20`

## 4. Update the spec

- [x] 4.1 Update `openspec/specs/tui-file-diff/spec.md` to the new requirement and scenario ids

## 5. Verification

- [x] 5.1 Cross-check every `atunko:` id used in source annotations against the reqstool files; none may be unresolvable
- [x] 5.2 Run `./gradlew spotlessApply` then `./gradlew build`
- [x] 5.3 Run `openspec validate --all --strict`
