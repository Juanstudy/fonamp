# Player Specification

## Purpose

Define the single Media3 background player shared by every source: continuous playback with the screen off, notification and headset control, honest stream error handling, and one consistent mini-player plus full sheet.

## Requirements

### Requirement 1: Single player via MediaSessionService

The system MUST route all v1 audio (local files and radio streams) through one Media3/ExoPlayer instance hosted in a `MediaSessionService`, and every source MUST reach it only as a `MediaItem`.

#### Scenario: One instance for all sources

- GIVEN a local song playing
- WHEN the user taps a radio station and then another local song
- THEN the same player session transitions between all three without spawning a second player, notification, or service.

### Requirement 2: Background playback and notification

The system MUST continue audio with the screen off and while other tabs or apps are in use, keep the foreground service alive only while playing, and expose a media notification with play/pause/next/previous that stays in sync with player state, plus headset-button handling.

#### Scenario: Screen-off continuity

- GIVEN a station or song playing
- WHEN the screen is turned off and then the notification pause and play buttons are pressed
- THEN audio pauses and resumes accordingly, the notification reflects the current state, and the service does not persist after playback is stopped.

### Requirement 3: Per-source playback model

The system MUST offer radio as play/stop with ICY metadata display when the stream provides it, and local as queue playback with next/previous plus seek bar, and MUST NOT offer shuffle, repeat, speed, or sleep-timer controls in v1.

#### Scenario: Radio versus local controls

- GIVEN the full player sheet for a radio station showing ICY metadata when available
- WHEN compared with the sheet for a local song
- THEN the radio sheet shows play/stop and metadata without a seek bar, the local sheet shows next/previous and a working seek bar, and neither sheet shows shuffle, repeat, speed, or sleep controls.

### Requirement 4: Stream failure handling

The system MUST time out stalled streams at 10 seconds, show a visible inline error with a manual retry action, keep the UI responsive throughout, and never freeze, crash, or silently stall on failure.

#### Scenario: Dead stream

- GIVEN a station whose stream never connects
- WHEN playback is attempted
- THEN within 10 seconds plus tolerance an inline error banner with retry appears in the player, the rest of the UI remains interactive, and tapping retry re-attempts the stream.

#### Scenario: Mid-stream network loss

- GIVEN a playing stream that loses connectivity
- WHEN the drop persists
- THEN playback enters the visible error state with retry rather than crashing or spinning forever.

### Requirement 5: Mini-player and full sheet

The system MUST show a mini-player (icon, title, play/pause, close) above the bottom tabs on every tab during playback or pause, expand it to a full sheet on tap showing title, artist or station, source badge (`radio` or `local`), ICY metadata for radio, queue controls for local, a favorite toggle, and the error banner with retry when failing.

#### Scenario: Consistent player chrome

- GIVEN playback started from Radio and separately from Collection
- WHEN the mini-player and expanded sheet are inspected on each tab
- THEN both show the same layout contract with the correct source badge and source-appropriate controls and metadata.

### Requirement 6: Best-effort queue restore

The system MUST attempt best-effort restoration of the queue and position after process death, with no guarantee, and MUST always land in a coherent state (restored queue or clean idle) rather than a broken or phantom-playing UI.

#### Scenario: Process death

- GIVEN a local queue mid-album and a killed process
- WHEN the app restarts
- THEN it either resumes the queue context or shows the idle player chrome, never a stuck progress bar or controls for audio that is not playing.
