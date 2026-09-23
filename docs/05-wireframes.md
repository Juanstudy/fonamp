# Fonamp — Shipped Wireframes

> Status: current through v0.0.13 · Updated: 2026-09-23
> Notation: `[x]` action/button, `(...)` content, `~~~` scrollable content.

## Global chrome

- Bottom tabs: **Collection | Radio | Favorites | Settings**.
- The mini-player appears above the tabs while audio is playing or paused.
- A missing or failed artwork URI falls back to a generic icon.
- Closing the mini-player or sheet dismisses the chrome without clearing the queue.

## Collection

```text
┌─────────────────────────────┐
│ Collection          [search]│
├─────────────────────────────┤
│ Songs | Artists | Albums    │
├─────────────────────────────┤
│ ~~~ song rows ~~~~~~~~~~~~~ │
│ [art] Title — Artist   3:42 │
│ [art] Title — Artist   4:01 │
│ ...                         │
├─────────────────────────────┤
│ ♪ mini-player, if present  │
├─────────────────────────────┤
│ Collection Radio ♥Favorites │
│                    Settings │
└─────────────────────────────┘
```

- Search filters the current local song list by title, artist, or album.
- First entry requests audio permission; denied and no-audio states remain designed.
- Tapping a song starts the visible filtered queue at that index.

## Radio — Discover

```text
┌─────────────────────────────┐
│ Curadas             [radio] │
│ [Lofi] [Chill] [Ambient]    │
├─────────────────────────────┤
│ [Filter countries or tags]  │ [refresh]
│ Countries | Genres & tags   │
│ ~~~ index rows ~~~~~~~~~~~~ │
│ Country / tag · station count│
├─────────────────────────────┤
│ Search results              │
│ [art] Station  128k [♡]     │
│ [art] Station   64k [♡]     │
│ loading / empty / [retry]   │
├─────────────────────────────┤
│ ♪ mini-player, if present  │
├─────────────────────────────┤
│ Collection Radio ♥Favorites │
│                    Settings │
└─────────────────────────────┘
```

- The filter narrows the local index immediately.
- Queries of two or more characters also trigger a debounced RadioBrowser name search.
- Curated tiles and station rows use the standard play/favorite path and station artwork.
- Offline/failure states preserve usable cached or index content where available.

## Radio — Station list

```text
┌─────────────────────────────┐
│ [back] Country or tag [radio]│ [refresh]
├─────────────────────────────┤
│ [art] Station       128k [♡] │
│ [art] Station        64k [♡] │
│ ...                         │
└─────────────────────────────┘
```

- Bitrate and codec appear only when supplied by the directory.
- Tapping the row plays; the heart updates Room favorites.

## Favorites

```text
┌─────────────────────────────┐
│ Favorites                   │
├─────────────────────────────┤
│ [art] Station      country   │
│                    [play][x]│
│ ...                         │
├─────────────────────────────┤
│ Removed Station     [Undo]  │
└─────────────────────────────┘
```

## Settings

```text
┌─────────────────────────────┐
│ Settings                    │
├─────────────────────────────┤
│ Appearance                  │
│  ◉ System default           │
│  ○ Light                    │
│  ○ Dark                      │
├─────────────────────────────┤
│ Storage                     │
│ Directory cache: entries, B │
│ [Clear directory cache]     │
├─────────────────────────────┤
│ About                       │
│ Fonamp 0.0.13                │
│ License pointer, sources     │
│ [Check for updates]          │
└─────────────────────────────┘
```

- Clear reports the freed cache bytes; Room data is not cleared.
- Settings reports directory-cache usage, not total app-storage usage.
- Update Download and Later are separate actions.

## Player — mini and full sheet

```text
mini: ┌───────────────────────────┐
      │ [art] Title       source │
      │      Subtitle      [▶/⏸]│ [x]
      └───────────────────────────┘

sheet:
┌─────────────────────────────┐
│ [radio/local]          [x]   │
│          [artwork]           │
│ Title                        │
│ Artist/station · ICY         │
│ ⚠ stream error        [retry]│
│ ─────●──────  1:12 / 3:42    │ ← local seek only
│ shuffle [prev] [play] [next] │
│ repeat [speed] [sleep] [heart]│
│ Up next                      │
│ [art] next title             │
│ [art] next title             │
└─────────────────────────────┘
```

- The queue section is shown for a non-empty multi-item context.
- Radio omits seek and uses stop rather than pause.
- Sleep presets: 5, 10, 15, 30, 45, 60 minutes, plus Off.
- Speed cycles 1×, 1.25×, 1.5×, 1.75×, 2×, 0.5×, and 0.75×.
- Repeat cycles Off → All → One → Off.

## Shared states

- **Loading:** visible rows/progress, never a blank screen
- **Empty:** explanation and useful next action
- **Offline:** cached content when available plus retry
- **Denied:** why audio access is needed plus re-request/settings path
- **Error:** inline explanation and retry

## Not shipped

Podcast/Download tabs, EQ, tab editor, total-storage management, artwork-cache controls, widgets, and Android Auto do not belong in current wireframes.
