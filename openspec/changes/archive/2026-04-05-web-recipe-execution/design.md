# Design: Web UI Recipe Execution

## Key Decisions

1. **AppServices in `atunko-core`** — mirrors `SessionHolder`: a static singleton set once
   at startup. Shared by TUI (future) and Web UI without duplication. Lives in core because
   both UIs need the same engine, parser, and applier instances.

2. **Execution flow mirrors TUI `runSelectedRecipes()`** — parse sources via
   `AppServices.getSourceParser().parse(SessionHolder.getProjectInfo())`, then run each
   selected recipe in sequence with `engine.execute(recipeName, sources)`, accumulate
   `FileChange` lists into a single `ExecutionResult`.

3. **Dry-run skips `ChangeApplier.apply()`** — same semantics as TUI: dry-run shows the
   dialog with changes but does not write to disk.

4. **Result shown in Vaadin `Dialog`** — each `FileChange` shows path and `after` content
   (or "(deleted)" if `after == null`). Scrollable `Pre` per file.

5. **No-op guards** — `runRecipes()` returns immediately if `AppServices.getEngine()` is
   null (services not initialised) or if no recipes are selected.
