# AGENTS.md — fonamp

## Proyecto
App Android nativa (Kotlin + Jetpack Compose + Hilt + Media3).
- `applicationId`: `com.fonamp.app` — `minSdk 29`, `targetSdk 35`, JDK 17 (Corretto)
- Módulos: `:app`, `:core:*` (player, network, database, permissions, ui), `:provider:*` (api, radio, local), `:feature:*` (library, radio, settings)
- Versión actual: `0.0.6` (`versionCode 6`) en `app/build.gradle.kts`

## Workflows CI/CD

### CI — `.github/workflows/android.yml`
Corre en `push` a `main`, `feat/**` y `pull_request` a `main`:
1. `scripts/audit-toolchain.sh` — 12 módulos, pins, SDKs, R8 solo en release (debug sin minify)
2. `./gradlew test` — gate de unit tests
3. `./gradlew assembleDebug` — genera `app/build/outputs/apk/debug/app-debug.apk`
4. `scripts/audit-gates.sh` — sin trackers (ads/admob/firebase/analytics/crashlytics), permisos en allowlist, APK monitoreado

### Release APK — `.github/workflows/release.yml`
Publica el APK en GitHub Releases.
- Trigger: `push` de tag `v*` (ej: `v0.1.0-slice-a`)
- Permisos: `contents: write`
- Pasos: checkout → JDK 17 → Android SDK → Gradle → restaura keystore desde secrets → `assembleRelease` (R8 + shrink, **firmado**) → gate `<40MB` → `cp app-release.apk fonamp-<tag>.apk` → `softprops/action-gh-release@v2` con `generate_release_notes: true`
- Resultado: Release con asset `fonamp-<tag>.apk` (build **release firmado** con keystore propio, R8 + shrink — para prueba interna, no Play Store)
- Secrets requeridos: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`. Local: `keystore.properties` (gitignorado, ver `keystore.properties.example`)

### Cómo sacar un release
```bash
git tag -a v0.0.2 -m "fonamp 0.0.2"
git push origin v0.0.2
# GitHub → Releases → aparece en 3-5 min con el .apk adjunto
```

## Versiones (cómo trabajamos)
- Fuente única: `app/build.gradle.kts` (`versionCode` int creciente, `versionName` semver `X.Y.Z`).
- El feature se commitea **tal cual se revisó**; el bump va en un commit aparte `chore: bump to X.Y.Z (versionCode N)` para no invalidar el review.
- Ese mismo commit actualiza la línea `Versión actual` de este AGENTS.md.
- El tag `vX.Y.Z` se crea sobre el commit del bump y se pushea: eso dispara `release.yml`.
- Releases publican build **release firmado** con keystore propio (R8 + shrink, prueba interna). El gate `<40MB` es duro sobre el APK release.
- Ejemplo 0.0.2: `735c3d6` feat artwork + `5ba032f` bump → tag `v0.0.2`.
- Tags **anotados** siempre: `git tag -a vX.Y.Z -m "fonamp X.Y.Z"` (autor+fecha+mensaje; los livianos pierden metadata).
- `main` está **protegida**: requiere check `build` (CI) en verde, sin force-push ni borrado. Se puso por API; equivale en UI a Settings → Branches → Add rule → `main` → ✅ Require status checks (`build`) + ✅ Do not allow force pushes/deletions. `enforce_admins: false` (solo-dev: podés pushear directo, pero nada se mergea en rojo).
- Release notes: `generate_release_notes` + 3-5 bullets humanos por versión (qué trae, en lenguaje de usuario). Cuando haya usuarios externos, el release sale en `draft` primero y se publica a mano tras probar el APK.

## Gates y deuda conocida
- `scripts/audit-gates.sh`: debug APK **monitoreado** (sin gate duro en v1); gate `<40MB` duro sobre el APK release (`app-release-unsigned.apk` sin firmar, `app-release.apk` el día que se firme).
- Medición 2026-09-12: release sin firmar **7,3MB** (R8 + shrink, muy por debajo del gate); debug local ~34MB monitoreado. La firma agrega ~1KB, no mueve la aguja.
- Permisos allowlist v1: `READ_MEDIA_AUDIO`, `READ_EXTERNAL_STORAGE`, `INTERNET`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `POST_NOTIFICATIONS`.

## Comandos útiles
```bash
./gradlew test
./gradlew assembleDebug
./scripts/audit-toolchain.sh
./scripts/audit-gates.sh
```
