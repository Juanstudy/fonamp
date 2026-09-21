# ODD — Restore total de cola

## Objective
Completar el restore de cola: hoy el snapshot se guarda (mediaIds + index + position) pero la restauración total sigue pendiente. Al reabrir, la app debe reinstalar la cola pausada en el índice y posición guardados, sin phantom-playing.

## Problem
`PlaybackService` persiste con `PrefsQueueStore` y expone `lastSnapshot`, pero arranca idle y nadie re-resuelve los `mediaId` a `MediaItem`. `FakePlayerManager.restore(items, index)` existe pero ignora `positionMs` y no hay `restore` en la interfaz ni en `DefaultPlayerManager`.

## Why
Coherencia post-muerte de proceso (player Req 6): el usuario vuelve a su contexto sin darle play a nada fantasma. Costo bajo: resolver + instalar pausado.

## Scope
- `PlayerManager.restore(items, index, positionMs)` + `Fake` (con posición, 0 para live) + `Default` (pausado: `setMediaItems` + `prepare` + `pause`, nunca auto-play).
- `QueueResolver` puro en `core/player`: mapea `SavedQueue.mediaIds` a candidatos por `mediaId`, dropea irresolubles, clamp de índice, posición 0 si el item es live.
- `QueueRestorer` en `app`: carga `PrefsQueueStore`, junta candidatos (local via `LocalSource.browse` + radio curada via `CuratedStations`→`radioMediaItem`), llama a `player.restore`. Best-effort con `runCatching`, un solo intento al arrancar si la cola está vacía.
- Wiring `FonampRoot` con `LaunchedEffect` una vez.
- Tests: restore con posición, live fuerza 0, índice clamp, resolver dropea faltantes y vacía→null.

Out of scope: persistir shuffle/repeat/speed en el snapshot (lo de `feat/playback-controls` queda en defaults al restaurar), resolver estaciones de directorio no curadas (se dropean y se documenta), auto-play al restaurar.

## Constraints
- Nunca `isPlaying=true` al restaurar (ver precedente `FakePlayerManager.restore`).
- `runCatching` en todo IO de restore: persistencia/restore jamás crashea playback.
- Tests junto al código (`Fake` + Turbine + Robolectric, sin MockK); `./gradlew :core:player:test` verde por tarea.
- Worktree aislado: `../fonamp-queue-restore`, rama `feat/queue-restore` desde `main` (el otro agente sigue en `feat/playback-controls` en el cwd original).

## Authorized scope
Rama `feat/queue-restore` en worktree `../fonamp-queue-restore`. Archivos: `core/player` (`PlayerManager`, `FakePlayerManager`, `DefaultPlayerManager`, `QueueResolver` + tests), `app` (`QueueRestorer`, `FonampShell`), este documento. Sin bump de versión (el bump va aparte por convención).

## Acceptance criteria
- [x] Reabrir con snapshot local restaura cola + índice + posición, pausado.
- [x] Item live restaura con posición 0.
- [x] IDs irresolubles se dropean; si no queda nada, idle limpio.
- [x] Snapshot vacío/nulo no toca el player.
- [x] `./gradlew :core:player:test` verde; `./gradlew test` + `assembleDebug` + audits al cierre.

## Checks
- `./gradlew :core:player:test` (por tarea)
- `./gradlew test` + `./gradlew assembleDebug` + `scripts/audit-toolchain.sh` + `scripts/audit-gates.sh` (cierre)

## Tasks
- [x] **T1** — `PlayerManager.restore` + `Fake` con `positionMs` + tests.
- [x] **T2** — `QueueResolver` puro + tests.
- [x] **T3** — `DefaultPlayerManager.restore` pausado + `syncPending` con posición.
- [x] **T4** — `QueueRestorer` en `app` + wiring `FonampRoot` + verificación final.

## Progress
- 2026-09-21: documento creado en worktree `../fonamp-queue-restore`, rama `feat/queue-restore` desde `main` (d86c410). Base: snapshot guardado en `PlaybackService`/`PrefsQueueStore`, `lastSnapshot` sin consumir. TDD estándar (Fake+Turbine, sin MockK).
- 2026-09-21: T1–T4 implementados en 2 work-unit commits (`6bd4c92` player, `b32fe55` app). Verificación: `./gradlew test` verde, `assembleDebug` verde, `audit-toolchain.sh` y `audit-gates.sh` GREEN. Wiring real solo verificable en dispositivo.

## Next step
Probar en dispositivo (instalar debug, reproducir, matar proceso, reabrir y verificar cola pausada en posición) y luego PR a `main`. Sin bump (va aparte por convención).
