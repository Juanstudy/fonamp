# Source Contract Specification

## Purpose

Define the single `Source` seam that makes Fonamp expandable: one contract in `provider/api`, two v1 implementations (`LocalSource`, `RadioBrowserSource`), and a player that only ever sees `MediaItem`. Adding a source is days, not weeks.

## Requirements

### Requirement 1: Source contract shape

The system MUST define a `Source` interface in `provider/api` exposing a stable `id` (e.g. `local`, `radio-browser`), `browse`, `search`, and `streamOf` mapping an `AudioItem` to a `MediaItem`, and the player MUST consume only `MediaItem` without knowledge of the producing source.

#### Scenario: Player is source-blind

- GIVEN a local track and a radio station
- WHEN each is handed to the player
- THEN both arrive as `MediaItem` with source-identifying metadata attached, and `core/player` imports nothing from `provider/local` or `provider/radio`.

### Requirement 2: LocalSource via MediaStore

`LocalSource` with id `local` MUST enumerate device audio through MediaStore exposing songs, artists, and albums with title, artist, album, duration, and content URI, and MUST NOT require any network access or configuration.

#### Scenario: Zero-setup enumeration

- GIVEN audio permission granted and device files present
- WHEN `LocalSource.browse` is called
- THEN it returns song entries with correct metadata and playable content URIs without network calls.

### Requirement 3: RadioBrowserSource directory with resilience

`RadioBrowserSource` with id `radio-browser` MUST resolve directory servers via multi-mirror fallback, apply a 10-second network timeout, serve the country/genre/tag index and station lists from `provider/api` types, and surface typed failures (offline, timeout, server error) without throwing raw network exceptions to the UI.

#### Scenario: Mirror fallback

- GIVEN the primary radio-browser mirror is unreachable and a secondary mirror is healthy
- WHEN the directory index is requested
- THEN the source automatically retries the next mirror and returns the index, recording which mirror served it for diagnostics.

#### Scenario: Timeout surfaces as typed error

- GIVEN all mirrors stall
- WHEN a directory fetch runs
- THEN it fails within 10 seconds plus tolerance as a typed timeout/offline error the UI maps to the Offline/ErrorRetry state, never freezing the UI.

### Requirement 4: Click-count fire-and-forget

The system MUST report a click-count to radio-browser on station play as fire-and-forget that never blocks, delays, or fails playback.

#### Scenario: Click-count never blocks playback

- GIVEN the click-count endpoint is down
- WHEN the user taps a station
- THEN audio starts within the normal playback budget and the click-count failure is swallowed or logged with no user-visible error.

### Requirement 5: No download surface in v1

The system MUST NOT require `Source` implementations to provide downloads in v1; any `downloadOf`/`DownloadRequest` member MUST be absent or non-abstract so v1 sources compile without it.

#### Scenario: Source compiles without downloads

- GIVEN the v1 `Source` interface
- WHEN implementing a mock source with only `id`, `browse`, `search`, and `streamOf`
- THEN it compiles and plays without stubbing any download API.
