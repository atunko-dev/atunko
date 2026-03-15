## 1. Requirements & Traceability

- [ ] 1.1 Add CLI_0005 requirement (JBang Distribution) to `docs/reqstool/requirements.yml`
- [ ] 1.2 Add SVC_CLI_0005 verification case to `docs/reqstool/software_verification_cases.yml`
- [ ] 1.3 Update subproject reqstool filters to include CLI_0005 and SVC_CLI_0005

## 2. JBang Catalog Repo

> **Note:** Tasks 2.x are in the external `atunko-dev/jbang-catalog` repo, not this repo.

- [ ] 2.1 Create `atunko-dev/jbang-catalog` GitHub repo (public, minimal)
- [ ] 2.2 Add `jbang-catalog.json` with `atunko` alias using GAV coordinates and Gradle repo
- [ ] 2.3 Add README with catalog description and usage examples

## 3. Maven Central Publishing Setup

- [ ] 3.1 Register/verify Sonatype account and claim `io.github.atunkodev` namespace in Central Portal
- [ ] 3.2 Generate GPG signing key and publish to public keyserver
- [ ] 3.3 Apply `com.vanniktech.maven.publish` plugin for Central Portal integration
- [ ] 3.4 Configure POM metadata (description, license Apache-2.0, SCM, developers)
- [ ] 3.5 Configure sources and Javadoc JAR publishing (Maven Central requirement)
- [ ] 3.6 Configure signing plugin with GPG key (in-memory for CI)
- [ ] 3.7 Add GitHub Secrets: `SIGNING_KEY`, `SIGNING_PASSWORD`, `SONATYPE_USERNAME`, `SONATYPE_PASSWORD`
- [ ] 3.8 Add GitHub Actions workflow to publish on release tag
- [ ] 3.9 Validate POM locally with `./gradlew publishToMavenLocal`

## 4. End-to-End Verification (after atunko is published)

- [ ] 4.1 Publish atunko to Maven Central (blocked on TamboUI 0.2.0 release)
- [ ] 4.2 Update catalog `script-ref` version to match published version
- [ ] 4.3 Test `jbang atunko@atunko-dev tui` launches the TUI
- [ ] 4.4 Test `jbang atunko@atunko-dev list` lists recipes
- [ ] 4.5 Test `jbang atunko@atunko-dev search spring` returns results

## 5. Documentation

- [ ] 5.1 Add JBang installation section to atunko README
