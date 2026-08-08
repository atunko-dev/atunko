## Why

atunko pins TamboUI 0.3.0; 0.4.0 is the latest release (2026-06-18). The release brings
keyboard/bindings reliability fixes directly relevant to atunko (`Builder.bindings()`
propagation, Shift+F1–F4 parsing, Windows arrow-key fix in the Panama backend), a
`ScrollableElement` container, panel title alignment, and new sparkline widgets that may
be useful for future TUI features. No breaking API changes are advertised; the only
rename (`BrailleGridFrames` → `BraillePatterns`) is not used by atunko.

Verified before upgrading: the 0.4.0 test-fixtures artifacts (toolkit/tui/core) are
published on Maven Central, and 0.4.0's `TestBackend.draw()` is still a no-op — the
`PilotTestSupport` frame-capture harness remains necessary. 0.4.0's `EventRouter` still
routes only left-button mouse presses, so the right-click limitation documented in the
`tui-headless-testing` change is unchanged.

## What Changes

- `gradle/libs.versions.toml`: `tamboui = "0.3.0"` → `"0.4.0"` (single version bump; all
  TamboUI modules resolve through the BOM)
- No source changes expected; verified by full build and the complete test suite,
  including the headless Pilot end-to-end tests from `tui-headless-testing`

## Capabilities

No capability changes — dependency maintenance only.

## Impact

- All modules consuming TamboUI (`atunko-tui`, transitively `atunko-cli`)
- No reqstool changes; existing SVCs re-verified by the test suite
