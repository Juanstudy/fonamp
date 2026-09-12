# Settings Specification

## Purpose

Define the v1 Settings tab: the first customization slice (theme) plus cache, storage, and about transparency, without opening any v2+ customization surface.

## Requirements

### Requirement 1: Theme selection

The system MUST offer theme choices System, Light, and Dark, persist the choice across restarts, and apply it without requiring an app restart, satisfying the v1 theme customization scope on Material3 defaults.

#### Scenario: Theme round-trip

- GIVEN the default System theme
- WHEN the user selects Dark, restarts the app, then selects Light
- THEN each selection applies immediately across all tabs and the player, and the choice survives the restart.

### Requirement 2: Cache and storage transparency

The system MUST show directory-cache and storage usage and provide a clear-directory-cache action that frees the cached bytes and confirms completion.

#### Scenario: Clear cache

- GIVEN a populated directory cache with reported size
- WHEN the user clears it
- THEN the reported usage drops accordingly and a confirmation is shown; the next Discover visit refetches as needed.

### Requirement 3: About information

The system MUST display the app version, open-source licenses, and source reference in Settings.

#### Scenario: About visible

- GIVEN the Settings tab
- WHEN scrolled to About
- THEN version, licenses entry, and source link are all present and readable.

### Requirement 4: No advanced customization in v1

The system MUST NOT expose tab order or visibility editing, parametric EQ or presets, sleep timer, or playback speed controls anywhere in v1.

#### Scenario: No v2 surfaces

- GIVEN the Settings tab and player sheet
- WHEN all actions and controls are inspected
- THEN no tab editor, EQ, sleep timer, or speed control exists.
