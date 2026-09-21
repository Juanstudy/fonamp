# Library (Local Collection) Specification

## Purpose

Define the v1 Collection tab: device music visible with zero configuration after permission, grouped by songs, artists, and albums, playing through the single shared player.

## Requirements

### Requirement 1: Zero-setup collection

The app MUST list device audio grouped by songs, artists, and albums immediately after audio permission is granted, with no accounts, servers, paths, or manual scans to configure.

#### Scenario: Fresh install to music

- GIVEN a fresh install on a device with audio files and permission granted on first entering Local
- WHEN the Collection tab opens
- THEN song, artist, and album groups are populated from MediaStore with title, artist, album, and duration shown.

### Requirement 2: Song, artist, and album browsing

The system MUST provide segmented Songs, Artists, and Albums views where selecting an artist or album filters to its songs, and tapping a song starts playback with a local queue supporting next/previous and seek.

#### Scenario: Browse and play

- GIVEN the Collection tab with multiple artists and albums
- WHEN the user opens an album and taps a song
- THEN playback starts and next/previous advance within the album/queue context.

### Requirement 3: Generic artwork and metadata honesty

The system MUST render generic icons for artwork in v1 (no fetched or embedded artwork pipeline) and MUST only display metadata fields actually present, never fabricating bitrate, year, or art.

#### Scenario: Missing metadata

- GIVEN tracks lacking embedded art or optional fields
- WHEN shown in lists, mini-player, and player sheet
- THEN a generic icon appears and absent fields are omitted rather than showing placeholders-as-data.

### Requirement 4: Empty collection state

The system MUST show a designed Empty state with a one-line explanation and a hint on how to add music when the device holds no readable audio files.

#### Scenario: No audio files

- GIVEN permission granted and zero readable audio files
- WHEN the Collection tab opens
- THEN the Empty state appears with the add-music hint instead of a blank list.

### Requirement 5: Collection search is stretch-only

The system MAY offer a simple title/artist/album text filter in v1 only if it is cheap; the v1 slice MUST be accepted with or without it, and its absence MUST NOT block release.

#### Scenario: Search optional

- GIVEN the v1 acceptance run
- WHEN collection search is present or absent
- THEN acceptance passes either way; if present, filtering by title, artist, or album narrows the visible songs without network calls.

### Requirement 6: Shuffle and repeat for local playback

The system MUST offer shuffle (randomized order) and repeat (off/all/one) for local playback from the player sheet; with both off, order is the selected queue order with next/previous only.

#### Scenario: Shuffle and repeat surfaces

- GIVEN local playback in mini-player and full sheet
- WHEN shuffle is enabled and repeat is set to all
- THEN next/previous follow the shuffled order and playback wraps at the queue end instead of stopping.
