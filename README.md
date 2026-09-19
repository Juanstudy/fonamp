<!-- HERO SWAP: when the AI-generated raster hero is ready, change the src below
     from ./assets/readme/hero.svg to ./assets/readme/hero.png (one-line change). -->
<p align="center">
  <img src="./assets/readme/hero.png" width="100%" alt="fonamp — local library plus internet radio in one fast, tiny, tracker-free Android player">
</p>

<h1 align="center">fonamp</h1>

<p align="center">
  <a href="https://github.com/Juanstudy/fonamp/actions/workflows/android.yml"><img src="https://img.shields.io/github/actions/workflow/status/Juanstudy/fonamp/android.yml?label=build" alt="CI build status"></a>
  <a href="https://github.com/Juanstudy/fonamp/releases"><img src="https://img.shields.io/github/v/release/Juanstudy/fonamp?label=release" alt="Latest release"></a>
  <img src="https://img.shields.io/badge/minSdk-29-blue" alt="minSdk 29">
  <img src="https://img.shields.io/badge/targetSdk-35-blue" alt="targetSdk 35">
  <img src="https://img.shields.io/badge/APK-7.3_MB-green" alt="Release APK 7.3 MB">
</p>

**fonamp** is a lightweight native Android music player: your on-device library (MediaStore) and internet radio (RadioBrowser) in one fast, tiny app — with curated Lofi/Chill/Ambient tiles for instant listening. No ads, no trackers, no Firebase.

> [!NOTE]
> fonamp is in internal testing (v0.0.7). Releases ship a signed APK for direct install — not via Play Store yet. See [CHANGELOG.md](./CHANGELOG.md) for what each version brings.

## Screenshots

<p align="center">
  <img src="./assets/readme/screenshot.jpeg" width="360" alt="fonamp on a real device: Discover radio, Library songs, and the Now-Playing sheet with progress slider and transport controls">
</p>

*Real device capture (v0.0.7): Discover radio, Library, and Now-Playing with the new progress slider.*

## Why fonamp

| Need | What you get |
| ---- | ------------ |
| Local music | Browse songs, albums, and artists from MediaStore, with Coil-loaded artwork and a fast mini-player |
| Internet radio | Search and play RadioBrowser stations, favorite them into a Room database |
| Instant mood | Curated Lofi / Chill / Ambient tiles with the same play + favorite flow |
| Privacy | Zero ads, zero trackers, zero Firebase — verified by CI on every push |

## Proof, not promises

- **7.3 MB signed release APK** (R8 + shrink; debug is ~34 MB and intentionally unminified for speed).
- **6-permission allowlist**, enforced in CI: `READ_MEDIA_AUDIO`, `READ_EXTERNAL_STORAGE`, `INTERNET`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `POST_NOTIFICATIONS`.
- **CI gates on every push**: toolchain audit (12 modules, pins, SDKs) → unit tests → debug APK → tracker/permission/size audits.

> [!TIP]
> The release gate fails the build if the APK ever exceeds 40 MB.

## How it is wired

<p align="center">
  <img src="./assets/readme/architecture.svg" width="100%" alt="Module map: the app shell hosts library, radio, and settings features over local, API, and radio providers and player, network, database, permissions, and UI core modules">
</p>

One `app` shell (navigation, Hilt DI, Media3 session) hosts three features — `library`, `radio`, `settings` — over `provider:local/api/radio` sources and `core:player/network/database/permissions/ui`. Playback runs through Media3 with a foreground service; favorites and theme live in Room.

## Get it

**Quick path (users):**

1. Open [GitHub Releases](https://github.com/Juanstudy/fonamp/releases).
2. Download `fonamp-<tag>.apk` (signed release, internal testing).
3. Install on Android 10+ (SDK 29) and grant media + notification permissions.

**Quick path (developers):**

1. Requirements: JDK 17, Android SDK (compile/target 35).
2. `./gradlew installDebug` — builds and installs the debug APK.
3. `./gradlew test` — runs the unit-test gate; `./scripts/audit-gates.sh` runs the tracker/permission/size audits.

## Compatibility

| Item | Value |
| ---- | ----- |
| minSdk / targetSdk | 29 (Android 10) / 35 |
| JDK / language | 17 (Corretto in CI) / Kotlin |
| UI / playback / DI | Jetpack Compose / Media3 / Hilt |
| Images / storage | Coil / Room |

More detail: [CHANGELOG.md](./CHANGELOG.md) tracks every version; `docs/` holds deeper notes.
