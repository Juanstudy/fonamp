# Fonamp — Product Vision (Global Documentation)

> Status: draft · Owner: founder (primary user) · Last updated: 2026-09-10

## 1. What is Fonamp
Fonamp (*fono*: sound + phone, *amp*: amplifier lineage) is a personal, open-source,
all-in-one audio hub for Android: your music (local + downloads), radio, and podcasts
in a single lightweight player — customizable, expandable, secure, with zero ads.

## 2. Problem
A heavy listener (music 24/7, plus radio, podcasts, downloads) currently juggles 3–4
apps: a local player, a radio app, a podcast app, a downloader. No unified collection,
no integrated offline, and nearly every app ships ads and trackers. No open-source
stable combo exists in one app (validated during the cliamp-android prototype, 2026-09).

## 3. Target users
- **Primary: the founder.** Power listener, music 24/7, occasional radio/podcasts, likes
  downloading music. Every decision is validated against this user first.
- **Secondary (future):** privacy-minded Android users wanting one ad-free audio app.

## 4. Vision statement
One app opens to *your collection* — everything you listen to, online or offline —
playing through one stable player, shaped the way you want it.

## 5. Product principles (permanent constraints)
1. **Customizable** — themes, tab/home order, EQ/audio, behavior (sleep timer, speed).
2. **Expandable** — every audio source implements the `Source` contract
   (browse / search / tracks / download); adding a source is days, not weeks.
3. **Lightweight** — minimal dependency footprint, reasonable APK size, respectful battery.
4. **Secure** — minimal permissions granted on demand, zero trackers, downloads in own storage.
5. **Ad-free** — no ad SDK ever; an architectural constraint, not a promise.

## 6. Product areas
| Area | Content | Slice |
|------|---------|-------|
| Collection | Local files + downloads + subscriptions, unified | v1 (local), v4 (downloads) |
| Radio | Directory discovery (country/genre/tag), favorites | v1 |
| Podcasts | Directory + subscriptions + offline episodes | v2 |
| Downloads | URL audio download, watched folder, queue | v4 |
| Providers | Self-hosted (Navidrome/Jellyfin/Plex), public streaming, Spotify last | v3 |
| Player | Single Media3 player, background + notification, queue | v1 |
| Personalization | Theme → tabs → EQ → behavior | v1 (theme), v2+ (rest) |

## 7. Roadmap
- **v1 — Foundation + Local + Radio.** Modular structure, `Source` contract, local via
  MediaStore, radio discovery, favorites, background player, base settings + theme.
- **v2 — Podcasts.** Directory + subscriptions, new-episode feed, offline episodes,
  playback speed, sleep timer.
- **v3 — External providers.** Self-hosted first, public streaming next, Spotify last
  (SDK/DRM cost). One proposal per provider (auth, cache, offline, cost).
- **v4 — Downloads.** Paste-URL audio download, watched folder, WorkManager queue.

## 8. Forever non-goals
- Ads or ad/mediation SDKs, analytics/crash SDKs phoning home (self-hosted crash
  reporting only if ever needed).
- iOS port, own cloud backend/accounts for core features (self-hosted providers are
  *user-owned* servers, not ours).
- Social features, comments, sharing feeds.

## 9. Success signals
- Daily driver for the founder within v2 (replaces 3 apps).
- v1 expandability proof: a mock `Source` implemented in <1 day.
- Cold start to audio ≤3 taps; stable background playback; test suite green.
