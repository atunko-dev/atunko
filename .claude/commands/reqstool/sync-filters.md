---
name: "Reqstool: Sync Filters"
description: Synchronize subproject filters to include all current requirement and SVC IDs
category: Requirements
tags: [reqstool, filters, sync]
---

Sync subproject `requirement_ids.includes` and `svc_ids.includes` filters to match the current
system-level `docs/reqstool/` files.

**Argument**: Optional module — `core`, `app`, or `all` (default).

Useful after manually editing system-level requirements or SVCs to ensure subproject filters are up to date.
