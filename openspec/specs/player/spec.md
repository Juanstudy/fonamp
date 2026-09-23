# Player Specification

## Purpose

Define the single Media3 background player shared by current sources: screen-off playback, notification controls, source-appropriate transport behavior, visible queue context, recovery states, and best-effort restore.

## Requirements

### Requirement 1: Single player via MediaSessionService

The system MUST route current local files and radio streams through one Media3/ExoPlayer session hosted by a `MediaSessionService`, and every source MUST reach it only as `MediaItem`.

#### Scenario: One instance for all sources

- GIVEN local audio playing
- WHEN the user starts radio and then local audio
- THEN the same session transitions without a second player, notification, or service

### Requirement 2: Background playback and notification

The system MUST continue audio with the screen off and expose a media notification with synchronized play/pause and source-appropriate next/previous controls.

#### Scenario: Screen-off continuity

- GIVEN a station or song playing
- WHEN the screen is turned off and notification controls are used
- THEN playback state and notification state remain synchronized

### Requirement 3: Source-appropriate playback model

Radio MUST use stop semantics for a playing live stream and MUST NOT show a seek bar. Local audio MUST expose pause/resume, seek, previous/next, and queue context. Both sources MUST expose shuffle, repeat off/all/one, playback speed, and a one-shot sleep timer.

#### Scenario: Radio versus local controls

- GIVEN radio and local sheets
- WHEN their controls are compared
- THEN radio omits seek and uses stop, local exposes seek/previous/next, and both expose the shared transport modes and sleep timer

### Requirement 4: Stream failure handling

The system MUST map playback failures to visible inline error states with manual retry and MUST keep the rest of the UI responsive. A separately enforced 10-second stalled-stream deadline is not part of the current verified requirement.

#### Scenario: Failed stream

- GIVEN a station that cannot connect or loses connectivity
- WHEN the failure reaches the player
- THEN an inline retry action appears and the rest of the UI remains interactive

#### Scenario: Retry

- GIVEN the current queue and visible error
- WHEN the user taps retry
- THEN the current item is prepared and started again without a blank player state

### Requirement 5: Mini-player and full sheet

The system MUST show a mini-player with artwork, title, source, play/pause, and close across tabs while queue context is present. Tapping it MUST open a full sheet with metadata, source badge, ICY title when available, favorite support for radio, source-appropriate transport, progress where valid, and error/retry where needed. Closing the chrome MUST NOT clear the queue.

#### Scenario: Consistent player chrome

- GIVEN playback started from Radio and Collection
- WHEN mini-player and sheet are inspected
- THEN both show the shared contract with correct source and available artwork/metadata

### Requirement 6: Visible Up next queue

The player sheet MUST list the remaining non-empty queue context and allow a user to select a row to jump within the current queue without replacing its sleep timer or error context.

#### Scenario: Select Up next row

- GIVEN a multi-item local queue
- WHEN the user taps a visible Up next row
- THEN playback jumps to that item and remains in the same queue context

### Requirement 7: Sleep timer

The player MUST offer 5, 10, 15, 30, 45, and 60 minute presets plus Off. Arming MUST be one-shot: when the deadline expires, playback stops without auto-resuming; starting a new queue or manually stopping clears the timer.

#### Scenario: Timer expires

- GIVEN an armed sleep timer
- WHEN its deadline arrives
- THEN playback stops and the queue remains available without automatic playback

### Requirement 8: Best-effort queue restore

The system MUST persist and attempt to restore queue, index, and local position after process death. Restored context MUST start paused; the system MUST land in either coherent restored context or clean idle state, never phantom-playing.

#### Scenario: Process death

- GIVEN a local queue and saved position before the process is killed
- WHEN the app restarts
- THEN a valid queue is restored paused at a bounded position, or the app presents a coherent idle player

#### Scenario: Invalid restore input

- GIVEN an empty or out-of-range persisted snapshot
- WHEN restore runs
- THEN the invalid context is ignored and the player does not expose stuck controls or progress
