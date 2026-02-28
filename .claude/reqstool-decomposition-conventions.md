# Requirement Decomposition Conventions

## Principle: Stakeholder-Readable Requirements, Developer-Readable SVCs

Requirements are written for stakeholders — they must be meaningful and self-contained
without reading SVCs. SVCs are written for developers and testers — they describe how
to verify a requirement.

## Requirement Hierarchy

Use a parent-child pattern when a capability has multiple concrete behaviors:

- **Parent requirement**: Describes the full capability, stakeholder-readable on its own
- **Child requirements**: Spell out specific, verifiable behaviors

Children link to parents via the `references` field:

```yaml
# Parent — stakeholder reads this and understands the capability
- id: CLI_0004
  title: CLI Recipe Search
  significance: shall
  description: >-
    The tool shall search recipes by keyword via CLI subcommand,
    with selectable output format and sort order
  categories: [functional-suitability]
  revision: 0.1.0

# Child — specific behavior, linked to parent
- id: CLI_0004.1
  title: Search — Text Format
  significance: shall
  description: The search subcommand shall display matching recipes in human-readable text format
  references:
    requirement_ids: ["CLI_0004"]
  categories: [functional-suitability]
  revision: 0.1.0

- id: CLI_0004.2
  title: Search — JSON Format
  significance: shall
  description: The search subcommand shall display matching recipes in JSON format
  references:
    requirement_ids: ["CLI_0004"]
  categories: [functional-suitability]
  revision: 0.1.0
```

### When to Decompose

Decompose a requirement into children when:
- The capability has multiple distinct behaviors (e.g., output formats, sort orders)
- Children may have different significance levels (`shall` vs `should`)
- Children may ship in different versions
- You need independent verification of each behavior

Keep a requirement monolithic when:
- It describes a single, atomic behavior
- There is nothing to decompose

## ID Conventions

Use **dot notation** for sub-requirements and sub-SVCs (aligned with ISO 29148, INCOSE):

| Type | Pattern | Example |
|------|---------|---------|
| Parent requirement | `PREFIX_NNNN` | `CLI_0004` |
| Child requirement | `PREFIX_NNNN.N` | `CLI_0004.1`, `CLI_0004.2` |
| Parent SVC | `SVC_PREFIX_NNNN` | `SVC_CLI_0004` |
| Child SVC | `SVC_PREFIX_NNNN.N` | `SVC_CLI_0004.1`, `SVC_CLI_0004.2` |

### Why Dot Notation

- Standards-aligned (ISO 29148, INCOSE, Sparx EA)
- Supports multi-level nesting if needed (e.g., `CLI_0004.1.1`)
- Not limited to 26 children (unlike letter suffixes)
- Parent ID is visually embedded in the child ID

## SVC-to-Requirement Mapping

- **Child requirements may have one or more SVCs** — each SVC verifies a specific aspect
- **SVCs for child requirements reference the child** (e.g., `SVC_CLI_0004.1` → `CLI_0004.1`)
- **Parent requirements also need an SVC** for reqstool traceability — see below

### Parent SVC

reqstool does not yet have a formal parent-child hierarchy. Every requirement needs
at least one SVC for full traceability. Therefore, parent requirements need their own
SVC even when children cover the specific behaviors.

The parent SVC describes the overall capability:

```yaml
- id: SVC_CLI_0004
  requirement_ids: ["CLI_0004"]
  title: Search recipes via CLI
  verification: automated-test
  description: |
    GIVEN recipes on classpath
    WHEN user runs `atunko search <query>`
    THEN matching recipes are returned with selectable format and sort order
  revision: "0.1.0"
```

For annotations, placing the parent `@SVCs` at the **class level** of the test class
where the child SVCs are tested is a natural fit — the class as a whole verifies the
parent capability, while individual test methods verify the children:

```java
@SVCs({"SVC_CLI_0004"})  // parent SVC — class level (recommended)
class SearchCommandTest {

    @Test
    @SVCs({"SVC_CLI_0004.1"})  // child SVC — method level
    void search_displaysMatchingRecipesAsText() { ... }

    @Test
    @SVCs({"SVC_CLI_0004.2"})  // child SVC — method level
    void search_displaysMatchingRecipesAsJson() { ... }
}
```

### Non-decomposed requirements

If a requirement has no children, the SVC references it directly and the annotation
goes on the method level (per annotation conventions):

```yaml
- id: SVC_CORE_0001
  requirement_ids: ["CORE_0001"]
  title: Discover all recipes from classpath
  ...
```

## Lifecycle

Requirements and SVCs support lifecycle states via the `lifecycle` field:

| State | Description | Reason required? |
|-------|-------------|-----------------|
| `effective` | Active and approved (default — omit field) | No |
| `draft` | Not yet approved | No |
| `deprecated` | Superseded by another requirement | Yes |
| `obsolete` | No longer relevant | Yes |

### Rules

- Omit the `lifecycle` field for effective requirements (it is the default)
- When deprecating, always include a `reason` explaining what supersedes it
- Post-1.0.0: never delete or modify released requirements — deprecate them instead
- Pre-1.0.0: requirements can be freely restructured

### Example: Deprecating a Requirement

```yaml
- id: CLI_0002
  title: CLI Recipe Discovery
  significance: shall
  description: The tool shall list and search available recipes via CLI subcommand
  categories: [functional-suitability]
  revision: 0.1.0
  lifecycle:
    state: deprecated
    reason: Replaced by CLI_0002 (revised) with sub-requirements CLI_0002.1–CLI_0002.4
```

## Post-1.0.0 Extension Pattern

After a baseline release, extend functionality without breaking existing IDs:

1. **Parent requirement stays frozen** — its wording is abstract enough to cover future children
2. **Add new child requirements** — e.g., `CLI_0004.5` for a new output format
3. **Add matching SVCs** — e.g., `SVC_CLI_0004.5`
4. **Never modify or delete released requirement IDs**

This follows the Open-Closed Principle: closed for modification, open for extension.
