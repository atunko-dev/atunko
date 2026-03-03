---
name: "Reqstool: Add SVC"
description: Add a new Software Verification Case and update subproject filters
category: Requirements
tags: [reqstool, svc, verification, add]
---

Add a new SVC to the system-level software verification cases file (SSOT) and update the relevant subproject filter.

**Argument**: Requirement ID to verify and/or a description of the test scenario.

Read `.reqstool-ai.yaml` to determine modules, paths, and SVC ID prefixes.

Examples:
- `/reqstool:add-svc CORE_0005` — will draft a GIVEN/WHEN/THEN based on the requirement
- `/reqstool:add-svc CORE_0005: GIVEN a Maven project WHEN the resolver scans THEN classpath is returned`
