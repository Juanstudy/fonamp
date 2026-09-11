# Fonamp — Wireframes (v1)

> Status: draft · Text wireframes for review before any UI code · Last updated: 2026-09-10
> Notation: `[x]` button/action, `(...)` placeholder content, `~~~` scrollable list.

## 0. Global chrome
- Bottom tabs (v1): Collection | Radio | Favorites | Settings. (Podcasts/Downloads hidden until their slice.)
- Mini-player docks above bottom tabs on every tab whenever audio plays or pauses.
- Top app bar per tab: screen title + contextual actions (search/filter).

## 1. Collection (home)
```
┌─────────────────────────────┐
│ Fonamp            [search]  │  ← top bar
├─────────────────────────────┤
│ Songs | Artists | Albums    │  ← segmented tabs
├─────────────────────────────┤
│ ~~~ song rows ~~~~~~~~~~~~~ │
│ ▶ Title — Artist      3:42 │  ← tap = play w/ queue
│ ▶ Title — Artist      4:01 │
│ ...                         │
├─────────────────────────────┤
│ ♪ mini-player (if playing)  │
├─────────────────────────────┤
│ Collection Radio ♥Fav ⚙Set │
└─────────────────────────────┘
```
- First run: permission sheet → granted: list; denied: Denied state (see §6).
- Empty (no audio files): Empty state + "how to add music" hint.

## 2. Radio — Discover index
```
┌─────────────────────────────┐
│ Radio              [↻]      │  ← ↻ = pull/refresh directory
├─────────────────────────────┤
│ [filter country/genre...]   │
├─────────────────────────────┤
│ Countries | Genres & tags   │  ← segmented
├─────────────────────────────┤
│ ~~~ index rows ~~~~~~~~~~~~ │
│ 🇦🇷 Argentina        1.2k  │  ← tap = station list
│ 🇩🇪 Germany          3.4k  │
│ ...                         │
├─────────────────────────────┤
│ ♪ mini-player (if playing)  │
├─────────────────────────────┤
│ tabs...                     │
└─────────────────────────────┘
```
- Offline / fetch fail: Offline card with [retry] (cached index shown if available).

## 3. Radio — Station list
```
┌─────────────────────────────┐
│ ← Argentina                 │
├─────────────────────────────┤
│ ~~~ station rows ~~~~~~~~~~ │
│ ▶ Radio Mitre    128k [♡] │  ← tap plays, ♡ saves
│ ▶ La 100         64k  [♥] │
│ ...                         │
└─────────────────────────────┘
```

## 4. Favorites
```
┌─────────────────────────────┐
│ Favorites                   │
├─────────────────────────────┤
│ ~~~ saved stations ~~~~~~~~ │
│ ▶ Name  country [▶][x]     │  ← play / remove (undo snackbar)
│ ...                         │
│ (empty: "No favorites yet — │
│  tap ♡ on any station")     │
└─────────────────────────────┘
```

## 5. Settings
```
┌─────────────────────────────┐
│ Settings                    │
├─────────────────────────────┤
│ Appearance                  │
│   Theme: (System|Light|Dark)│
│ Storage                     │
│   Directory cache   [clear] │
│   Used: 12 MB               │
│ About                       │
│   Version, licenses, source │
└─────────────────────────────┘
```

## 6. Player — mini + sheet
```
mini: ┌───────────────────────────┐
      │ [art] Title — Artist  [⏸] │  ← tap expands, ⏸ toggles
      └───────────────────────────┘
sheet:
┌─────────────────────────────┐
│  (drag handle)        [x]   │
│                             │
│         [artwork]           │  ← generic icon in v1
│      Title                  │
│      Artist · source badge  │  ← [radio] / [local]
│      ICY metadata (radio)   │
│  ◀◀  [⏸]  ▶▶  + [♡]       │  ← radio: play/stop only
│  ─────●────────  1:12/3:42 │  ← local only (seek)
│  ⚠ stream error [retry]    │  ← only on failure
└─────────────────────────────┘
```

## 7. Shared states (one component each)
- **Loading**: shimmer rows (never blank).
- **Empty**: icon + 1-line why + action (e.g. "No favorites yet").
- **Offline**: icon + "No connection" + [retry] (+ cached content if any).
- **Denied** (permission): icon + why-needed text + [grant again].
- **Error** (stream): banner in player + toast-free inline message + [retry].

## 8. Explicitly NOT in v1 wireframes
Podcasts/Downloads tabs, EQ screen, tab editor, sleep timer UI, widgets, Auto.
(Placeholders for hidden tabs: none — tabs simply absent.)
