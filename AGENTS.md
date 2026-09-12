# AGENTS.md — fonamp

## Proyecto
App Android nativa (Kotlin + Jetpack Compose + Hilt + Media3).
- `applicationId`: `com.fonamp.app` — `minSdk 29`, `targetSdk 35`, JDK 17 (Corretto)
- Módulos: `:app`, `:core:*` (player, network, database, permissions, ui), `:provider:*` (api, radio, local), `:feature:*` (library, radio, settings)
- Versión actual: `0.1.0-slice-a` (`versionCode 1`) en `app/build.gradle.kts`

## Workflows CI/CD

### CI — `.github/workflows/android.yml`
Corre en `push` a `main`, `feat/**` y `pull_request` a `main`:
1. `scripts/audit-toolchain.sh` — 12 módulos, pins, SDKs, sin minify
2. `./gradlew test` — gate de unit tests
3. `./gradlew assembleDebug` — genera `app/build/outputs/apk/debug/app-debug.apk`
4. `scripts/audit-gates.sh` — sin trackers (ads/admob/firebase/analytics/crashlytics), permisos en allowlist, APK monitoreado

### Release APK — `.github/workflows/release.yml`
Publica el APK en GitHub Releases.
- Trigger: `push` de tag `v*` (ej: `v0.1.0-slice-a`)
- Permisos: `contents: write`
- Pasos: checkout → JDK 17 → Android SDK → Gradle → `assembleDebug` → `cp app-debug.apk fonamp-<tag>.apk` → `softprops/action-gh-release@v2` con `generate_release_notes: true`
- Resultado: Release con asset `fonamp-<tag>.apk` (build **debug**, sin firma — para prueba interna, no Play Store)

### Cómo sacar un release
```bash
git tag v0.1.0-slice-a
git push origin v0.1.0-slice-a
# GitHub → Releases → aparece en 3-5 min con el .apk adjunto
```

## Gates y deuda conocida
- `scripts/audit-gates.sh`: debug APK **monitoreado** (sin gate duro en v1); gate `<40MB` aplica solo a `app-release.apk` cuando exista pipeline release.
- Deuda: debug APK local ~64MB — ver issue "APK pesa 64MB" (optimización pendiente: R8/minify, recursos, splits por ABI).
- Permisos allowlist v1: `READ_MEDIA_AUDIO`, `READ_EXTERNAL_STORAGE`, `INTERNET`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `POST_NOTIFICATIONS`.

## Comandos útiles
```bash
./gradlew test
./gradlew assembleDebug
./scripts/audit-toolchain.sh
./scripts/audit-gates.sh
```
