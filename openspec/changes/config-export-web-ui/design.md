## Context

`ConfigExportService` (core) generates Gradle `rewrite {}` and Maven `<plugin>` snippets from a `RunConfig`. The CLI exposes this via `atunko config export --gradle/--maven`. The Web UI (`RecipeBrowserView`) has Save/Load buttons that use `RunConfigService` and `cascadeHandler.getSelectedItems()` to build a `RunConfig`.

`ConfigExportService` is being extended with an `ExportMode` enum (`MINIMAL` / `FULL`) and overloaded export methods. The existing no-arg methods default to `MINIMAL` for backward compatibility.

Vaadin's clipboard API is available via `UI.getCurrent().getPage().executeJs("navigator.clipboard.writeText($0)", text)`.

## Goals / Non-Goals

**Goals:**
- Surface the export feature in the Web UI status bar alongside Save/Load
- Show a live snippet in a modal so users can copy-paste into their build file
- Support both Gradle and Maven formats with an in-dialog toggle
- Support **Minimal** (plugin snippet) and **Full** (standalone file) modes
- Full Gradle: Groovy DSL `build.gradle` with `plugins {}`, `repositories {}`, `rewrite {}`
- Full Maven: complete `pom.xml` with GAV `io.github.atunkodev:atunko-rewrite:0.1.0-SNAPSHOT`
- Reuse `ConfigExportService` from core — no duplication

**Non-Goals:**
- File download (copy-paste is sufficient)
- TUI export (tracked separately)
- Persisting format/mode preferences
- Options/configuration values in exported snippet (options model not yet stable)

## Decisions

### 1. Modal dialog with copy-to-clipboard instead of file download

**Decision:** Show snippet in a read-only `TextArea` inside a `Dialog` with a copy button.

**Rationale:** Even full standalone files are short (< 50 lines). A dialog lets the user see and verify before copying. File download adds a browser interrupt and complicates testing.

**Alternative considered:** `Anchor` + `StreamResource` for file download. Rejected — heavier, adds UI noise.

### 2. `RadioButtonGroup<ExportFormat>` for format selection

**Decision:** `RadioButtonGroup` with enum `ExportFormat { GRADLE, MAVEN }` defaulting to GRADLE.

**Rationale:** Two mutually exclusive options; radio buttons are idiomatic. Value-change listener live-updates the `TextArea`.

### 3. `RadioButtonGroup<ExportMode>` for mode selection

**Decision:** Second `RadioButtonGroup` with enum `ExportMode { MINIMAL, FULL }` defaulting to MINIMAL.

**Rationale:** Mirrors the format selector pattern; consistent UX. Both selectors trigger `updateSnippet()` on change.

**Alternative considered:** A single `Checkbox` ("Full standalone file"). Rejected — less consistent with the format selector.

### 4. `ExportMode` enum in `ConfigExportService` with overloaded methods

**Decision:** Add `ExportMode { MINIMAL, FULL }` to `ConfigExportService`. Existing `exportToGradle(RunConfig)` and `exportToMaven(RunConfig)` delegate to the new `exportToGradle(RunConfig, ExportMode)` / `exportToMaven(RunConfig, ExportMode)` overloads passing `MINIMAL`, preserving backward compatibility.

**Rationale:** Keeps the public API stable; all existing tests and callers require no changes.

### 5. `--full` flag in CLI `config export` subcommand

**Decision:** Add an optional `--full` boolean flag (default false = MINIMAL). When set, passes `ExportMode.FULL` to `ConfigExportService`.

**Rationale:** Consistent with the minimal/full distinction; `--full` is self-documenting.

### 6. Empty-selection handled inside the dialog, not as a guard toast

**Decision:** If `selectedRecipes.isEmpty()`, open dialog but disable controls with "No recipes selected."

**Rationale:** Keeps interaction consistent with other dialogs; avoids abrupt toast-and-abort.

### 7. `Set.copyOf` snapshot at construction time

**Decision:** Immutable copy of recipe set at dialog construction.

**Rationale:** Prevents mid-display state changes from the grid affecting the dialog's output.

## Risks / Trade-offs

- **Clipboard API requires HTTPS or localhost** → `navigator.clipboard.writeText` silently fails on insecure non-localhost origins. Acceptable: atunko's web UI is always local.
- **No options in export** → `RecipeEntry` is name-only. If recipes have configured options they are silently dropped. Documented in Non-Goals.
- **GAV placeholders in full Maven** → `io.github.atunkodev:atunko-rewrite:0.1.0-SNAPSHOT` are placeholder values. Users must update them. Acceptable: this is clearly a starting point, not production-ready POM.
- **MockVaadin required in tests** → `ExportConfigDialog` extends `Dialog`. Mitigated by `@BeforeEach`/`@AfterEach` MockVaadin setup.
