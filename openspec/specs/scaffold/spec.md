# Scaffold Specification

## Purpose

Define fonamp's current modular foundation, dependency boundaries, shared UI shell, Android build posture, and quality gates.

## Requirements

### Requirement 1: Twelve-module layout

The system MUST provide `app`, `core/player`, `core/network`, `core/database`, `core/permissions`, `core/ui`, `provider/api`, `provider/radio`, `provider/local`, `feature/library`, `feature/radio`, and `feature/settings`, with single-purpose module boundaries.

#### Scenario: Module inventory present

- GIVEN a current checkout
- WHEN the Gradle module list is inspected
- THEN all twelve modules exist and `core/ui` provides the shared state and player surfaces

### Requirement 2: Dependency direction

The system MUST keep `feature/*` modules independent of one another, make `app` the composition root for navigation/DI/update installation, and keep `core/player` free of concrete provider dependencies. A new source MUST be implementable behind `provider/api` and registered in `app` without changing player internals or bottom-tab routes.

#### Scenario: New source registration

- GIVEN a new `Source` implementation behind `provider/api`
- WHEN it is registered in the Hilt source set
- THEN the app can resolve it by id without changing `core/player` or bottom-tab navigation

#### Scenario: Feature isolation

- GIVEN the Gradle dependency graph
- WHEN feature modules are inspected
- THEN no edge exists between any two `feature/*` modules

### Requirement 3: Audited dependencies and prohibited SDKs

The build MUST remain on the documented Android/Kotlin stack and MUST NOT include ad, ad mediation, analytics, crash-reporting, or Firebase SDKs. New dependencies outside the documented stack require an explicit what/why/size-license justification.

#### Scenario: Dependency audit

- GIVEN a merged debug build
- WHEN dependency and build files are scanned
- THEN prohibited SDK names are absent and every additional dependency has a recorded justification

### Requirement 4: UI shell and tab visibility

The system MUST show exactly Collection, Radio, Favorites, and Settings as bottom tabs. Podcasts, Downloads, and EQ MUST remain absent until implemented, and the mini-player MUST remain above the tabs while player context is present.

#### Scenario: Current tab inventory

- GIVEN the app on any current tab
- WHEN the bottom bar is inspected
- THEN it contains exactly Collection, Radio, Favorites, and Settings with no roadmap-only entry points

### Requirement 5: Material3 states and accessibility baseline

The system MUST use Material3 light/dark themes and shared `Loading`, `Empty`, `Offline`, `Denied`, and `ErrorRetry` states. Playback actions MUST meet the 48dp touch-target/content-description baseline.

#### Scenario: State coverage

- GIVEN current list and player screens in a failure or empty condition
- WHEN rendered in light and dark themes
- THEN a designed state is shown with an action where appropriate rather than a blank screen or crash

### Requirement 6: Android toolchain and release posture

The system MUST compile with `minSdk 29`, `compile/targetSdk 35`, JDK 17, Gradle 8.10, and AGP 8.7 lineage. Debug MUST remain unminified and monitored; release MUST enable R8 and resource shrinking and enforce the `<40 MB` workflow gate.

#### Scenario: Toolchain and artifact check

- GIVEN a checkout with JDK 17 and Android SDK 35
- WHEN the audits and Gradle builds run
- THEN module/version/SDK checks pass, tests and debug assembly are CI gates, and release configuration has R8/shrink enabled with debug minification disabled

### Requirement 7: Performance target remains open evidence

Cold-start under two seconds on a defined mid-range device is a product target, not a current measured guarantee. The app MUST use cache-backed directory loading and avoid idle polling, and a future acceptance record MUST identify the device, build, and measurement method before claiming the target is met.

#### Scenario: Performance claim requires evidence

- GIVEN a proposed claim that cold start is under two seconds
- WHEN it is documented as achieved
- THEN current build, device profile, and reproducible measurement evidence are attached
