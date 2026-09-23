# ODD — Lista visible de cola en player sheet

## Objective
Mostrar la cola ("A continuación") dentro del player sheet: hoy la cola funciona por debajo (persist + restore pausado) pero el sheet solo muestra el tema actual. El usuario tiene que ver qué sigue y poder saltar con un toque.

## Problem
`PlayerUiState.queue: List<MediaItem>` + `index` ya se exponen vía `PlayerManager.state`, pero `FonampShell` solo deriva el item actual (`queue.getOrNull(index)`) y se lo pasa a `PlayerSheet` como `title`/`subtitle`/`artworkUri`. `PlayerSheet` no tiene ningún parámetro de cola. Además no existe salto a índice: solo `next()`/`prev()`; reutilizar `play(items, index)` limpiaría el sleep timer y el banner (efectos no deseados).

## Why
Cerrar el loop de la cola: restore sin lista visible es contexto ciego. Costo bajo: los datos ya fluyen, falta render + salto que preserve timer/banner. Issue: #16.

## Scope
- `QL-1` — Sección `UpNext` en `core/ui`: modelo UI propio (`QueueRow`: title, subtitle, artworkUri, duration label, isActive) + composable con `LazyColumn` (o `Column`), highlight del actual, `onSelect(index)`, testTags (`up-next-section`, `up-next-row-N`, `up-next-active`). Tests Robolectric (compose rule, estilo `UiStatesTest`): render, highlight, tap callback, oculta con cola vacía.
- `QL-2` — `PlayerManager.playAt(index)`: salto dentro de la cola actual, clamp, no-op con cola vacía, **preserva** sleep timer y banner. `Default` (seek a item vía controller) + `Fake` + tests en `PlayerManagerTest`.
- `QL-3` — Wiring en `FonampShell`: mapear `queue`→`QueueRow` (títulos desde `mediaMetadata`, duración vía `durationMs()`), pasar `index` actual y `onSelect = player::playAt`. Sin cambios a prev/next (siguen ocultos en live). Verificación final + commits work-unit.

Out of scope: reordenar (drag), swipe-to-remove, menú por fila, diferencias radio vs local más allá de lo actual, bump de versión (va aparte por convención), release.

## Constraints
- `core/ui` sigue desacoplado de `core/player`: ni `MediaItem` ni `PlayerUiState` cruzan al sheet; solo el modelo `QueueRow` + primitivas (precedente: modos de transporte como primitivas).
- `playAt` jamás auto-limpia timer ni banner; con cola vacía o índice igual al actual es no-op observable.
- Tests junto al código (Fake + Robolectric compose; sin MockK salvo que el módulo ya lo use). Verde por tarea en el módulo tocado.
- Tamaño: forecast <400 líneas autoradas (UI + player + wiring + tests); un solo PR salvo que el conteo real lo desmienta.

## Authorized scope
Rama `feat/queue-list` desde `main` @ `8d8a842`. Archivos: `core/ui` (`PlayerSheet.kt` o nuevo `UpNextSection.kt` + tests), `core/player` (`PlayerManager`, `DefaultPlayerManager`, `FakePlayerManager`, `PlayerManagerTest`), `app` (`FonampShell.kt`), este documento. TDD: sin resolver (no hay registro `sdd-init`; presencia de tests no lo activa) → checks funcionales ordinarios, RED-before-GREEN no exigible salvo que lo pidas.

## Acceptance criteria
- [ ] Con cola no vacía el sheet muestra "Up next" con una fila por item y el actual resaltado.
- [ ] Tocar la fila N reproduce N; timer y banner intactos.
- [ ] Índice fuera de rango → clamp; cola vacía → no-op y sección oculta.
- [ ] `./gradlew test` verde en módulos tocados + `assembleDebug` verde al cierre.

## Checks
- Por tarea: `./gradlew :core:ui:testDebugUnitTest` (QL-1) / `./gradlew :core:player:test` (QL-2).
- Cierre: `./gradlew test` + `./gradlew assembleDebug` (+ audits si aplica).

## Tasks
- [x] **QL-1** — `UpNextSection` + `QueueRow` en `core/ui` + tests Robolectric. Ruta: inline (parent; delegación `Task` no disponible — restricción free-tier, ver Progreso). Commit `6f71046`.
- [x] **QL-2** — `PlayerManager.playAt` + `Default` + `Fake` + tests. Ruta: inline (mismo motivo). Commit `ee5e315`.
- [x] **QL-3** — Wiring `FonampShell` + verificación final + work-unit commits. Ruta: inline (mismo motivo). Commit `9d9301c`.

## Progress
- 2026-09-23: base sincronizada (`main` @ `8d8a842`, issue #16, rama `feat/queue-list`). Exploración: `Task/explore` falló (free tier solo dentro de OpenCode); mapeo fallback vía `codegraph_explore` + lecturas acotadas (PlayerSheet, PlayerUiState, QueueStore, FonampShell 140-309, Default/Fake métodos, UiStatesTest). Comportamiento propuesto por default (lista completa, tap reproduce, timer intacto) documentado en #16; si lo corregís, se actualiza intent + TODOs.
- 2026-09-23: QL-1 hecho (`6f71046`, 299 inserciones): `UpNextSection.kt` + `QueueRow`, `PlayerSheet(upNext, onSelectQueueItem)`, `UpNextSectionTest` (6 tests). Tropezón: fila `clickable` fusiona semántica — tags internos no consultables en árbol fusionado; tests usan texto + row tag (patrón del repo). `:core:ui:testDebugUnitTest` verde.
- 2026-09-23: QL-2 hecho (`ee5e315`): `PlayerManager.playAt` + `Default` (`seekTo(index,0)` + prepare/play; `connect()` cubre desconectado vía `syncPendingToController`) + `Fake` (copy preserva timer/banner) + 5 tests Turbine. `:core:player:test` verde. Edición intermedia mal anclada reparada en el acto (línea 243 Default).
- 2026-09-23: QL-3 hecho (`9d9301c`): `FonampShell` mapea `queue→QueueRow`, `onSelectQueueItem = player::playAt`. `./gradlew test` verde (2m54s) + `assembleDebug` verde. RDD on (global): assess medium + `review_due` (`slice_budget_reached`, 453 líneas / 8 paths) → preflight STATUS pide `intended_untracked_selection` (los 4 `odd/*.md` ajenos + este doc).

## Review
- `assess --base-ref 8d8a842 --committed-only`: `risk: medium` (`executable_change` FonampShell), `review_due: true`, `review_due_reason: slice_budget_reached` (453 > ~400).
- STATUS preflight: `action: start`, `applicability: unrelated`, collect `intended_untracked_selection_required`. Este doc se commitea para salir del inventario untracked; los 4 restantes son ajenos al candidato.

## Next step
Completar el collect de untracked para START; si los lentes no pueden correr (Task roto en este entorno), dejar review en blocked con evidencia y pedir decisión de push/PR.
