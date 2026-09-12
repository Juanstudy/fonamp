# Design — fonamp-foundation (v1: foundation + local + radio)

> Status: design · Scope: proposal + 7 specs (scaffold, source-contract, library, radio, player, settings, permissions) · Env: Gradle 8.10 + AGP 8.7, JDK 17 Corretto, minSdk 29, compile/targetSdk 35 · No code in this doc, contracts only.

## 0. Locked constraints (not reopened)

Kotlin + Compose M3 defaults (no custom tokens). Media3 single ExoPlayer in `MediaSessionService` + `MediaController` UI, `PlayerManager` seam (MediaItem mapping needs Robolectric on JVM), `AudioAttributes(handleAudioFocus=true)`. Hilt. Retrofit + OkHttp with 24h directory cache + mirror fallback + 10s timeouts (directory fetch and stream connect alike). kotlinx.serialization. Room (favorites; in-memory fakes for tests). Coroutines/Flow. Foreground service only while playing. JUnit + Robolectric + Turbine + MockWebServer (no MockK). Debug APK, no R8/minify. Direct APK distribution. `usesCleartextTraffic=true` (third-party radio streams are http).

**Media3 version: `VERIFY-AT-APPLY`, default `1.10.1`.** No network/version lookup was available to the design phase (repo-local execution, no registry tool). At apply time, resolve latest stable `androidx.media3:*` and pin the **identical** version across every media3 artifact (`exoplayer`, `session`, `ui`, `datasource`, `common`). If resolution fails, use `1.10.1`.

## 1. Module layout + dependency rules

Greenfield confirmed (no `settings.gradle*`, no version catalog at design time). Create:

```
app/                  wiring + navigation only (Application, NavHost, Hilt graph, service declaration)
core/player/          PlaybackService, PlayerManager, MediaItem mappers, PlayerError/PlayerState
core/network/         RadioBrowser client, mirror policy, cache store, timeouts, click-count sender
core/database/        Room v1 (FavoriteStation + ThemePref), DAOs
core/permissions/     AudioPermissionGate (API-split), NotificationGate helpers
core/ui/              design system: FonampTheme(M3 defaults), Loading(shimmer)/Empty/Offline/Denied/ErrorRetry, MiniPlayer, PlayerSheet, SourceBadge
provider/api/         Source contract + AudioItem/BrowseQuery types (pure Kotlin, zero Android deps except Media3 MediaItem)
provider/local/       LocalSource (MediaStore) — depends only on provider/api + core/player mappers
provider/radio/       RadioBrowserSource — depends only on provider/api + core/network
feature/library/      Collection UI + LibraryViewModel → LocalSource only
feature/radio/        Discover/Stations/Favorites UI + RadioViewModel/FavoritesViewModel → RadioBrowserSource + core/database
feature/settings/     Settings UI + SettingsViewModel → core/database (theme pref) + core/network (cache stats/clear)
```

Dependency rules (enforced by convention + apply-time Gradle review; no feature-to-feature edges):

- `feature/*` → allowed: `core/*`, `provider/api`, `provider/local|radio` (the one it displays). **Never** `feature → feature`.
- `provider/*` → allowed: `provider/api` + its infra (`core/network` for radio, ContentResolver for local). Never `provider → feature`, never `provider → app`.
- `core/player` imports **nothing** from any `provider/*`; it consumes `MediaItem` only (source-blind, §3).
- `app` alone wires navigation + Hilt bindings (`@Binds Source` multibinding `Set<Source>` keyed by `id`) and declares the service/receivers.
- New source (v2+) = new `provider/*` module + one `@Binds @IntoSet` line in `app`. Zero edits to `core/player`, `core/ui`, bottom tabs. Acceptance: add `provider/demo` mock, `./gradlew test` + `assembleDebug` green with no shell/player diff (scaffold Req 2).

