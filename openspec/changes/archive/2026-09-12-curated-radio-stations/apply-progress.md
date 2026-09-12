# Apply progress — `curated-radio-stations` (v0.0.3)

## Status: implementation complete in working tree — OVER REVIEW BUDGET, delivery decision required (ask-on-risk)

All 12 implementation checkboxes are done and verified (`./gradlew test` GREEN,
`./scripts/audit-gates.sh` GREEN). Total authored diff = **579 changed lines
(520 insertions + 59 deletions) across 7 code files**, over the 400-line budget.
Per the assigned `ask-on-risk` parameter: STOP, do not open a single PR, report
the PR1/PR2 split below for the parent to route. No commit, no tag (bump 0.0.3 +
`v0.0.3` go in a separate commit per AGENTS.md).

## Curl decision evidence (Req 12, red local 2026-09-12, `de1.api.radio-browser.info`)

Top-5 `bytag/<tag>?order=votes&reverse=true&limit=5` (+ top-20 https recount):

| tag | top votes | https in top-5 | https in top-20 | verdict |
|-----|-----------|----------------|-----------------|---------|
| `lofi` | 4877 (france info) | 1 (REYFM) | 13 | **TILE** (beats `lo-fi` 1110) |
| `lo-fi` | 1110 (Chillofi) | 3 | 14 | discarded variant |
| `chill` | 47564 (SomaFM Groove Salad) | 3 | 11 | discarded variant |
| `chillout` | 47564 (same top) | 4 | 11 | **TILE** (beats `chill`) |
| `ambient` | 54283 (Ambient Sleeping Pill) | ~3 | 13 | **TILE** |

- Tiles pinned in `CuratedTags.TILES`: `Lofi→lofi`, `Chill→chillout`, `Ambient→ambient`.
- `byname/omarchy` → `[]`, `bytag/omarchy` → `[]`: **omarchy out of scope, no preset**.
- Presets are curl-verified https MP3 rows with real `stationuuid`s (no invented URLs):
  - SomaFM Groove Salad `960cf833-0601-11e8-ae97-52543be04c81` / `https://ice6.somafm.com/groovesalad-128-mp3`
  - REYFM -#LOFI `4e681355-3ebb-4b6c-a79a-d7cc81a0afd5` / `https://listen.reyfm.de/lofi_320kbps.mp3`
  - Smooth Chill `478fd7f4-dc36-11e9-a8ba-52543be04c81` / `https://media-ssl.musicradio.com/ChillMP3`
- Full outputs must be pasted into the PR description (raw curl logs in shell history).

## Completed tasks (12/12, all `- [x]` in tasks.md)

1. ✅ curl tags + omarchy, tile list fixed (`lofi/chillout/ambient`), omarchy out of scope.
2. ✅ ≥3 https streams per winning tag (top-20 recount: 13/11/13).
3. ✅ `CuratedStations.kt` (new, 85 lines): `CuratedStation` + `DEFAULT` + `browseCurated()`,
   `sourceId="radio-browser"`, `stableId = stationUuid ?: "curated:<slug>"`, never throws.
4. ✅ `CuratedStationsTest.kt` (new, 91 lines): blank URL filtered, malformed (`"not a url"`,
   `ftp://…`) filtered without crash.
5. ✅ click-count fire-and-forget: no production change needed (`recordClick` already
   `scope.launch + runCatching`); test proves `streamOf(curated)` returns the `MediaItem`
   with a dead client (`http://127.0.0.1:1/`).
6. ✅ `RadioViewModel`: `Discover.curated` (default `emptyList()`), `curatedProvider`
   ctor param (default `CuratedStations::browseCurated`), populated in both emit paths,
   preserved by `copy` in `onFavorites`; `setQuery` doesn't touch `curated`.
7. ✅ D3 favoritable rule: all DEFAULT presets carry real `stationUuid`s; VM `toggleFavorite`
   guard unchanged (no-uuid → no-op); UI shows no heart without uuid.
