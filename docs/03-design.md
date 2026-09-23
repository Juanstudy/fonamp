# Fonamp — Product Design

> Status: current UI through v0.0.13 · Updated: 2026-09-23

## UX principles

1. **Audio quickly** — discovery and playback take priority over chrome.
2. **Every state is designed** — loading, empty, offline, denied, and error states are explicit.
3. **One player everywhere** — the mini-player persists across tabs; the full sheet holds detail and controls.
4. **Honest data** — render only available artwork and metadata, with clear generic fallbacks.
5. **No roadmap ghosts** — tabs and controls appear only when their slices ship.

## Information architecture

| Bottom tab | Shipped through v0.0.13 | Planned, not shipped |
| ----------- | ------------------------ | --------------------- |
| Collection | Songs, artists, albums, local search, artwork, play-from-queue | Downloads, subscriptions, history |
| Radio | Discover, curated tiles, local filter, name search, station lists, refresh/offline | Tops/random and richer discovery |
| Favorites | Room-backed radio stations with artwork, play, remove, and undo | Local-track favorites and sync/export |
| Settings | Theme, directory-cache stats/clear, About, update check/download | Total storage, artwork cache, EQ, tab editor |

Podcasts and Downloads remain hidden because those surfaces are not implemented.

## Key flows

### Discover → search or play

1. Open Radio.
2. Browse a country/tag, tap a curated tile, or type at least two characters to search station names.
3. Station results use the normal artwork, play, and favorite behavior.
4. A failed refresh/search keeps the index or cached list visible and exposes retry.

### First run in Collection

1. Enter Collection.
2. Grant audio access when prompted.
3. Browse songs, artists, or albums; local search narrows title/artist/album text without another network call.
4. A denied or empty permission state explains the next action without blocking Radio, Favorites, or Settings.

### Playback

- A persistent mini-player shows artwork, title, source, play/pause, and close.
- Close dismisses the player chrome without clearing the saved playback context.
- The full sheet adds metadata, ICY title when available, favorite, retry, and source-appropriate controls.
- Local items expose progress/seek and queue controls; radio uses stop semantics and no seek bar.
- Both sources expose shuffle, repeat, playback speed, and sleep timer.
- Multi-item local queues show **Up next**; tapping a row jumps within the current context.
- A one-shot sleep timer offers 5/10/15/30/45/60 minutes and Off.

### Updates

- The app checks GitHub Releases at cold start; Settings also offers a manual check.
- A newer installable release presents Download and Later.
- Download progress/completion is handled outside Settings, and installation always hands off to the Android system installer.

## Design system baseline

- Material3 light/dark themes with System, Light, and Dark selection persisted in Room.
- Shared `Loading`, `Empty`, `Offline`, `Denied`, and `ErrorRetry` states.
- Playback actions use at least 48dp targets and content descriptions.
- Coil loads local album art and RadioBrowser favicons; generic icons remain visible when art is missing or fails.

## Current screen inventory

- Collection
- Radio Discover
- Radio station list
- Favorites
- Settings
- Global mini-player
- Global player sheet
- About update dialog and completion/failure notifications

## Not current UI

Podcasts, Downloads, EQ, tab editor, widgets, Android Auto, total-storage management, and artwork-cache controls are roadmap work. Do not show them in current wireframes or release claims.
