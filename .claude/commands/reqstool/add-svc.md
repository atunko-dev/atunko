---
name: "Reqstool: Add SVC"
description: Add a new Software Verification Case and update subproject filters
category: Requirements
tags: [reqstool, svc, verification, add]
---

Add a new SVC to `docs/reqstool/software_verification_cases.yml` (SSOT) and update the relevant subproject filter.

**Argument**: Requirement ID to verify and/or a description of the test scenario.

Examples:
- `/reqstool:add-svc CORE_0005` — will draft a GIVEN/WHEN/THEN based on the requirement
- `/reqstool:add-svc CORE_0005: GIVEN a Maven project WHEN the resolver scans THEN classpath is returned`

SVC ID mirrors the requirement number: `CORE_0005` -> `SVC_CORE_0005`.
