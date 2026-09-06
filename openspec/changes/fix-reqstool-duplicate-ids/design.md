## Context

See proposal.md — Why. The constraint that shapes the work: the two colliding families are
**interleaved within the same files**, so this cannot be done with a global find-and-replace.

| File | File-diff family (renumber) | Load-config family (leave) |
|---|---|---|
| `TuiController.java` | 1023, 1028, 1036, 1041, 1048 | 1197, 1204 |
| `TuiControllerTest.java` | 1418 (comment), 1421–1542 | 529, 547, 568, 578, 650, 666 |
| `FileDiffView.java` | 19 | — |
| `DetailView.java` | — | 22 |
| `DetailViewTest.java` | — | all |

`SVC_TUI_0001.19.1` is the sharpest case: it appears at `TuiControllerTest:547` meaning "load
config replaces existing selection" and at `:1528`/`:1542` meaning "resets file index on new
execution result". One string, two meanings, in one file.

## Goals / Non-Goals

**Goals:**

- One ID names exactly one requirement, across `docs/reqstool/` and every annotation.
- `reqstool validate local -p docs/reqstool` passes.

**Non-Goals:**

- No behaviour change. Only annotation strings, YAML entries, and spec IDs move.
- No rewriting of archived OpenSpec changes. They record what was proposed at the time; editing
  them would falsify the record for no traceability gain, since reqstool reads `docs/reqstool/`.
- Not adding reqstool validation to CI here. Worth doing, but it is a separate change with its own
  failure modes to think through, and bundling it would hide this fix inside a CI change.

## Decisions

- **The file-diff family moves, not the load-config family.** Both were introduced on 2026-05-17;
  `0dad0aa` (load-config/markdown, 22:54:29) preceded `3f3da48` (file-diff, 22:56:54), so the
  file-diff family is the one that collided with IDs already taken. First definition keeps the ID.
  The alternative — moving whichever has fewer references — was rejected as arbitrary; the counts
  are close, and "first definition wins" is a rule that can be applied again next time.
- **New IDs are `.29`/`.30`, not `.28`.** `TUI_0001.28` is claimed by the unmerged PR #90. This
  change is therefore stacked on that branch rather than based on `main`, so the ID space is read
  from the state this will actually merge into.
- **Per-site edits, verified by count, not by `sed`.** Each annotation is repointed by which
  feature it describes. The check that this was done correctly is mechanical: after the change, no
  duplicate IDs exist, every annotated ID resolves to a defined requirement, and the number of
  sites naming each family matches the table above.
- **`CLI_0009.3` gets `functional-suitability`**, matching `CLI_0009.1` and `CLI_0009.2` — its
  siblings under the same parent, all describing CLI behaviour — and `revision: 0.1.0`, matching
  every other entry in the file. The fields were lost to a truncation when the following comment
  block was appended, so this restores what the entry was written with rather than deciding
  anything new.

## Risks / Trade-offs

- [A missed annotation site leaves a dangling ID] → after the change, every `atunko:` ID in the
  source is cross-checked against the requirement and SVC files; an unresolvable ID fails that
  check.
- [Renumbering breaks a reader's saved links to `TUI_0001.19`] → unavoidable, and the ambiguity is
  worse: today that ID names two different features. The migration mapping is recorded in the
  delta spec.
- [Archived changes keep the old IDs and now read as inconsistent with the SSOT] → accepted and
  stated in the proposal; they are historical records, and reqstool does not read them.
