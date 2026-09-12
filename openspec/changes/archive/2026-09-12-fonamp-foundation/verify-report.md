```yaml
schema: gentle-ai.verify-result/v1
evidence_revision: sha256:b721c7f42d0a11ba00eb2abbba5bb3ced2ad467f568ac0c76adf38472c7f2e69
verdict: fail
blockers: 1
critical_findings: 1
requirements: 37/38
scenarios: 44/45
test_command: ./gradlew test --rerun-tasks
test_exit_code: 0
test_output_hash: sha256:92e3ec8b274569012ca373642d6861870d8caf81bd82769eb9a2d3198194a112
build_command: ./gradlew clean assembleDebug
build_exit_code: 0
build_output_hash: sha256:99b8174fcbc3bf8f0c5300c72b3f598e4e4a9ad2d174418e29105918a53f23a9
```

# Verify report — fonamp-foundation (Slices A–I)

- **Change:** `fonamp-foundation` · **Branch:** `feat/fonamp-foundation` · **Commits:** `cbe2b7f..29b8aa4` (9 slice commits)
- **Mode:** read-only verification (no code/docs/openspec touched except this file). Skills loaded: `android-jetpack-compose`, `android-kotlin`, `testing-setup` (binding overrides honored: minSdk 29, compile/targetSdk 35, no MockK — zero MockK references in repo).
- **Status:** `FAIL` — 1 blocker (Slice I gates box honestly OPEN), 1 critical finding (APK size gate). Everything else passes.

## 1. Gates evidence (exact commands + outputs)

| Gate | Command (env: Corretto 17 `17.0.20.1`, `ANDROID_HOME=~/Android/Sdk`) | Result |
|------|------|--------|
| Full tests (forced) | `./gradlew test --rerun-tasks` (backgrounded + polled, 3m54s, 605 tasks executed) | **BUILD SUCCESSFUL** — 62 result files, **306 tests, 0 failures, 0 errors, 0 skipped** (matches expected ~306) |
| Cached confirm | `./gradlew test` | BUILD SUCCESSFUL, 605 up-to-date |
| Debug build | `./gradlew clean assembleDebug` (backgrounded + polled, 30s, 318 executed) | **BUILD SUCCESSFUL** — `app-debug.apk` **66,687,418 bytes (63.6 MiB)** |
| Toolchain audit | `bash scripts/audit-toolchain.sh` | **GREEN** (12/12 modules, pins, minSdk 29, target/compile 35, no minify) |
| Gates audit | `bash scripts/audit-gates.sh` | **RED only on size**: workflow/JDK17 ✓, zero prohibited SDKs ✓, permissions allowlist ✓, **FAIL: APK 64M exceeds 40 MB** |

## 2. Per-requirement verdicts (38 reqs / 45 scenarios)

### scaffold (7 reqs, 9 scenarios) — 6/7 PASS, Req 6 FAIL on size only

| Req | Verdict | Evidence |
|-----|---------|----------|
| 1 Multi-module layout | PASS | `settings.gradle.kts` 12 modules (audit GREEN); `core/ui` exposes Loading/Empty/Offline/Denied/ErrorRetry (`UiStates.kt`, `UiStatesTest` 17/17) |
| 2 Dependency direction | PASS | `feature/*` build files depend only on `core/*` + `provider/api` + their one source — zero feature→feature edges (grep); `core/player` imports nothing from `provider/*` (grep: only a KDoc mention); `app/.../ExpandabilityTest.kt` (mock-source, green in suite) |
| 3 Allowlist / no trackers | PASS | Dep audit: zero `ads\|admob\|firebase\|analytics\|crashlytics` in all build files + `audit-gates.sh` prohibited-SDK check ✓ |
| 4 Tabs + mini-player | PASS | `AppNav.kt`: exactly Collection/Radio/Favorites/Settings, no Podcasts/Downloads/EQ/sleep; `FonampShell.kt` persistent `MiniPlayer` above tabs |
| 5 M3 defaults + states | PASS | `FonampTheme` uses `lightColorScheme()`/`darkColorScheme()` defaults, no custom tokens; all list screens render a designed state |
| 6 Toolchain + size | **FAIL (size)** | Toolchain ✓ (minSdk 29, compile/target 35, JDK 17, `isMinifyEnabled=false`, `./gradlew test` green) but **APK 63.6 MiB > 40 MB** |
| 7 Perf/battery/a11y | PASS (unit-level) | 48dp `sizeIn` targets + content descriptions (tested); FGS `stopSelf()` when not playing; zero `WorkManager`/periodic APIs (grep); cold-start <2s on-device unverifiable headless (open item) |

### source-contract (5 reqs, 6 scenarios) — ALL PASS

