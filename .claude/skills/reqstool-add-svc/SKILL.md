---
name: reqstool-add-svc
description: Add a new Software Verification Case (SVC) to the system-level SVCs file and update the relevant subproject filter. Use when the user wants to add a test scenario for a requirement.
license: MIT
metadata:
  author: atunko
  version: "1.0"
---

Add a new SVC and update subproject filters.

---

**Input**: Requirement ID to verify, or a description of the test scenario.

**Project structure**

| File | Purpose |
|------|---------|
| `docs/reqstool/software_verification_cases.yml` | System-level SVCs (SSOT) |
| `core/docs/reqstool/software_verification_cases.yml` | Core module — imports SVC_CORE_* via filter |
| `app/docs/reqstool/software_verification_cases.yml` | App module — imports SVC_CLI_* via filter |

**ID conventions**

| Prefix | Module | Linked to | Example |
|--------|--------|-----------|---------|
| `SVC_CORE_` | core | `CORE_*` requirements | `SVC_CORE_0005` |
| `SVC_CLI_` | app | `CLI_*` requirements | `SVC_CLI_0002` |

**Steps**

1. **Gather SVC details**

   If not provided, use **AskUserQuestion** to collect:
   - **Requirement ID**: Which requirement this verifies (e.g., `CORE_0005`)
   - **Title**: Short description (e.g., "Resolve Maven classpath")
   - **Verification type**: `automated-test` or `manual-test` (default: `automated-test`)
   - **Description**: GIVEN/WHEN/THEN scenario

   If only a requirement ID is given, read the requirement from `docs/reqstool/requirements.yml`
   to help draft the GIVEN/WHEN/THEN scenario.

2. **Determine the next SVC ID**

   Read `docs/reqstool/software_verification_cases.yml` and find the highest existing SVC ID
   with the relevant prefix. The SVC ID typically mirrors the requirement number:
   - For `CORE_0005` → try `SVC_CORE_0005` first
   - If that exists, use the next available number in the `SVC_CORE_*` sequence

3. **Add the SVC to system-level file**

   Append the new SVC to `docs/reqstool/software_verification_cases.yml`.

   Format for automated-test:
   ```yaml
     - id: <SVC_ID>
       requirement_ids: ["<REQ_ID>"]
       title: <title>
       verification: automated-test
       description: |
         GIVEN <precondition>
         WHEN <action>
         THEN <expected result>
       revision: "0.1.0"
   ```

   Format for manual-test (includes instructions):
   ```yaml
     - id: <SVC_ID>
       requirement_ids: ["<REQ_ID>"]
       title: <title>
       verification: manual-test
       description: |
         GIVEN <precondition>
         WHEN <action>
         THEN <expected result>
       instructions: "<how to manually verify>"
       revision: "0.1.0"
   ```

4. **Update the subproject filter**

   Add the new SVC ID to the `filters.<urn>.svc_ids.includes` list in the relevant subproject's
   `docs/reqstool/software_verification_cases.yml`:
   - `SVC_CORE_*` → `core/docs/reqstool/software_verification_cases.yml`
   - `SVC_CLI_*` → `app/docs/reqstool/software_verification_cases.yml`

5. **Verify with reqstool**

   Run `reqstool status local -p <subproject>/docs/reqstool` and confirm the new SVC appears
   under the linked requirement.

6. **Report**

   Show the user:
   - The new SVC ID, linked requirement, and verification type
   - Which files were modified
   - For automated-test SVCs, remind: "Add `@SVCs({"<SVC_ID>"})` annotation to the test method."

**Guardrails**
- Validate that the linked requirement ID exists in `docs/reqstool/requirements.yml`
- Never create duplicate SVC IDs
- All SVCs live in the system-level file (SSOT) — subproject files only contain filters
- Use GIVEN/WHEN/THEN format for all descriptions
- Preserve existing formatting and indentation
