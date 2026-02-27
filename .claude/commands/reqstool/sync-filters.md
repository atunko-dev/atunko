---
name: "Reqstool: Sync Filters"
description: Synchronize subproject filters to include all current requirement and SVC IDs
category: Requirements
tags: [reqstool, filters, sync]
---

Sync subproject `requirement_ids.includes` and `svc_ids.includes` filters to match the current
system-level reqstool files.

**Argument**: Optional module name from `.reqstool-ai.yaml`, or `all` (default).

Read `.reqstool-ai.yaml` to determine modules, paths, and prefixes.

Useful after manually editing system-level requirements or SVCs to ensure subproject filters are up to date.
