## Purpose

Provide git integration for safe recipe execution — `GitService` detects whether a project is a git repository and records a stash-based safety checkpoint (`git stash create` + `git stash store`) without touching the working tree, and `GitCheckpointService` orchestrates the `run --git-checkpoint` flow with honest outcome reporting and graceful degradation when git is unavailable, the directory is not a repository, or the tree has nothing to stash.

## Requirements

### Requirement: CORE_0006
The system SHALL implement CORE_0006.

#### Scenario: SVC_CORE_0006
The system SHALL pass SVC_CORE_0006.

### Requirement: CORE_0006.1
The system SHALL implement CORE_0006.1.

#### Scenario: SVC_CORE_0006.1
The system SHALL pass SVC_CORE_0006.1.

### Requirement: CORE_0006.2
The system SHALL implement CORE_0006.2.

#### Scenario: SVC_CORE_0006.2
The system SHALL pass SVC_CORE_0006.2.

### Requirement: CORE_0006.3
The system SHALL implement CORE_0006.3.

#### Scenario: SVC_CORE_0006.3
The system SHALL pass SVC_CORE_0006.3.
