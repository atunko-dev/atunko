## Purpose

Give `atunko run` the cross-invocation source reuse that `lst-in-process-cache` (CORE_0018) only gives long-lived TUI and Web UI sessions. OpenRewrite LSTs cannot be serialized to disk, so a per-project-root daemon holds them in memory instead: `atunko run --project-dir` executes through a daemon it starts automatically, and a second run against an unchanged project skips both the build-system scan and the parse. The daemon parses through `JavaSourceParser` — the same parser the in-process path uses — so its results are identical to `--no-daemon`, and it shares `InputFingerprint` with the in-process cache so both decide staleness the same way. It binds loopback on an ephemeral port and gates every request on a per-daemon token held in an owner-only registry file; it exits after 30 minutes idle; at most three daemons are retained, evicting the least recently used idle one; and a daemon started by a different atunko version is stopped rather than reused, so trees parsed by another OpenRewrite are never served. Every failure mode — unstartable, unreachable, crashed, mismatched — degrades to in-process execution with a warning rather than an error.

## Requirements

### Requirement: CORE_0023
The system SHALL implement CORE_0023.

#### Scenario: SVC_CORE_0023
The system SHALL pass SVC_CORE_0023.

### Requirement: CORE_0023.1
The system SHALL implement CORE_0023.1.

#### Scenario: SVC_CORE_0023.1
The system SHALL pass SVC_CORE_0023.1.

### Requirement: CORE_0023.2
The system SHALL implement CORE_0023.2.

#### Scenario: SVC_CORE_0023.2
The system SHALL pass SVC_CORE_0023.2.

### Requirement: CORE_0023.3
The system SHALL implement CORE_0023.3.

#### Scenario: SVC_CORE_0023.3
The system SHALL pass SVC_CORE_0023.3.

### Requirement: CORE_0023.4
The system SHALL implement CORE_0023.4.

#### Scenario: SVC_CORE_0023.4
The system SHALL pass SVC_CORE_0023.4.

### Requirement: CLI_0009
The system SHALL implement CLI_0009.

#### Scenario: SVC_CLI_0009
The system SHALL pass SVC_CLI_0009.

### Requirement: CLI_0009.1
The system SHALL implement CLI_0009.1.

#### Scenario: SVC_CLI_0009.1
The system SHALL pass SVC_CLI_0009.1.

### Requirement: CLI_0009.2
The system SHALL implement CLI_0009.2.

#### Scenario: SVC_CLI_0009.2
The system SHALL pass SVC_CLI_0009.2.

### Requirement: CLI_0009.3
The system SHALL implement CLI_0009.3.

#### Scenario: SVC_CLI_0009.3
The system SHALL pass SVC_CLI_0009.3.
