## Purpose

Launch the TUI and Web UI immediately for single-project sessions by deferring the expensive build-system scan until it is actually needed — `SessionHolder.initLazy(...)` records the project directory at startup (cheap `ProjectScannerFactory.detect` validation still runs, so a directory with no build files fails fast) and `ensureScanned()` performs the Gradle Tooling API / Maven scan exactly once on the first recipe execution, synchronized and memoized for concurrent Web executions. Scan failures are not cached and are surfaced at execution time — an error panel in the TUI, the existing error notification in the Web UI — leaving the session alive so the next run retries. Workspace sessions keep scanning eagerly.

## Requirements

### Requirement: CORE_0017
The system SHALL implement CORE_0017.

#### Scenario: SVC_CORE_0017
The system SHALL pass SVC_CORE_0017.

### Requirement: CORE_0017.1
The system SHALL implement CORE_0017.1.

#### Scenario: SVC_CORE_0017.1
The system SHALL pass SVC_CORE_0017.1.

### Requirement: CORE_0017.2
The system SHALL implement CORE_0017.2.

#### Scenario: SVC_CORE_0017.2
The system SHALL pass SVC_CORE_0017.2.

### Requirement: TUI_0005
The system SHALL implement TUI_0005.

#### Scenario: SVC_TUI_0005
The system SHALL pass SVC_TUI_0005.

### Requirement: TUI_0005.1
The system SHALL implement TUI_0005.1.

#### Scenario: SVC_TUI_0005.1
The system SHALL pass SVC_TUI_0005.1.

### Requirement: WEB_0004
The system SHALL implement WEB_0004.

#### Scenario: SVC_WEB_0004
The system SHALL pass SVC_WEB_0004.
