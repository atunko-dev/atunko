## Context

`ProjectSourceParser.parse()` walks a project's source/resource dirs and parses files by
extension: Java via `JavaParser`, and XML/YAML/JSON/properties via their generic parsers.
Consequences:

- `pom.xml` becomes a plain `Xml.Document` without a `MavenResolutionResult` marker.
  Every `org.openrewrite.maven.*` recipe requires that marker (via
  `MavenVisitor.isAcceptable`), so Maven recipes visit nothing and report "no changes".
- `.gradle`/`.gradle.kts` files are never collected (extension map has no entry), so
  `org.openrewrite.gradle.*` recipes have nothing to visit either.

The UIs (TUI `BrowserView`/`TagBrowserView`/`DetailView`, Web `RecipeBrowserView`)
present all ~4000 discovered recipes as equally runnable. reqstool is SSOT for
requirements; TUI behaviour is e2e-verifiable via the TamboUI Pilot harness added
in PR #71.

## Goals / Non-Goals

**Goals**

- Stage 1: users can see, before running, which recipes cannot act on the parsed
  source set, and why.
- Stage 2: Maven recipes genuinely work on Maven projects (`pom.xml` parsed with
  `MavenParser`, resolution markers present).
- Shared logic lives in `atunko-core`; TUI and Web only render what core computes.

**Non-Goals**

- Parsing Gradle build files (`GradleParser` needs a Gradle project model /
  tooling-API round-trip; deferred — Gradle recipes stay labeled as inapplicable).
- Per-recipe static analysis of visitor types. Detection is by recipe name prefix,
  which is accurate for the bundled recipe modules.
- Source-set provenance markers (main vs test) — separate backlog item.

## Decisions

