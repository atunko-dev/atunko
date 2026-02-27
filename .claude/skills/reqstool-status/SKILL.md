---
name: reqstool-status
description: Show reqstool traceability status for system, core, or app module. Use when the user wants to check requirement coverage, missing implementations, or test status.
license: MIT
metadata:
  author: atunko
  version: "1.0"
---

Show reqstool requirements traceability status.

---

**Input**: Optional module name — `system`, `core`, `app`, or `all`. Defaults to `all`.

**Paths**

| Module | Path |
|--------|------|
| system | `docs/reqstool` |
| core   | `core/docs/reqstool` |
| app    | `app/docs/reqstool` |

**Steps**

1. **Determine which module(s) to report**

   Parse the argument after the command. Accept:
   - `system` / `sys` — system-level only
   - `core` — core module only
   - `app` — app module only
   - `all` or no argument — all three

2. **Run reqstool status**

   For each selected module, run:
   ```bash
   reqstool status local -p <path>
   ```

   If running `all`, run them sequentially with a header before each:
   ```
   ## System (docs/reqstool)
   ## Core (core/docs/reqstool)
   ## App (app/docs/reqstool)
   ```

3. **Summarize results**

   After running, provide a brief summary:
   - Total requirements per module
   - How many are implemented vs missing
   - How many SVCs have tests vs missing
   - Any manual verification results missing

4. **If reqstool is not installed**

   If the command fails with "not found", tell the user:
   ```
   reqstool is not installed. Install with: pipx install reqstool
   ```

**Guardrails**
- Always run from the project root directory
- Do not modify any files — this is a read-only status command
