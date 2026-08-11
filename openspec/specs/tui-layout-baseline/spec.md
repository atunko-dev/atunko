## Purpose

Make the TUI's consistency structural rather than remembered. Every screen implements one `TuiView` contract — declaring its title, status, key hints and help sections — and is drawn by `TuiShell` inside a single frame: a fixed three-row header carrying the title and a tab bar, a filling content region, a collapsible details pane, and a two-row footer separating session state from key hints. Key hints render as emphasised key plus dim label and change with the active mode. Tabs address screens and carry live counts rather than encoding a sort order, which moved to the status row beside the other filters. Help and diff draw over the current screen instead of replacing it, the footer shows the overlay's own hints while it is open, and the dismissing keystroke still reaches the screen beneath. `Tab`/`Shift-Tab` cycle focus between the list and details panes, indicated by a `:focus` rule in both themes. Every binding is declared once in `AtunkoBindings`, built on TamboUI's vim-free `BindingSets.standard()`, and both the footer hints and the help screen derive from that one declaration — a test asserts the documented key tables agree with it, so the drift that had README documenting an unbound key cannot recur.

## Requirements

### Requirement: TUI_0009
The system SHALL implement TUI_0009.

#### Scenario: SVC_TUI_0009
The system SHALL pass SVC_TUI_0009.

### Requirement: TUI_0009.1
The system SHALL implement TUI_0009.1.

#### Scenario: SVC_TUI_0009.1
The system SHALL pass SVC_TUI_0009.1.

### Requirement: TUI_0009.2
The system SHALL implement TUI_0009.2.

#### Scenario: SVC_TUI_0009.2
The system SHALL pass SVC_TUI_0009.2.

### Requirement: TUI_0009.3
The system SHALL implement TUI_0009.3.

#### Scenario: SVC_TUI_0009.3
The system SHALL pass SVC_TUI_0009.3.

### Requirement: TUI_0009.4
The system SHALL implement TUI_0009.4.

#### Scenario: SVC_TUI_0009.4
The system SHALL pass SVC_TUI_0009.4.

### Requirement: TUI_0009.5
The system SHALL implement TUI_0009.5.

#### Scenario: SVC_TUI_0009.5
The system SHALL pass SVC_TUI_0009.5.

### Requirement: TUI_0009.6
The system SHALL implement TUI_0009.6.

#### Scenario: SVC_TUI_0009.6
The system SHALL pass SVC_TUI_0009.6.

### Requirement: TUI_0009.7
The system SHALL implement TUI_0009.7.

#### Scenario: SVC_TUI_0009.7
The system SHALL pass SVC_TUI_0009.7.
