## Context

atunko builds a shadow JAR (`atunko-cli/build/libs/atunko-<version>.jar`, main class
`io.github.atunkodev.App`, Java 25) that `release.yml` attaches to GitHub releases
(none published yet; versioning via axion from git tags, `publish-snapshot.yml`
maintains a rolling `-SNAPSHOT` pre-release). atunko is not published to Maven
Central, so a JBang alias cannot use GAV coordinates — it must reference a JAR URL.

JBang catalog resolution: `alias@org/repo` reads `jbang-catalog.json` from that
repo's default branch; the shorter `alias@org` form reads it from an
`<org>/jbang-catalog` repo.

## Goals / Non-Goals

**Goals**

- `jbang atunko@atunko-dev/atunko tui` (and `discover`, `run`, …) works with zero
  install once the first release is published.
- The catalog never needs editing when new versions are released.

**Non-Goals**

- Maven Central publication (separate, larger effort; would later allow a GAV-based
  alias and true version pinning).
- Creating the `atunko-dev/jbang-catalog` org repo inside this PR — it is a
  one-file repo created manually (or via `gh repo create`) as a follow-up; its
  content is the same catalog file this change adds.
- A snapshot alias (`atunko-snapshot`) — pre-releases are excluded from
  `releases/latest`; can be added later pointing at the rolling snapshot tag if
  wanted.

## Decisions

1. **Alias targets a stable-named asset via the permanent latest-release URL**:
   `https://github.com/atunko-dev/atunko/releases/download/latest/…` does not exist,
   but `https://github.com/atunko-dev/atunko/releases/latest/download/<asset>` does
   and always points at the newest non-prerelease release. The versioned JAR name
   would make that URL churn per release, so `release.yml` uploads a second,
   stable-named copy `atunko.jar` alongside the versioned one.
   *Alternative considered*: catalog rewritten by CI on each release (version-pinned
   URL) — more moving parts, catalog commits polluting history, no user benefit.

2. **Catalog lives at the repo root** (`jbang-catalog.json`), the location JBang
   looks up for `alias@atunko-dev/atunko`. Minimal content: one `atunko` alias with
   `script-ref` = the latest-release JAR URL, a description, and `"java": "25+"`
   so JBang provisions a matching JDK.

3. **README quick-start** mirrors the issue's examples (`tui`,
   recipe search, `run -r …`) and states the JAR-URL nature (no
   version pinning until Maven Central publication).
   *Correction during implementation*: the issue's `discover --search "spring"`
   example is not a real command — the CLI subcommands are `tui`, `list`,
   `search`, `run`, `config`. The README uses `search "spring"` instead.

4. **Verification**: a unit-style test is impossible for an external `jbang`
   binary; SVC verification is (a) JSON validity of `jbang-catalog.json` asserted
   by a small test in `atunko-cli` (catalog parses, alias present, URL well-formed)
   and (b) manual `jbang --verbose run` smoke test documented in the SVC. CI-side,
   `release.yml` gains `fail_on_unmatched_files` coverage for the new stable asset
   (already enabled — the glob just must match).

5. **Stable-asset copy step must disambiguate the shadow JAR** (found during
   implementation): `atunko-cli/build/libs/` contains both the shadow JAR
   (`atunko-<version>.jar`) and the thin module JAR (`atunko-cli-<version>.jar`),
   and the existing release glob `atunko-*.jar` matches both. The copy step
   therefore filters out `atunko-cli-*` before asserting a single match, and
   `files:` lists `atunko.jar` explicitly since it does not match `atunko-*.jar`.

## Risks / Trade-offs

- [No release exists yet → alias 404s until the first release] → acceptable and
  explicit in README; publishing the first release is on the maintainer.
- [`releases/latest` ignores pre-releases] → intended: JBang users get stable
  builds only.
- [Stable-named asset means old releases' `atunko.jar` differs from the versioned
  file listing] → each release carries both names; the versioned JAR remains the
  canonical artifact.

## Migration Plan

Single PR to main; catalog becomes functional at the first published release.
Follow-up (manual, org-level): create `atunko-dev/jbang-catalog` containing the
same `jbang-catalog.json` so `jbang atunko@atunko-dev` works.

## Open Questions

- None blocking.
