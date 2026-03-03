---
name: reqstool-sync-filters
description: Synchronize subproject reqstool filters to include all current requirement and SVC IDs from the system-level files. Use when requirements or SVCs have been added/removed and filters need updating.
license: Apache-2.0
metadata:
  author: reqstool-ai
  version: "1.0"
---

Sync subproject filters to match current system-level requirements and SVCs.

---

**Input**: Optional module name or `all`. Defaults to `all`.

**Configuration**

Read `.reqstool-ai.yaml` to determine modules, paths, prefixes, and URN.

The config defines:
- `urn` — the project URN used in filter keys (e.g., `filters.<urn>.requirement_ids.includes`)
- `system.path` — path to system-level reqstool directory
- `modules.<name>.path` — path to each subproject's reqstool directory
- `modules.<name>.req_prefix` — requirement ID prefix for this module
- `modules.<name>.svc_prefix` — SVC ID prefix for this module

**Why this is needed**

Subproject `requirements.yml` and `software_verification_cases.yml` files use list-based filters
(`requirement_ids.includes` / `svc_ids.includes`) to import specific IDs from the system-level files.
When new requirements or SVCs are added to the system level, the subproject filters must be updated
to include them.

**Steps**

1. **Read config**

   Read `.reqstool-ai.yaml`. Build a mapping of prefixes to modules.

2. **Determine which module(s) to sync**

   Accept a module name from config, or `all` (default) for all modules.

3. **Read system-level files**

   Read both:
   - `<system.path>/requirements.yml` — extract all requirement IDs
   - `<system.path>/software_verification_cases.yml` — extract all SVC IDs

4. **Group IDs by prefix**

   For each module, match requirement IDs by `req_prefix` and SVC IDs by `svc_prefix`.

5. **Read current subproject filters**

   For each selected module, read the current filter lists from:
   - `<module.path>/requirements.yml` -> `filters.<urn>.requirement_ids.includes`
   - `<module.path>/software_verification_cases.yml` -> `filters.<urn>.svc_ids.includes`

6. **Compute diff**

   For each filter list, determine:
   - **Missing**: IDs in system that are not in the subproject filter (need to add)
   - **Stale**: IDs in the subproject filter that no longer exist in system (need to remove)

7. **If no changes needed**

   Report "All filters are in sync." and exit.

8. **Apply updates**

   For each filter that needs updating, replace the `includes` list with the correct sorted IDs.
   Keep IDs in natural sort order (e.g., CORE_0001 before CORE_0010).

9. **Verify with reqstool**

   Run `reqstool status local -p <module.path>` for each updated module.

10. **Report**

    Show a summary table:
    ```
    Module | File                          | Added    | Removed
    <name> | requirements.yml              | <ID>     | -
    <name> | software_verification_cases.yml | <ID>   | -
    ```

**Guardrails**
- Never modify system-level files — only update subproject filter lists
- Preserve all other content in the subproject files (metadata, imports, cases, etc.)
- Only add/remove IDs — do not reformat or restructure the files
- Validate that reqstool status passes after sync
