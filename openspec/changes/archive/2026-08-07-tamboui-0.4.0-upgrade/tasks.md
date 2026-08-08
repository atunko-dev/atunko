## 1. Upgrade

- [x] 1.1 Bump `tamboui` to `0.4.0` in `gradle/libs.versions.toml`

## 2. Verification

- [x] 2.1 `./gradlew build` — compiles against 0.4.0, no Checkstyle/Error Prone violations
- [x] 2.2 `./gradlew test` — all module tests pass, including `AtunkoTuiPilotTest`
  headless end-to-end journeys (run 3× to check for timing regressions)
- [x] 2.3 Manual smoke check not required — the Pilot tests cover launch, navigation,
  search, selection, mouse, and overlay rendering end-to-end