Gradle shape: `settings.gradle.kts` includes all 12 modules above; root `build.gradle.kts` holds toolchain (AGP 8.7 lineage, Kotlin, Hilt, KSP); `gradle/libs.versions.toml` is the single version source (AGP, Kotlin, Compose BOM, Media3 VERIFY-AT-APPLY, Retrofit/OkHttp, Room, Hilt, serialization, Coroutines, JUnit/Robolectric/Turbine/MockWebServer). Each module has its own `build.gradle.kts` with `namespace`. `core/ui`, `feature/*`, `app` enable Compose (`compose=true`, BOM-managed).

## 2. Finalized `Source` contract (`provider/api`)

Pure-Kotlin module (no Android framework imports except `androidx.media3.common.MediaItem`, which forces Robolectric only for `streamOf` tests — see §9).

```kotlin
// provider/api
enum class SourceKind { LOCAL, RADIO }

data class AudioItem(
  val sourceId: String,          // "local" | "radio-browser"
  val stableId: String,          // local: MediaStore audioId; radio: stationuuid
  val title: String,
  val subtitle: String?,         // artist OR station tags/country; null when absent (never fabricated)
  val album: String?,            // local only; null for radio
  val durationMs: Long?,         // local only; null for radio (live stream)
  val streamUri: String,         // local: content://…; radio: resolved stream url
  val bitrate: Int?, codec: String?,   // radio only when directory provides them
  val stationUuid: String?, country: String?, tags: List<String>,
)

data class BrowseQuery(val country: String? = null, val genre: String? = null, val tag: String? = null)

sealed interface SourceError {
  data object Offline : SourceError
  data object Timeout : SourceError
  data class Server(val code: Int?) : SourceError
  data class Unknown(val cause: Throwable) : SourceError
}
sealed interface SourceResult<out T> { data class Ok<T>(val v: T) : SourceResult<T>; data class Fail(val e: SourceError) : SourceResult<Nothing> }

interface Source {
  val id: String
  val kind: SourceKind
  suspend fun browse(query: BrowseQuery): SourceResult<List<AudioItem>>
  suspend fun search(q: String): SourceResult<List<AudioItem>>
  fun streamOf(item: AudioItem): MediaItem
  // No downloadOf in v1 (non-abstract/absent by spec). v3 adds:
  // fun downloadOf(item: AudioItem): DownloadRequest?  ← default null, non-abstract
}
```

**MediaItem mapping (the only thing the player sees):**

- Extras bundle keys (contract, owned by `provider/api` as `const val`s in `SourceExtras`): `source_id`, `stable_id`, `is_live (Boolean)`, `station_uuid?`, `country?`. Player reads only these + standard `mediaMetadata` (title/artist/album art = generic icon URI or null).
- `local` → `MediaItem(mediaId="local:<audioId>", uri=contentUri, mediaMetadata{title, artist, album}, extras{is_live=false})`.
- `radio-browser` → `MediaItem(mediaId="radio:<stationuuid>", uri=streamUrl, mediaMetadata{title=station name, artist=country/tags joined or null, isPlayable=true, isBrowsable=false}, extras{is_live=true, station_uuid, country})`. Live flag drives player UI: no seek bar, play/stop semantics (§4).
- `LocalSource.browse` ignores `BrowseQuery` filters it cannot serve (returns full grouped catalog; grouping Songs/Artists/Albums happens in `feature/library` ViewModel from one `browse(BrowseQuery())` result — one MediaStore pass, `contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, IS_MUSIC != 0 …)`). No network, no config.
- `RadioBrowserSource.browse`: `BrowseQuery()` → country/genre/tag index (from `core/network` cached index); `BrowseQuery(country|genre|tag)` → station list endpoint. `search(q)` in v1: local text filter only (directory name-search excluded per radio Req 6); radio source returns `Fail`→ UI falls back to local index filter, or implement as client-side filter over cached station list. Typed errors only — Retrofit/OkHttp/IO exceptions are mapped to `SourceError` inside `provider/radio`, never leak to UI.

## 3. Playback: `PlaybackService` + `PlayerManager` (`core/player`)

