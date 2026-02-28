## Context

CORE_0007 (save) is implemented. CORE_0008 adds the load counterpart so saved configs can be re-executed. The `RunConfigService` already has an `ObjectMapper` configured for YAML.

## Goals / Non-Goals

**Goals:**
- Load a `RunConfig` from a `.atunko.yml` file via `RunConfigService.load(Path)`
- Round-trip compatibility: `save` then `load` produces equivalent `RunConfig`

**Non-Goals:**
- Executing the loaded recipes (that's the CLI layer's responsibility)
- Config migration between versions
- Schema validation beyond Jackson deserialization

## Decisions

### Add `load` to existing `RunConfigService`

**Rationale:** Keeps save/load co-located. The `ObjectMapper` is already configured for YAML; `load` is a one-liner using `readValue`.

### Throw on missing or invalid file

**Rationale:** The caller (CLI) should handle user-facing errors. The service throws `IOException` for missing files and Jackson's `JsonProcessingException` for invalid YAML — both are checked exceptions the caller must handle.

## Risks / Trade-offs

- **No schema validation**: If the YAML has extra fields, Jackson ignores them by default. If required fields are missing, Jackson throws. This is sufficient for Phase 1.