1. **`SourceCapability` enum in core** (`JAVA`, `XML`, `YAML`, `JSON`, `PROPERTIES`,
   `MAVEN`, `GRADLE`). `ProjectSourceParser` returns (alongside sources) the set of
   capabilities it actually produced — e.g. `MAVEN` only when at least one `pom.xml`
   was parsed by `MavenParser` (Stage 2), never `GRADLE` for now. A small record
   `ParsedSources(List<SourceFile> sources, Set<SourceCapability> capabilities)` is
   introduced; `parse()` keeps its signature and a new `parseWithCapabilities()` (or
   changed return type — implementer's choice, callers are few) exposes it.
   *Alternative considered*: inferring capabilities downstream by inspecting
   `SourceFile` markers — rejected: parser already knows, and marker sniffing is
   fragile.

2. **Applicability by recipe-name prefix.** `RecipeApplicabilityService` maps
   `org.openrewrite.maven.` → requires `MAVEN`, `org.openrewrite.gradle.` → requires
   `GRADLE`; everything else is assumed applicable. A composite is applicable iff at
   least one transitive child is applicable (reuse the traversal pattern from
   `RecipeCoverageUtils`). Result exposed as
   `Applicability { boolean applicable; String reason; }` per recipe name, computed
   once per capability set and cached in a map.
   *Alternative considered*: instantiating each `Recipe` and checking visitor
   `isAcceptable` against parsed sources — far more precise but requires running
   visitor machinery over the whole source set per recipe (~4000 recipes); rejected
   for cost and complexity at Stage 1.

3. **TUI rendering**: `RecipeListRenderer` dims inapplicable recipes and appends a
   short badge (e.g. `⊘ needs Maven` / `⊘ needs Gradle`); `DetailView` shows a full
   reason line. Selection of inapplicable recipes stays allowed (running is still
   possible — it just reports no changes), so no interaction-model change; the badge
   is purely informational. Pilot e2e tests assert badge presence/absence against
   fixture projects.

4. **Web rendering**: `RecipeBrowserView` tree items get a muted style + suffix badge
   and a tooltip with the reason, using existing Vaadin idioms (no new dependency).

5. **Stage 2 Maven parsing**: `ProjectSourceParser` routes files named exactly
   `pom.xml` to `MavenParser.builder()` (all poms of the project parsed in one call so
   multi-module relationships resolve); remaining XML files go to `XmlParser` as
   before. Parse failures of `MavenParser` (unresolvable parents/dependencies —
   offline, private repos) fall back to `XmlParser` for those files and the `MAVEN`
   capability is omitted, so badges stay honest.
   *Alternative considered*: full `MavenMojoProjectParser`/tooling integration —
   heavyweight, unnecessary for marker-correct parsing.

6. **reqstool**: new requirements CORE_0015 (applicability detection), CORE_0016
   (Maven parsing with resolution markers), TUI_0004 (badging), WEB_0003 (badging),
   each with SVCs; annotations on the implementing classes/tests per existing
   conventions.

## Risks / Trade-offs

- [Prefix heuristic misses recipes outside `org.openrewrite.maven/gradle` that still
  require build-file models] → acceptable at Stage 1; the mapping is one table,
  easily extended.
- [`MavenParser` needs network for dependency resolution; offline/air-gapped runs
  degrade] → fall back to `XmlParser` + no `MAVEN` capability; recipes badge as
  inapplicable instead of silently no-opping — strictly better than today.
- [Parsing poms with resolution is slower than plain XML] → poms are few; resolution
  results are cached by OpenRewrite's `MavenPomCache` within the run.

## Migration Plan

Two PRs, Stage 1 then Stage 2, both behind no flags (pure additive behaviour).
Rollback = revert. Stage 1 ships badges with `MAVEN`/`GRADLE` absent (all Maven and
Gradle recipes badged); Stage 2 flips `MAVEN` to present on successful Maven parse,
which automatically un-badges Maven recipes.

7. **Pom discovery (Stage 2 addition).** Decision 5 only covered routing, but a Maven
   project's poms live *above* its source directories and `MavenProjectScanner` reported
   only `src/main/java`, so `ProjectSourceParser` would never have seen a pom in real
   usage. `ProjectInfo` therefore gains a `buildFiles` component (existing 2- and 5-arg
   constructors preserved, so no call site changed); `MavenProjectScanner` fills it with
   every `pom.xml` under the project root, skipping `target`/`build`/`.git`/`node_modules`.
   *Alternative considered*: inferring the project root by walking up from the source
   directories — rejected as guesswork; the scanner already knows.

8. **Success criterion for Maven parsing.** `MavenParser` does not throw on an
   unresolvable pom: it returns an `Xml.Document` carrying a `ParseExceptionResult`
   marker and no `MavenResolutionResult`. Success is therefore judged per document by the
   presence of `MavenResolutionResult`. Poms without it are re-parsed with `XmlParser` so
   they are still present as clean XML with no failure markers, and `MAVEN` is reported
   only when at least one pom resolved. Maven parsing runs on its own
   `InMemoryExecutionContext` with a swallowing error handler so a broken pom cannot fail
   the whole project parse.

## Open Questions

- **Resolved — badges before the first run.** Capability sets are only refreshed when a
  run happens, because that is when parsing occurs, so on a Maven project every Maven
  recipe would be badged "needs Maven" until the user's first run. Implemented fix:
  `SourceCapabilityHints.forProjectDir(dir)` returns `{MAVEN}` exactly when the project
  root has a `pom.xml` — the same signal `ProjectScannerFactory` uses to pick the Maven
  scanner — and the TUI (`TuiController` constructor) and Web (`WebUiCommand.run()`) seed
  their capability set from it. The hint is replaced wholesale by the real capability set
  on the first parse, so a pom that does not actually resolve corrects itself then.
  `GRADLE` is never hinted: Gradle build files are not parsed at all, so no run could
  ever make Gradle recipes applicable and hinting it would be a lie the parse never
  corrects. Traced as CORE_0016.3 / SVC_CORE_0016.3.
- Badge glyph/wording finalized during TUI implementation against Pilot snapshots.
