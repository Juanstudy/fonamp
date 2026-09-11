# Fonamp — Technology Decisions

> Status: draft · Binding for v1 unless a written ADR changes it · Last updated: 2026-09-10

## 1. Stack (v1)
| Layer | Choice | Why |
|-------|--------|-----|
| Language | Kotlin | Null-safety, coroutines, first-class Android support |
| UI | Jetpack Compose (Material3) | Lists/filters/states velocity; single paradigm |
| Player | Media3 (ExoPlayer) + MediaSessionService | Background, notification, headset, queue in one API |
| DI | Hilt (decided) | Standard, testable seams via interfaces |
| Network | Retrofit + OkHttp (cache) | Directory APIs + cache control |
| Serialization | kotlinx.serialization (decided) | Kotlin-first, no reflection, lighter than Moshi |
| Database | Room | Favorites, subscriptions/downloads later |
| Async | Coroutines + Flow/StateFlow | UI state + player state |
| Background work | Foreground service (player, v1); WorkManager (downloads, v4) | Right tool per job |
| Tests | JUnit + Robolectric + Turbine + MockWebServer | JVM tests without devices; player seams keep VMs Robolectric-free where possible |
| Build | Gradle 8.10 + AGP 8.7, JDK 17 | Validated in prototype (JDK 26 does NOT run this Gradle) |

## 2. Module map (v1 target)
```
app/                  wiring + navigation only
core/player/          PlaybackService, PlayerManager, mappers, errors
core/network/         RadioBrowser client, mirrors, cache
core/database/        Room base, DAOs
core/permissions/     runtime permission helpers
core/ui/              design system + shared states
provider/api/         Source contract (browse/search/tracks/download)
provider/radio/       RadioBrowserSource implements Source
provider/local/       LocalSource implements Source (MediaStore)
feature/library/      Collection UI + VMs
feature/radio/        Discover UI + VMs
feature/settings/     Settings UI
```
Rules: `feature/*` never depend on each other; all share `core/*` + `provider/api`;
`app` wires everything. New source (v2+) = new `provider/*` module, no player/UI-shell changes.

## 3. The `Source` contract (v1 sketch, finalized in design phase)
```kotlin
interface Source {
  val id: String               // "local", "radio-browser", ...
  suspend fun browse(query: BrowseQuery): List<AudioItem>
  suspend fun search(q: String): List<AudioItem>
  fun streamOf(item: AudioItem): MediaItem   // maps to the single player
  // downloads (v3): fun downloadOf(item): DownloadRequest?
}
```
Player consumes `MediaItem` only — it never knows which source produced it.

## 4. Explicitly rejected (with reason)
- **Flutter / multiplatform**: audio/background friction on Android; native wins for a player.
- **Go/gomobile core**: JNI complexity, kills background simplicity (validated in prototypediscussions).
- **Firebase / analytics / crash SDKs**: violates principles 4–5.
- **Ad SDKs**: violates principle 5, permanently.
- **Raw ExoPlayer without Media3 session**: more code for notification/background done for free.

## 5. Dependency allowlist process
Anything beyond §1 needs 3 lines in the requesting doc: what, why existing libs can't do it,
size/license impact. Reviewer (founder) approves. No drive-by dependencies.

## 6. Environment (validated)
- JDK 17 (Corretto), `ANDROID_HOME` → SDK with platform-34/35 + build-tools.
- `minSdk 29` (Android 10+, decided), `target/compileSdk 35`, `usesCleartextTraffic=true` (third-party radio streams are http).
- Commands: `./gradlew test`, `./gradlew assembleDebug`, `./gradlew installDebug`.

## 7. Open technical questions
1. R8/minify + app bundle posture for release (later; debug APK fine for v1).
2. Artwork cache strategy — RESOLVED: deferred to v2 (generic icons in v1).
3. Player queue persistence across process death — v1 best-effort or v2?