| Req | Verdict | Evidence |
|-----|---------|----------|
| 1 Contract shape | PASS | `Source` (`id/browse/search/streamOf`), `SourceContractTest` 6/6 incl. reflection guard against `download*`; player consumes `MediaItem` only |
| 2 LocalSource | PASS | `LocalSourceTest` 9/9: single-pass `IS_MUSIC` query, honest metadata, playable content URIs, zero network |
| 3 RadioBrowserSource resilience | PASS | `MirrorPolicyTest` 6/6 (de1→de2→nl1 + all.api bootstrap), 10s timeouts → typed `Timeout`, 24h TTL (`DirectoryCache.isStale`, TTL_MS), stale-serve + refresh |
| 4 Click-count | PASS | `clickScope.launch { runCatching { client.click(..) } }` — swallowed, never blocks (RadioBrowserSource.kt:88-90) |
| 5 No downloads | PASS | No `downloadOf` in interface; mock source compiles with 4 members (contract test) |

### library (6 reqs, 6 scenarios) — ALL PASS

Reqs 1–2 (zero-setup, segments+queue), 3 (generic icons, absent fields omitted), 4 (Empty + add-music hint), 5 (stretch text filter kept — in-memory, asserted exactly 1 browse call), 6 (no shuffle/repeat — grep finds only doc mentions, zero behavior): all PASS via `LocalSourceTest` 9/9, `LibraryViewModelTest` 7/7, `CollectionScreenTest` 6/6.

### radio (6 reqs, 8 scenarios) — ALL PASS

Reqs 1 (index+counts+local filter, zero per-keystroke network — tested), 2 (bitrate/codec honesty, generic icons), 3 (3-tap play raises live mini-player — tested), 4 (favorites round-trip + restart + 10s undo `UNDO_WINDOW_MS=10_000`, stations-only by construction), 5 (24h cache, Offline card + retry without clearing cache, pull-to-refresh), 6 (no tops/random/name-search — grep finds only doc mentions): all PASS via `RadioViewModelTest` 13/13, `FavoritesViewModelTest` 6/6, `RadioScreensTest` 7/7.

### player (6 reqs, 7 scenarios) — ALL PASS (unit-level; on-device sync open)

Reqs 1 (single ExoPlayer in `MediaSessionService`, source-blind `PlayerManager`), 2 (FGS only while playing, notification via session automatics — screen-off sync unverifiable headless), 3 (radio play/stop + ICY `Icy-MetaData: 1` → `icyTitle`; local queue + seek; no shuffle/repeat/speed/sleep), 4 (10s connect/read `CONNECT_TIMEOUT_MS=READ_TIMEOUT_MS=10_000`, `TIMEOUT|OFFLINE|STREAM_UNAVAILABLE` banner + retry, UI interactive — via fake), 5 (mini-player + sheet contract with `SourceBadge`), 6 (best-effort restore, never phantom-playing — tested): PASS via `PlayerManagerTest` 16/16, `MediaItemMapperTest` 4/4.

### settings (4 reqs, 4 scenarios) — ALL PASS

Reqs 1 (System/Light/Dark round-trip, restart persistence, no-restart apply — `SettingsViewModelTest` 5/5), 2 (cache usage + clear with confirmation + freed bytes), 3 (About: version `0.1.0`, licenses, source — `SettingsScreen.kt:96-99`), 4 (no tab editor/EQ/sleep/speed — grep finds only doc mentions): PASS.

### permissions (4 reqs, 5 scenarios) — ALL PASS (unit-level; system-sheet flows open on device)

Reqs 1 (merged manifest: only `READ_MEDIA_AUDIO`, `READ_EXTERNAL_STORAGE` maxSdk-32, `INTERNET`, `FOREGROUND_SERVICE`, `MEDIA_PLAYBACK`, `POST_NOTIFICATIONS`), 2 (API-split `sdkInt >= 33` gate, Collection-only first entry, rest ungated), 3 (Denied + grant-again + settings path — `AudioPermissionGateTest` 9/9), 4 (egress = 4 radio-browser hosts + click-count + streams only; zero trackers; `usesCleartextTraffic=true` justified for http streams): PASS.

## 3. Task completion — 22/23, 1 unchecked (the blocker)

Exact unchecked implementation task line from `tasks.md`:
```
- [ ] GREEN: Run full gates — `./gradlew test` green, `assembleDebug` <40 MB, cold-start→radio-audible ≤3 taps, screen-off + notification sync, 10s-timeout banner+retry, theme + favorites round-trips, permission/manifest + egress (radio-browser mirrors + click-count + streams only, zero trackers) audit. <!-- sdd-owner: implementation -->
```
Partial results inside that box: `./gradlew test` green ✓, `assembleDebug` builds ✓, permission/manifest + egress audit ✓ (all verified above); **APK <40 MB ✗**; on-device checks impossible headless. **Archive is NOT ready.**

## 4. Strict TDD (config `strict_tdd: false` → advisory, not blocking)

`TDD Cycle Evidence` lives in `apply-progress.md` per slice (not a single table). Spot-checks are credible: Slice B RED = `Unresolved reference` compile failure before impl; Slice G RED likewise + GREEN fixes (nested KDoc comments, Media3 1.9.0 API corrections); Slice I RED = KSP `Unclosed comment` + 4× `UncompletedCoroutinesError` hangs, fixed via `backgroundScope` + shared-scheduler dispatcher. Assertion quality: no tautologies found in spot-checks (state-flow emissions, round-trips, exact-call-count asserts). No MockK anywhere (hand fakes per stack).

