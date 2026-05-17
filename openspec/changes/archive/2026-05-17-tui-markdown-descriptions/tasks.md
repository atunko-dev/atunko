## 1. reqstool — Requirements & SVCs

- [x] 1.1 Add TUI_0001.20 (TUI — Markdown Recipe Descriptions) to `docs/reqstool/requirements.yml`
- [x] 1.2 Add SVC_TUI_0001.20 to `docs/reqstool/software_verification_cases.yml`

## 2. Dependency

- [x] 2.1 Add `tamboui-toolkit-markdown` to `gradle/libs.versions.toml` and `atunko-tui/build.gradle`

## 3. Implementation (TUI_0001.20)

- [x] 3.1 Extract `metadataLineCount(RecipeInfo, int)` as package-private static method in `DetailView`
- [x] 3.2 Add `@Requirements({"atunko:TUI_0001.20"})` to `DetailView` class annotation
- [x] 3.3 Replace plain-text description rendering with `markdown(description)` in `dock().center()`
- [x] 3.4 Use `metadataLineCount()` for the top panel `Constraint.length(n)`

## 4. Tests (SVC_TUI_0001.20)

- [x] 4.1 Write `DetailViewTest` covering `metadataLineCount` for leaf/composite with/without parents
- [x] 4.2 Annotate all `DetailViewTest` methods with `@SVCs({"atunko:SVC_TUI_0001.20"})`

## 5. Build & Verification

- [x] 5.1 Run `./gradlew spotlessApply` to fix formatting
- [x] 5.2 Run `./gradlew build` — all checks pass
