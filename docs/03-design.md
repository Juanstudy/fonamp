# Fonamp — Product Design

> Status: draft · Scope: v1 (+placeholders for v2–v4) · Last updated: 2026-09-10

## 1. UX principles
1. **Audio in ≤3 taps** from cold start — discovery and playback beat chrome.
2. **Every state is designed**: loading, content, empty, offline, denied, error — no blank screens, no crashes-as-UI.
3. **One player, everywhere**: mini-player persistent across tabs; full sheet for detail. Same gestures for every source.
4. **Forgiving by default**: destructive actions (delete download, remove favorite) confirm or undo.

## 2. Information architecture (bottom tabs)
| Tab | v1 | Later |
|-----|----|-------|
| Collection | Local songs/artists/albums | + downloads badge, + subscriptions |
| Radio | Countries + genres/tags → stations | tops/search (if v2 decides) |
| Podcasts | — (hidden) | Shows, subscriptions, new episodes, downloads |
| Downloads | — (hidden) | Queue, history, storage |
| Settings | Theme, cache, storage, about | + tabs order, EQ, sleep timer |

Tabs for future areas are **hidden until their slice ships** (no dead tabs).

## 3. Key flows
### 3.1 Discover → play → favorite (Radio)
Discover → filter country/tag → station list → tap = plays (mini-player appears) →
heart = saved (snackbar + undo). Pull-to-refresh reloads directory. Offline → retry card.

### 3.2 First-run Local
Local tab → system permission sheet → granted: list appears; denied: illustration +
"why we need it" + re-request button. Never a wall before value.

### 3.3 Player
Mini-player (artwork/icon, title, play/pause, close) → tap expands sheet (artwork,
title/artist, source badge [radio/local], ICY metadata if radio, queue controls if
local, heart, error banner with retry when failing).

## 4. Design system (v1: Material3 defaults — decided 2026-09-12)
- Material3 default baseline, light + dark via system setting (CUS-1 satisfied with defaults, no custom tokens in v1).
- No custom color roles, typography scale, or iconography work in v1 — full visual identity deferred to v2.
- Shared state components still required: `Loading / Empty / Offline / Denied / ErrorRetry` (behavior + layout, default styling).

## 5. Screen inventory (v1)
- Collection (list + tabs songs/artists/albums + search if PRD-Q2 says v1)
- Discover (index + filter + station list)
- Favorites (stations; local favorites TBD by PRD-Q3 outcome — v1: stations only)
- Settings (theme, cache clear, storage, licenses, version)
- Player (mini + sheet) — global overlay, not a tab.

## 6. What design does NOT do in v1
No custom visualizers, no widgets, no Auto layout, no onboarding carousel, no artwork/images (generic icons; artwork deferred to v2).
First-run value is immediate: tabs already show content or a designed empty state.
