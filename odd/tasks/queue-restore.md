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
- [ ] Reabrir con snapshot local restaura cola + índice + posición, pausado.
- [ ] Item live restaura con posición 0.
- [ ] IDs irresolubles se dropean; si no queda nada, idle limpio.
- [ ] Snapshot vacío/nulo no toca el player.
- [ ] `./gradlew :core:player:test` verde; `./gradlew test` + `assembleDebug` + audits al cierre.

## Checks
- `./gradlew :core:player:test` (por tarea)
- `./gradlew test` + `./gradlew assembleDebug` + `scripts/audit-toolchain.sh` + `scripts/audit-gates.sh` (cierre)

## Tasks
- [ ] **T1** — `PlayerManager.restore` + `Fake` con `positionMs` + tests.
- [ ] **T2** — `QueueResolver` puro + tests.
- [ ] **T3** — `DefaultPlayerManager.restore` pausado + `syncPending` con posición.
- [ ] **T4** — `QueueRestorer` en `app` + wiring `FonampRoot` + verificación final.

## Progress
- 2026-09-21: documento creado en worktree `../fonamp-queue-restore`, rama `feat/queue-restore` desde `main` (d86c410). Base: snapshot guardado en `PlaybackService`/`PrefsQueueStore`, `lastSnapshot` sin consumir. TDD estándar (Fake+Turbine, sin MockK).

## Next step
Implementar T1.
