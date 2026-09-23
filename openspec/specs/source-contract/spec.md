# Source Contract Specification

## Purpose

Define the shared source seam used by current fonamp: two shipped implementations (`LocalSource` and `RadioBrowserSource`), typed source outcomes, and a player that receives only Media3 `MediaItem` values.

## Requirements

### Requirement 1: Source contract shape

The system MUST define a `Source` interface in `provider/api` with a stable `id`, a `SourceKind`, typed `browse` and `search` operations returning `SourceResult`, and `streamOf` mapping an `AudioItem` to `MediaItem`. `core/player` MUST consume only `MediaItem` and MUST NOT import concrete providers.

#### Scenario: Player is source-blind

- GIVEN a local track and a radio station
- WHEN each source maps its item and hands it to the player
- THEN playback receives `MediaItem` values with source metadata and `core/player` has no dependency on `provider/local` or `provider/radio`

### Requirement 2: LocalSource via MediaStore

`LocalSource` with id `local` MUST enumerate device audio through MediaStore without network access or user configuration, preserving title, artist, album, duration, content URI, and optional album-art URI. Its search MUST filter title, artist, and album locally.

#### Scenario: Zero-setup enumeration and search

- GIVEN audio permission and files on the device
- WHEN the source browses or searches MediaStore
- THEN matching items include honest optional metadata and playable content URIs without network calls

### Requirement 3: RadioBrowserSource directory with resilience

`RadioBrowserSource` with id `radio-browser` MUST use configured RadioBrowser mirrors, apply the current network timeout, provide country/tag index and station-list browsing, provide server-side name search, and map failures to typed `SourceError` values rather than raw exceptions.

#### Scenario: Mirror fallback

- GIVEN the primary mirror is unreachable and a secondary mirror is healthy
- WHEN a directory request runs
- THEN the request uses a healthy mirror and returns the requested index or stations

#### Scenario: Failure is typed

- GIVEN all mirrors are unavailable or time out
- WHEN a directory operation runs
- THEN the source returns a typed offline, timeout, server, or unknown error and does not throw a raw network exception into feature UI

### Requirement 4: Click-count fire-and-forget

The system MUST report station click counts asynchronously so that analytics failure never blocks, delays, or fails playback.

#### Scenario: Click-count never blocks playback

- GIVEN the click-count endpoint is unavailable
- WHEN the user starts a station
- THEN playback starts through the normal path and the click-count failure is swallowed or logged without a user-visible error

### Requirement 5: No download surface in the current contract

The current `Source` interface MUST NOT require a download method. Downloads remain a planned product area and cannot block current source implementations.

#### Scenario: Current source compiles without downloads

- GIVEN a new source implements `id`, `kind`, `browse`, `search`, and `streamOf`
- WHEN it is registered in the app graph
- THEN it builds and plays without stubbing any download API
