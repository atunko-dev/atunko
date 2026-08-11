## Purpose

Support Maven projects end to end: `MavenProjectScanner` resolves classpaths and source directories via `mvn dependency:build-classpath` and reports every `pom.xml` under the project root as build files, and `ProjectSourceParser` parses those poms with `MavenParser` in a single call — so multi-module relationships resolve and the resulting documents carry `MavenResolutionResult` markers that `org.openrewrite.maven.*` recipes require. Poms that fail to resolve (offline runs, private repositories) fall back to `XmlParser` and the `MAVEN` capability is withheld, keeping recipe applicability badges honest; before the first parse, `SourceCapabilityHints` seeds `MAVEN` from the presence of a root `pom.xml`.

## Requirements

### Requirement: CORE_0005
The system SHALL implement CORE_0005.

#### Scenario: SVC_CORE_0005
The system SHALL pass SVC_CORE_0005.

### Requirement: CORE_0016
The system SHALL implement CORE_0016.

#### Scenario: SVC_CORE_0016
The system SHALL pass SVC_CORE_0016.

#### Scenario: SVC_CORE_0016.1
The system SHALL pass SVC_CORE_0016.1.

#### Scenario: SVC_CORE_0016.2
The system SHALL pass SVC_CORE_0016.2.

#### Scenario: SVC_CORE_0016.3
The system SHALL pass SVC_CORE_0016.3.
