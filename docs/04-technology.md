# Fonamp — Technology Decisions

> Status: current through v0.0.13 · Updated: 2026-09-23

## Stack

| Layer | Current choice | Purpose |
| ----- | --------------- | ------- |
| Language | Kotlin 2.0 / JVM 17 | Android-native state and coroutines |
| UI | Jetpack Compose + Material3 | Declarative screens and shared states |
| Navigation | Navigation Compose | App routes and bottom tabs |
| Player | Media3 ExoPlayer + MediaSessionService | One background playback session |
| DI | Hilt | App graph and source registration |
| Network | Retrofit + OkHttp + kotlinx.serialization | RadioBrowser and GitHub APIs |
| Database | Room | Radio favorites and theme |
| Images | Coil Compose | Local album art and radio favicons |
| Async | Coroutines + Flow/StateFlow | Source, player, and UI state |
| Build | Gradle 8.10, AGP 8.7.3, KSP | Multi-module Android build |
| Tests | JUnit, Robolectric, Turbine, MockWebServer, coroutine-test | Unit/integration seams |

## Current module map

```text
app/                  navigation, Hilt graph, retained holders, update installation
core/player/          PlaybackService, DefaultPlayerManager, queue persistence/restore
core/network/         RadioBrowser/GitHub clients, mirrors, 24-hour directory cache
core/database/        Room favorites and theme
core/permissions/     audio and notification gates
core/ui/              shared states, artwork, mini-player, player sheet, Up next
provider/api/         Source contract, AudioItem, SourceResult, MediaItem mapping
provider/radio/       RadioBrowserSource and curated stations
provider/local/       LocalSource over MediaStore
feature/library/      Collection UI/state
feature/radio/        Discover, station, search, Favorites UI/state
feature/settings/     theme, directory cache, About, update-check UI/state
```

Dependency boundaries:

- `feature/*` modules do not depend on one another.
- `app` is the composition root for navigation, Hilt, Media3 access, queue restore, and APK installation.
- `core/player` consumes Media3 `MediaItem` and source metadata, not concrete provider classes.
- New sources implement `provider/api` and are added to the Hilt source set.

## Source contract

The current `Source` interface is deliberately source-neutral:

```kotlin
interface Source {
    val id: String
    val kind: SourceKind
    suspend fun browse(query: BrowseQuery): SourceResult<List<AudioItem>>
    suspend fun search(q: String): SourceResult<List<AudioItem>>
    fun streamOf(item: AudioItem): MediaItem
}
```

`SourceResult` carries typed success/failure values so UI layers can render offline, timeout, server, and unknown states without importing concrete providers. Downloads are not part of the shipped contract.

## Build and release posture

- Debug: R8/minification disabled; size is monitored, not subject to the hard release gate.
- Release: R8 and resource shrinking enabled.
- Release signing: local `keystore.properties` or `FONAMP_STORE_FILE`, `FONAMP_STORE_PASSWORD`, `FONAMP_KEY_ALIAS`, and `FONAMP_KEY_PASSWORD`; absent signing inputs, local release builds are unsigned.
- CI release: tag push → restore keystore secrets → `assembleRelease` → hard `<40 MB` check → publish `fonamp-<tag>.apk`.
- Last recorded repository size: 7.3 MB on 2026-09-12. Re-measure before making a current-size claim.
- Current source version: `versionName 0.0.13`, `versionCode 13` in `app/build.gradle.kts`.

## Permissions and network posture

The manifest declares exactly seven permissions:

- `READ_MEDIA_AUDIO`
- `READ_EXTERNAL_STORAGE` with `maxSdkVersion=32`
- `INTERNET`
- `FOREGROUND_SERVICE`
- `FOREGROUND_SERVICE_MEDIA_PLAYBACK`
- `POST_NOTIFICATIONS`
- `REQUEST_INSTALL_PACKAGES` for the in-app update system-installer handoff

Radio mirrors, station streams, GitHub release metadata, and downloaded APK assets are the intentional network destinations. Cleartext traffic remains enabled because third-party radio streams may use HTTP. Prohibited ad, analytics, crash-reporting, and Firebase SDKs are audited out.

## Rejected approaches

- Flutter/multiplatform for this Android-first background audio app
- A separate cloud backend or account for core local/radio behavior
- Firebase, ad SDKs, analytics SDKs, and third-party crash SDKs
- Raw ExoPlayer without Media3 session management

## Open technical items

1. Reproducible cold-start performance measurement on a defined mid-range device.
2. Explicit stalled-stream timeout policy and test coverage.
3. Complete artwork-cache ownership and eviction strategy; Coil loading and fallback behavior are shipped, but no full cache manager is exposed.
4. Total app-storage measurement in Settings.
5. Public distribution and any future provider-specific download policy.
