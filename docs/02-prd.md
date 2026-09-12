# Fonamp — Product Requirements Document (PRD)

> Status: draft · Covers: v1 (binding) + v2–v4 (directional) · Last updated: 2026-09-10

## 1. Background
See `00-vision.md` (problem, users, principles) and `01-mvp.md` (v1 scope).
The prototype (`~/Projects/cliamp-android`, archived) validated: Media3 single player,
radio-browser.info integration with mirror fallback, background + notification,
Room favorites, Compose UI. This PRD carries those learnings forward — patterns, not code.

## 2. Functional requirements

### 2.1 Collection (v1: local · v3: +downloads)
- **LIB-1** App lists device audio (MediaStore) grouped by songs/artists/albums with zero setup. *(v1)*
- **LIB-2** Audio permission is requested on first entering Local, with explanatory empty state if denied. *(v1)*
- **LIB-3** Downloaded files (v4) appear in the same Collection with an offline badge and storage management (per-item delete, total size). *(v3)*
- **LIB-4** Search across collection (title/artist/album). *(v2, stretch for v1 if cheap)*

### 2.2 Radio (v1)
- **RAD-1** Browse directory by country with station counts + text filter.
- **RAD-2** Browse by genre/tag with text filter.
- **RAD-3** Station list shows name and bitrate/codec when available, with generic icons (artwork deferred to v2).
- **RAD-4** Favorite/unfavorite in 1 tap from list and player; persisted locally.
- **RAD-5** Pull-to-refresh directory; cached index (24h) with explicit offline + retry state.
- **RAD-6** Click-count reported to radio-browser on play (fire-and-forget, never blocks playback).

### 2.3 Player (v1)
- **PLY-1** One player for every source (radio streams + local files + later podcasts/downloads).
- **PLY-2** Background playback with media notification (play/pause/next/prev) and headset controls.
- **PLY-3** Radio: play/stop + ICY metadata when available. Local: queue with next/prev + shuffle (shuffle: v1 or v2 — open).
- **PLY-4** Stream failures: 10s timeout, visible message, manual retry; never freezes UI.
- **PLY-5** Mini-player on every tab + full player sheet with artwork/metadata.

### 2.4 Podcasts (v2, directional)
- **POD-1** Search shows, subscribe, new-episode feed.
- **POD-2** Per-episode download for offline + auto-download rules (e.g. latest N, Wi-Fi only).
- **POD-3** Playback speed + sleep timer (sleep timer also serves music).
- **POD-4** OPML import (stretch).

### 2.5 Downloads (v4, directional)
- **DL-1** Paste URL → audio download with progress, queue, retry (WorkManager).
- **DL-2** Watched folder: files copied to device are picked up automatically.
- **DL-3** Download source scope decided in v4 proposal (legal/ToS review per source type).

### 2.6 Providers (v3, directional)
- **PRV-1** Self-hosted (Navidrome/Jellyfin/Plex) against the `Source` contract.
- **PRV-2** Public streaming sources per own proposals.
- **PRV-3** Spotify only if justified (SDK/DRM cost documented first).

### 2.7 Customization (v1: theme · rest later)
- **CUS-1** Light/dark theme. *(v1)*
- **CUS-2** Tab order/visibility. *(v2+)*
- **CUS-3** Parametric EQ + presets. *(v2+)*
- **CUS-4** Sleep timer, playback speed (podcasts first). *(v2)*

### 2.8 Settings
- **SET-1** Theme, cache management (directory + artwork), storage usage, licenses, version.
- **SET-2** Every permission justified inline at request time (no pre-permission walls).

## 3. Non-functional requirements
- **Performance**: cold start to interactive <2s on mid-range; directory lists cached, paged.
- **Size**: release APK <40 MB (validated post-v1 when the release pipeline exists); debug APK monitored with no hard gate in v1 (currently ~63 MB); R8/minify posture post-v1; no dependency without justification (allowlist, see `04-technology.md`).
- **Battery**: no polling; downloads/uploads only on demand; foreground service only while playing.
- **Reliability**: player survives network loss (error state, not crash); process-death restore of queue where feasible.
- **Security/privacy**: INTERNET + audio + foreground-playback (+notifications) only; cleartext only where streams require it; zero third-party trackers; no data leaves the device except user-configured servers and directory APIs.
- **Accessibility**: touch targets ≥48dp, content descriptions on controls, dynamic text support.

## 4. Constraints
- Android native, Kotlin; minSdk 29 (Android 10+, decided).
- Dependency allowlist: ExoPlayer/Media3, Room, Retrofit/Moshi, Hilt, Coroutines/Flow, Compose BOM + testing libs. Anything else needs a written reason.
- No ad/analytics/crash SDKs. No Firebase.

## 5. Open questions (resolved for v1 — 2026-09-12)
1. Shuffle/repeat — RESOLVED: no shuffle/repeat in v1 (local next/prev, radio play/stop; shuffle deferred to v2).
2. Collection search — RESOLVED: stretch goal, simple title/artist/album filter only if cheap; cut if costly (otherwise v2).
3. Favorites sync — RESOLVED: local-only for v1 (Room, no sync); sync/export model TBD post-v1.
4. Download source scope for v4 (legal/ToS) — OPEN, deferred to v4 proposal.
5. Release signing / distribution — RESOLVED for v1 dev: direct APK only; F-Droid/Play deferred to a future public build.
