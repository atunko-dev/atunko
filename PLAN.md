# JBang Support — Implementation Plan

Issue: [#18](https://github.com/atunko-dev/atunko/issues/18)

## Goal

`jbang atunko@atunko-dev tui` should just work — zero-install, no cloning, no building.

## Distribution Strategy: GAV (Maven Coordinates)

TamboUI 0.1.0 is now on Maven Central. Once TamboUI 0.2.0 is released and atunko is
published, GAV-based distribution works cleanly. The Gradle Tooling API repo
(`repo.gradle.org`) is declared in the catalog's `repositories` field.

**Blocker:** Atunko release is blocked until TamboUI 0.2.0 is published.

## Catalog Convention: Dedicated `atunko-dev/jbang-catalog` Repo

JBang resolves `alias@org` via a `jbang-catalog.json` at the root of `org/jbang-catalog`
on GitHub. The dedicated repo pattern is the established convention:

| Org | Repo | Invocation |
|-----|------|------------|
| [jbangdev](https://github.com/jbangdev/jbang-catalog) | `jbangdev/jbang-catalog` | `jbang hello@jbangdev` |
| [Microsoft](https://github.com/microsoft/jbang-catalog) | `microsoft/jbang-catalog` | `jbang minecraft-server@microsoft` |
| [jOOQ](https://github.com/jOOQ/jbang-catalog) | `jOOQ/jbang-catalog` | `jbang <alias>@jooq` |
| [KIE Group](https://github.com/kiegroup/jbang-catalog) | `kiegroup/jbang-catalog` | `jbang <alias>@kiegroup` |

**Decision:** Create `atunko-dev/jbang-catalog` for the clean `jbang atunko@atunko-dev` UX.

## Recommended Catalog Shape

```json
{
  "catalogs": {},
  "aliases": {
    "atunko": {
      "script-ref": "io.github.atunkodev:atunko-cli:VERSION",
      "description": "OpenRewrite TUI + CLI tool — recipe browsing, search, and execution",
      "java": "25+",
      "repositories": ["https://repo.gradle.org/gradle/libs-releases"]
    }
  },
  "templates": {}
}
```

Key fields:
- **`script-ref`**: GAV coordinates — JBang resolves from Maven Central + declared repos
- **`java`**: JBang will auto-provision Java 25 if not available
- **`repositories`**: Gradle Tooling API repo (not on Maven Central)
- **`description`**: Shown in `jbang alias list`

Arguments pass through naturally — `jbang atunko@atunko-dev tui` passes `tui` to the main class.

## Implementation Steps

1. **Create `atunko-dev/jbang-catalog` repo** — single `jbang-catalog.json` file
2. **Configure Maven Central publishing** — Gradle publishing plugin, POM metadata, signing
3. **Publish atunko** (blocked on TamboUI 0.2.0 release)
4. **Update catalog version** to match published release
5. **Test end-to-end**: `jbang atunko@atunko-dev tui`, `jbang atunko@atunko-dev list`, etc.
6. **Document** in README — add JBang as an installation option

## Future Enhancements

- **CI/CD automation**: GitHub Actions to publish to Maven Central and update catalog on tag push
- **Version aliases**: `atunko-latest`, `atunko-0.1.0` for pinning

## References

- [JBang Aliases & Catalogs docs](https://www.jbang.dev/documentation/jbang/latest/alias_catalogs.html)
- [jbangdev/jbang-catalog](https://github.com/jbangdev/jbang-catalog) — reference implementation
- [microsoft/jbang-catalog](https://github.com/microsoft/jbang-catalog)
