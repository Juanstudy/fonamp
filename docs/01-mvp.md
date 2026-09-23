# Fonamp v1 — MVP Definition and Shipped Baseline

> Status: v1 definition completed; current baseline v0.0.13 · Updated: 2026-09-23

## Goal

Prove a modular Android foundation where local music and internet radio share one stable background player, with source implementations isolated behind a common contract.

## Original v1 scope

### Shipped

1. **12-module scaffold:** app shell, five core modules, three providers, and three features.
2. **Source contract:** `LocalSource` and `RadioBrowserSource` return typed results and map `AudioItem` to Media3 `MediaItem`.
3. **Local collection:** MediaStore songs, artists, and albums after on-demand audio permission.
4. **Radio:** country and genre/tag discovery, 24-hour cache, one-tap playback, and Room favorites.
5. **Player:** one Media3 session, background service, notification controls, and visible failure/retry states.
6. **Settings and theme:** System/Light/Dark persistence plus directory-cache transparency and About information.

### Additions shipped after the original v1 cut

- Local collection search
- Local album artwork and RadioBrowser station artwork
- Station-name search with debounce and retry
- Progress seek, previous/next, and visible Up next queue
- Shuffle, repeat, playback speed, and one-shot sleep timer
- Best-effort queue/position restore after process death
- In-app GitHub release check, download, and system-installer handoff
- Signed R8 release pipeline

## Current acceptance evidence

| Criterion | Status | Current evidence |
| --------- | ------ | ---------------- |
| MediaStore collection after permission | Shipped | `LocalSource` and Collection UI/state |
| Discover → station → playback | Shipped | Radio Discover, station screen, shared `PlayerManager` |
| Background playback and notification controls | Shipped | Media3 `PlaybackService` and `PlayerManager` |
| Designed failure, offline, denied, and empty states | Shipped | Shared states across current feature screens |
| New source without player/tab changes | Shipped architecture seam | `Source` contract plus Hilt source set and `ExpandabilityTest` |
| Signed release below 40 MB | Shipped pipeline | Tag workflow; last recorded size 7.3 MB on 2026-09-12 |
| Unit-test/build gate | Repository gate | `./gradlew test` and `assembleDebug`; not re-run for this documentation-only work |

## Still out of scope

- Podcasts and episode subscriptions
- URL downloads and watched folders
- Navidrome, Jellyfin, Plex, or public streaming providers
- Tab customization, EQ, widgets, and Android Auto
- Play Store distribution and accounts

## Open verification items

- Cold-start performance under two seconds on a defined mid-range device
- A separately enforced playback-stall timeout; current code surfaces typed player failures but does not document a measured stall threshold
- Total app-storage usage in Settings
- A complete artwork-cache eviction policy

These remain open; do not present them as accepted or measured behavior.
