## Context

atunko currently requires cloning and building from source. Users need Git, Java 25, and
Gradle just to try the tool. JBang enables zero-install distribution — it downloads, caches,
and runs a JAR with automatic Java version provisioning.

TamboUI has released 0.1.0 to Maven Central, removing the main blocker for GAV-based
distribution. The Gradle Tooling API lives on `repo.gradle.org` (not Maven Central) but
JBang catalogs support custom repositories via the `repositories` field.

Atunko release is blocked until TamboUI 0.2.0 is published (atunko depends on 0.2.0 features).

## Goals / Non-Goals

**Goals:**
- Enable `jbang atunko@atunko-dev tui` as a zero-install entry point
- Follow established JBang catalog conventions (dedicated `jbang-catalog` repo)
- Use GAV-based distribution (Maven coordinates) — the canonical JBang pattern

**Non-Goals:**
- Fat JAR URL distribution (unnecessary now that TamboUI is on Maven Central)
- CI/CD automation for release publishing (future work)
- Version pinning aliases (`atunko-0.1.0`, `atunko-latest`)

## Decisions

### 1. GAV-based distribution

**Decision:** Use Maven coordinates (`io.github.atunkodev:atunko-cli:VERSION`) as the
JBang alias `script-ref`.

**Rationale:** TamboUI 0.1.0 is now on Maven Central. Once TamboUI 0.2.0 is released and
atunko is published, all dependencies resolve cleanly. The Gradle Tooling API repo can be
declared in the catalog's `repositories` field. GAV is the canonical JBang pattern — cleaner
than fat JAR URLs and lets JBang handle caching and resolution natively.

**Alternative considered:** Fat JAR URL from GitHub Releases — rejected because the Maven
Central blocker (TamboUI snapshots) is resolved, and GAV is the better long-term approach.

### 2. Dedicated `atunko-dev/jbang-catalog` repo

**Decision:** Create `atunko-dev/jbang-catalog` with a `jbang-catalog.json` at root.

**Rationale:** Every major org using JBang follows this convention (jbangdev, Microsoft, jOOQ,
KIE Group). It enables the clean `jbang atunko@atunko-dev` syntax vs the longer
`jbang atunko@atunko-dev/atunko` with an in-repo catalog.

**Alternative considered:** In-repo `jbang-catalog.json` in `atunko-dev/atunko` — rejected
because it produces a worse UX and doesn't follow community convention.

### 3. Java 25+ in catalog

**Decision:** Set `"java": "25+"` in the alias — JBang auto-provisions if needed.

**Rationale:** atunko requires Java 25 features. JBang's Java provisioning ensures users
don't need to manually install the correct JDK.

### 4. Custom repository for Gradle Tooling API

**Decision:** Include `repo.gradle.org` in the catalog's `repositories` field.

**Rationale:** The Gradle Tooling API is only available from Gradle's own repository, not
Maven Central. JBang supports per-alias `repositories` for exactly this case.

### Catalog shape

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

## Risks / Trade-offs

- **[Blocked on TamboUI 0.2.0]** Atunko cannot be published to Maven Central until TamboUI 0.2.0 is released → Catalog repo can be created now; version updated when atunko is published.
- **[Gradle Tooling API repo]** Custom repo in catalog adds a non-Maven-Central dependency → Well-supported by JBang; Gradle's repo is stable and widely used.
- **[Transitive dependency resolution]** ~200 transitive deps on first run → JBang caches aggressively; one-time cost. Acceptable for a developer tool.
- **[Catalog version drift]** The GAV version in the catalog must be updated for each release → Document the release checklist. Automate later with CI/CD.
