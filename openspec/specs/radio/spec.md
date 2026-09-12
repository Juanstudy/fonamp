# Radio Specification

## Purpose

Define v1 radio: discover stations by country, genre, and tag, play in one tap, keep local favorites, and survive directory outages with a 24-hour cache and explicit offline states.

## Requirements

### Requirement 1: Discover index with counts and filter

The system MUST present a Discover index segmented into Countries and Genres/Tags, each row showing the name and station count where the directory provides it, plus a text filter narrowing the index locally.

#### Scenario: Filter countries

- GIVEN a loaded directory index with country counts
- WHEN the user types a country substring in the filter
- THEN only matching countries are shown, each still displaying its station count, with no additional network request per keystroke.

### Requirement 2: Station lists

The system MUST show station lists for the selected country, genre, or tag displaying the station name and bitrate/codec when the directory provides them, with generic icons since artwork is deferred to v2.

#### Scenario: Station row content

- GIVEN a country with stations carrying mixed metadata completeness
- WHEN its station list renders
- THEN every row shows the name, shows bitrate/codec only when available, and uses a generic icon throughout.

### Requirement 3: Three-tap playback

The system MUST let the user go from cold launch to audible station playback in at most 3 taps, with a single tap on a station row starting playback and raising the mini-player.

#### Scenario: Discover to audio in 3 taps

- GIVEN a cold start on the default tab with a cached or reachable directory
- WHEN the user taps Radio, then a country or tag, then a station
- THEN audio starts and the mini-player appears showing the station name and radio source badge.

### Requirement 4: Local-only favorites with undo

The system MUST provide one-tap favorite and unfavorite from both the station list and the player sheet, persist station favorites locally in Room across restarts with no sync or account, show them in the Favorites tab, and confirm removal with a snackbar plus undo.

#### Scenario: Favorite round-trip

- GIVEN an unfavorited station
- WHEN the user taps its heart in the list and later removes it from Favorites
- THEN the heart fills immediately, the station appears in Favorites after restart, and removal shows a snackbar whose undo restores the favorite.

#### Scenario: Favorites are stations-only

- GIVEN the Favorites tab
- WHEN its contents are inspected
- THEN only radio stations appear; no local-track favorites, sync status, or export affordance is shown in v1.

### Requirement 5: Twenty-four-hour cache with offline and retry

The system MUST cache the directory index for 24 hours, serve the cached index when offline or when fetching fails, show an explicit Offline card with retry while still displaying cached content where available, and support pull-to-refresh that reloads the directory.

#### Scenario: Offline with cache

- GIVEN a cached index younger than 24 hours and no connectivity
- WHEN Discover opens
- THEN the cached index is shown alongside an Offline card with a retry action, and tapping retry re-attempts the fetch without clearing the cache.

#### Scenario: Stale cache refresh

- GIVEN a cached index older than 24 hours with connectivity
- WHEN Discover opens or the user pulls to refresh
- THEN a fresh fetch replaces the cache and the list updates; on fetch failure the stale cache remains visible with the Offline/retry card.

### Requirement 6: Directory scope exclusions

The system MUST NOT provide tops, random, or name-search directory browsing in v1 beyond the country/genre/tag index, filter, and station lists defined above.

#### Scenario: No excluded entry points

- GIVEN the Radio tab in all its states
- WHEN navigation and actions are inspected
- THEN no tops, random-play, or station-name-search control exists.
