## Why

All `org.openrewrite.maven.*` and `org.openrewrite.gradle.*` recipes silently no-op today:
`ProjectSourceParser` parses `pom.xml` with the generic `XmlParser` (no Maven resolution
markers) and skips `.gradle`/`.kts` files entirely, so these recipes run and report
"no changes" with no explanation. This is the biggest correctness flaw in the project —
users cannot tell a genuinely clean project from a recipe that never had a chance to act.

## What Changes

- **Stage 1 (honest reporting)**: New core service that determines, for each recipe,
  whether it can act on the capabilities of the parsed source set (Maven model, Gradle
  files, Java, XML, YAML, JSON, properties). Recipes that cannot act are greyed
  out/badged in the TUI recipe browser and detail view, and in the Web UI recipe tree,
  with a reason (e.g. "requires Maven model — not parsed"). Composite recipes are
  applicable if any descendant is applicable. Verified end-to-end in the TUI via Pilot
  tests.
- **Stage 2 (real capability)**: `ProjectSourceParser` parses `pom.xml` files with
  `MavenParser` (producing proper `MavenResolutionResult` markers) so
  `org.openrewrite.maven.*` recipes actually work. Gradle build files remain
  excluded-but-labeled (their applicability badge stays). Other XML continues to use
  `XmlParser`.

## Capabilities

### New Capabilities

- `recipe-applicability`: Core detection of whether a recipe can act on the parsed
  source set, surfaced as badges/grey-out in TUI and Web UI (planned reqstool IDs:
  CORE_0015 + sub-requirements, TUI_0004, WEB_0003).

### Modified Capabilities

- `maven-project-support`: Maven projects' `pom.xml` is parsed with `MavenParser`
  with resolution markers instead of generic `XmlParser`, so Maven recipes produce
  real results (planned reqstool ID: CORE_0016).

## Impact

- `atunko-core`: new `RecipeApplicabilityService` (+ `SourceCapability` model) in
  `io.github.atunkodev.core.recipe`; `ProjectSourceParser` gains Maven parsing and
  reports the capability set of its output; `AppServices` wiring.
- `atunko-tui`: `RecipeListRenderer`, `BrowserView`/`TagBrowserView`, `DetailView` —
  badge rendering + reason line; Pilot e2e tests.
- `atunko-web`: `RecipeBrowserView` tree — grey-out/badge with tooltip.
- Dependencies: `rewrite-maven` (already on the classpath via the recipe BOM).
- reqstool: new requirements CORE_0015, CORE_0016, TUI_0004, WEB_0003 with SVCs.