8. ✅ `CuratedTags.kt` (new, 23 lines): tag strings only — `grep streamUrl` empty.
9. ✅ `CuratedSection` in `RadioScreens.kt`: `testTag("curated-section")`,
   tiles `testTag("curated-tag-<tag>")` → `onOpenSelection(BrowseQuery(tag=…))`, preset rows
   reuse `station-play-<key>` / `station-fav-<uuid>` + `SourceBadgeKind.RADIO`, wired to
   `viewModel::playStation` / `viewModel::toggleFavorite`; no name-search.
10. ✅ tile delegation captured in fake `Source.queries` (`BrowseQuery(tag="lofi")`); cache +
    mirror untouched.
11. ✅ favorites reuse `FavoriteStation`/`FavoriteDao` (upsert/delete/undo covered by VM +
    existing `FavoritesViewModelTest`); `git status` confirms **zero** changes under
    `core/database/*`, `core/player/*`, `provider/api/*`, `core/network/*`, no new migration.
12. ✅ `./gradlew test` GREEN (full suite) + `./scripts/audit-gates.sh` GREEN (allowlist v1
    intact, no trackers, no new permissions).

## Files changed

- `provider/radio/.../CuratedStations.kt` (new, +85)
- `provider/radio/.../CuratedStationsTest.kt` (new, +91)
- `feature/radio/.../CuratedTags.kt` (new, +23)
- `feature/radio/.../RadioViewModel.kt` (+5)
- `feature/radio/.../RadioScreens.kt` (+~150/−~60: CuratedSection + single-LazyColumn restructure)
- `feature/radio/.../RadioViewModelTest.kt` (+78: curated state/play/fav/no-uuid/tile-query)
- `feature/radio/.../RadioScreensTest.kt` (+79/−~6: 2 curated tests + offline-test scroll fix)

## Test commands run (JDK Corretto 17 via `JAVA_HOME=/tmp/amazon-corretto-17.0.20.10.1-linux-x64`)

- `./gradlew :provider:radio:testDebugUnitTest --offline --rerun-tasks` → 12/12 pass
- `./gradlew :feature:radio:testDebugUnitTest --offline` → 35/35 pass
- `./gradlew test --offline` → BUILD SUCCESSFUL (all modules)
- `./scripts/audit-gates.sh` → GATES AUDIT: GREEN

## Deviation from design (documented)

- **DiscoverScreen is now a single `LazyColumn`** (Curadas → filter → tabs → offline →
  index rows) instead of `Column{section, filter, tabs, offline, LazyColumn}`. Reason:
  with the new fixed section on top, the inner list collapsed to **zero height** in small
  viewports (proven: `discover-list` node at t=470,b=470 in the Robolectric viewport),
  hiding all cached rows with no way to scroll. The single container scrolls as a unit on
  small screens and is identical on normal phones. Trade-off: filter/tabs scroll away
  (no `stickyHeader` to avoid experimental API). The pre-existing offline test now scrolls
  to "Germany" (`performScrollToNode`) instead of asserting it initially visible — same
  intent (offline card + cached content + both retries).
- `station-play-<key>` uses `stationUuid ?: stableId` so uuid-less presets have a stable
  play tag while keeping `station-fav-<uuid>` parity for favoritable rows.

## Remaining

- [ ] Parent delivery decision: `size:exception` single PR (579 lines) OR chain:
  - **PR1 presets+sección (Req 7–10, ~430 lines)**: CuratedStations(+Test), VM curated,
    CuratedSection preset rows, VM/screens preset tests, offline-test fix.
  - **PR2 tiles (Req 11–12, ~150 lines)**: CuratedTags, tiles row in CuratedSection,
    single-LazyColumn restructure churn, tile-tap + scroll test updates.
- [ ] No commit / tag made here. After PR(s) merge: separate `chore: bump to 0.0.3
  (versionCode 3)` + AGENTS.md line + tag `v0.0.3` (triggers `release.yml` debug APK).

## Structured status consumed

Authoritative native status `apply-001` (`curated-radio-stations`, `applyState: ready`,
`repo-local`, 0/12 → now 12/12). Token
`sha256:0dce5749…d85d3987` used for this attempt; parent settles on close.
`actionContext` warnings: none; all edits inside `/home/juan-arch/Projects/fonamp`.
