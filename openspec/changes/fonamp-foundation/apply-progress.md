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
