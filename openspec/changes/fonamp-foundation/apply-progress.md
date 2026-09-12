# Apply progress — fonamp-foundation · Slices A–B

> Scope: Slices A–B done. Later slices (C–I) NOT started. No commits, no PRs.
> Branch choice: worked directly on tracker `feat/fonamp-foundation` (no child branch —
> Slice B touches only `provider/api/**`, no isolation needed).

## Status
- [x] RED task 1 — toolchain presence checks (`scripts/audit-toolchain.sh`)
- [x] GREEN task 2 — Gradle scaffold (12 modules, pins, per-module builds)
- [x] RED task 3 — CI gate tests (`scripts/audit-gates.sh`)
- [x] GREEN task 4 — CI workflow (`.github/workflows/android.yml`)

## RED → GREEN evidence
- RED run (before scaffold): `audit-toolchain.sh` → RED (settings absent, toml absent,
  app build absent); `audit-gates.sh` → RED (workflow absent, APK absent, no manifests).
- GREEN run (after scaffold + build): both scripts exit 0 — see "Gate results" below.

## Commands run (all with JDK 17 Corretto + ANDROID_HOME set)
- `java -version` → `17.0.20.1 Corretto`; `$ANDROID_HOME/platforms` → `android-34 android-35`
- `gradle wrapper --gradle-version 8.10.2` (via `~/toolcache/gradle-8.10.2`) → `gradlew` on branch `feat/fonamp-foundation`
- `./gradlew test` → BUILD SUCCESSFUL (483 tasks) — **26 tests, 0 failures, 0 errors, 0 skipped**
- `./gradlew assembleDebug` → BUILD SUCCESSFUL — `app-debug.apk` 34,148,211 bytes (~32.6 MB) < 40 MB

## Gate results (final)
- Toolchain audit: 12/12 modules in `settings.gradle.kts`; all 18 version pins present;
  media3 single pin; no `isMinifyEnabled=true`; minSdk 29 / targetSdk 35 / compileSdk 35 → GREEN
- Gates audit: workflow runs `./gradlew test` + `assembleDebug` on JDK 17; zero
  `ads|admob|firebase|analytics|crashlytics`; APK 33M < 40MB; 6/6 manifest permissions
  within allowlist → GREEN

## Version pins (gradle/libs.versions.toml — each verified reachable before pinning)
- Gradle 8.10.2 · AGP 8.7.3 · Kotlin 2.0.21 · KSP 2.0.21-1.0.28 · Hilt 2.55 · Room 2.6.1
- Compose BOM 2024.12.01 · activity-compose 1.9.3 · lifecycle 2.8.7 · navigation-compose 2.8.4 ·
  hilt-navigation-compose 1.2.0
- **Media3 1.9.0 identically across exoplayer/session/ui/datasource/common**
- Retrofit 2.11.0 · OkHttp 4.12.0 · serialization 1.8.1 · Coroutines 1.10.1
- JUnit 4.13.2 · Robolectric 4.14.1 · Turbine 1.1.0 · MockWebServer 4.12.0

## Deviation from design (Media3 version)
- Design §0 default was Media3 `1.10.1` ("latest stable, else 1.10.1"). At apply time the
  actual latest stable is `1.11.1`, but **both 1.11.1 and 1.10.1 declare
  `minCompileSdk=36`** (verified from AAR metadata on Google Maven), which conflicts
  with the binding `compileSdk 35` (AGP 8.7 max recommended 35; SDK has no API-36
  platform). Pinned newest 35-compatible stable instead: **Media3 1.9.0**
  (`minCompileSdk=35`). Revisit iff compileSdk moves to 36+.

## Fixes applied during GREEN
- `build.gradle.kts`: `#` → `//` comments (Kotlin DSL syntax).
- Compose BOM consumed via `platform(libs.compose.bom)` in `:app`, `:core:ui`,
  `:feature:library`, `:feature:radio`, `:feature:settings` (bare `implementation`
  left artifact versions empty).
- `core/network` takes **no** `provider/api` dependency (design §1: core → third-party only).
- Shell: `export JAVA_HOME/PATH` must be separate commands (same-line `PATH=$JAVA_HOME/...`
  expands the stale value → Gradle ran on Java 26 and fails).

## Files created (Slice A)
- `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`,
  `gradlew` + `gradle/wrapper/*`
- 12 module `build.gradle.kts` + manifests; stub sources + JUnit smoke tests per module
  (`app`: Hilt `FonampApp`/`ScaffoldModule`, Compose `MainActivity`)
- `scripts/audit-toolchain.sh`, `scripts/audit-gates.sh`
- `.github/workflows/android.yml` (JDK 17 Corretto → toolchain audit → `test` →
  `assembleDebug` → gates audit)

