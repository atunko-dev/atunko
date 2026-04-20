## Why

Users who discover and configure recipes in the Web UI have no way to export that selection into their project's build file. The core export logic (`ConfigExportService`) and CLI command already exist; bridging them to the Web UI closes the workflow gap so developers can go straight from recipe browser to build-tool integration.

A minimal snippet (plugin block only) is useful for patching an existing build file, but users starting from scratch need a complete, runnable file. Adding a **Minimal / Full** mode gives both audiences what they need in one dialog.

## What Changes

- Add an **Export** button to the Web UI status bar
- New `ExportConfigDialog` modal: format selector (Gradle / Maven), mode selector (Minimal / Full), live-updating read-only snippet, copy-to-clipboard
- Extend `ConfigExportService` with `ExportMode` enum and full-file export overloads
- Add `--full` flag to `atunko config export` CLI subcommand (default: minimal)
- New requirement `WEB_0001.17` and SVCs `SVC_WEB_0001.28`–`.31`

**Full Gradle** (`build.gradle`, Groovy DSL) — standalone file with `plugins {}`, `repositories {}`, and `rewrite {}` blocks.

**Full Maven** (`pom.xml`) — complete POM with GAV `io.github.atunkodev:atunko-rewrite:0.1.0-SNAPSHOT` and `<build><plugins>` block.

## Capabilities

### New Capabilities

- `web-config-export`: Export the current recipe selection from the Web UI as a Gradle or Maven plugin configuration, in either Minimal (snippet) or Full (standalone file) mode

### Modified Capabilities

- `web-config-export`: Adding Minimal/Full mode selector to the export dialog and core service

## Impact

- **`atunko-core`**: `ConfigExportService` — new `ExportMode` enum, overloaded `exportToGradle(RunConfig, ExportMode)` and `exportToMaven(RunConfig, ExportMode)`
- **`atunko-cli`**: `ConfigExportCommand` — new `--full` flag (default: minimal)
- **`atunko-web`**: `RecipeBrowserView.java` (new button + method), new `ExportConfigDialog.java` (format + mode selectors)
- **`docs/reqstool/`**: New requirement `WEB_0001.17`, new SVCs `SVC_WEB_0001.28`–`.31`
- **No new dependencies**
