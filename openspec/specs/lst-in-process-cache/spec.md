## Purpose

Provide an in-process LST cache — `ParsedSourcesCache` in `atunko-core` keeps `ParsedSources` in memory per project directory for the lifetime of a TUI/Web session, invalidated by fingerprinting the parser's inputs (source/resource files, build files, classpath entries), so repeated executions skip the dominant parse cost. Disableable via the `atunko.lst.cache.disabled` system property.

## Requirements

### Requirement: CORE_0018
The system SHALL implement CORE_0018.

#### Scenario: SVC_CORE_0018
The system SHALL pass SVC_CORE_0018.

### Requirement: CORE_0018.1
The system SHALL implement CORE_0018.1.

#### Scenario: SVC_CORE_0018.1
The system SHALL pass SVC_CORE_0018.1.

### Requirement: CORE_0018.2
The system SHALL implement CORE_0018.2.

#### Scenario: SVC_CORE_0018.2
The system SHALL pass SVC_CORE_0018.2.

### Requirement: CORE_0018.3
The system SHALL implement CORE_0018.3.

#### Scenario: SVC_CORE_0018.3
The system SHALL pass SVC_CORE_0018.3.
