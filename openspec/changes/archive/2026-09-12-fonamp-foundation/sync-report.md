# Sync report — fonamp-foundation

- **Change:** `fonamp-foundation` · **Branch:** `feat/fonamp-foundation` · **Store:** openspec
- **Status:** `synced` (sync only — change NOT archived, nothing committed)
- **Date (UTC):** 2026-09-12
- **Mode:** repo-local · **Workspace root:** /home/juan-arch/Projects/fonamp

## Domains synced (7/7, greenfield creation)

`openspec/specs/` was EMPTY before this sync. Each canonical spec was created
verbatim from its change delta (byte-identical, verified with `diff -q`).
Requirement IDs, titles, and scenarios are preserved exactly as written.

| Domain | Canonical file | Reqs |
|--------|---------------|------|
| scaffold | `openspec/specs/scaffold/spec.md` | 7 reqs / 9 scenarios |
| source-contract | `openspec/specs/source-contract/spec.md` | 5 reqs / 6 scenarios |
| library | `openspec/specs/library/spec.md` | 6 reqs / 6 scenarios |
| radio | `openspec/specs/radio/spec.md` | 6 reqs / 8 scenarios |
| player | `openspec/specs/player/spec.md` | 6 reqs / 7 scenarios |
| settings | `openspec/specs/settings/spec.md` | 4 reqs / 4 scenarios |
| permissions | `openspec/specs/permissions/spec.md` | 4 reqs / 5 scenarios |

Total: **38 requirements / 45 scenarios** now canonical.

## Requirement names synced

- **scaffold (ADDED, new file):** 1 Multi-module layout · 2 Dependency direction ·
  3 Dependency allowlist and prohibited SDKs · 4 UI shell and tab visibility ·
  5 Material3 defaults and shared states · 6 Build toolchain and size ·
  7 Performance, battery, and accessibility baseline
- **source-contract (ADDED, new file):** 1 Source contract shape · 2 LocalSource via
  MediaStore · 3 RadioBrowserSource directory with resilience · 4 Click-count
  fire-and-forget · 5 No download surface in v1
- **library (ADDED, new file):** 1 Zero-setup collection · 2 Song, artist, and album
  browsing · 3 Generic artwork and metadata honesty · 4 Empty collection state ·
  5 Collection search is stretch-only · 6 No shuffle or repeat in v1
- **radio (ADDED, new file):** 1 Discover index with counts and filter ·
  2 Station lists · 3 Three-tap playback · 4 Local-only favorites with undo ·
  5 Twenty-four-hour cache with offline and retry · 6 Directory scope exclusions
- **player (ADDED, new file):** 1 Single player via MediaSessionService ·
  2 Background playback and notification · 3 Per-source playback model ·
  4 Stream failure handling · 5 Mini-player and full sheet · 6 Best-effort queue restore
- **settings (ADDED, new file):** 1 Theme selection · 2 Cache and storage transparency ·
  3 About information · 4 No advanced customization in v1
- **permissions (ADDED, new file):** 1 Minimal permission set · 2 On-demand audio
  permission with inline justification · 3 Denied and permanently-denied states ·
  4 Privacy posture
- **MODIFIED:** none. **REMOVED:** none. **RENAMED:** none.

## Faithful-semantics notes (per sync instructions)

- **Stretch-MAY search (library Req 5):** kept as MAY/optional — acceptance passes
  with or without the title/artist/album text filter. NOT hardened into a MUST.
- **Best-effort queue restore (player Req 6):** kept as best-effort with no guarantee;
  coherent-state landing (restored queue or clean idle, never phantom-playing)
  preserved as the testable MUST. NOT hardened into guaranteed restore.
- **Toolchain pin recorded:** Media3 **1.9.0** pin + **compileSdk 35** constraint
  (with minSdk 29, targetSdk 35, JDK 17, no R8/minify for v1 dev) recorded here from
  verify evidence (`scripts/audit-toolchain.sh` GREEN 12/12); scaffold Req 6 states
  the compile/targetSdk 35 + minSdk 29 + JDK 17 + no-minify constraints canonically.
- **Release-APK gate decision recorded:** the v1 `assembleDebug < 40 MB` gate from
  scaffold Req 6 / Slice I gates box is MOVED TO RELEASE per parent resolution
  2026-09-12 (debug APK measured 63.6 MiB; Compose+Media3+Room+Retrofit debug floor
  without minify is ~45–50 MB; validated post-v1). Debug size is monitored, not
  gating. `docs/01-mvp.md` + `docs/02-prd.md` updated accordingly (worktree edits
  left untouched per instructions — not part of this sync).

## Verification basis + waiver

- `verify-report.md` verdict is **FAIL-with-conditions solely over the APK gate**:
  37/38 reqs, 44/45 scenarios, 306/306 tests green, debug build green, toolchain
  and gates audits green except size. The single FAIL is scaffold Req 6 (size limb
  only); all other requirement/scenario verdicts are PASS.
- Parent resolved 2026-09-12: 40 MB gate moved to RELEASE (validated post-v1),
  debug monitored. This sync proceeds under that explicit parent waiver and records
  the gate move above. No code defects were found in verification.
- Slice I gates-box task remains honestly unchecked in `tasks.md` (touched nothing
  there — read-only outside allowed surfaces).

## Collisions / destructive checks

- **Same-domain active changes:** none — `fonamp-foundation` is the only active
  change; `openspec/changes/archive/` is empty. No archive/sync order decision needed.
- **Legacy flat spec:** none — change carries domain specs under `specs/{domain}/`.
- **Destructive sync:** none — zero REMOVED requirements, zero MODIFIED blocks;
  all deltas are greenfield ADDED (new canonical files). No approval required.
- **RENAMED sections:** none found (grep over all 7 delta specs).

## Validation performed

- `ls openspec/specs/` before: EMPTY (greenfield confirmed).
- `diff -q` change-spec vs canonical-spec for all 7 domains: identical.
- `grep` for `RENAMED / ADDED / MODIFIED / REMOVED Requirements` headers: only
  greenfield content, no delta-section headers to apply.
- `ls openspec/changes/` — sole active change; archive empty (no collision).
- `git status --short` — confirmed pre-existing uncommitted `docs/01-mvp.md` +
  `docs/02-prd.md` product-doc edits NOT touched; no code/docs/change-planning
  files modified by this sync.
- No commit made. No archive move made.

## Structured status + actionContext findings

- Incoming authoritative status for this run: `nextRecommended: "sync"`,
  `artifactStore: openspec`, `trackerBranch: feat/fonamp-foundation`,
  `actionContext.mode: repo-local`, workspace `/home/juan-arch/Projects/fonamp`.
- Earlier engine snapshot had marked sync `blocked` pending clean verify; the parent
  waiver (APK gate → RELEASE) resolves the sole blocker, so sync proceeded and is
  recorded here explicitly rather than treated as an unresolved FAIL.
- Edit-surface discipline honored: wrote only `openspec/specs/**` (7 new files) +
  this report; everything else read-only.

## Next recommended phase

- **`sdd-archive`** — sync is clean (7/7 domains canonical, no collisions, no
  destructive deltas, gate decision recorded). Archive readiness still depends on
  the parent/verify owner closing the Slice I gates-box line and attaching the
  slice-review follow-up advisories (esp. slice A advisories) at archive time.
