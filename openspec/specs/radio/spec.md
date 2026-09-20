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

The system MUST NOT provide tops or random directory browsing beyond the country/genre/tag index, filter, station lists, and name search defined here.

#### Scenario: No excluded entry points

- GIVEN the Radio tab in all its states
- WHEN navigation and actions are inspected
- THEN no tops or random-play control exists.

### Requirement 7: Presets curados mapeados a AudioItem

The system MUST expose a fixed curated preset list (`CuratedStation(name, streamUrl, country?, tags?, stationUuid?)` with a provider such as `browseCurated()` or equivalent in `provider/radio`) that maps each preset to an `AudioItem` reusing the existing `toAudioItem` / `radioMediaItem` mapping, travelling as `sourceId="radio-browser"`.

#### Scenario: Preset válido mapea con identidad preservada

- GIVEN a curated preset with non-blank `name` and `streamUrl` (plus optional `country`, `tags`, `stationUuid`)
- WHEN the curated provider maps it
- THEN the resulting `AudioItem` carries the preset `name`, `streamUri` equal to the preset fixed URL, the same `sourceId="radio-browser"` as directory stations, and the preset `stationUuid` when provided.

#### Scenario: Click-count no bloquea playback de curada

- GIVEN a curated `AudioItem` with a `stationUuid`
- WHEN playback starts via the existing `streamOf` / `playStation` path
- THEN the click-count POST (if any) is fire-and-forget and audio starts even if the count request fails.

### Requirement 8: Filtrado de presets con URL vacía o inválida

The system MUST filter out any curated preset whose `streamUrl` is blank or malformed before it reaches Discover state or UI, so no curated row with empty `streamUri` is ever rendered or playable.

#### Scenario: URL vacía se filtra

- GIVEN a preset list containing one entry with an empty/blank `streamUrl` and one valid entry
- WHEN `browseCurated()` executes
- THEN the output list contains only the valid entry, and a unit test asserts the invalid entry is excluded.

#### Scenario: URL malformada se filtra

- GIVEN a preset whose `streamUrl` is not a parseable http(s) URL
- WHEN the curated provider validates it
- THEN the preset is excluded from the exposed `curated` state and no crash or empty player action occurs.

### Requirement 9: Sección "Curadas" con play + heart idénticos

The system MUST present a "Curadas" section on the initial Radio Discover screen (`RadioViewModel` exposes `curated` state; `RadioScreens` renders the section) where each curated row offers the same one-tap play and one-tap heart as normal station rows, reusing `playStation` / `toggleFavorite`, the same mini-player, and the same source badge.

#### Scenario: Curada visible en ≤2 taps

- GIVEN a cold start on the Radio tab with the curated section configured
- WHEN the user taps Radio (tap 1) and then a curated station row (tap 2)
- THEN audio starts and the mini-player appears, identical to a normal station play.

#### Scenario: Play + heart paridad

- GIVEN a rendered curated row
- WHEN inspected against a normal directory station row
- THEN both expose play on row tap and a heart toggle, and favoriting the curada fills the heart immediately with the same visual treatment.

### Requirement 10: Favoritas de curadas sin migración

The system MUST persist favorites of curated stations through the existing `FavoriteStation` Room entity with no database migration, showing them in the Favorites tab with the same undo-snackbar behavior as Requirement 4, and surviving removal of the curated section as still-valid favorite rows.

#### Scenario: Favorita curada persiste across restart con undo

- GIVEN an unfavorited curated station
- WHEN the user taps its heart, restarts the app, then removes it from Favorites
- THEN the heart fills immediately, the curada appears in Favorites after restart, and removal shows a snackbar whose undo restores the favorite.

#### Scenario: Sin migración de base de datos

- GIVEN the app database schema before this change
- WHEN the change is applied and `./gradlew test` plus Room schema inspection run
- THEN no new entity, no altered table, and no migration block exists; favorited curadas are plain `FavoriteStation` rows.

### Requirement 11: Tiles por tag precableados con path existente

