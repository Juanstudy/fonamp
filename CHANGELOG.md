# Changelog — fonamp

This project follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/). Release notes are curated separately for users; this file is the technical shipping record.

## [Unreleased]

### Changed
- Synchronized current documentation with shipped behavior through v0.0.13 and separated future roadmap work from current product claims.

## [0.0.13] — 2026-09-23

### Added
- **Podcasts Feature**: Discover, search, listen to, and subscribe to podcasts via the iTunes Search API and RSS feeds. Added new `:feature:podcast` and `:provider:podcast` modules.
- Radio station artwork from RadioBrowser favicons across Discover, search results, station lists, Favorites, and the player, with generic fallbacks.
- A visible **Up next** queue in the player sheet; selecting a row jumps to that queue item.

### Changed
- FavoriteStation now persists `artworkUri` through Room schema migration v1 → v2.

## [0.0.12] — 2026-09-23

### Added
- **Podcasts Feature**: Discover, search, listen to, and subscribe to podcasts via the iTunes Search API and RSS feeds. Added new `:feature:podcast` and `:provider:podcast` modules.
- Best-effort queue and playback-position restore after process death. Restored playback starts paused, never phantom-playing.

### Fixed
- In-app update installation now signals completed and failed downloads through notifications, re-offers a ready APK at the next launch, and confirms when a download starts.

## [0.0.11] — 2026-09-21

### Added
- **Podcasts Feature**: Discover, search, listen to, and subscribe to podcasts via the iTunes Search API and RSS feeds. Added new `:feature:podcast` and `:provider:podcast` modules.
- Player-sheet playback speed, shuffle, and repeat controls (off/all/one).

## [0.0.10] — 2026-09-21

### Added
- **Podcasts Feature**: Discover, search, listen to, and subscribe to podcasts via the iTunes Search API and RSS feeds. Added new `:feature:podcast` and `:provider:podcast` modules.
- In-app GitHub release check with download and system-installer handoff.

## [0.0.9] — 2026-09-20

### Added
- **Podcasts Feature**: Discover, search, listen to, and subscribe to podcasts via the iTunes Search API and RSS feeds. Added new `:feature:podcast` and `:provider:podcast` modules.
- One-shot sleep timer with 5/10/15/30/45/60-minute presets and Off.

## [0.0.8] — 2026-09-20

### Added
- **Podcasts Feature**: Discover, search, listen to, and subscribe to podcasts via the iTunes Search API and RSS feeds. Added new `:feature:podcast` and `:provider:podcast` modules.
- Debounced RadioBrowser station-name search integrated into the Discover filter, including empty/error/retry states and normal play/favorite behavior.
- Real-device screenshot with player controls in the README.

### Fixed
- Closing the mini-player no longer clears the queue (issue #8).
- About reads the version from `BuildConfig` instead of a hardcoded value.

## [0.0.7] — 2026-09-19

### Added
- **Podcasts Feature**: Discover, search, listen to, and subscribe to podcasts via the iTunes Search API and RSS feeds. Added new `:feature:podcast` and `:provider:podcast` modules.
- Player-sheet progress slider, elapsed/total time, and previous/next controls (issue #5).

### Fixed
- Local-file playback no longer fails immediately when offline.
- CI Android setup moved from the removed `tools` package workflow to `setup-android` v4.
- Player review follow-ups: normalized slider fractions, clamped seeks, source-gated transport controls, duration fallback, and sheet gating.

## [0.0.6] — 2026-09-18

### Added
- **Podcasts Feature**: Discover, search, listen to, and subscribe to podcasts via the iTunes Search API and RSS feeds. Added new `:feature:podcast` and `:provider:podcast` modules.
- Signed release pipeline with a private keystore and CI secrets.
- Complete GitHub README with hero, device screenshot, and architecture diagram.

### Changed
- Release builds enable R8 and resource shrinking, with a hard `<40 MB` gate.
- `main` branch protection requires the `build` CI check.

## [0.0.5] — 2026-09-12

### Fixed
- Removed `material-icons-extended`; the debug APK dropped from 67.4 MB to 35.6 MB in the recorded build (issue #2).

## [0.0.4] — 2026-09-12

### Added
- **Podcasts Feature**: Discover, search, listen to, and subscribe to podcasts via the iTunes Search API and RSS feeds. Added new `:feature:podcast` and `:provider:podcast` modules.
- Coil-loaded local album artwork in the mini-player, full player sheet, songs, and albums, with generic fallbacks.

## [0.0.3] — 2026-09-12

### Added
- **Podcasts Feature**: Discover, search, listen to, and subscribe to podcasts via the iTunes Search API and RSS feeds. Added new `:feature:podcast` and `:provider:podcast` modules.
- Curated Discover presets plus Lofi, Chill, and Ambient tag tiles using the standard station-list path.

## [0.0.2] — 2026-09-12

### Added
- **Podcasts Feature**: Discover, search, listen to, and subscribe to podcasts via the iTunes Search API and RSS feeds. Added new `:feature:podcast` and `:provider:podcast` modules.
- Tag-triggered APK release pipeline.
- MediaStore `artworkUri` exposure for local player metadata.
- Issue templates and version conventions in `AGENTS.md`.
