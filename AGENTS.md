# AGENTS.md — fonamp

## Project

Fonamp is a native Android music and radio player built with Kotlin, Jetpack Compose, Hilt, Media3, Room, and a 12-module architecture.

- `applicationId`: `com.fonamp.app`
- SDK: `minSdk 29`, `compileSdk 35`, `targetSdk 35`
- Toolchain: JDK 17 (Corretto in CI), Gradle 8.10, AGP 8.7.x
- Current release: `0.0.13` (`versionCode 13`) from `app/build.gradle.kts`
- Current modules: `:app`, five `:core:*`, three `:provider:*`, and three `:feature:*` modules

## Mandatory documentation contract

Documentation impact is part of every delivery boundary. Before **every implementation, addition, commit, PR, and release**:

1. Review the change for documentation impact.
2. Update every affected current document in the **same work unit**. Check the relevant current specs under `openspec/specs/`, the `Unreleased` section of `CHANGELOG.md`, and the applicable `README.md` / product / design / technology / wireframe documents.
3. Add an `Unreleased` changelog entry for shipped behavior or fixes. Do not present planned work as shipped.
4. Verify `versionCode`, `versionName`, tags, and release metadata against `app/build.gradle.kts` and repository history.
5. Curate release notes after publishing; generated GitHub notes alone are not the final internal-release notes.
6. Search for contradictory shipped-versus-planned claims before delivery.

**Archived OpenSpec artifacts under `openspec/changes/archive/` are immutable historical records. Never rewrite them to reflect later behavior.** Update the current specs under `openspec/specs/` instead.

## Current architecture

```text
app                  navigation, Hilt graph, retained holders, update installation
core/player          Media3 service/controller, playback state, queue persistence
core/network         RadioBrowser/GitHub clients, mirrors, 24-hour directory cache
core/database        Room favorites and theme persistence
core/permissions     audio and notification runtime gates
core/ui              shared states, artwork, mini-player, player sheet, Up next
provider/api         source contract and MediaItem mapping
provider/radio       RadioBrowser source and curated stations
provider/local        MediaStore source
feature/library      Collection UI and state
feature/radio        Discover, station, search, and favorites UI/state
feature/settings     theme, directory cache, About, and update-check UI/state
```

Dependency rules:

- `feature/*` modules do not depend on one another.
- `app` wires navigation, Hilt, Media3 access, queue restore, and APK installation.
- `core/player` consumes `MediaItem` and does not import concrete providers.
- New sources implement `provider/api` and are registered in the app graph.

## CI and release gates

### CI — `.github/workflows/android.yml`

Runs on pushes to `main` and `feat/**`, and on pull requests to `main`:

1. `scripts/audit-toolchain.sh` — 12 modules, version pins, SDK levels, R8 only in `:app:release`
2. `./gradlew test` — unit-test gate
3. `./gradlew assembleDebug` — debug APK build
4. `scripts/audit-gates.sh` — prohibited SDKs, permission allowlist, debug monitoring, and release-size gate when a release APK is present

### Signed release APK — `.github/workflows/release.yml`

- Trigger: an annotated `v*` tag push.
- `assembleRelease` uses R8 and resource shrinking.
- CI restores the private keystore from `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, and `KEY_PASSWORD`; the published APK is signed.
- The release workflow enforces a hard `<40 MB` check on `app-release.apk`.
- Local release builds are signed only when `keystore.properties` or the `FONAMP_*` signing environment variables are present; otherwise they are unsigned.
- The asset name is `fonamp-<tag>.apk`. Releases are for direct/internal installation, not Play Store distribution.

The last recorded size measurement in repository documentation is **7.3 MB on 2026-09-12**. Re-measure before quoting a current size.

## Versioning and release workflow

- `app/build.gradle.kts` is the source of truth for `versionCode` and `versionName`.
- Keep the feature commit focused; put the version bump in a separate `chore: bump to X.Y.Z (versionCode N)` work unit when that is the accepted workflow.
- The bump updates the current version in this file and `README.md` in the same work unit.
- Create annotated tags: `git tag -a vX.Y.Z -m "fonamp X.Y.Z"`.
- Push the tag to trigger `release.yml`.
- After publication, curate release notes in this format:
  - user-facing highlights;
  - fixes with issue/PR references when available;
  - technical details including measured size, signing, and CI status;
  - the generated full changelog link.
- For external distribution, publish as a draft, test the APK, then publish manually.

## Permissions and known gaps

The current CI allowlist contains exactly seven declarations:

- `READ_MEDIA_AUDIO`
- `READ_EXTERNAL_STORAGE` (`maxSdkVersion=32`)
- `INTERNET`
- `FOREGROUND_SERVICE`
- `FOREGROUND_SERVICE_MEDIA_PLAYBACK`
- `POST_NOTIFICATIONS`
- `REQUEST_INSTALL_PACKAGES` (in-app update handoff to the system installer)

Current open or unverified product claims include cold-start performance on a mid-range device, an explicit playback-stall timeout, total app-storage reporting, and a full artwork-cache policy. Keep these labeled open rather than presenting them as measured behavior.

## Useful commands

```bash
./gradlew test
./gradlew assembleDebug
./gradlew assembleRelease
./scripts/audit-toolchain.sh
./scripts/audit-gates.sh
```
