## Context

The core `RunConfigService` (CORE_0007 / CORE_0008) and Web UI save/load (WEB_0001.11 /
WEB_0001.12) were already complete. The TUI needed a UI layer on top of the existing service:
a save-mode inline text input in the browser and a dedicated `LOAD_CONFIG` screen.

## Goals / Non-Goals

**Goals:**
- Save: press `S` in the browser, type a name, Enter to write `atunko/runs/<name>.yml`
- Load: press `L` to open a file picker screen, navigate with j/k, Enter to load
- Blank name on save silently cancels (no empty files created)
- Empty runs directory shows an empty list (no crash)

**Non-Goals:**
- Delete / rename saved configs (future work)
- Config validation beyond what `RunConfigService` provides

## Decisions

### Inline save-mode text input in `BrowserView`

**Rationale:** Keeps the user on the same screen without a modal. The save-mode state
(`isSaveConfigMode`, `saveConfigName`) is held in `TuiController` so `BrowserView` stays
stateless. `Toolkit.handleTextInputKey` handles character input and backspace uniformly.

### Dedicated `LoadConfigView` screen (new `Screen.LOAD_CONFIG`)

**Rationale:** The load flow needs a scrollable list of files — reusing the browser for
this would require complex temporary state. A dedicated lightweight screen (no focusable
sub-elements, single key handler) is simpler and consistent with `ExecutionResultsView`.

### Files stored under `atunko/runs/` relative to `projectDir`

**Rationale:** Keeps all atunko artefacts in one place, co-located with the project so
they can be version-controlled alongside the code.

## Risks / Trade-offs

- **`projectDir` at construction time**: `TuiController` resolves `projectDir` on
  construction. If the user changes project mid-session (future multi-project work) the
  runs path won't update automatically. Acceptable for now; multi-project support is a
  separate change.
