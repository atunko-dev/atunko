## Context

`ConfigExportService` in `atunko-core` already provides `exportToGradle(RunConfig, ExportMode)` and `exportToMaven(RunConfig, ExportMode)` with MINIMAL and FULL modes. The Web UI surfaces this via a modal dialog. The TUI has no equivalent; the `ConfirmRunView` is the natural trigger point because `RunConfig` is fully assembled there.

## Goals / Non-Goals

**Goals:**
- Add `ExportConfigView` — a full-screen TUI overlay showing the exported snippet
- Wire `e` on `ConfirmRunView` to open the overlay
- Support format toggle (Gradle/Maven) and mode toggle (Minimal/Full) inside the overlay
- Update help text in `ConfirmRunView` to document the new keybinding

**Non-Goals:**
- Writing the snippet to a file (clipboard/file export is out of scope for this change)
- Exporting from `ExecutionResultsView` (confirm-run is the right moment; results are after execution)
- Any changes to `ConfigExportService` or `RunConfig`

## Decisions

**D1 — Full-screen overlay, not a floating popup**
TamboUI does not have a built-in modal dialog primitive. Existing overlays (`HelpOverlay`) are rendered inline as a `row(spacer(), content, spacer())`. For export we need a scrollable text area, so a full-screen approach (like `FileDiffView`) is cleaner. The overlay replaces the center content of `ConfirmRunView` when active, controlled by a flag on `TuiController`.

**D2 — State on `TuiController`, not local to the view**
TUI views are stateless renderers; all mutable state lives on `TuiController`. Two new fields: `exportFormat` (GRADLE/MAVEN enum) and `exportMode` (MINIMAL/FULL) are added. This is consistent with how `selectedFileIndex`, `showHelp`, etc. are managed.

**D3 — Key bindings inside the overlay**
- `g` → Gradle, `m` → Maven (format toggle)
- `Tab` → toggles Minimal/Full mode
- `Esc` → close overlay (return to confirm-run)

These are unambiguous and consistent with the Web UI's Gradle/Maven concepts.

**D4 — `markupTextArea` for display**
`FileDiffView` uses `markupTextArea` for scrollable read-only content. The same widget is appropriate here for the snippet. No syntax highlighting needed; plain text is sufficient.

## Risks / Trade-offs

- [TamboUI snapshot instability] `markupTextArea` API may shift between snapshots → Mitigation: follow `FileDiffView` pattern exactly; pin TamboUI version in `gradle/libs.versions.toml`
- [Long snippets] FULL mode generates ~20-line files; `markupTextArea` handles this natively with scrollbar → no special handling needed
