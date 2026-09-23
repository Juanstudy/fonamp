# Fonamp — Product Vision

> Status: active · Current baseline: v0.0.13 · Updated: 2026-09-23

## Vision

Fonamp is a lightweight, open-source Android audio hub: local music and internet radio in one stable, private player. The long-term vision also includes podcasts, downloads, and user-controlled providers, but those areas are not shipped.

## Problem

Listeners often juggle separate apps for local music, radio, podcasts, and downloads. Those apps commonly add ads, trackers, accounts, and inconsistent playback. Fonamp aims to provide one focused player without those costs.

## Users

- **Primary — founder/internal tester:** validates daily listening, direct-install releases, and a lightweight daily-driver workflow.
- **Secondary — future:** privacy-minded Android users who want a small, ad-free player.

## Product principles

1. **Private by default** — no ads, analytics, or crash-reporting SDKs; permissions are requested only when needed.
2. **One reliable player** — every shipped source uses the same Media3 background session.
3. **Lightweight and inspectable** — a modular native app with explicit dependency and release gates.
4. **Honest metadata** — show available artwork and fields; use generic fallbacks instead of fabricating data.
5. **Expandable sources** — new sources implement the shared `Source` contract without coupling to the player.
6. **Roadmap discipline** — planned product areas stay hidden and are never described as shipped.

## Product status

| Area | Status through v0.0.13 | Next direction |
| ---- | ------------------------ | -------------- |
| Local collection | **Shipped:** MediaStore songs/artists/albums, local search, artwork, queue playback | History, richer library organization, downloads |
| Radio | **Shipped:** directory discovery, 24-hour cache, curated tiles, name search, favorites, artwork | Improve resilience and discovery UX |
| Player | **Shipped:** background playback, seek, next/previous, Up next, shuffle/repeat/speed, sleep timer, best-effort restore | Source-specific enhancements and richer queue policy |
| Settings | **Shipped:** theme, directory-cache stats/clear, About, GitHub update check | Total storage reporting, richer cache controls |
| Podcasts | **Current** | Directory, subscriptions, episodes (online playback only) |
| Downloads | **Planned** | URL downloads, retry queue, watched folders |
| External providers | **Planned** | Self-hosted providers first; evaluate public services separately |
| Advanced customization | **Planned** | Tab order, EQ, richer behavior settings |

## Roadmap

### Shipped foundation

- Modular local-library and RadioBrowser providers
- One Media3 player with background service and notification controls
- Room favorites and theme persistence
- Material3 UI, designed loading/empty/offline/denied/error states
- Direct-install release pipeline

### Planned slices

- **Podcasts:** directory, subscriptions, new episodes, offline episodes
- **Providers:** Navidrome/Jellyfin/Plex proposals with explicit auth, cache, offline, and licensing tradeoffs
- **Downloads:** paste-URL downloads, WorkManager queue, watched folders
- **Personalization:** tab order, EQ, richer storage/cache controls

## Non-goals

- Advertising or ad/mediation SDKs
- Analytics or crash SDKs that send data to third parties
- Social feeds, comments, or social sharing
- A Fonamp-operated cloud account or backend for core features
- Silent APK installation; Android still shows the system installer

## Success signals

- Stable daily playback for the founder on a real device
- Fast path to local or radio audio with designed recovery states
- A new source can be added behind `provider/api` without changing `core/player` or bottom-tab navigation
- CI remains green and the signed release stays below 40 MB
- Documentation consistently distinguishes shipped behavior from proposals
