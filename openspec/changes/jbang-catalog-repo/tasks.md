## 1. Requirements & Traceability

- [ ] 1.1 Add CLI_0005 requirement (JBang Distribution) to `docs/reqstool/requirements.yml`
- [ ] 1.2 Add SVC_CLI_0005 verification case to `docs/reqstool/software_verification_cases.yml`
- [ ] 1.3 Update subproject reqstool filters to include CLI_0005 and SVC_CLI_0005

## 2. JBang Catalog Repo

- [ ] 2.1 Create `atunko-dev/jbang-catalog` GitHub repo (public, minimal)
- [ ] 2.2 Add `jbang-catalog.json` with `atunko` alias using GAV coordinates and Gradle repo
- [ ] 2.3 Add README with catalog description and usage examples

## 3. Maven Central Publishing Setup

- [ ] 3.1 Configure Gradle for Maven Central publishing (group: `io.github.atunkodev`)
- [ ] 3.2 Set up POM metadata (description, license, SCM, developers)
- [ ] 3.3 Configure signing plugin for artifact signing

## 4. End-to-End Verification (after atunko is published)

- [ ] 4.1 Publish atunko to Maven Central (blocked on TamboUI 0.2.0 release)
- [ ] 4.2 Update catalog `script-ref` version to match published version
- [ ] 4.3 Test `jbang atunko@atunko-dev tui` launches the TUI
- [ ] 4.4 Test `jbang atunko@atunko-dev list` lists recipes
- [ ] 4.5 Test `jbang atunko@atunko-dev search spring` returns results

## 5. Documentation

- [ ] 5.1 Add JBang installation section to atunko README