```
PlaybackService : MediaSessionService
  - owns the ONE ExoPlayer (built once, AudioAttributes(handleAudioFocus=true, usage=USAGE_MEDIA, contentType=MUSIC))
  - exposes MediaSession; onGetSession returns it; foreground-service start/stop bound to playback state
  - config: 10s stream connect/read timeout via DefaultHttpDataSource (setConnectTimeoutMs=10_000, setReadTimeoutMs=10_000); ICY metadata enabled (IcyHeaders) → surfaced as MediaMetadata extras for radio sheet
  - process-death: best-effort queue save (mediaIds + position) to DataStore/prefs on state change; restore on create; always land idle-or-restored, never phantom-playing

PlayerManager (interface, Hilt-bound; FakePlayerManager for VM tests)
  - interface: StateFlow<PlayerUiState> { queue, index, isPlaying, isLive, icyTitle?, error?: PlayerError? }, fun play(items: List<MediaItem>, index: Int), playSingle(MediaItem), togglePlayPause(), stop(), next(), prev(), seekTo(ms) (no-op when isLive), retry()
  - impl DefaultPlayerManager: holds MediaController (async connect to PlaybackService session), maps controller callbacks → PlayerUiState; radio = play/stop (stop() not pause, keeps live edge honest); local = pause/resume + next/prev/seek
  - PlayerError { TIMEOUT, OFFLINE, STREAM_UNAVAILABLE } + inline banner + manual retry; never freeze/crash/silent-stall; UI stays interactive
  - v1 explicitly absent: shuffle/repeat/speed/sleep/EQ (no affordance, no behavior)
```

UI connects via `MediaController` only (Compose `collectAsStateWithLifecycle` on `PlayerManager.state`). Mini-player + sheet (§7) never touch ExoPlayer directly.

## 4. RadioBrowser client (`core/network`)

- Retrofit service (`RadioBrowserApi`, kotlinx.serialization converter) + OkHttp: `connectTimeout=10s`, `readTimeout=10s`, `callTimeout` ≤ ~12s; disk cache for HTTP; JSON fields nullable-tolerant (`stationuuid`, `name`, `url_resolved`→fallback `url`, `bitrate`, `codec`, `country`, `tags`, `stationcount`).
- **Mirror policy:** ordered list `de1.api.radio-browser.info → de2 → nl1` (+ `all.api.radio-browser.info` bootstrap optional). On `Offline/Timeout/5xx`, rotate to next mirror and retry once per mirror; record serving mirror in diagnostics log (not UI). First healthy mirror wins per process; order resets on total failure.
- **Cache store (`DirectoryCache`):** serializes index (countries+genres/tags with counts) + per-selection station lists with `fetchedAt`. TTL **24h**. Serve cache when offline/fetch-fails; show Offline card + retry while displaying cache; stale cache stays visible on failed refresh; pull-to-refresh forces fetch. Settings "clear cache" wipes store and reports freed bytes; next Discover refetches.
- **Click-count:** `POST …/json/url/{stationuuid}` fire-and-forget from `RadioBrowserSource` on play: `scope.launch { runCatching { api.click(id) } }` — never blocks, never delays, failure swallowed/logged, never user-visible.
- Endpoints used (v1 only): country/genre/tag index + station-by-filter + click-count. No tops/random/name-search (spec exclusion).

## 5. Room schema v1 (`core/database`)

```kotlin
// DB: FonampDatabase v1, entities:
FavoriteStation(stationUuid PK, name, streamUrl, country?, tagsCsv?, bitrate?, codec?, favoritedAt)
ThemePref(id=1 PK, mode: SYSTEM|LIGHT|DARK)
@Dao FavoriteDao { observeAll(): Flow<List<…>>; suspend upsert/delete/byId; }
@Dao ThemeDao { observe(): Flow<ThemePref?>; suspend set(…) }
```

