#!/bin/bash
# Slice A RED task 1: toolchain presence checks.
# Fails until the Gradle scaffold exists with the pinned toolchain.
# Usage: scripts/audit-toolchain.sh  (exit 0 = all gates pass)
set -u
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
FAIL=0
fail() { echo "FAIL: $1"; FAIL=1; }
pass() { echo "PASS: $1"; }

# 1. settings.gradle.kts includes all 12 modules
EXPECTED_MODULES=":app :core:player :core:network :core:database :core:permissions :core:ui :provider:api :provider:radio :provider:local :feature:library :feature:radio :feature:settings"
if [ -f "$ROOT/settings.gradle.kts" ]; then
  MISSING=""
  for m in $EXPECTED_MODULES; do
    grep -q "\"$m\"\|'$m'\|$m" "$ROOT/settings.gradle.kts" || MISSING="$MISSING $m"
  done
  if [ -z "$MISSING" ]; then pass "settings.gradle.kts includes all 12 modules";
  else fail "settings.gradle.kts missing modules:$MISSING"; fi
else
  fail "settings.gradle.kts absent"
fi

# 2. gradle/libs.versions.toml pins single versions for every toolchain entry
TOML="$ROOT/gradle/libs.versions.toml"
REQUIRED_VERSIONS="agp kotlin ksp hilt room composeBom activityCompose lifecycle navigationCompose media3 retrofit okhttp serialization coroutines junit robolectric turbine mockwebserver"
if [ -f "$TOML" ]; then
  MISSING_VER=""
  for v in $REQUIRED_VERSIONS; do
    grep -Eq "^$v[[:space:]]*=" "$TOML" || MISSING_VER="$MISSING_VER $v"
  done
  if [ -z "$MISSING_VER" ]; then pass "libs.versions.toml pins single versions";
  else fail "libs.versions.toml missing version pins:$MISSING_VER"; fi
  # media3 pinned identically across all media3 artifacts
  MEDIA3_VERS=$(grep -Eo 'androidx\.media3:[a-z-]+:[0-9][^"\047 ]*' "$TOML" $ROOT/*/build.gradle.kts $ROOT/*/*/build.gradle.kts 2>/dev/null | grep -Eo '[0-9]+\.[0-9]+\.[0-9]+(-[a-z0-9]+)?$' | sort -u | tr '\n' ' ')
  if [ -z "$MEDIA3_VERS" ]; then
    grep -Eq '^media3[[:space:]]*=' "$TOML" && pass "media3 single version pin present (media3 = $(grep -Eo '^media3[[:space:]]*=[^#]*' "$TOML"))" || fail "no media3 version pin found"
  elif [ "$(echo "$MEDIA3_VERS" | wc -w)" -eq 1 ]; then
    pass "media3 identical across artifacts ($MEDIA3_VERS)"
  else
    fail "media3 versions diverge: $MEDIA3_VERS"
  fi
else
  fail "gradle/libs.versions.toml absent"
fi

# 3. R8 allowed ONLY in :app release (debug ships unminified in v1).
APP_BUILD_FILE="$ROOT/app/build.gradle.kts"
if grep -rn 'isMinifyEnabled\s*=\s*true' "$ROOT" --include='*.kts' --include='*.gradle' 2>/dev/null | grep -v '.git/' | grep -v "$APP_BUILD_FILE" | grep -q .; then
  fail "isMinifyEnabled=true found outside app/build.gradle.kts (only :app release may minify)"
else
  if grep -qzP '(?s)debug\s*\{[^}]*isMinifyEnabled\s*=\s*true' "$APP_BUILD_FILE" 2>/dev/null; then
    fail "isMinifyEnabled=true found in debug block (debug must stay unminified)"
  elif grep -qzP '(?s)release\s*\{[^}]*isMinifyEnabled\s*=\s*true' "$APP_BUILD_FILE" 2>/dev/null; then
    pass "R8 enabled only in :app release (debug unminified)"
  else
    fail "app/build.gradle.kts must set isMinifyEnabled=true in release { } and keep debug { } at false"
  fi
fi

# 4. app module declares minSdk 29 / target+compile 35
APP_BUILD="$ROOT/app/build.gradle.kts"
if [ -f "$APP_BUILD" ]; then
  grep -Eq 'minSdk\s*=\s*29' "$APP_BUILD" && pass "minSdk 29" || fail "minSdk 29 not declared in app/build.gradle.kts"
  grep -Eq 'targetSdk\s*=\s*35' "$APP_BUILD" && pass "targetSdk 35" || fail "targetSdk 35 not declared in app/build.gradle.kts"
  grep -Eq 'compileSdk\s*=\s*35' "$APP_BUILD" && pass "compileSdk 35" || fail "compileSdk 35 not declared in app/build.gradle.kts"
else
  fail "app/build.gradle.kts absent"
fi

if [ "$FAIL" -eq 0 ]; then echo "TOOLCHAIN AUDIT: GREEN"; else echo "TOOLCHAIN AUDIT: RED"; fi
exit "$FAIL"
