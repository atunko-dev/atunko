## Purpose

Make atunko runnable with zero install through JBang — a `jbang-catalog.json` at the repository root defines an `atunko` alias whose `script-ref` points at a stable-named `atunko.jar` asset published by the release workflows, so `jbang atunko@atunko-dev/atunko <args>` resolves the shadow JAR and provisions a matching Java 25 runtime without the user cloning or building the project, and the catalog never needs editing per release because the asset name and download URL are permanent.

## Requirements

### Requirement: CLI_0006
The system SHALL implement CLI_0006.

#### Scenario: SVC_CLI_0006
The system SHALL pass SVC_CLI_0006.

#### Scenario: SVC_CLI_0006.1
The system SHALL pass SVC_CLI_0006.1.
