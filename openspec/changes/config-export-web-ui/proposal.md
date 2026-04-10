## Why

Users who discover and configure recipes in the Web UI have no way to export that selection into their project's build file. The core export logic (`ConfigExportService`) and CLI command already exist; bridging them to the Web UI closes the workflow gap so developers can go straight from recipe browser to build-tool integration.

## What Changes

- Add an **Export** button to the Web UI status bar
- New `ExportConfigDialog` modal: format selector (Gradle / Maven), live-updating read-only snippet, copy-to-clipboard
- Wire `ConfigExportService` (already in `atunko-core`) into the web view
- New requirement `WEB_0001.17` and SVCs `SVC_WEB_0001.28`–`.31`

## Capabilities

### New Capabilities

- `web-config-export`: Export the current recipe selection from the Web UI as a Gradle or Maven plugin configuration snippet

### Modified Capabilities

<!-- none — no existing spec-level behavior changes -->

## Impact

- **`atunko-web`**: `RecipeBrowserView.java` (new button + method), new `ExportConfigDialog.java`
- **`atunko-core`**: No changes — `ConfigExportService` is reused as-is
- **`docs/reqstool/`**: New requirement `WEB_0001.17`, new SVCs `SVC_WEB_0001.28`–`.31`
- **No new dependencies**
