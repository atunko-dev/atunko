## Context

The tool needs a way to persist recipe selections so users can re-run the same set of recipes without re-selecting them each time. The saved config should be human-readable, editable, and version-controllable.

## Goals / Non-Goals

**Goals:**
- Save a `RunConfig` (list of recipe names) to a `.atunko.yml` file
- Use YAML format for human-readability
- Support round-tripping (save → load in CORE_0008)

**Non-Goals:**
- Loading configs (CORE_0008 — separate change)
- Recipe options/parameters (Phase 1 saves recipe names only)
- Config validation beyond basic structure

## Decisions

### Use Jackson YAML for serialization

**Rationale:** Jackson with `jackson-dataformat-yaml` is already on the classpath transitively via OpenRewrite (2.21.1). Jackson handles Java records natively, produces clean YAML, and avoids direct SnakeYAML usage (which has had many CVEs). No additional dependency needed.

### Config format

```yaml
# .atunko.yml
recipes:
  - org.openrewrite.java.cleanup.RemoveUnusedImports
  - org.openrewrite.java.format.AutoFormat
```

Simple list of fully-qualified recipe names. Options/parameters can be added later.

### RunConfig as a record

**Rationale:** Immutable value type, clean API. `record RunConfig(List<String> recipes)`.

## Risks / Trade-offs

- **Jackson transitive dependency**: If OpenRewrite drops Jackson YAML in a future version, we'd need to add it explicitly. → Low risk, Jackson is deeply embedded in OpenRewrite's processing.