- Favorites are **stations-only**, local-only, no sync/account/export. Undo: delete → snackbar with 10s undo window re-upserts the deleted row (ViewModel holds the row, not just id).
- Theme choice persists across restarts, applies without restart (activity recomposes via `collectAsStateWithLifecycle` in `app`).
- Directory cache lives in `core/network` disk store, **not** Room (keeps DB schema minimal; cache clear ≠ DB wipe).
- Tests: in-memory Room (`Room.inMemoryDatabaseBuilder`) for DAO tests + hand-written fakes (`FakeFavoriteDao`) for ViewModel tests — no MockK per locked stack.

## 6. Permission flows (`core/permissions` + manifest)

Manifest (merged max set): `READ_MEDIA_AUDIO` (API 33+), `READ_EXTERNAL_STORAGE` max-sdk-32 (audio-scoped usage), `INTERNET`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `POST_NOTIFICATIONS` (33+). No location/contacts/write. `usesCleartextTraffic=true` (http radio streams).

- `AudioPermissionGate`: requested **only on first entering Collection** (`READ_MEDIA_AUDIO` API 33+, `READ_EXTERNAL_STORAGE` API 29–32), with inline why-needed text at request time. Rest of app never gated.
- States: granted → MediaStore list; denied → `Denied` component (why-needed + grant-again); permanently denied → `Denied` with system-settings deep-link; never crash/blank.
- Notification permission: requested lazily on first playback (33+), failure tolerated (playback continues, system may hide notification).
- Egress posture: off-device traffic limited to radio-browser mirrors + click-count + chosen streams; zero trackers (audit per scaffold Req 3).

## 7. Navigation + mini-player / global sheet (`app` + `core/ui` + `feature/*`)

- `app` NavHost (Navigation-Compose), bottom tabs exactly **Collection, Radio, Favorites, Settings** (no Podcasts/Downloads/EQ/sleep). Routes: `collection`, `radio/discover`, `radio/stations?filter=…`, `favorites`, `settings`. Discover→stations→play = 3 taps max from cold start.
- Persistent `Scaffold`: `MiniPlayer` (icon, title, play/pause, close; 48dp targets, content descriptions) docked above bottom bar whenever `PlayerUiState` is playing-or-paused; same instance across all tabs. Tap → modal `PlayerSheet`: title, artist-or-station, `SourceBadge(radio|local)`, ICY line for radio, queue controls + seek for local, favorite toggle (radio), error banner + retry on failure. No shuffle/repeat/speed/sleep anywhere.
- `core/ui` states: `Loading` (shimmer rows, never blank), `Empty` (+add-music hint in Collection), `Offline` (card + retry, cache still shown), `Denied` (why + grant/settings), `ErrorRetry`. Every list screen renders one; M3 default light/dark roles, dynamic text supported.
- Performance: first tab interactive <2s cold; paged/cached directory lists (no per-keystroke network; index filter is local); no polling; FGS only while playing.

## 8. Test seams per module

Locked stack: JUnit + Robolectric + Turbine + MockWebServer, no MockK — hand fakes everywhere:

| Module | Seam | Test |
|---|---|---|
| `provider/api` | pure types | JVM unit: extras keys, mapping helpers |
| `provider/local` | `ContentResolver` wrapper interface | Robolectric: MediaStore cursor fake → browse grouping/metadata honesty |
| `provider/radio` + `core/network` | `RadioBrowserApi` via MockWebServer; `MirrorPolicy` injectable list | JVM: mirror fallback, 10s timeout → typed `Timeout`, 24h TTL serve-stale + refresh, click-count failure never fails play |
| `core/player` | `PlayerManager` interface → `FakePlayerManager` | VM tests Robolectric-free; `streamOf` mapping tests use Robolectric (MediaItem needs it); Timeout→banner+retry via fake |
| `core/database` | DAO interfaces → in-memory Room + `FakeFavoriteDao` | DAO round-trip + restart persistence; undo restores |
| `feature/*` VMs | inject `Source` fakes + `FakePlayerManager` + Turbine | state flows: Loading/Empty/Offline/Denied/ErrorRetry, 3-tap play, favorite round-trip, theme round-trip |
| `app` | Hilt test graph with `provider/demo` mock `Source` | expandability proof: new source, no shell/player edits, `test` + `assembleDebug` green |

