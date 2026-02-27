---
name: reqstool-status
description: Show reqstool traceability status for system, subproject, or all modules. Use when the user wants to check requirement coverage, missing implementations, or test status.
license: Apache-2.0
metadata:
  author: reqstool-ai
  version: "1.0"
---

Show reqstool requirements traceability status.

---

**Input**: Optional module name — `system`, a subproject name, or `all`. Defaults to `all`.

**Configuration**

Read `.reqstool-ai.yaml` to determine modules, paths, and prefixes.

**Steps**

1. **Read config**

   Read `.reqstool-ai.yaml`. It defines:
   - `system.path` — path to the system-level reqstool directory
   - `modules.<name>.path` — path to each subproject's reqstool directory

2. **Determine which module(s) to report**

   Parse the argument after the command. Accept:
   - `system` / `sys` — system-level only
   - A module name from config (e.g., `core`, `app`) — that module only
   - `all` or no argument — system + all modules

3. **Run reqstool status**

   For each selected module, run:
   ```bash
   reqstool status local -p <path>
   ```

   If running `all`, run them sequentially with a header before each:
   ```
   ## System (<system.path>)
   ## <module-name> (<module.path>)
   ```

4. **Summarize results**

   After running, provide a brief summary:
   - Total requirements per module
   - How many are implemented vs missing
   - How many SVCs have tests vs missing
   - Any manual verification results missing

5. **If reqstool is not installed**

   If the command fails with "not found", tell the user:
   ```
   reqstool is not installed. Install with: pipx install reqstool
   ```

**Guardrails**
- Always run from the project root directory
- Do not modify any files — this is a read-only status command