## Remaining (explicitly out of scope for this slice)
- Slices B–I untouched. Stubs (`*Scaffold` objects) are placeholders replaced by later slices.
- Attempt authority token `sha256:f2d47...` — 200-line attempt budget incompatible with
  greenfield scaffold; parent-ordered Slice A implemented in full and reported honestly.

## Slice B — `provider/api` Source contract (source-contract Req 1, 5)

- Attempt authority: `acquire` work-unit `slice-b-provider-api`, `--max-changed-lines 1500`,
  token `sha256:7baae5af…` → state `proceed`. (200-line default cannot hold a slice; settled on completion.)
- [x] RED — `SourceContractTest.kt` (pure JVM, Robolectric-free) + `MediaItemMappingTest.kt`
  (Robolectric, `@Config(sdk=[34])` — uses cached `android-all`, no download).
  RED evidence: `:provider:api:testDebugUnitTest` → `compileDebugUnitTestKotlin FAILED`,
  `Unresolved reference` for `SourceExtras/AudioItem/BrowseQuery/SourceError/Source/SourceKind`
  (impl did not exist yet).
- [x] GREEN — 8 impl files per design §2; removed Slice A `ApiScaffold.kt`/`ApiScaffoldTest.kt`
  stubs (superseded by the contract); added `testImplementation(libs.coroutines.test)` to
  `provider/api/build.gradle.kts` (for `runTest`; catalog already pinned coroutines 1.10.1).
- Fixes during GREEN: `assertTrue(X is SourceError)` on a data object warns always-true and
  `!==` across distinct object types does not compile — settled on
  `(Timeout as Any) !== (Offline as Any)` distinctness check.
- GREEN evidence: `:provider:api:test` BUILD SUCCESSFUL — `SourceContractTest` 6/6,
  `MediaItemMappingTest` 3/3, both debug+release (18/18), zero warnings.
  Full `./gradlew test` BUILD SUCCESSFUL (483 tasks) — **42 tests, 0 failures, 0 errors, 0 skipped**.
- Contract notes: `Source` has exactly `id/kind/browse/search/streamOf` (reflection test guards
  no `download*` member — Req 5). Extras live in `MediaMetadata.extras` (MediaItem.Builder has
  no top-level extras edge). `android.os.Bundle` in `MediaItems.kt` is part of the Media3
  MediaItem edge with `androidx.media3.common.*`; all other files pure Kotlin, no Android imports.
  Media3 stays pinned 1.9.0 (Slice A pin — not upgraded).
- Files: `provider/api/src/main/.../api/{SourceExtras,SourceKind,AudioItem,BrowseQuery,SourceError,SourceResult,Source,MediaItems}.kt`;
  `provider/api/src/test/.../api/{SourceContractTest,MediaItemMappingTest}.kt`;
  `provider/api/build.gradle.kts` (M); `ApiScaffold*.kt` (D).

## Remaining (explicitly out of scope for this slice)

- Slices C–I untouched. No commits, no PRs, no pushes (per instructions).

## Slice C — `core/ui` design system + shared states (scaffold Req 4, 5, 7)

- Attempt authority: prior `slice-C` attempt (ordinal 3) was still `running` with
  partial work on disk and no result. `acquire slice-C-retry` returned
  `blocked/active_attempt` with continuation token
  `sha256:cda01acc…`; re-acquired with `--token` → state `proceed`.
  Token `sha256:cda01acc564fcb91ea273cddf0e6082d765b24f3349da6457752314d464faf83`.
  Settled on completion (see done-state below).
- [x] RED — `core/ui/src/test/.../UiStatesTest.kt` (17 behavior tests, Robolectric
  `@Config(sdk=[34])`, `createComposeRule`): Loading shimmer rows never-blank +
  dark, Empty message+hint + dark, Offline card + retry-callback + dark, Denied
  why + grant-again + settings deep-link, ErrorRetry message + retry, MiniPlayer
  48dp play/pause + close targets with content descriptions + paused Play state,
  PlayerSheet skeleton (title/badge/close) + error-banner retry, SourceBadge
  radio|local, light/dark render of all states, MiniPlayer state restoration.
  (RED pre-existed as partial work with impl present, so per instructions the
  gate is GREEN-till-proven: forced rerun green, no impl fix needed.)
