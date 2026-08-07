# Tasks

## 1. reqstool (Stage 1)

- [ ] 1.1 Add requirements CORE_0015 (recipe applicability detection), TUI_0004
      (TUI badging), WEB_0003 (Web badging) with rationale to
      `docs/reqstool/requirements.yml`
- [ ] 1.2 Add SVCs SVC_CORE_0015..0015.3, SVC_TUI_0004..0004.1, SVC_WEB_0003 to
      `docs/reqstool/software_verification_cases.yml`

## 2. Core: applicability detection (Stage 1)

- [ ] 2.1 Add `SourceCapability` enum and `ParsedSources` record
      (sources + capability set) in `io.github.atunkodev.core.project`;
      `ProjectSourceParser` reports capabilities of its output
      (JAVA/XML/YAML/JSON/PROPERTIES present per parsed files; MAVEN/GRADLE absent)
- [ ] 2.2 Tests first: `RecipeApplicabilityServiceTest` — maven/gradle prefix
      recipes inapplicable without MAVEN/GRADLE capability (with reason),
      others applicable, composite applicable iff any transitive child is
- [ ] 2.3 Implement `RecipeApplicabilityService` in
      `io.github.atunkodev.core.recipe` with per-capability-set caching;
      wire through `AppServices`

## 3. TUI badging (Stage 1)

- [ ] 3.1 `RecipeListRenderer`: dim + `⊘ needs Maven`/`⊘ needs Gradle` badge for
      inapplicable recipes; `DetailView`: full reason line
- [ ] 3.2 Pilot e2e tests: badge shown for a maven recipe on a plain-Java fixture
      project, absent for an applicable recipe; DetailView reason visible

## 4. Web badging (Stage 1)

- [ ] 4.1 `RecipeBrowserView`: muted style + badge + tooltip reason on
      inapplicable tree items (unit-test applicability wiring; rendering manual)

## 5. Stage 1 wrap-up

- [ ] 5.1 `./gradlew spotlessApply build` green; reqstool annotations
      (`@Requirements`/`@SVCs`) on implementing classes and tests
- [ ] 5.2 PR for Stage 1

## 6. Core: Maven parsing (Stage 2)

- [ ] 6.1 Add requirement CORE_0016 + SVCs SVC_CORE_0016..0016.2 to reqstool
- [ ] 6.2 Tests first: fixture Maven project (single- and multi-module) —
      parsed `pom.xml` carries `MavenResolutionResult` marker; capability set
      contains MAVEN; a real `org.openrewrite.maven.*` recipe (e.g.
      ChangePropertyValue/UpgradeDependencyVersion) produces changes end-to-end
- [ ] 6.3 `ProjectSourceParser`: route `pom.xml` files to `MavenParser` (all poms
      in one parse call); other XML unchanged; on Maven parse failure fall back
      to `XmlParser` and omit MAVEN capability
- [ ] 6.4 Verify badges flip: Maven recipes un-badged on Maven fixture project
      (unit + Pilot test)

## 7. Stage 2 wrap-up

- [ ] 7.1 `./gradlew spotlessApply build` green; annotations in place
- [ ] 7.2 PR for Stage 2
