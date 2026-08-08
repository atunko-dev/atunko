# Tasks

## 1. reqstool

- [x] 1.1 Add requirement CLI_0006 (JBang zero-install distribution) to
      `docs/reqstool/requirements.yml`
- [x] 1.2 Add SVCs SVC_CLI_0006 (catalog file valid: parses as JSON, `atunko`
      alias present, script-ref is the latest-release stable-asset URL) and
      SVC_CLI_0006.1 (manual smoke: `jbang atunko@atunko-dev/atunko --help`
      runs after a release exists) to
      `docs/reqstool/software_verification_cases.yml`

## 2. Catalog + release asset

- [ ] 2.1 Test first: `JbangCatalogTest` in atunko-cli — reads
      `jbang-catalog.json` from repo root, asserts JSON validity, `atunko` alias,
      well-formed `releases/latest/download/atunko.jar` script-ref, and a java
      version attribute; add `@SVCs({"SVC_CLI_0006"})`
- [ ] 2.2 Add `jbang-catalog.json` at repo root (alias `atunko`, description,
      script-ref per design decision 1, `"java": "25+"`)
- [ ] 2.3 `.github/workflows/release.yml`: also upload a stable-named
      `atunko.jar` copy of the shadow JAR (keep versioned asset;
      `fail_on_unmatched_files` still on)

## 3. Docs

- [ ] 3.1 README: "Run with JBang (zero install)" section with the issue's three
      examples and a note that it resolves from the latest GitHub release

## 4. Wrap-up

- [ ] 4.1 `./gradlew spotlessApply build` green;
      `openspec validate --all --strict` passes
- [ ] 4.2 PR to main, closes #18; note follow-up: create
      `atunko-dev/jbang-catalog` org repo with the same catalog for the short
      `jbang atunko@atunko-dev` form
