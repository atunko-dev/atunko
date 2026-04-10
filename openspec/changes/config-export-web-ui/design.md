## Context

`ConfigExportService` (core) already generates Gradle `rewrite {}` and Maven `<plugin>` snippets from a `RunConfig`. The CLI already exposes this via `atunko config export --gradle/--maven`. The Web UI (`RecipeBrowserView`) has Save/Load buttons that use `RunConfigService` and `cascadeHandler.getSelectedItems()` to build a `RunConfig` from selected recipes. No download or snippet-sharing mechanism exists in the web view today.

Vaadin's clipboard API is available via `UI.getCurrent().getPage().executeJs("navigator.clipboard.writeText($0)", text)`.

## Goals / Non-Goals

**Goals:**
- Surface the export feature in the Web UI status bar alongside Save/Load
- Show a live snippet in a modal so users can copy-paste into their build file
- Support both Gradle and Maven formats with an in-dialog toggle
- Reuse `ConfigExportService` from core — no duplication

**Non-Goals:**
- File download (copy-paste is sufficient for build snippets)
- TUI export (tracked separately)
- Persisting the export format preference
- Options/configuration values in the exported snippet (options not yet fully supported in core)

## Decisions

### 1. Modal dialog with copy-to-clipboard instead of file download

**Decision:** Show snippet in a read-only `TextArea` inside a `Dialog` with a copy button.

**Rationale:** Build-tool snippets are short (< 30 lines). A dialog lets the user see what they're copying and supports immediate verification. File download requires a `StreamResource` + `Anchor`, adds a browser download interrupt, and is harder to test. The issue description explicitly says "copy-pasteable snippets".

**Alternative considered:** `Anchor` + `StreamResource` for file download. Rejected — heavier, adds UI noise for small snippets.

### 2. `RadioButtonGroup<ExportFormat>` for format selection

**Decision:** Use a `RadioButtonGroup` with enum `ExportFormat { GRADLE, MAVEN }` defaulting to GRADLE.

**Rationale:** Exactly two mutually exclusive options; radio buttons are the idiomatic Vaadin/HTML control. Switching format live-updates the `TextArea` via a value-change listener — no extra button needed.

**Alternative considered:** A `Select` dropdown. Rejected — overkill for two options.

### 3. Empty-selection handled inside the dialog, not as a guard toast

**Decision:** If `selectedRecipes.isEmpty()`, open the dialog but disable the text area, format selector, and copy button, with a "No recipes selected." message.

**Rationale:** A toast-and-abort pattern is abrupt; the dialog clearly explains why nothing is available and keeps the interaction consistent with other dialogs in the UI.

### 4. `Set.copyOf` snapshot at construction time

**Decision:** Take an immutable copy of the recipe set when the dialog is constructed.

**Rationale:** The user may deselect recipes after opening the export dialog. The snapshot makes the dialog's state deterministic and independent of grid changes.

## Risks / Trade-offs

- **Clipboard API requires HTTPS or localhost** → `navigator.clipboard.writeText` silently fails on insecure non-localhost origins. Mitigated: atunko's web UI is always local (`localhost`), so this is acceptable.
- **No options in export** → `RecipeEntry` is constructed as name-only, matching `saveConfig()`. If recipes have configured options, those are silently dropped. Mitigated: documented in Non-Goals; options export can be added later when the options model stabilises.
- **MockVaadin required in tests** → `ExportConfigDialog` extends `Dialog`, so it needs a Vaadin session to instantiate. Mitigated: `ExportConfigDialogTest` sets up MockVaadin in `@BeforeEach` / `@AfterEach` following the established pattern in `RecipeBrowserViewTest`.