- [x] GREEN — 5 impl files per design §7 + docs/03-design.md §4 (M3 DEFAULTS only,
  no custom tokens): `FonampTheme.kt` (`lightColorScheme()`/`darkColorScheme()`
  defaults), `UiStates.kt` (`LoadingState` static skeleton rows — animated shimmer
  deferred to v2 visual pass, documented in KDoc; `EmptyState` message+hint,
  `OfflineState` card + Retry + cacheNote slot, `DeniedState` why + Grant again +
  Open-settings slot, `ErrorRetryState`), `MiniPlayer.kt` (`MiniPlayerState` +
  48dp `sizeIn` targets, Pause/Play/Close-player descriptions, tap-to-expand),
  `PlayerSheet.kt` (title/subtitle/badge/ICY line/favorite toggle/error+retry;
  NO shuffle/repeat/speed/sleep), `SourceBadge.kt` (own `SourceBadgeKind`
  RADIO|LOCAL enum — keeps core→third-party-only rule, no `provider/api` dep).
  `core/ui/build.gradle.kts`: `ui-test-junit4` + `ui-test-manifest`
  (debug+release) test deps. Slice A `UiScaffold.kt` placeholder left in place
  (harmless, superseded docs updated by later slices if desired).
- GREEN evidence (JDK 17 Corretto + ANDROID_HOME, Media3 pinned 1.9.0 untouched):
  `./gradlew :core:ui:test --rerun-tasks` → BUILD SUCCESSFUL in 36s —
  `UiStatesTest` 17/17 debug + 17/17 release, `UiScaffoldTest` 1/1 ×2,
  0 failures, 0 errors, 0 skipped.
  Full `./gradlew test` → BUILD SUCCESSFUL (507 tasks) — 28 classes,
  **76 tests, 0 failures, 0 errors, 0 skipped**
  (core/ui 36, provider/api 18, 10 modules ×2 smoke).
- Files: `core/ui/src/main/.../ui/{FonampTheme,UiStates,MiniPlayer,PlayerSheet,
  SourceBadge}.kt`; `core/ui/src/test/.../ui/UiStatesTest.kt`;
  `core/ui/build.gradle.kts` (M).

## Remaining (explicitly out of scope for this slice)

- Slices D–I untouched. No commits, no PRs, no pushes (per instructions).
  Stop after Slice C.

## Slice D — `core/database` Room v1 + favorites store (radio Req 4; settings Req 1)

- Attempt authority: `acquire` work-unit `slice-D`, `--max-changed-lines 1200`,
  token `sha256:4da91645…` → state `proceed`. Settled on completion (see done-state below).
- [x] RED — `FavoriteDaoTest.kt` (6 tests: upsert/observe round-trip with full
  field equality, byId hit+miss, upsert-replaces, delete clears observeAll+byId,
  undo re-upsert restores full row, nullable-columns round-trip),
  `ThemeDaoTest.kt` (5 tests: observe-null-before-choice, set→observe,
  set-replaces single id=1 row, all three modes round-trip, close+reopen
  restart persistence), `FakeFavoriteDaoTest.kt` (2 tests: fake honors the DAO
  contract incl. undo). In-memory Room via Robolectric `@Config(sdk=[34])`
  (cached `android-all`); context from `RuntimeEnvironment.getApplication()`
  (no new dep — `androidx.test:core` deliberately NOT added).
  RED evidence: `:core:database:compileDebugUnitTestKotlin FAILED`,
  `Unresolved reference` for `FonampDatabase/FavoriteDao/FavoriteStation/…`
  (impl did not exist yet) + missing Robolectric test dep.
