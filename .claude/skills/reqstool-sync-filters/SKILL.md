---
name: reqstool-sync-filters
description: Synchronize subproject reqstool filters to include all current requirement and SVC IDs from the system-level files. Use when requirements or SVCs have been added/removed and filters need updating.
license: MIT
metadata:
  author: atunko
  version: "1.0"
---

Sync subproject filters to match current system-level requirements and SVCs.

---

**Input**: Optional module name — `core`, `app`, or `all`. Defaults to `all`.

**Why this is needed**

Subproject `requirements.yml` and `software_verification_cases.yml` files use list-based filters
(`requirement_ids.includes` / `svc_ids.includes`) to import specific IDs from the system-level files.
When new requirements or SVCs are added to the system level, the subproject filters must be updated
to include them.

**Mapping rules**

| System ID prefix | Target subproject | Filter file |
|------------------|-------------------|-------------|
| `CORE_*` | `core/docs/reqstool/requirements.yml` | `filters.atunko.requirement_ids.includes` |
| `CLI_*` | `app/docs/reqstool/requirements.yml` | `filters.atunko.requirement_ids.includes` |
| `SVC_CORE_*` | `core/docs/reqstool/software_verification_cases.yml` | `filters.atunko.svc_ids.includes` |
| `SVC_CLI_*` | `app/docs/reqstool/software_verification_cases.yml` | `filters.atunko.svc_ids.includes` |

**Steps**

1. **Read system-level files**

   Read both:
   - `docs/reqstool/requirements.yml` — extract all requirement IDs
   - `docs/reqstool/software_verification_cases.yml` — extract all SVC IDs

2. **Group IDs by prefix**

   - `CORE_*` requirements → core module
   - `CLI_*` requirements → app module
   - `SVC_CORE_*` SVCs → core module
   - `SVC_CLI_*` SVCs → app module

3. **Read current subproject filters**

   For each selected module, read the current filter lists from:
   - `<module>/docs/reqstool/requirements.yml` → `filters.atunko.requirement_ids.includes`
   - `<module>/docs/reqstool/software_verification_cases.yml` → `filters.atunko.svc_ids.includes`

4. **Compute diff**

   For each filter list, determine:
   - **Missing**: IDs in system that are not in the subproject filter (need to add)
   - **Stale**: IDs in the subproject filter that no longer exist in system (need to remove)

5. **If no changes needed**

   Report "All filters are in sync." and exit.

6. **Apply updates**

   For each filter that needs updating, replace the `includes` list with the correct sorted IDs.
   Keep IDs in natural sort order (e.g., CORE_0001 before CORE_0010).

7. **Verify with reqstool**

   Run `reqstool status local -p <module>/docs/reqstool` for each updated module.

8. **Report**

   Show a summary table:
   ```
   Module | File                          | Added    | Removed
   core   | requirements.yml              | CORE_0010 | -
   core   | software_verification_cases.yml | SVC_CORE_0005 | -
   ```

**Guardrails**
- Never modify system-level files — only update subproject filter lists
- Preserve all other content in the subproject files (metadata, imports, cases, etc.)
- Only add/remove IDs — do not reformat or restructure the files
- Validate that reqstool status passes after sync
