---
name: "Reqstool: Add Requirement"
description: Add a new requirement to the system-level requirements and update subproject filters
category: Requirements
tags: [reqstool, requirements, add]
---

Add a new requirement to `docs/reqstool/requirements.yml` (SSOT) and update the relevant subproject filter.

**Argument**: Description of the requirement, optionally with module (`core`/`app`), significance, and position.

Examples:
- `/reqstool:add-req core: The core engine shall resolve classpaths for Maven projects`
- `/reqstool:add-req CLI_0004 after CLI_0003: The tool shall display recipe details via CLI subcommand`

ID prefixes: `CORE_*` for core module, `CLI_*` for app module.
