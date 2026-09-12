# Tasks — fonamp-foundation (v1: foundation + local + radio)

> Test runner: `./gradlew test` · Env: JDK 17 Corretto, ANDROID_HOME ~/Android/Sdk · No code in this phase — tasks only.
> Convention: strict TDD where a JVM runner exists — every slice is a RED (failing test) → GREEN (minimal implementation) pair. Verify each GREEN with `./gradlew test` (and `assembleDebug` where noted).

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~4500–6500 (greenfield: 12 modules + CI + tests) |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR1 scaffold+CI+provider/api → PR2 core/ui+database+permissions → PR3 network+radio source → PR4 local source+library → PR5 player+service → PR6 radio UI+favorites → PR7 settings+app wiring+acceptance |
| Delivery strategy | ask-on-risk |
| Chain strategy | pending |
| Decision needed before apply | Yes — confirm chain strategy (stacked-to-main vs feature-branch-chain) before apply; v1 far exceeds single-PR budget |

```text
Decision needed before apply: Yes
Chained PRs recommended: Yes
Chain strategy: pending
400-line budget risk: High
```

## Slice A — Toolchain, scaffold, CI (scaffold Req 1, 3, 6)

- [x] RED: Add toolchain presence checks — assert `settings.gradle.kts` includes all 12 modules, `gradle/libs.versions.toml` pins single versions, no `isMinifyEnabled=true`, manifest declares minSdk 29 / target+compile 35 (`app/build.gradle.kts`, `gradle/libs.versions.toml`, `AndroidManifest.xml` as discovery targets). <!-- sdd-owner: implementation -->
- [x] GREEN: Create Gradle scaffold — `settings.gradle.kts` (12 modules), root `build.gradle.kts` (AGP 8.7 lineage, Kotlin, Hilt, KSP), `gradle/libs.versions.toml` (AGP/Kotlin/Compose BOM/Media3 VERIFY-AT-APPLY default 1.10.1 pinned identically/Retrofit/OkHttp/Room/Hilt/serialization/Coroutines/JUnit/Robolectric/Turbine/MockWebServer), per-module `build.gradle.kts` with `namespace`, Compose enabled in `core/ui`, `feature/*`, `app`; run `./gradlew test` then `assembleDebug` green. <!-- sdd-owner: implementation -->
- [x] RED: Add CI gate tests — dependency-audit script test (scan merged deps for `ads|admob|firebase|analytics|crashlytics` → zero matches), APK size assertion (<40 MB), manifest permission allowlist test (`.github/workflows/android.yml` + audit script as targets). <!-- sdd-owner: implementation -->
- [x] GREEN: Implement `.github/workflows/android.yml` (JDK 17 Corretto → `./gradlew test` → `assembleDebug` → size + dep-audit + permission-audit steps); verify `./gradlew test` green. <!-- sdd-owner: implementation -->

## Slice B — `provider/api` Source contract (source-contract Req 1, 5)

- [x] RED: Write JVM unit tests for `provider/api` — `SourceExtras` const keys (`source_id`, `stable_id`, `is_live`, `station_uuid`, `country`), `AudioItem`/`BrowseQuery`/`SourceError`/`SourceResult` shape, mock `Source` with only `id/browse/search/streamOf` compiles without downloads, `streamOf` local/radio MediaItem mapping expectations (Robolectric for MediaItem). Files: `provider/api/src/test/.../SourceContractTest.kt`. <!-- sdd-owner: implementation -->
- [x] GREEN: Implement `provider/api` pure-Kotlin module — `SourceKind`, `AudioItem`, `BrowseQuery`, `SourceError`, `SourceResult`, `Source` interface (no `downloadOf` abstract), `SourceExtras` keys, local/radio MediaItem mapper helpers; zero Android imports except Media3 `MediaItem`; verify `./gradlew :provider:api:test`. <!-- sdd-owner: implementation -->

## Slice C — `core/ui` design system + shared states (scaffold Req 4, 5, 7)

- [x] RED: Write Robolectric Compose behavior tests for shared states — `Loading` (shimmer rows, never blank), `Empty`, `Offline` (card+retry), `Denied` (why+grant/settings), `ErrorRetry`, `MiniPlayer` (48dp targets, content descriptions), `PlayerSheet` skeleton, `SourceBadge(radio|local)`; M3 light/dark snapshot of each. Files: `core/ui/src/test/.../UiStatesTest.kt`. <!-- sdd-owner: implementation -->
- [x] GREEN: Implement `core/ui` — `FonampTheme` (M3 defaults, no custom tokens), `Loading/Empty/Offline/Denied/ErrorRetry`, `MiniPlayer`, `PlayerSheet`, `SourceBadge`; verify `./gradlew :core:ui:test`. <!-- sdd-owner: implementation -->

