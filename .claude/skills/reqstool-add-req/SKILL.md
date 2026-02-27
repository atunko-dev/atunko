---
name: reqstool-add-req
description: Add a new requirement to the system-level requirements.yml and update the relevant subproject filter. Use when the user wants to add a new requirement.
license: MIT
metadata:
  author: atunko
  version: "1.0"
---

Add a new requirement to the system and update subproject filters.

---

**Input**: Description of the requirement. Optionally: module (`core` or `app`), position (after which ID), significance, categories.

**Project structure**

| File | Purpose |
|------|---------|
| `docs/reqstool/requirements.yml` | System-level requirements (SSOT) |
| `core/docs/reqstool/requirements.yml` | Core module — imports CORE_* via filter |
| `app/docs/reqstool/requirements.yml` | App module — imports CLI_* via filter |

**ID conventions**

| Prefix | Module | Example |
|--------|--------|---------|
| `CORE_` | core | `CORE_0010` |
| `CLI_` | app | `CLI_0004` |

**Steps**

1. **Gather requirement details**

   If not provided in the input, use **AskUserQuestion** to collect:
   - **Title**: Short name (e.g., "Maven Project Support")
   - **Description**: Full requirement statement using shall/should/may
   - **Module**: `core` or `app` (determines ID prefix and filter file)
   - **Significance**: `shall`, `should`, or `may` (default: `shall`)
   - **Categories**: ISO 25010 categories (default: `[functional-suitability]`)
   - **Position**: After which existing requirement ID (optional — defaults to end of the relevant group)

   Valid categories: `functional-suitability`, `performance-efficiency`, `compatibility`,
   `interaction-capability`, `reliability`, `security`, `maintainability`, `flexibility`, `safety`

2. **Determine the next ID**

   Read `docs/reqstool/requirements.yml` and find the highest existing ID with the relevant prefix:
   - For core: find highest `CORE_XXXX`, increment by 1
   - For app: find highest `CLI_XXXX`, increment by 1

   Format: 4-digit zero-padded (e.g., `CORE_0010`, `CLI_0004`)

3. **Add the requirement to system-level file**

   Insert the new requirement into `docs/reqstool/requirements.yml` at the specified position
   (or at the end of the relevant group). Use revision `0.1.0`.

   Format:
   ```yaml
     - id: <ID>
       title: <title>
       significance: <significance>
       description: <description>
       categories: [<categories>]
       revision: 0.1.0
   ```

4. **Update the subproject filter**

   Add the new requirement ID to the `filters.<urn>.requirement_ids.includes` list in the
   relevant subproject's `docs/reqstool/requirements.yml`:
   - `CORE_*` IDs → `core/docs/reqstool/requirements.yml`
   - `CLI_*` IDs → `app/docs/reqstool/requirements.yml`

5. **Verify with reqstool**

   Run `reqstool status local -p <subproject>/docs/reqstool` and confirm the new requirement appears.

6. **Report**

   Show the user:
   - The new requirement ID and title
   - Which files were modified
   - Remind them to create a matching SVC if needed: "Run `/reqstool:add-svc` to create a verification case for this requirement."

**Guardrails**
- Never modify requirements in subproject files — they only contain filters and imports
- All requirements live in the system-level `docs/reqstool/requirements.yml` (SSOT)
- Preserve existing formatting and indentation
- Do not renumber existing requirements unless explicitly asked