The system MUST provide "Lofi" / "Chill" / "Ambient" tiles (exact tag strings fixed at apply time, defaults `lofi`, `chillout`, `ambient`) on the Discover screen, each tile delegating to the existing `browse(BrowseQuery(tag=...))` path including `StationQuery` mapping, the 24-hour `DirectoryCache`, and `withMirrorFallback`, with zero maintained stream URLs.

#### Scenario: Tile reutiliza browse(tag) + cache 24h

- GIVEN the "Lofi" tile configured with the apply-confirmed tag string
- WHEN the user taps the tile
- THEN the app calls the same `browse(BrowseQuery(tag=...))` used by the Genres/Tags index, serves the 24h-cached result when fresh, fetches through the mirror fallback on miss/stale, and renders the standard station list.

#### Scenario: Cero URLs mantenidas para tiles

- GIVEN the tile configuration
- WHEN the codebase is inspected
- THEN no hardcoded `streamUrl` exists for any tag tile; only the tag string is pinned.

### Requirement 12: Decisión omarchy/lofi por curl como criterio de apply

The system MUST resolve the exact tile tag list and the omarchy inclusion exclusively through the apply-time curl evidence defined in the proposal (run against `https://de1.api.radio-browser.info` with local network), and the PR MUST attach that evidence: a tag becomes a tile only with ≥3 stable streams (https, MP3/AAC, high votes) from `bytag/<tag>`; `omarchy` becomes a manual preset only with a maintainer-provided functional URL, otherwise it is declared out of scope with the empty `byname/omarchy` + `bytag/omarchy` output attached. No stream URL SHALL be invented without evidence.

#### Scenario: Tile aceptado por evidencia

- GIVEN the apply-time output of `curl "$BASE/json/stations/bytag/lofi?order=votes&reverse=true&limit=5"` (and variant tags `lo-fi` / `chill` / `chillout` / `ambient` as needed)
- WHEN the tile list is finalized
- THEN each pinned tile tag has ≥3 rows with https `url_resolved`, playable codec, and high votes visible in the attached output, and the chosen variant is the one with the most votes/streams.

#### Scenario: Omarchy con URL o fuera de alcance con evidencia

- GIVEN the apply-time outputs of `curl "$BASE/json/stations/byname/omarchy?limit=5"` and `curl "$BASE/json/stations/bytag/omarchy?limit=5"`
- WHEN omarchy handling is decided
- THEN either (a) a manual preset exists whose `streamUrl` is the provided, curl- or playback-verified functional URL, or (b) both omarchy queries returned 0 rows and the PR contains that output with omarchy explicitly declared out of scope and no omarchy preset in code.

#### Scenario: Sin name-search cableado en la app

- GIVEN the Radio tab in all its states after this change
- WHEN navigation and actions are inspected
- THEN no separate station-name-search screen exists (Requirement 13 wires search into the existing filter field instead); `byname/omarchy` was used only as apply-time curl verification, not wired into the app.

### Requirement 13: Server-side name search integrated in the Discover filter

The system MUST offer directory name search wired into the existing Discover filter field (`RadioViewModel.setQuery`): typing at least 2 characters fires a debounced (~400ms) `byname` query reusing mirror fallback, 10s timeout, and typed errors; a blank/short query performs zero network; hits render as a "Search results" section below the index with the same one-tap play and one-tap heart as normal station rows; an empty result renders a designed Empty state; a failed search renders retry without clearing the index; navigating away cancels any pending search.

#### Scenario: Debounced search with local filter intact

- GIVEN the Discover screen with a loaded index
- WHEN the user types "lofi" in the filter field
- THEN the index narrows locally with zero source calls per keystroke AND, after the debounce, one `byname` directory call fires and its hits appear in the "Search results" section.

#### Scenario: Short query costs zero network

- GIVEN the Discover screen
- WHEN the query is blank or a single character
- THEN no directory call is made and no search section is shown.

#### Scenario: Play + heart parity

- GIVEN rendered search results
- WHEN a result row is tapped and its heart is toggled
- THEN audio starts with the mini-player raised and the station persists as a favorite with the same undo-snackbar behavior as Requirement 4.

#### Scenario: Failure keeps the index

- GIVEN a directory search that fails (offline/timeout/server)
- WHEN the failure surfaces
- THEN the index stays visible and the search section shows retry; tapping retry re-fires only the search.
