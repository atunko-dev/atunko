## Context

The TUI detail view renders a full-screen panel for the highlighted recipe. The description
field, authored as markdown by OpenRewrite recipe maintainers, was displayed as raw text.
The `tamboui-toolkit-markdown` toolkit library provides a `markdown()` element that renders
CommonMark inside a TUI dock slot.

## Goals / Non-Goals

**Goals:**
- Render `recipe.description()` as formatted markdown in the centre of the detail panel
- Extract a testable `metadataLineCount()` helper to fix the top-panel height so the
  markdown region gets a stable centre slot

**Non-Goals:**
- Custom markdown extensions (code highlighting, tables)
- Fallback rendering for terminals without ANSI support (handled by TamboUI)

## Decisions

### Use `tamboui-toolkit-markdown`

**Rationale:** The dependency is provided by TamboUI's own toolkit — it is the idiomatic
choice for the framework, avoids pulling in a separate CommonMark library, and keeps the
rendering behaviour consistent with any future TamboUI-rendered markdown in the app.

### Extract `metadataLineCount()` as a package-private static method

**Rationale:** The top panel must have a fixed `Constraint.length(n)` so the markdown
element's `dock().center()` slot has room. Extracting the calculation as a static method
makes it independently testable without spinning up the full TUI.

## Risks / Trade-offs

- **TamboUI markdown dependency**: If TamboUI drops or renames the module the import
  breaks. Low risk — the toolkit is maintained in lock-step with the core library.