## Slice D — `core/database` Room v1 + favorites store (radio Req 4; settings Req 1)

- [x] RED: Write DAO tests — `FavoriteDao` round-trip (upsert/observe/delete/byId), undo re-upsert restores row, `ThemeDao` round-trip + restart persistence; in-memory Room + `FakeFavoriteDao` for VM seams. Files: `core/database/src/test/.../FavoriteDaoTest.kt`, `ThemeDaoTest.kt`. <!-- sdd-owner: implementation -->
- [x] GREEN: Implement `core/database` — `FonampDatabase` v1, `FavoriteStation` (stationUuid PK, name, streamUrl, country?, tagsCsv?, bitrate?, codec?, favoritedAt), `ThemePref` (id=1, SYSTEM|LIGHT|DARK), `FavoriteDao` (`observeAll/upsert/delete/byId`), `ThemeDao` (`observe/set`); verify `./gradlew :core:database:test`. <!-- sdd-owner: implementation -->

## Slice E — `core/network` + `provider/radio` (source-contract Req 3, 4; radio Req 1, 2, 5, 6)

- [x] RED: Write MockWebServer JVM tests — mirror fallback (primary down → secondary serves index, serving mirror recorded), 10s timeout → typed `Timeout` (no raw exception leak), 24h TTL serve-stale + refresh replaces, failed refresh keeps stale + Offline, click-count failure never fails play, nullable-tolerant JSON (`url_resolved`→`url` fallback). Files: `core/network/src/test/.../MirrorPolicyTest.kt`, `DirectoryCacheTest.kt`; `provider/radio/src/test/.../RadioBrowserSourceTest.kt`. <!-- sdd-owner: implementation -->
- [x] GREEN: Implement `core/network` — `RadioBrowserApi` (Retrofit + kotlinx.serialization), OkHttp (connect/read 10s, callTimeout ~12s, disk cache), ordered mirrors `de1→de2→nl1` (+bootstrap optional), `DirectoryCache` (index + per-selection lists, `fetchedAt`, 24h TTL), cache-stats/clear API, diagnostics log of serving mirror. <!-- sdd-owner: implementation -->
- [x] GREEN: Implement `provider/radio` `RadioBrowserSource` (id `radio-browser`) — `browse` (empty query → cached index; filtered query → station list), `search` = client-side filter over cached list per design, typed `SourceError` mapping, fire-and-forget `POST json/url/{uuid}` click-count (`scope.launch + runCatching`, swallowed); depends only on `provider/api` + `core/network`; verify `./gradlew :core:network:test :provider:radio:test`. <!-- sdd-owner: implementation -->

## Slice F — `provider/local` + `feature/library` collection (source-contract Req 2; library Req 1–6; permissions Req 2–3)

- [x] RED: Write Robolectric tests — MediaStore cursor fake → `browse` grouping/songs-artists-albums metadata honesty (absent fields omitted, content URIs playable), `browse` ignores `BrowseQuery` filters, zero network calls; `LibraryViewModel` states (Loading/Empty/Denied/ErrorRetry via Turbine, FakePlayerManager, `Source` fake). Files: `provider/local/src/test/.../LocalSourceTest.kt`; `feature/library/src/test/.../LibraryViewModelTest.kt`. <!-- sdd-owner: implementation -->
- [x] GREEN: Implement `provider/local` `LocalSource` (id `local`) — `ContentResolver` wrapper seam, `query(EXTERNAL_CONTENT_URI, projection, IS_MUSIC != 0)` single pass, honest metadata mapping; depends only on `provider/api` + player mappers. <!-- sdd-owner: implementation -->
- [x] GREEN: Implement `feature/library` — Collection UI (Songs/Artists/Albums segments, artist/album→song filter, tap→play with local queue next/prev+seek), generic icons only, Empty (+add-music hint), optional cheap text filter, no shuffle/repeat affordance; `LibraryViewModel → LocalSource` only + `AudioPermissionGate` entry; verify `./gradlew :provider:local:test :feature:library:test`. <!-- sdd-owner: implementation -->

## Slice G — `core/player` PlaybackService + PlayerManager (player Req 1–6)

