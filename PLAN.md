# JBang Support — Implementation Plan

Issue: [#18](https://github.com/atunko-dev/atunko/issues/18)

## Goal

`jbang atunko@atunko-dev tui` should just work — zero-install, no cloning, no building.

## Distribution Strategy: Fat JAR URL (Phase 1) → GAV (Phase 2)

### Why fat JAR first

Two dependencies block GAV (Maven coordinate) distribution today:

| Dependency | Problem |
|------------|---------|
| **TamboUI** `0.2.0-SNAPSHOT` | Only on Sonatype snapshots, not Maven Central |
| **Gradle Tooling API** `9.3.1` | Lives on `repo.gradle.org`, not Maven Central |

JBang supports `--repos` and catalog-level `repositories`, but relying on snapshot repos
for end-user distribution is fragile. The shadow JAR already bundles everything and
is ~126 MB (one-time download, JBang caches it).

**Phase 2:** Once TamboUI publishes a release to Maven Central and atunko itself is
published, switch the catalog alias from a JAR URL to a GAV coordinate
(`io.github.atunkodev:atunko-cli:VERSION`).

### Fat JAR vs GAV comparison

```
Fat JAR URL                              GAV (Maven coordinates)
─────────────────────────────────────    ─────────────────────────────────────
✓ Works today — shadow JAR exists        ✓ Clean, canonical JBang pattern
✓ No Maven Central publishing needed     ✓ JBang resolves transitive deps
✓ Single download, no dep resolution     ✗ Requires Maven Central publishing
✗ Larger download (~126 MB)              ✗ Snapshot/custom repo deps block it
✗ Less "JBang-native"                    ✗ ~200 transitive deps = slow first resolve
```

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
      "script-ref": "https://github.com/atunko-dev/atunko/releases/download/v0.1.0/atunko.jar",
      "description": "OpenRewrite TUI + CLI tool — recipe browsing, search, and execution",
      "java": "25+",
      "main": "io.github.atunkodev.App"
    }
  },
  "templates": {}
}
```

Key fields:
- **`script-ref`**: URL to the shadow JAR attached to the GitHub Release
- **`java`**: JBang will auto-provision Java 25 if not available
- **`main`**: Explicit main class (required for fat JARs without `Main-Class` manifest — verify if shadow JAR has it)
- **`description`**: Shown in `jbang alias list`

Arguments pass through naturally — `jbang atunko@atunko-dev tui` passes `tui` to the main class.

## Implementation Steps

1. **Verify shadow JAR manifest** — check if `Main-Class` is set in the shadow JAR; if so, `main` in the catalog is optional
2. **Create `atunko-dev/jbang-catalog` repo** — single `jbang-catalog.json` file
3. **Create a GitHub Release** on `atunko-dev/atunko` with the shadow JAR attached (manual for now, automated later with CI/CD)
4. **Test end-to-end**: `jbang atunko@atunko-dev tui`, `jbang atunko@atunko-dev list`, etc.
5. **Document** in README — add JBang as an installation option

## Future Enhancements

- **CI/CD automation**: GitHub Actions to build shadow JAR, create release, and update catalog `script-ref` URL on tag push
- **GAV migration** (Phase 2): Switch to Maven coordinates once dependencies are on Maven Central
- **Version aliases**: `atunko-latest`, `atunko-0.1.0` for pinning

## References

- [JBang Aliases & Catalogs docs](https://www.jbang.dev/documentation/jbang/latest/alias_catalogs.html)
- [jbangdev/jbang-catalog](https://github.com/jbangdev/jbang-catalog) — reference implementation
- [microsoft/jbang-catalog](https://github.com/microsoft/jbang-catalog)
