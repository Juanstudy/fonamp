# Delta for Radio

## ADDED Requirements

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
- THEN no station-name-search control exists (Requirement 6 intact); `byname/omarchy` was used only as apply-time curl verification, not wired into the app.