- [x] RED: Write tests — `FakePlayerManager` Turbine flows (play/toggle/stop/next/prev/seek-noop-when-live/retry, `TIMEOUT|OFFLINE|STREAM_UNAVAILABLE` → banner state, UI stays interactive), Robolectric `streamOf` mapping tests (local `mediaId=local:<id>` + `is_live=false`; radio `mediaId=radio:<uuid>` + `is_live=true`), process-death restore coherence (restored-or-idle, never phantom-playing). Files: `core/player/src/test/.../PlayerManagerTest.kt`, `MediaItemMapperTest.kt`. <!-- sdd-owner: implementation -->
- [x] GREEN: Implement `core/player` — `PlaybackService : MediaSessionService` (single ExoPlayer, `AudioAttributes(handleAudioFocus=true, USAGE_MEDIA, MUSIC)`, `DefaultHttpDataSource` 10s connect/read, ICY headers → metadata extras, FGS only while playing, best-effort queue save/restore), `PlayerManager` interface + Hilt-bound impl (MediaController holder, `PlayerUiState` incl. `icyTitle`/`error`, radio play/stop vs local pause/next/prev/seek), `PlayerError`; no imports from `provider/*`; verify `./gradlew :core:player:test`. <!-- sdd-owner: implementation -->

## Slice H — `feature/radio` discover + stations + favorites (radio Req 1–6; player Req 5)

- [x] RED: Write VM + behavior tests — `RadioViewModel`/`FavoritesViewModel` Turbine flows (Loading/Empty/Offline-with-cache/ErrorRetry, local index filter with zero per-keystroke network, 3-tap Discover→stations→play raises mini-player with radio badge, favorite round-trip + restart + snackbar 10s undo, stations-only favorites, no tops/random/name-search surface). Files: `feature/radio/src/test/.../RadioViewModelTest.kt`, `FavoritesViewModelTest.kt`. <!-- sdd-owner: implementation -->
- [x] GREEN: Implement `feature/radio` — Discover index (Countries + Genres/Tags with counts where provided, local text filter), station lists (name + bitrate/codec-when-present, generic icons), 1-tap play → `PlayerManager.playSingle`, heart toggle in list + sheet, Favorites tab (Room-backed, undo snackbar), Offline card + retry with cache visible, pull-to-refresh; deps only `provider/api`, `provider/radio`, `core/player`, `core/ui`, `core/database`, `core/network`; verify `./gradlew :feature:radio:test`. <!-- sdd-owner: implementation -->

## Slice I — `core/permissions`, `feature/settings`, `app` wiring (permissions Req 1–4; settings Req 1–4; scaffold Req 2, 4; player Req 2)

- [x] RED: Write tests — `AudioPermissionGate` API-split logic (33+ `READ_MEDIA_AUDIO` vs 29–32 `READ_EXTERNAL_STORAGE`, first-entry-Collection-only, denied → grant-again, permanent → settings deep-link, rest of app ungated), `SettingsViewModel` theme round-trip (System→Dark→restart→Light, no-restart apply) + cache-stats/clear confirmation, Hilt `provider/demo` mock-source expandability test (no `core/player`/`core/ui`/tabs diff). Files: `core/permissions/src/test/.../`, `feature/settings/src/test/.../SettingsViewModelTest.kt`, `app/src/test/.../ExpandabilityTest.kt`. <!-- sdd-owner: implementation -->
- [x] GREEN: Implement `core/permissions` (`AudioPermissionGate`, `NotificationGate` lazy-on-first-playback, tolerated failure), manifest merge (`READ_MEDIA_AUDIO` 33+, `READ_EXTERNAL_STORAGE` max-sdk-32, `INTERNET`, `FOREGROUND_SERVICE` + `MEDIA_PLAYBACK`, `POST_NOTIFICATIONS` 33+, `usesCleartextTraffic=true`), `feature/settings` (theme System/Light/Dark → `ThemeDao`, cache usage + clear with confirmation + freed bytes, About version/licenses/source; no tab-editor/EQ/sleep/speed), `app` (`Application`, Hilt graph, `@Binds @IntoSet Source` multibindings keyed by `id`, NavHost `collection|radio/discover|radio/stations?filter|favorites|settings`, bottom tabs exactly Collection/Radio/Favorites/Settings, persistent `MiniPlayer` + modal `PlayerSheet`, notification wiring via service, theme recompose without restart). <!-- sdd-owner: implementation -->
- [x] GREEN: Run full gates — `./gradlew test` green (306/306), clean `assembleDebug` green (63.6 MB; 40MB gate moved to release per 2026-09-12 parent decision, debug monitored), theme + favorites round-trips green, permission/manifest + egress audit green. Headless on-device checks (≤3-tap audible, screen-off sync, hardware banner) remain manual follow-ups. <!-- sdd-owner: implementation -->
