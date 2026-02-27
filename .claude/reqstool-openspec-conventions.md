# OpenSpec + reqstool Spec Conventions

## Principle: reqstool is SSOT

reqstool owns all requirements and verification scenarios (GIVEN/WHEN/THEN).
OpenSpec spec.md files reference reqstool IDs — they NEVER duplicate requirement
descriptions or scenario steps.

## How to look up requirements and SVCs

Requirements and SVCs are defined in the system-level reqstool files. Subprojects
(core, app) import them via filters — the actual content lives in the parent.

**To get requirement and SVC details, use the reqstool CLI:**

```bash
# For core module requirements/SVCs
reqstool generate-json local -p core/docs/reqstool

# For app module requirements/SVCs
reqstool generate-json local -p app/docs/reqstool

# For system-level (all requirements/SVCs)
reqstool generate-json local -p docs/reqstool
```

The JSON output contains:
- `requirements` — keyed by `urn:ID` (e.g., `atunko:CORE_0001`), includes title, description, significance
- `svcs` — keyed by `urn:SVC_ID` (e.g., `atunko:SVC_CORE_0001`), includes title, description (GIVEN/WHEN/THEN), requirement_ids
- `svcs_from_req` — maps requirement IDs to their SVC IDs

**Do NOT read YAML files directly** — use the CLI. The subproject YAML files only contain
filters and imports, not the actual requirement/SVC content.

## spec.md Format

Reference requirement and SVC IDs only. The word SHALL is required by OpenSpec strict validation.

```markdown
## ADDED Requirements

### Requirement: <REQUIREMENT_ID>
The system SHALL implement <REQUIREMENT_ID>.

#### Scenario: <SVC_ID>
The system SHALL pass <SVC_ID>.
```

### Multiple scenarios under one requirement

```markdown
### Requirement: CORE_0002
The system SHALL implement CORE_0002.

#### Scenario: SVC_CORE_0002a
The system SHALL pass SVC_CORE_0002a.

#### Scenario: SVC_CORE_0002b
The system SHALL pass SVC_CORE_0002b.
```

### Multiple requirements

```markdown
## ADDED Requirements

### Requirement: CORE_0001
The system SHALL implement CORE_0001.

#### Scenario: SVC_CORE_0001
The system SHALL pass SVC_CORE_0001.

### Requirement: CORE_0002
The system SHALL implement CORE_0002.

#### Scenario: SVC_CORE_0002
The system SHALL pass SVC_CORE_0002.
```

## Rules

1. **DRY**: Never duplicate requirement descriptions or GIVEN/WHEN/THEN from reqstool
2. **SHALL**: Every requirement and scenario MUST contain the word SHALL (required by `openspec validate --strict`)
3. **IDs only**: Reference requirement IDs and SVC IDs — do not include file paths or descriptions
4. **One requirement per `### Requirement:` header**: Use the reqstool ID as the header name
5. **One scenario per `#### Scenario:` header**: Use the reqstool SVC ID as the header name
6. **Use reqstool CLI**: To look up details, run `reqstool generate-json local -p <path>`
7. **Validate**: Always run `openspec validate --all --strict` after creating or modifying specs

## Validation

```bash
# Validate a specific change
openspec validate <change-name> --type change --strict --json

# Validate everything
openspec validate --all --strict --json
```
