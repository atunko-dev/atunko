---
name: reqstool-add-svc
description: Add a new Software Verification Case (SVC) to the system-level SVCs file and update the relevant subproject filter. Use when the user wants to add a test scenario for a requirement.
license: Apache-2.0
metadata:
  author: reqstool-ai
  version: "1.0"
---

Add a new SVC and update subproject filters.

---

**Input**: Requirement ID to verify, or a description of the test scenario.

**Configuration**

Read `.reqstool-ai.yaml` to determine modules, paths, prefixes, URN, and revision.

The config defines:
- `urn` — the project URN used in reqstool files
- `revision` — the current revision string for new entries
- `system.path` — path to system-level reqstool directory
- `modules.<name>.path` — path to each subproject's reqstool directory
- `modules.<name>.req_prefix` — requirement ID prefix (e.g., `CORE_`) to match requirement IDs to modules
- `modules.<name>.svc_prefix` — SVC ID prefix (e.g., `SVC_CORE_`) for this module

**Steps**

1. **Read config**

   Read `.reqstool-ai.yaml`. Build a mapping of requirement prefixes to modules and SVC prefixes.

2. **Gather SVC details**

   If not provided, use **AskUserQuestion** to collect:
   - **Requirement ID**: Which requirement this verifies (e.g., `CORE_0005`)
   - **Title**: Short description (e.g., "Resolve Maven classpath")
   - **Verification type**: `automated-test` or `manual-test` (default: `automated-test`)
   - **Description**: GIVEN/WHEN/THEN scenario

   If only a requirement ID is given, read the requirement from `<system.path>/requirements.yml`
   to help draft the GIVEN/WHEN/THEN scenario.

3. **Determine the module from the requirement ID**

   Match the requirement ID prefix against `modules.<name>.req_prefix` to find the correct module.
   Use that module's `svc_prefix` for the new SVC ID.

4. **Determine the next SVC ID**

   Read `<system.path>/software_verification_cases.yml` and find the highest existing SVC ID
   with the relevant `svc_prefix`. The SVC ID typically mirrors the requirement number:
   - For `CORE_0005` with `svc_prefix: SVC_CORE_` -> try `SVC_CORE_0005` first
   - If that exists, use the next available number in the sequence

5. **Add the SVC to system-level file**

   Append the new SVC to `<system.path>/software_verification_cases.yml`.

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
       revision: "<revision>"
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
       revision: "<revision>"
   ```

6. **Update the subproject filter**

   Add the new SVC ID to the `filters.<urn>.svc_ids.includes` list in the relevant module's
   `<module.path>/software_verification_cases.yml`.

7. **Verify with reqstool**

   Run `reqstool status local -p <module.path>` and confirm the new SVC appears
   under the linked requirement.

8. **Report**

   Show the user:
   - The new SVC ID, linked requirement, and verification type
   - Which files were modified
   - For automated-test SVCs, remind: "Add `@SVCs({"<SVC_ID>"})` annotation to the test method."

**Guardrails**
- Validate that the linked requirement ID exists in `<system.path>/requirements.yml`
- Never create duplicate SVC IDs
- All SVCs live in the system-level file (SSOT) — subproject files only contain filters
- Use GIVEN/WHEN/THEN format for all descriptions
- Preserve existing formatting and indentation
