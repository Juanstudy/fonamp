# Settings Specification

## Purpose

Define the current Settings surface: persisted theme selection, directory-cache transparency, About information, and GitHub release update checks/downloads without opening roadmap-only customization surfaces.

## Requirements

### Requirement 1: Theme selection

The system MUST offer System, Light, and Dark themes, persist the choice in Room across restarts, and apply it without requiring an app restart.

#### Scenario: Theme round-trip

- GIVEN the System theme
- WHEN the user selects Dark, restarts the app, then selects Light
- THEN each selection applies across the app immediately and the latest choice survives restart

### Requirement 2: Directory-cache transparency

The system MUST report directory-cache entry count and byte size and provide a confirmed clear action that removes cached directory data without clearing Room favorites or theme data.

#### Scenario: Clear directory cache

- GIVEN a populated directory cache
- WHEN the user confirms clear
- THEN the reported cache size drops, freed bytes are confirmed, and the next Discover visit refetches as needed

#### Scenario: Scope is explicit

- GIVEN favorites and theme exist in Room
- WHEN the user clears the directory cache
- THEN favorites and theme remain available

### Requirement 3: About and update information

The system MUST display the `BuildConfig` version, a license pointer, a source summary, and a manual update action. The app shell MUST also run a cold-start update check. A newer GitHub release with an APK MUST offer separate Download and Later actions; download/install work MUST remain owned by `app`.

#### Scenario: About visible

- GIVEN Settings is open
- WHEN the About section is inspected
- THEN the current app version, license pointer, source summary, and `Check for updates` action are present

#### Scenario: Update available

- GIVEN GitHub Releases contains a newer version with an APK asset
- WHEN the user accepts Download
- THEN the app starts an APK download and later notifies the user when installation is available; the Android system installer remains responsible for final user confirmation

#### Scenario: No newer release

- GIVEN the installed version is current or the release has no installable APK
- WHEN a check completes
- THEN the app stays coherent and does not present a broken update action

### Requirement 4: No roadmap-only customization

The system MUST NOT expose tab order/visibility editing or parametric EQ. Sleep timer and playback speed belong in the player sheet, not Settings.

#### Scenario: Current surfaces remain scoped

- GIVEN Settings and the player sheet
- WHEN all controls are inspected
- THEN no tab editor or EQ surface exists, while the shipped sleep and speed controls remain in the player sheet
