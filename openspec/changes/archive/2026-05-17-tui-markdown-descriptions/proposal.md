## Why

Recipe descriptions in the TUI detail view were displayed as plain text, losing all
formatting that recipe authors intended (bold, code spans, lists). The `tamboui-toolkit-markdown`
library makes it straightforward to render markdown natively in TUI panels.

## What Changes

- Add `tamboui-toolkit-markdown` dependency to `atunko-tui`
- Refactor `DetailView` to extract `metadataLineCount()` helper so the top panel has a
  known height, leaving the centre region for the markdown-rendered description
- Render the description via `markdown(description)` in a `dock().center()` slot

## Capabilities

### New Capabilities

- `tui-markdown-descriptions`: The TUI detail view renders recipe descriptions as formatted
  markdown (TUI_0001.20)

### Modified Capabilities

- `tui-launch`: The detail view layout is restructured to use a fixed-height metadata top
  panel and a markdown description centre panel

## Impact

- `atunko-tui`: `DetailView.java` (refactored + markdown), `DetailViewTest.java` (new tests
  for `metadataLineCount`), `build.gradle` (+1 dependency), `gradle/libs.versions.toml` (+1 entry)
- No breaking changes — purely visual enhancement within the TUI detail screen
