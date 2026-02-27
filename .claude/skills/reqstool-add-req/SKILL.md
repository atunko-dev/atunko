---
name: reqstool-add-req
description: Add a new requirement to the system-level requirements.yml and update the relevant subproject filter. Use when the user wants to add a new requirement.
license: Apache-2.0
metadata:
  author: reqstool-ai
  version: "1.0"
---

Add a new requirement to the system and update subproject filters.

---

**Input**: Description of the requirement. Optionally: module name, position (after which ID), significance, categories.

**Configuration**

Read `.reqstool-ai.yaml` to determine modules, paths, prefixes, URN, and revision.

The config defines:
- `urn` — the project URN used in reqstool files
- `revision` — the current revision string for new entries
- `system.path` — path to system-level reqstool directory
- `modules.<name>.path` — path to each subproject's reqstool directory
- `modules.<name>.req_prefix` — requirement ID prefix for this module (e.g., `CORE_`)

**Steps**

1. **Read config**

   Read `.reqstool-ai.yaml`. Build a mapping of module names to their paths and prefixes.

2. **Gather requirement details**

   If not provided in the input, use **AskUserQuestion** to collect:
   - **Title**: Short name (e.g., "Maven Project Support")
   - **Description**: Full requirement statement using shall/should/may
   - **Module**: One of the module names from config (determines ID prefix and filter file)
   - **Significance**: `shall`, `should`, or `may` (default: `shall`)
   - **Categories**: ISO 25010 categories (default: `[functional-suitability]`)
   - **Position**: After which existing requirement ID (optional — defaults to end of the relevant group)

   Valid categories: `functional-suitability`, `performance-efficiency`, `compatibility`,
   `interaction-capability`, `reliability`, `security`, `maintainability`, `flexibility`, `safety`

3. **Determine the next ID**

   Read `<system.path>/requirements.yml` and find the highest existing ID with the module's `req_prefix`.
   Increment by 1. Format: 4-digit zero-padded (e.g., `CORE_0010`, `CLI_0004`).

4. **Add the requirement to system-level file**

   Insert the new requirement into `<system.path>/requirements.yml` at the specified position
   (or at the end of the relevant group). Use the `revision` from config.

   Format:
   ```yaml
     - id: <ID>
       title: <title>
       significance: <significance>
       description: <description>
       categories: [<categories>]
       revision: <revision>
   ```

5. **Update the subproject filter**

   Add the new requirement ID to the `filters.<urn>.requirement_ids.includes` list in the
   relevant module's `<module.path>/requirements.yml`.

6. **Verify with reqstool**

   Run `reqstool status local -p <module.path>` and confirm the new requirement appears.

7. **Report**

   Show the user:
   - The new requirement ID and title
   - Which files were modified
   - Remind them to create a matching SVC if needed: "Run `/reqstool:add-svc` to create a verification case for this requirement."

**Guardrails**
- Never modify requirements in subproject files — they only contain filters and imports
- All requirements live in the system-level `<system.path>/requirements.yml` (SSOT)
- Preserve existing formatting and indentation
- Do not renumber existing requirements unless explicitly asked
