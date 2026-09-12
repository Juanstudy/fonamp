#!/bin/bash
# Slice A RED task 3: CI gate checks (dependency audit, APK size, permission allowlist).
# Fails until the workflow + debug APK exist and audits are clean.
# Usage: scripts/audit-gates.sh (exit 0 = all gates pass)
set -u
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
FAIL=0
fail() { echo "FAIL: $1"; FAIL=1; }
pass() { echo "PASS: $1"; }

# 1. CI workflow exists and runs test + assembleDebug on JDK 17
WF="$ROOT/.github/workflows/android.yml"
if [ -f "$WF" ]; then
  grep -q '\./gradlew test' "$WF" && pass "workflow runs ./gradlew test" || fail "workflow missing ./gradlew test"
  grep -q 'assembleDebug' "$WF" && pass "workflow runs assembleDebug" || fail "workflow missing assembleDebug"
  grep -qiE 'java-version:\s*.17.|corretto.*17|17.*corretto' "$WF" && pass "workflow uses JDK 17" || fail "workflow missing JDK 17 setup"
else
  fail ".github/workflows/android.yml absent"
fi

# 2. Zero prohibited SDKs (ads/admob/firebase/analytics/crashlytics) in catalog + build files
HITS=$(grep -rniE 'ads|admob|firebase|analytics|crashlytics' "$ROOT/gradle/libs.versions.toml" "$ROOT"/build.gradle.kts "$ROOT"/settings.gradle.kts "$ROOT"/app/build.gradle.kts "$ROOT"/core/*/build.gradle.kts "$ROOT"/provider/*/build.gradle.kts "$ROOT"/feature/*/build.gradle.kts 2>/dev/null | grep -v 'audit-gates' || true)
if [ -z "$HITS" ]; then pass "dependency audit: zero prohibited SDKs";
else fail "prohibited SDK references found:"; echo "$HITS"; fi

# 3. Debug APK exists (monitored, no hard gate in v1) + release APK under 40 MB when present
# Parent decision 2026-09-12: 40MB gate moved to RELEASE builds; debug is monitored.
APK="$ROOT/app/build/outputs/apk/debug/app-debug.apk"
if [ -f "$APK" ]; then
  SIZE=$(stat -c%s "$APK")
  pass "debug APK present: $(numfmt --to=iec "$SIZE") (monitored, no hard gate in v1)";
else
  fail "debug APK absent (run ./gradlew assembleDebug)"
fi
REL="$ROOT/app/build/outputs/apk/release/app-release.apk"
if [ -f "$REL" ]; then
  RSIZE=$(stat -c%s "$REL")
  MAX=$((40 * 1024 * 1024))
  if [ "$RSIZE" -lt "$MAX" ]; then pass "release APK $(numfmt --to=iec "$RSIZE") < 40 MB";
  else fail "release APK $(numfmt --to=iec "$RSIZE") exceeds 40 MB"; fi
else
  pass "no release APK present (gate applies when the release pipeline exists)";
fi

# 4. Manifest permission allowlist (v1 max set only)
ALLOW="android.permission.READ_MEDIA_AUDIO android.permission.READ_EXTERNAL_STORAGE android.permission.INTERNET android.permission.FOREGROUND_SERVICE android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK android.permission.POST_NOTIFICATIONS"
DECLARED=$(grep -rhoE 'android:name="android\.permission\.[A-Z_]+"' "$ROOT/app/src/main/AndroidManifest.xml" "$ROOT"/core/*/src/main/AndroidManifest.xml "$ROOT"/provider/*/src/main/AndroidManifest.xml "$ROOT"/feature/*/src/main/AndroidManifest.xml 2>/dev/null | grep -oE 'android\.permission\.[A-Z_]+' | sort -u || true)
if [ -z "$DECLARED" ]; then
  fail "no permissions declared in manifests"
else
  EXTRA=""
  for p in $DECLARED; do
    echo "$ALLOW" | grep -qw "$p" || EXTRA="$EXTRA $p"
  done
  if [ -z "$EXTRA" ]; then pass "permissions within allowlist: $(echo "$DECLARED" | tr '\n' ' ')";
  else fail "permissions outside allowlist:$EXTRA"; fi
fi

if [ "$FAIL" -eq 0 ]; then echo "GATES AUDIT: GREEN"; else echo "GATES AUDIT: RED"; fi
exit "$FAIL"