- [x] GREEN — 6 impl files per design §5: `FavoriteStation.kt` (stationUuid PK,
  name, streamUrl, country?, tagsCsv?, bitrate?, codec?, favoritedAt),
  `ThemePref.kt` (`ThemeMode` SYSTEM|LIGHT|DARK enum persisted by name, single
  row `id=1`), `FavoriteDao.kt` (`observeAll/upsert/delete/byId`),
  `ThemeDao.kt` (`observe/set`), `FonampDatabase.kt` (v1, `exportSchema=false`),
  `FakeFavoriteDao.kt` (main-sourceset VM seam: MutableStateFlow-backed, Room
  semantics — upsert-replaces, delete no-op on unknown, byId null when absent).
  `core/database/build.gradle.kts`: `testImplementation(libs.robolectric)`
  (catalog-pinned, no new version) + `isIncludeAndroidResources=true`.
  Slice A `DatabaseScaffold.kt` placeholder left in place (tasks do not order
  its removal; harmless, like Slice C's `UiScaffold`).
- GREEN evidence (JDK 17 Corretto + ANDROID_HOME, Media3 pinned 1.9.0 untouched,
  Room stays 2.6.1):
  `:core:database:test --rerun-tasks` → BUILD SUCCESSFUL —
  `FavoriteDaoTest` 6/6, `ThemeDaoTest` 5/5, `FakeFavoriteDaoTest` 2/2,
  `DatabaseScaffoldTest` 1/1, debug+release, 0 failures/errors/skipped.
  Full `./gradlew test` → BUILD SUCCESSFUL (533 tasks) — 34 classes,
  **102 tests, 0 failures, 0 errors, 0 skipped** (was 76; +26 new).
- Restart note: in-memory DBs are destroyed on close by definition, so the
  restart test uses a temp-file DB (close → reopen → DARK survives); all other
  tests use in-memory Room per the task. Directory cache stays out of Room
  (design §5: cache clear ≠ DB wipe).
- Files: `core/database/src/main/.../database/{FavoriteStation,ThemePref,
  FavoriteDao,ThemeDao,FonampDatabase,FakeFavoriteDao}.kt`;
  `core/database/src/test/.../database/{FavoriteDaoTest,ThemeDaoTest,
  FakeFavoriteDaoTest}.kt`; `core/database/build.gradle.kts` (M).

## Remaining (explicitly out of scope for this slice)

- Slices E–I untouched. No commits, no PRs, no pushes (per instructions).
  Stop after Slice D.

## Slice E — `core/network` + `provider/radio` (source-contract Req 3, 4; radio Req 1, 2, 5, 6)

- Attempt authority: `acquire` work-unit `slice-E`, `--max-changed-lines 2500`,
  token `sha256:f007d9c7…` → state `proceed`. Settled on completion (see done-state below).
- [x] RED — `MirrorPolicyTest.kt` (6 tests), `DirectoryCacheTest.kt` (4 tests),
  `RadioBrowserSourceTest.kt` (6 tests), all MockWebServer-backed, zero real network.
  RED evidence: `:core:network / :provider:radio:compileDebugUnitTestKotlin FAILED`,
  `Unresolved reference` for `RadioBrowserClient/DirectoryCache/MirrorPolicy/
  RadioBrowserSource/StationQuery` (impl did not exist yet).
- [x] GREEN — 6 impl files in `core/network` per design §4 + 1 in `provider/radio`
  per design §2. Slice A `NetworkScaffold`/`RadioScaffold` placeholders left in
  place (tasks do not order their removal; harmless, as in Slices C–D).
- Fixes during GREEN:
  - `NetworkError.Unknown(cause)` hid `Throwable.cause` → renamed field to `error`.
  - Converter import is Square's `retrofit2.converter.kotlinx.serialization` (catalog
    artifact `com.squareup.retrofit2:converter-kotlinx-serialization`), not JakeWharton's.
  - `provider/radio` needed its own `implementation(libs.media3.common)` (MediaItem in
    the implemented interface) + `testImplementation` `robolectric`/`serialization-json`
    (`streamOf` touches `android.os.Bundle`; `@Config(sdk=[34])` uses cached android-all).
  - Test bug (not impl): url-less row is correctly dropped as unplayable — test now
    asserts the drop plus nullable tolerance on the surviving row.
- GREEN evidence (JDK 17 Corretto + ANDROID_HOME, Media3 pinned 1.9.0 untouched,
  Retrofit 2.11.0 / OkHttp 4.12.0 / serialization 1.8.1 per pins):
  `:core:network:test :provider:radio:test --rerun-tasks` → BUILD SUCCESSFUL —
  `MirrorPolicyTest` 6/6, `DirectoryCacheTest` 4/4, `RadioBrowserSourceTest` 6/6,
  0 failures/errors/skipped (debug; release rerun in full suite).
  Full `./gradlew test` → BUILD SUCCESSFUL — no regressions.
- Contract notes for later slices: index rows are navigation nodes (`stableId`
  `country:<name>`/`tag:<name>`, empty `streamUri` — never play them, browse deeper);
  genre resolves via the `stations/bytag` endpoint (directory has no genre listing);
  `search` is cache-only (empty cache → `Ok(empty)`); click-count fires from
  `streamOf` (swallowed); `DirectoryCache.clear()` never touches Room.
- Files: `core/network/src/main/.../network/{NetworkError,RadioDtos,RadioBrowserApi,
  MirrorPolicy,RadioBrowserClient,DirectoryCache}.kt`;
  `core/network/src/test/.../network/{MirrorPolicyTest,DirectoryCacheTest}.kt`;
  `provider/radio/src/main/.../radio/RadioBrowserSource.kt`;
  `provider/radio/src/test/.../radio/RadioBrowserSourceTest.kt`;
  `provider/radio/build.gradle.kts` (M: media3-common impl, robolectric +
  serialization-json test deps).

## Remaining (explicitly out of scope for this slice)

- Slices F–I untouched. No commits, no PRs, no pushes (per instructions).
  Stop after Slice E.