CI (`.github/workflows/android.yml`): JDK 17 Corretto setup → `./gradlew test` (gate) → `./gradlew assembleDebug` → assert APK <40 MB + dependency audit (no ads/admob/firebase/analytics/crashlytics) + manifest permission audit.

## 9. Gradle module graph + CI

```
:app → feature/library, feature/radio, feature/settings, core/player, core/ui, core/permissions, core/database, provider/api, provider/local, provider/radio
feature/library → provider/api, provider/local, core/player, core/ui, core/permissions
feature/radio → provider/api, provider/radio, core/player, core/ui, core/database, core/network
feature/settings → core/ui, core/database, core/network
provider/radio → provider/api, core/network
provider/local → provider/api (+ core/player mappers)
core/* → third-party only (Media3/Retrofit/Room/Hilt per module need)
```

No `feature↔feature` edges. `assembleDebug` = debug APK, `isMinifyEnabled=false`, direct distribution.

## 10. Tradeoffs (required)

1. **Single shared ExoPlayer vs per-source players.** Chose single (spec-mandated). Pro: one notification/service/queue, source-blind expandability, <40 MB stays easy. Con: radio-live vs local-seek semantics must be branched in one `PlayerManager` (is_live flag); a live-edge bug affects all audio. Mitigation: `PlayerManager` interface + fake keeps branching unit-tested.
2. **MediaController indirection vs direct ExoPlayer in UI.** Chose controller. Pro: background/service lifecycle + headset + notification free; UI survives config change. Con: async connect race + Robolectric need for MediaItem tests. Mitigation: `PlayerManager` hides connect; VMs tested against fake.
3. **24h disk cache + mirror fallback vs always-fresh directory.** Chose cache+fallback. Pro: offline Discover, survives radio-browser instability, no per-keystroke network. Con: stale index up to 24h; cache-invalidation complexity. Mitigation: explicit Offline card + pull-to-refresh + stale-served-on-failure (spec Req 5).
4. **Room favorites vs DataStore JSON.** Chose Room. Pro: observable `Flow`, undo + restart persistence trivial, scales to v2 subscriptions/downloads. Con: more scaffolding than a JSON file for a stations-only list. Mitigation: two-entity v1 schema, in-memory fakes keep tests cheap.
5. **Retrofit+OkHttp vs Ktor.** Chose Retrofit. Pro: team-validated in prototype, MockWebServer story, cache interceptors standard. Con: larger API surface than Ktor client if KMP ever returns (rejected: native wins for a player, §4 tech doc).
6. **M3 defaults, no tokens/artwork pipeline.** Chose defaults + generic icons. Pro: velocity, theme (System/Light/Dark) without restart nearly free, defers artwork cache to v2. Con: visually plain v1; no embedded art. Mitigation: metadata-honesty rule (omit absent fields) keeps it coherent.
7. **No R8/minify, debug APK.** Chose deferred hardening. Pro: faster v1 iteration, simpler debugging. Con: APK larger than release-tuned (still gated <40 MB); no obfuscation. Accepted per tech-doc resolution; revisit post-v1.
8. **No MockK (hand fakes).** Pro: fewer deps (allowlist principle), explicit seams (`PlayerManager`, DAO, `Source` fakes) that double as v2 extension points. Con: more hand-written fake code. Accepted; fakes live in test sourcesets per module.

## 11. Rollout / acceptance mapping

Apply order: toolchain+catalog → `provider/api` → `core/ui` states → `core/database` → `core/network`+`provider/radio` → `provider/local` → `core/player`+service → `feature/*` → `app` wiring → CI. Gates: `./gradlew test` green; `assembleDebug` <40 MB; mock-source expandability check; permission/manifest audit; cold-start→radio-audible ≤3 taps; screen-off + notification sync; 10s-timeout banner+retry; theme round-trip; favorites round-trip + undo.
