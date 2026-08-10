## Purpose

Tell users, before they run anything, which recipes can actually act on their project instead of letting Maven and Gradle recipes silently report "no changes" — `ProjectSourceParser` reports the `SourceCapability` set it produced (Java, XML, YAML, JSON, properties, Maven, Gradle), `RecipeApplicabilityService` in core maps recipe-name prefixes to the capabilities they require and resolves composites as applicable when any transitive child is, and the TUI recipe list/detail views and the Web UI recipe tree render inapplicable recipes dimmed with a short badge and a reason. Selection and execution of inapplicable recipes stay allowed — the badge is purely informational.

## Requirements

### Requirement: CORE_0015
The system SHALL implement CORE_0015.

#### Scenario: SVC_CORE_0015
The system SHALL pass SVC_CORE_0015.

#### Scenario: SVC_CORE_0015.1
The system SHALL pass SVC_CORE_0015.1.

#### Scenario: SVC_CORE_0015.2
The system SHALL pass SVC_CORE_0015.2.

#### Scenario: SVC_CORE_0015.3
The system SHALL pass SVC_CORE_0015.3.

### Requirement: TUI_0004
The system SHALL implement TUI_0004.

#### Scenario: SVC_TUI_0004
The system SHALL pass SVC_TUI_0004.

#### Scenario: SVC_TUI_0004.1
The system SHALL pass SVC_TUI_0004.1.

### Requirement: WEB_0003
The system SHALL implement WEB_0003.

#### Scenario: SVC_WEB_0003
The system SHALL pass SVC_WEB_0003.
