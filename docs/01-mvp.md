# Fonamp v1 — MVP Definition

> Status: draft · Slice: v1 (Foundation + Local + Radio) · Last updated: 2026-09-10

## 1. MVP goal
Prove the foundation: a modular app where **local music + radio discovery** play through
**one stable background player**, and where a new audio source can be added against the
`Source` contract without touching the player or the UI shell.

## 2. User
The founder, validating daily use: discover a station in seconds, switch to local music,
keep it playing with the screen off.

## 3. Scope — IN
1. **Modular scaffold**: `app`, `core/player`, `core/network`, `core/database`,
   `provider/api`, `feature/library`, `feature/radio`, `feature/settings`, design system.
2. **`Source` contract** (`provider/api`) + `LocalSource` + `RadioBrowserSource`.
3. **Local**: MediaStore auto-load (songs/artists/albums), permission on demand.
4. **Radio**: country/genre/tag index with filter, station lists, 1-tap play, favorites (local).
5. **Player**: single Media3/ExoPlayer instance, background service + notification controls,
   stream error handling (timeout, visible error, retry).
6. **Settings + theme**: light/dark base theme (first customization slice).

## 4. Scope — OUT (explicit non-goals for v1)
- Podcasts, URL downloads, external providers.
- Advanced customization (tab order, EQ, sleep timer, speed) — theme only.
- Android Auto, widgets, history, M3U import, tops/random/name search.
- iOS, backend, accounts.

## 5. Acceptance criteria
- [ ] Fresh install → local collection visible with zero configuration (after audio permission).
- [ ] Discover → filter → play a station in ≤3 taps from launch.
- [ ] Audio continues with screen off; notification play/pause/next/prev respond.
- [ ] Dead stream → visible error + retry; UI never freezes; offline directory → explicit retry state.
- [ ] Permission denied → explanatory empty state, no broken screen.
- [ ] A mock `Source` (e.g. bundled demo feed) builds against `provider/api` in <1 day, no player/UI-shell changes.
- [ ] `./gradlew test` green; APK debug <40 MB; permissions limited to audio + internet + foreground playback (+notifications).

## 6. Risks
- "All-in-one" scope creep → defense: per-slice non-goals (this file).
- Unstable third-party streams → cache + multi-mirror fallback (validated in prototype).
- Radio Browser downtime → cached directory + explicit offline state.

## 7. What v1 does NOT need to prove
Monetization, growth, multi-user sync, or provider breadth. v1 proves **foundation +
daily usability + expandability**.
