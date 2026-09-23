# Library (Local Collection) Specification

## Purpose

Define the current Collection tab: MediaStore music available after on-demand permission, grouped by songs, artists, and albums, with local search, artwork, and queue playback through the shared player.

## Requirements

### Requirement 1: Zero-setup collection

The app MUST list device audio grouped by songs, artists, and albums immediately after audio permission is granted, without accounts, servers, paths, or manual scan configuration.

#### Scenario: Fresh install to music

- GIVEN a fresh install with audio files and permission granted
- WHEN Collection opens
- THEN song, artist, and album groups are populated from MediaStore

### Requirement 2: Song, artist, and album browsing

The system MUST provide Songs, Artists, and Albums views where selecting an artist or album filters to its songs, and tapping a song starts the visible local queue at that item.

#### Scenario: Browse and play

- GIVEN multiple artists and albums
- WHEN the user opens an album and taps a song
- THEN playback starts at the selected item and previous/next operate on the visible queue context

### Requirement 3: Local search

The system MUST filter the current collection by title, artist, or album without network calls and MUST preserve the active artist/album context while applying the text query.

#### Scenario: Local search

- GIVEN a populated Collection
- WHEN the user enters a title, artist, or album substring
- THEN only matching songs in the current context remain visible

### Requirement 4: Artwork and metadata honesty

The system MUST load MediaStore album artwork through Coil in list, mini-player, and player surfaces, show a generic fallback when art is absent or fails, and omit unavailable optional metadata rather than fabricating it.

#### Scenario: Artwork missing or invalid

- GIVEN a track without an album-art URI or an image that fails to load
- WHEN it is shown
- THEN a generic icon appears and playback/metadata remain usable

### Requirement 5: Empty and denied states

The system MUST show designed Empty and Denied states with explanations and useful next actions when no readable audio is available or permission is not granted.

#### Scenario: No audio files

- GIVEN permission is granted and no readable audio exists
- WHEN Collection opens
- THEN an Empty state explains how to add music instead of rendering a blank list

### Requirement 6: Player controls for local playback

The player sheet MUST expose seek, previous/next, shuffle, repeat, playback speed, sleep timer, and a visible Up next section for a non-empty local queue.

#### Scenario: Local queue controls

- GIVEN a multi-song local queue
- WHEN the user enables shuffle and repeat-all, or selects an Up next row
- THEN playback follows the selected mode and row selection jumps within the current queue context
