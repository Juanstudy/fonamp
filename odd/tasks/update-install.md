# ODD — Fix update-install: la descarga nunca termina de instalar

## Objective
Que el update in-app complete de verdad: hoy detecta la release pero la instalación nunca ocurre y no hay ninguna señal de progreso o fallo (issue #18).

## Problem
- `DownloadCompleteReceiver` hace `startActivity(installer)` desde background. Desde Android 10 (minSdk 29) esos launches están bloqueados → con la app en segundo plano al completar, el instalador jamás abre, en silencio.
- Sin pickup al arrancar: el docstring promete retomar vía `finishedDownload()`, pero nada lo llama (cero callers). El APK varado no se re-ofrece; `clearTracking()` tampoco se llama nunca.
- Cero feedback: `UpdateUiState` no tiene Downloading/Failed; una descarga fallida en DownloadManager resuelve a null y silencio permanente.

Descartado con evidencia: `REQUEST_INSTALL_PACKAGES` declarado, FileProvider + `filepaths.xml` coherentes, asset predicate y `isNewerVersion` correctos, receiver registrado y exento del ban a receivers implícitos.

## Why
El usuario reporta que "nunca funciona" y tiene razón con mecanismo identificado. Costo mediano, todo en `:app`, sin cambios en features.

## Scope
- `UP-1` — Receiver notifica en vez de `startActivity`: éxito → notificación "Update downloaded" con contentIntent `getActivity(ACTION_VIEW apk)` directo (el tap en notificación SÍ puede lanzar); fallo terminal → notificación "Update download failed" (tap reabre la app) + `clearTracking()`. Nuevo `app/update/UpdateNotifications.kt` (channel `fonamp_updates`) + `ApkInstaller.installIntent(apkFile): Intent?` reutilizado por `installNow` y la notificación. Tests Robolectric (ShadowNotificationManager): éxito postea con intent VIEW, fallo postea + limpia tracking.
- `UP-2` — Pickup al arrancar en `FonampShell`: `LaunchedEffect(Unit)` → `installer.pendingUpdateNewerThanInstalled(BuildConfig.VERSION_CODE)` → diálogo privado `UpdateReadyDialog` (Install/Later, testTags). `pendingUpdateNewerThanInstalled` = `finishedDownload` + `PackageManager.getPackageArchiveInfo` + comparativa `longVersionCode`; lookup inyectable para tests. Install y Later hacen `clearTracking()` (semántica: lo rastreado es "en vuelo o sin ofrecer"; re-descargar re-trackea).
- `UP-3` — Confirmación de inicio: snackbar "Downloading update — we'll notify you when it's ready." en los dos puntos de Download (Settings + cold-start). El progreso vivo lo da la notificación propia de DownloadManager (`VISIBILITY_VISIBLE`).

Out of scope: estados Downloading/Failed con progreso en el ViewModel (el sistema ya muestra progreso; se difiere), reintento automático, bump de versión, release.

## Constraints
- Sin activity-start desde background en ningún camino (ni receiver ni trampolines dudosos).
- Sin dependencias nuevas: `NotificationManager`/`NotificationChannel`/`PendingIntent` del framework (minSdk 29 lo permite), `FLAG_IMMUTABLE`.
- Si `POST_NOTIFICATIONS` está denegado, el sistema suprime las notificaciones en silencio: el backstop es el diálogo de UP-2 (sin permiso requerido). El permiso ya se pide lazy en primer playback.
- Tests junto al código (Robolectric + shadows, estilo `ApkInstallerTest`); device-check en celu por adb al cierre (instalación real no es unit-testeable).

## Authorized scope
Rama `fix/update-install-flow` desde `main` @ `8d8a842`. Archivos: `app/update` (`DownloadCompleteReceiver`, `UpdateNotifications` nuevo, `ApkInstaller`), `app` (`FonampShell.kt`), tests en `app/src/test/.../update`. Sin bump (va aparte).

## Acceptance criteria
- [ ] Descarga completada con app en background → notificación → tap abre el instalador.
- [ ] Descarga fallida → notificación de fallo; tracking limpio.
- [ ] App muerta durante la descarga + reabrir tras completar → diálogo Update-ready (solo si el APK es más nuevo que lo instalado).
- [ ] Tocar Download muestra confirmación de inicio.
- [ ] `./gradlew test` + `assembleDebug` verdes; device-check con la release instalada.

## Checks
- Por tarea: `./gradlew :app:testDebugUnitTest --tests "com.fonamp.app.update.*"`.
- Cierre: `./gradlew test` + `./gradlew assembleDebug`.

## Tasks
- [x] **UP-1** — Notificaciones de éxito/fallo en receiver + `installIntent` + tests. Ruta: inline (Task no disponible — free-tier). Commit `fix(update): signal download completion via notification`.
- [x] **UP-2** — Helper `pendingUpdateNewerThanInstalled` + diálogo pickup en shell + tests. Ruta: inline. Commit `fix(update): re-offer stranded apk at startup when newer`.
- [x] **UP-3** — Snackbars de inicio + verificación final + work-unit commits. Ruta: inline. Commit `fix(update): confirm download start with snackbar`.

## Progress
- 2026-09-23: issue #18, rama `fix/update-install-flow` desde `main`. Investigación previa (sin escritura): codegraph + manifest + filepaths + asset predicate + grep de callers. Defaults documentados en #18.
- 2026-09-23: UP-1 hecho: receiver notifica (éxito con intent VIEW directo, fallo con reapertura + clearTracking) + `installIntent` extraído + 2 test files nuevos. Tropezón honesto: `getLaunchIntentForPackage` devuelve null bajo Robolectric → cambiado a `MainActivity` explícito (más determinista en prod también). `:app` update-tests verdes.
- 2026-09-23: UP-2 hecho: `pendingUpdateNewerThanInstalled` (lookup inyectable) + `UpdateReadyDialog` en shell con pickup al arrancar + Install/Later limpian tracking. Dos ediciones mal ancladas reparadas en el acto (companion pegado, pickup antes de declarar `installer`).
- 2026-09-23: UP-3 hecho: snackbar en ambos Download. `./gradlew test` + `assembleDebug` verdes. Sin `POST_NOTIFICATIONS` las notificaciones se suprimen (sistema) y el diálogo UP-2 es el backstop.

## Next step
Assess RDD del rango y reportar; device-check real (descargar release vieja → update) en celu por adb.
