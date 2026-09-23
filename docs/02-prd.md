# Fonamp — Product Requirements

> Status: current through v0.0.13 · Updated: 2026-09-23

## Status legend

- **Shipped:** implemented and present in current source.
- **Planned:** roadmap requirement; no current app surface.
- **Open:** requirement or claim still needs design, evidence, or verification.

## Product requirements

### Collection

| ID | Requirement | Status |
| -- | ----------- | ------ |
| LIB-1 | List MediaStore audio as songs, artists, and albums without accounts or manual configuration | Shipped |
| LIB-2 | Request audio-read permission on entering Collection and provide a designed denied state | Shipped |
| LIB-3 | Keep filter context across the artist/album/song views | Shipped |
| LIB-4 | Filter the local collection by title, artist, or album without network calls | Shipped |
| LIB-5 | Show MediaStore album artwork with a generic fallback | Shipped |
| LIB-6 | Merge downloaded files into the collection with storage management | Planned |

### Radio

| ID | Requirement | Status |
| -- | ----------- | ------ |
| RAD-1 | Browse countries and genres/tags, then open a station list | Shipped |
| RAD-2 | Show station counts and optional bitrate/codec metadata honestly | Shipped |
| RAD-3 | Cache the directory for 24 hours and keep cached content visible with offline/retry states | Shipped |
| RAD-4 | Play stations in one tap and report click counts without blocking playback | Shipped |
| RAD-5 | Search station names through the Discover filter after two characters and a debounce | Shipped |
| RAD-6 | Show curated Lofi, Chill, and Ambient entry points through the normal tag path | Shipped |
| RAD-7 | Favorite/unfavorite stations in Room and support undo after removal | Shipped |
| RAD-8 | Show RadioBrowser favicons with generic fallbacks | Shipped |
| RAD-9 | Add tops/random browsing | Planned |

### Player

| ID | Requirement | Status |
| -- | ----------- | ------ |
| PLY-1 | Route local files and radio through one Media3 background session | Shipped |
| PLY-2 | Keep notification and headset controls synchronized with playback | Shipped |
| PLY-3 | Offer local seek, previous/next, and a visible Up next queue | Shipped |
| PLY-4 | Offer shuffle, repeat off/all/one, playback speed, and a one-shot sleep timer | Shipped |
| PLY-5 | Surface stream errors inline with retry without freezing the rest of the UI | Shipped |
| PLY-6 | Restore queue and position best-effort after process death, paused rather than auto-playing | Shipped |
| PLY-7 | Enforce and measure a specific 10-second stalled-stream threshold | Open |

### Podcasts, downloads, and providers

| ID | Requirement | Status |
| -- | ----------- | ------ |
| POD-1 | Discover podcasts, subscribe, and expose new episodes | Planned |
| POD-2 | Download and organize episodes offline | Planned |
| DL-1 | Download audio from a pasted URL with progress, retry, and WorkManager | Planned |
| PRV-1 | Integrate self-hosted providers behind `Source` | Planned |
| PRV-2 | Evaluate public streaming providers one proposal at a time | Planned |

### Customization and settings

| ID | Requirement | Status |
| -- | ----------- | ------ |
| CUS-1 | System/Light/Dark theme persisted in Room and applied immediately | Shipped |
| CUS-2 | Playback speed and sleep timer in the player sheet | Shipped |
| CUS-3 | Tab order/visibility and parametric EQ | Planned |
| SET-1 | Directory-cache entry count, size, clear action, and confirmation | Shipped |
| SET-2 | About version from `BuildConfig`, license pointer, and source summary | Shipped |
| SET-3 | Manual and cold-start GitHub update checks with download and installer handoff | Shipped |
| SET-4 | Total app-storage reporting and an artwork-cache manager | Planned |

## Non-functional requirements

| Area | Current requirement/status |
| ---- | ------------------------- |
| Privacy | Seven manifest permissions only; no prohibited ad, analytics, crash, or Firebase SDKs; update and directory egress is explicit |
| Size | Release R8 + resource shrinking; hard `<40 MB`; last recorded release size 7.3 MB on 2026-09-12; debug is monitored |
| Reliability | Typed source/player errors, explicit retry, best-effort paused queue restore |
| Accessibility | Playback actions use at least 48dp targets and content descriptions; dynamic-text review remains ongoing |
| Performance | Lists and cache behavior exist, but the under-two-second cold-start target has no current reproducible measurement |

## Current constraints

- Native Android/Kotlin, `minSdk 29`, `compile/targetSdk 35`, JDK 17
- Twelve modules with `feature/*` isolation and app-level wiring
- Direct signed APK distribution; no Play Store or required account
- Cleartext HTTP remains enabled because arbitrary third-party radio streams may use it
- No ad/analytics/crash SDKs or Firebase

## Open product decisions

1. Public-store distribution and release-signing policy beyond internal APK testing.
2. Playback-stall timeout target and how to measure it.
3. Queue persistence scope and retention policy.
4. Artwork cache ownership, eviction, and Settings controls.
5. External-provider order and licensing review.
