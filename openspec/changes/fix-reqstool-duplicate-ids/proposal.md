## Why

reqstool is the project's single source of truth for requirements, and it currently contradicts
itself. Two unrelated features were given the same IDs by two commits landing 2½ minutes apart on
2026-05-17:

| ID | `0dad0aa` (22:54) | `3f3da48` (22:56) |
|---|---|---|
| `TUI_0001.19` | TUI — Load Run Config | TUI — Results File Navigation |
| `TUI_0001.20` | TUI — Markdown Recipe Descriptions | TUI — File Diff View |

`SVC_TUI_0001.19`, `SVC_TUI_0001.19.1` and `SVC_TUI_0001.20` are duplicated the same way. Both
families are live and annotated in code, so an `@Requirements({"atunko:TUI_0001.20"})` means
"markdown descriptions" in `DetailView` and "file diff view" in `FileDiffView` — the same string
naming two different things. Traceability cannot be trusted while that is true: any report keyed
on these IDs silently merges two features.

Separately, `CLI_0009.3` is missing its `categories` and `revision` fields, so
`reqstool validate local -p docs/reqstool` fails outright:

```
1. 'categories' is a required property
2. 'revision' is a required property
```

That failure is not gating anything today, which is how the duplicates survived. Fixing it is what
makes the duplicate class of bug detectable rather than something a human has to notice.

## What Changes

- Renumber the **file-diff family** to `TUI_0001.29` / `TUI_0001.30`, with SVCs following
  (`SVC_TUI_0001.29`, `.29.1`, `.30`, `.30.1`, `.30.2`). The load-config/markdown family keeps
  `.19`/`.20` because it was defined first — the later commit is the one that collided.
- Update the `@Requirements`/`@SVCs` annotations for the renumbered family only. The two families
  are interleaved within `TuiController` and `TuiControllerTest`, so each site is repointed by
  meaning, not by pattern.
- Restore `categories: [functional-suitability]` and `revision: 0.1.0` on `CLI_0009.3`, so
  `reqstool validate local -p docs/reqstool` passes.

Not changed: archived OpenSpec changes under `openspec/changes/archive/` keep the IDs they were
written with. They are records of what was proposed at the time, not live specifications.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `tui-file-diff`: its two requirements are renamed — `TUI_0001.19` → `TUI_0001.29` and
  `TUI_0001.20` → `TUI_0001.30`, with their scenarios following. No behaviour changes; the
  capability describes exactly what it did before under IDs it does not share with another
  capability.

## Impact

- `docs/reqstool/requirements.yml`, `docs/reqstool/software_verification_cases.yml` — the SSOT fix.
- `atunko-tui`: annotations in `TuiController`, `FileDiffView`, `TuiControllerTest`. No executable
  code changes — annotation strings only.
- `openspec/specs/tui-file-diff/spec.md` — requirement and scenario IDs.
- No user-visible behaviour change; nothing in `docs/antora/` to update.
