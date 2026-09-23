# Radio Specification

## Purpose

Define current Radio behavior: country/tag discovery, curated entry points, station-name search, one-tap playback, artwork-backed local favorites, and a 24-hour directory cache with explicit offline/retry states.

## Requirements

### Requirement 1: Discover index with local filtering

The system MUST present Discover segmented into Countries and Genres/Tags, show station counts when supplied, and narrow the selected local index as the user types without a network request per keystroke.

#### Scenario: Filter the index locally

- GIVEN a loaded Discover index
- WHEN the user enters a country or tag substring
- THEN matching rows remain visible with their counts and no request is made for each keystroke

### Requirement 2: Station lists and artwork

The system MUST show country/tag station lists with the station name, bitrate/codec only when supplied, RadioBrowser favicon artwork when supplied, and a generic fallback otherwise.

#### Scenario: Mixed station metadata

- GIVEN stations with complete and incomplete metadata
- WHEN a station list renders
- THEN every row remains usable, optional fields are omitted honestly, and missing/failed artwork uses the fallback

### Requirement 3: One-tap playback

The system MUST start playback when the user taps a playable station and raise the mini-player. Index rows and curated tag tiles MUST never be treated as playable audio.

#### Scenario: Discover to audio

- GIVEN a reachable or cached directory
- WHEN the user opens Radio, selects a country/tag, then taps a station
- THEN playback starts and the mini-player appears with the station name and radio source badge

### Requirement 4: Local favorites with undo and artwork

The system MUST provide one-tap favorite/unfavorite from normal, search-result, curated, and player radio surfaces. It MUST persist station UUID, stream URL, artwork URI, country, tags, bitrate, and codec in Room and show removal undo in Favorites.

#### Scenario: Favorite round-trip

- GIVEN an unfavorited station
- WHEN the user favorites, restarts, then removes it from Favorites
- THEN the heart updates immediately, the station and artwork survive restart, and removal offers undo

#### Scenario: Favorites are station-only

- GIVEN the Favorites tab
- WHEN its contents are inspected
- THEN only radio stations are shown; local-track favorites and account sync are not current surfaces

### Requirement 5: Twenty-four-hour cache with offline and retry

The system MUST cache the directory index and per-selection station lists for 24 hours, use fresh cache without network, and keep stale cached content visible with an Offline card and retry when refresh fails. Pull-to-refresh MUST re-fetch without clearing the cache first.

#### Scenario: Offline with cache

- GIVEN cached directory content and no connectivity
- WHEN Discover or a station list opens or refreshes
- THEN cached content remains visible with an Offline state and retry

#### Scenario: No cache and fetch failure

- GIVEN no cached directory content and an unavailable network
- WHEN Discover opens
- THEN a designed error/retry state appears instead of a blank screen

### Requirement 6: Directory scope

The current system MUST provide country/tag index browsing, station lists, local filtering, name search, and curated tiles. Tops and random browsing are not current surfaces.

#### Scenario: Excluded entry points remain absent

- GIVEN the current Radio tab
- WHEN navigation and actions are inspected
- THEN no tops or random-play control exists

### Requirement 7: Curated stations use the standard path

The system MUST expose validated curated station presets through the same RadioBrowser `AudioItem`, playback, artwork, favorite, and click-count paths as directory stations. Blank or malformed preset URLs MUST be excluded before they reach UI.

#### Scenario: Valid curated station

- GIVEN a valid curated preset
- WHEN the curated provider maps it
- THEN the resulting `AudioItem` preserves name, stream URI, optional station UUID, and `sourceId="radio-browser"`

#### Scenario: Invalid curated URL

- GIVEN a blank or non-http(s) preset URL
- WHEN curated stations are built
- THEN that entry is filtered out and no empty playable row reaches the UI

### Requirement 8: Curated Discover section

The system MUST show a Curadas section on Discover with curated rows that provide the same artwork, one-tap play, and one-tap favorite behavior as normal station rows.

#### Scenario: Curated row parity

- GIVEN a rendered curated station
- WHEN it is compared with a normal directory station
- THEN both expose the same source badge, artwork fallback, play behavior, and favorite behavior

### Requirement 9: Curated favorites survive section changes

Curated favorites MUST remain ordinary `FavoriteStation` rows even if a preset later leaves the curated list. Current artwork persistence uses Room schema version 2 with migration 1 → 2.

#### Scenario: Artwork migration

- GIVEN a version-1 favorites database
- WHEN version 2 opens
- THEN existing favorites remain available and `artworkUri` defaults safely for rows that lack it

### Requirement 10: Curated tag tiles

The system MUST provide Lofi (`lofi`), Chill (`chillout`), and Ambient (`ambient`) tiles that delegate to the normal tag browse path, including station mapping, 24-hour cache, mirror fallback, artwork, play, and favorite behavior. The current app MUST NOT maintain hand-written stream URLs for these tiles.

#### Scenario: Tag tile reuses normal browsing

- GIVEN a user taps a curated tag tile
- WHEN the station screen opens
- THEN it uses the same cached/fetched tag-station path as the Genres/Tags index

#### Scenario: No maintained tile URLs

- GIVEN the curated tile configuration
- WHEN it is inspected
- THEN tag strings are present and tile-specific stream URLs are absent

### Requirement 11: Omarchy remains out of scope

The shipped tile set MUST remain Lofi, Chill, and Ambient. Omarchy MUST NOT be added as a manual preset or tile without a maintainer-provided, functional, evidence-backed stream URL and a new approved product decision.

#### Scenario: No invented Omarchy station

- GIVEN no approved manual Omarchy URL
- WHEN curated content is built
- THEN no Omarchy row is exposed

### Requirement 12: Curated rows do not invent station UUIDs

A curated row without a valid station UUID MUST remain playable but MUST NOT expose favorite persistence or click-count behavior that requires the missing identity.

#### Scenario: Curated row without UUID

- GIVEN a valid stream URL and no station UUID
- WHEN the row is rendered
- THEN playback remains available while the favorite action is omitted rather than persisted under a fabricated UUID

### Requirement 13: Debounced server-side name search

Typing at least two characters in the Discover filter MUST filter the local index immediately and start one debounced RadioBrowser name search. Blank/one-character queries MUST make no network call. Search results MUST provide normal artwork, play, and favorite behavior, with designed empty and retry states that preserve the index.

#### Scenario: Search and local filter work together

- GIVEN Discover is open
- WHEN the user types `lofi`
- THEN the index filters locally and one debounced name search runs; short or cleared queries do not search

#### Scenario: Search failure preserves Discover

- GIVEN a name search fails while the index is loaded
- WHEN the failure surfaces
- THEN the index remains visible and retry re-runs only the current search

#### Scenario: Leaving Discover cancels pending search

- GIVEN a debounced search is pending
- WHEN the user opens a station list or leaves the current search context
- THEN the superseded work cannot render stale results into the new screen
