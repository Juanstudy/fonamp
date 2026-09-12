# Permissions Specification

## Purpose

Define the v1 runtime-permission posture: minimal permissions requested on demand with a justification at request time, and designed states for every grant outcome.

## Requirements

### Requirement 1: Minimal permission set

The system MUST request at most audio-read access, internet, foreground-service playback, and notifications, and MUST NOT request location, contacts, storage-write, or any other runtime permission in v1.

#### Scenario: Permission audit

- GIVEN the merged manifest and a fresh install
- WHEN the requested permissions are listed
- THEN only the audio, internet, foreground-playback, and notification set appears, and the app functions without any additional grant.

### Requirement 2: On-demand audio permission with inline justification

The system MUST request audio-read permission only on first entering Local (using `READ_MEDIA_AUDIO` on API 33+ and `READ_EXTERNAL_STORAGE` scoped to audio on API 29–32), explain inline why access is needed at request time, and MUST NOT gate the rest of the app behind a pre-permission wall.

#### Scenario: First-run Local flow

- GIVEN a fresh install where permission has never been asked
- WHEN the user opens Collection
- THEN the system permission sheet appears with the inline why-needed text, and Radio, Favorites, and Settings remain usable regardless of the outcome.

### Requirement 3: Denied and permanently-denied states

The system MUST show a designed Denied state with the why-needed explanation and a grant-again action when audio permission is denied, and MUST direct the user to system settings without a crash or blank screen when the denial is permanent.

#### Scenario: Denied then re-granted

- GIVEN audio permission denied on first ask
- WHEN Collection is shown and the user taps grant again and accepts
- THEN the Denied state explains the need, the re-request fires, and the collection list populates after the grant.

#### Scenario: Permanently denied

- GIVEN audio permission permanently denied (don't-ask-again)
- WHEN Collection opens
- THEN the Denied state offers the system-settings path and the rest of the app continues to work with no broken screen.

### Requirement 4: Privacy posture

The system MUST include zero third-party trackers, keep favorites, theme, and cache on-device, and send no data off-device except directory API calls (radio-browser with mirrors and click-count) and user-initiated stream fetches, using cleartext HTTP only where third-party radio streams require it.

#### Scenario: Network egress check

- GIVEN the app exercising Collection, Discover, playback, and favorites
- WHEN outbound traffic is captured
- THEN destinations are limited to radio-browser mirrors, click-count, and the chosen audio streams, with no analytics, ads, or crash-reporter endpoints contacted.
