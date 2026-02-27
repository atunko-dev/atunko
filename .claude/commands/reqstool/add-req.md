---
name: "Reqstool: Add Requirement"
description: Add a new requirement to the system-level requirements and update subproject filters
category: Requirements
tags: [reqstool, requirements, add]
---

Add a new requirement to the system-level requirements file (SSOT) and update the relevant subproject filter.

**Argument**: Description of the requirement, optionally with module name, significance, and position.

Read `.reqstool-ai.yaml` to determine modules, paths, and ID prefixes.

Examples:
- `/reqstool:add-req core: The engine shall resolve classpaths for Maven projects`
- `/reqstool:add-req app: The tool shall display recipe details via CLI subcommand`
