# Scaffold Specification

## Purpose

Define the v1 modular foundation that makes Fonamp expandable, lightweight, and shippable: module layout, dependency rules, shared UI shell, build constraints, and quality gates. This is the structural slice all other v1 domains build on.

## Requirements

### Requirement 1: Multi-module layout

The system MUST provide the v1 modules `app`, `core/player`, `core/network`, `core/database`, `core/permissions`, `core/ui`, `provider/api`, `provider/radio`, `provider/local`, `feature/library`, `feature/radio`, `feature/settings`, plus a Material3 design system with shared state components.

#### Scenario: Module inventory present

- GIVEN a fresh checkout of the v1 tree
- WHEN the reviewer lists top-level Gradle modules
- THEN each of the listed modules exists with its own build file and single responsibility, and `core/ui` exposes `Loading`, `Empty`, `Offline`, `Denied`, and `ErrorRetry` components.

### Requirement 2: Dependency direction

The system MUST enforce that `feature/*` modules never depend on each other, all features depend only on `core/*` plus `provider/api`, and `app` alone wires navigation and DI; adding a new `provider/*` source MUST NOT require changes to the player or UI shell.

#### Scenario: New mock source without shell changes

- GIVEN the v1 tree builds
- WHEN a developer adds a `provider/demo` module implementing `Source` and registers it in `app` DI
- THEN `./gradlew test` and `./gradlew assembleDebug` succeed with zero edits to `core/player`, `core/ui`, or bottom-tab navigation.

#### Scenario: Feature isolation

- GIVEN the Gradle dependency graph
- WHEN inspected for `feature/library`, `feature/radio`, `feature/settings`
- THEN no edge exists between any two `feature/*` modules.

### Requirement 3: Dependency allowlist and prohibited SDKs

The system MUST build v1 only on Kotlin, Jetpack Compose (Material3, BOM), Media3/ExoPlayer, Hilt, Retrofit/OkHttp, kotlinx.serialization, Room, and Coroutines/Flow plus testing libs (JUnit, Robolectric, Turbine, MockWebServer); the app MUST NOT include any ad, analytics, crash-reporting, or Firebase SDK.

#### Scenario: Dependency audit

- GIVEN the merged dependency report for the debug APK
- WHEN scanned for `ads`, `admob`, `firebase`, `analytics`, or `crashlytics` artifacts
- THEN no match is found and every non-allowlist entry has a written 3-line justification (what, why existing libs cannot do it, size/license impact).

### Requirement 4: UI shell and tab visibility

The system MUST show exactly the bottom tabs Collection, Radio, Favorites, and Settings in v1, keep Podcasts and Downloads tabs hidden until their slices ship, and dock a persistent mini-player above the tabs whenever audio is playing or paused.

#### Scenario: Tab inventory

- GIVEN the app launched to any tab
- WHEN the bottom bar is inspected
- THEN it contains Collection, Radio, Favorites, Settings and no Podcasts, Downloads, EQ, or sleep-timer entry points.

#### Scenario: Mini-player persistence

- GIVEN audio is playing on the Radio tab
- WHEN the user switches to Collection and to Settings
- THEN the same mini-player remains visible above the tabs on every tab with working play/pause.

### Requirement 5: Material3 defaults and shared states

The system MUST use Material3 default light and dark color roles, typography, and iconography with no custom design tokens in v1, and every list screen MUST render one of `Loading` (shimmer rows, never blank), `Empty`, `Offline`, `Denied`, or `ErrorRetry` instead of a blank screen or crash-as-UI.

#### Scenario: Every state is designed

- GIVEN each of Collection, Discover, Station list, and Favorites in each of loading, empty, offline, denied, and error conditions
- WHEN rendered in light and dark themes
- THEN a designed state component is shown with default Material3 styling and an actionable control where applicable (e.g. retry, grant again).

### Requirement 6: Build toolchain and size

The system MUST compile with minSdk 29, compile/targetSdk 35, JDK 17, Gradle 8.10 + AGP 8.7 lineage, ship v1 as a direct debug APK under 40 MB with no R8/minify, and pass `./gradlew test` green.

#### Scenario: Toolchain and artifact check

- GIVEN a clean checkout with JDK 17 and a configured Android SDK
- WHEN `./gradlew test` then `./gradlew assembleDebug` run
- THEN tests pass, the debug APK is produced under 40 MB, and the manifest/build files declare minSdk 29 and compile/targetSdk 35 with no `isMinifyEnabled = true` for v1 dev.

### Requirement 7: Performance, battery, and accessibility baseline

The system MUST reach interactive in under 2 seconds on a mid-range device, page/cache directory lists, poll nothing, run the foreground service only while playing, expose touch targets of at least 48dp with content descriptions on playback controls, and support dynamic text.

#### Scenario: Baseline gates

- GIVEN a mid-range device with a large local library and radio directory
- WHEN cold-starting, scrolling lists, and backgrounding playback
- THEN the first tab is interactive in under 2 seconds, lists scroll without blocking fetches, no periodic background work runs while idle, and playback controls meet the 48dp and content-description checks.