## 5. Review workload (chain strategy: `feature-branch-chain`)

Per-commit insertions (`git diff parent..commit --shortstat`): A 1599 · B 370 · C 896 · D 525 · E 1140 · F 1200 · G 1277 · H 1730 · I 1421 (total ~8559). Each slice is one reviewable commit on the tracker branch — no scope creep beyond assigned slices (each slice's file list matches its task scope; scaffold placeholders left in place per convention). Note: A (1599) and H (1730) exceed the 400-line single-PR budget, but delivery is `chained`/`feature-branch-chain`, not single-PR — no boundary breach. (Minor: parent brief quoted E as "1098 + corrections"; measured 1140 insertions.) Review receipts are parent-attested and recorded, not repo-verifiable: A `review-6adb8fab9a0efbd7` (4 lenses + 4 advisories), B `review-d3261bab6065d7e8`, C `review-ed77e9cfb21daef0` clean, D `review-d92a316a5de2b4a9`, F `review-d3420ea6cb617f2f`, G `review-92968d4c0ae1f659`, H `review-f0e39538b4309e2e` clean, I `review-60e67bc4f6717bd9` clean; E committed by explicit user order with stuck lineage `review-44cfd43a4bd9b8c1` (`correction_required`, tooling defect gentle-pi#926).

## 6. APK WEIGHT ATTACK — breakdown + diet (analysis only, no implementation)

Clean Slice-I APK: **66,687,418 bytes (63.6 MiB)**. Composition (uncompressed): **dex 62.41 MB (98%)**, res+assets ~1.15 MB, `resources.arsc` 0.83 MB, native 37 KB (only `libandroidx.graphics.path.so` × 4 ABIs). Dex is Stored uncompressed, so APK ≈ dex footprint. (`apkanalyzer dex packages --defined-only` ranking; absolute MB below are its bytecode estimates — ~2.2× smaller than stored dex bytes, but ranking is what matters.)

| Rank | Contributor | Size signal | Diet lever (no R8/minify) |
|------|-------------|-------------|---------------------------|
| 1 | `material-icons-extended` — all 5 styles (filled/outlined/rounded/sharp/twotone × 4409 methods = 22k methods, ~13.5 MB) | App uses only **17 `Icons.Filled.*`** vectors | **Copy the 17 vectors into project drawables (~17 KB), drop `-extended`** — closes ~55% of the 23.6 MB gap. Tradeoff: manual upkeep for future icons |
| 2 | Compose core (ui/material/foundation/runtime ~74k methods, ~20 MB) | Framework floor | No lever without dropping Compose (rejected). Accept as floor |
| 3 | `media3-ui` — **zero imports** in any `*.kt`, yet depended on by `core/player` | Dead dep | Remove dep line — free saving (~0.5–1 MB). Tradeoff: none; re-add if sheet needs media3 widgets |
| 4 | Guava `com.google.common` 16k methods (~1.2 MB, transitive via ExoPlayer) | Transitive | Try `exclude group: com.google.guava` on exoplayer — **risky** (ListenableFuture in ExoPlayer paths); needs full test + playback regression. Minor lever |
| 5 | ExoPlayer renderers/extractors 9.5k methods | Full extractor set | If radio scope is progressive/ICY-only, drop DASH/HLS/SmoothStreaming modules — **product decision** (HLS radio streams would break) |
| — | ABI splits / `resConfigs` / resource shrinking | Native 37 KB; res 1.15 MB | **No lever** — nothing to split/shrink; `shrinkResources` needs minify (forbidden) |

**Honest bottom line:** levers 1+3+4 ≈ 14–15 MB → ~49 MB, still over gate. The Compose+Media3+Room+Retrofit debug floor without minify is ~45–50 MB. Reaching 40 MB almost certainly needs a parent decision: (a) raise the gate, (b) allow release-only minify (debug gate stays no-minify), or (c) aggressive ExoPlayer/Compose trim with product tradeoffs. Recommended: (b) + lever 1.

## 7. Open items (recorded honestly)

1. **[BLOCKER]** Slice I gates box unchecked (exact line in §3) — APK gate + headless on-device checks.
2. APK 63.6 MiB vs 40 MB gate — needs parent size decision (§6).
3. On-device checks impossible here (unit-covered only): cold-start→radio-audible ≤3 taps, screen-off + notification sync, 10s-timeout banner on hardware, theme/favorites round-trips on device.
4. Slice-review follow-up advisories (esp. A's 4 advisories) are parent-held — recommend attaching their texts to the sync/archive step.
5. Download-source scope deferred to v4 per PRD (source-contract Req 5 enforced absent/non-abstract — no action).

## 8. Verdict

**`FAIL` (with conditions for a fast flip):** 37/38 requirements, 44/45 scenarios, 306/306 tests green, build green, audits green except size. The single blocker is the honestly-OPEN Slice I gates box, driven by the APK size gate (63.6 MiB > 40 MB) plus headless on-device checks. No code defects found. Fix path: parent size decision (§6) → record exception or diet slice → re-run gates box → archive.
