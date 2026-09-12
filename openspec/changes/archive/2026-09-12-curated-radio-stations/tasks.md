# Tasks — `curated-radio-stations` (v0.0.3)

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 250–350 (nuevo fichero + VM/UI + tests) |
| 400-line budget risk | Medium |
| Chained PRs recommended | No |
| Suggested split | Single PR; si supera 400 en apply → PR1 presets+sección (Req 7–10) → PR2 tiles (Req 11–12) |
| Delivery strategy | ask-on-risk |
| Chain strategy | pending |

```text
Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: pending
400-line budget risk: Medium
```

> ask-on-risk: pausar y preguntar antes de partir en cadena o aceptar `size:exception`; nunca inferir `exception-ok`. Bump 0.0.3 (`versionCode 3` + AGENTS.md + tag `v0.0.3`) va en commit aparte, fuera de estas tareas.

## Apply preconditions (red local, evidencia obligatoria)

- [x] Ejecutar curl de decisión tags + omarchy contra `https://de1.api.radio-browser.info` (`bytag/lofi|lo-fi|chill|chillout|ambient` + `byname/omarchy` + `bytag/omarchy`), fijar lista exacta de tiles (variante con más votos/streams) y decidir omarchy (preset manual con URL aportada o fuera de alcance); adjuntar outputs al PR. Evidencia esperada: outputs de curl pegados en PR + lista final de tags en comentario. <!-- sdd-owner: implementation -->
- [x] Verificar que `bytag/<tag>` ganador tiene ≥3 streams https con `url_resolved` MP3/AAC y votos altos (Req 12 escenario 1); si ningún tag llega a 3, pausar y preguntar (ask-on-risk) antes de cablear tiles. Evidencia esperada: tabla tag → count/votos en descripción del PR. <!-- sdd-owner: implementation -->

## Provider — presets curados (Req 7–8, D1)

- [x] Crear `provider/radio/src/main/java/com/fonamp/provider/radio/CuratedStations.kt` con `CuratedStation(name, streamUrl, country?, tags?, stationUuid?)` + `object CuratedStations { DEFAULT; browseCurated() }` que valida (`blank` o no-`http(s)`-parseable → filtrado, nunca lanza) y mapea a `AudioItem(sourceId="radio-browser"`, `stableId = stationUuid ?: "curated:<slug>"`, `streamUri = streamUrl`). Evidencia esperada: `CuratedStationsTest` — preset válido mapea nombre/streamUri/sourceId/stationUuid. <!-- sdd-owner: implementation -->
- [x] Cubrir Req 8 en `provider/radio/src/test/.../CuratedStationsTest.kt` (nuevo): URL vacía se filtra y URL malformada (`"not a url"`, `ftp://…`) se filtra sin crash. Evidencia esperada: `./gradlew :provider:radio:test` verde con ambos casos. <!-- sdd-owner: implementation -->
- [x] Verificar click-count no bloquea playback de curada (Req 7 escenario 2) vía `RadioBrowserSource.streamOf` + fake `RadioBrowserClient` que lanza en `click()`: `streamOf` devuelve `MediaItem` igual. Tocar `provider/radio/src/main/java/com/fonamp/provider/radio/RadioBrowserSource.kt` solo para extracción interna compartida sin cambiar signatura pública; si no cabe limpio, duplicación mínima documentada. Evidencia esperada: test `streamOf` con click fallido en verde. <!-- sdd-owner: implementation -->

## Feature — ViewModel estado `curated` (Req 9–10, D2–D3)

- [x] Extender `feature/radio/src/main/java/com/fonamp/feature/radio/RadioViewModel.kt`: `RadioUiState.Discover` gana `curated: List<AudioItem> = emptyList()`, constructor acepta proveedor curado (valor/lambda, default `CuratedStations::browseCurated`), poblar en `emitDiscover`/`emitDiscoverRows` y re-emitir en `onFavorites`; `Stations`/`Error` no llevan `curated`; filtro `setQuery` no filtra `curated`. Evidencia esperada: `RadioViewModelTest` — `Discover.curated` poblado, `playStation` delega a `player.playSingle`, `toggleFavorite` hace upsert. <!-- sdd-owner: implementation -->
- [x] Aplicar regla favoritable (D3): todo preset que deba tener heart lleva `stationUuid` (para omarchy manual, uuid sintético estable p.ej. `"curated-omarchy"` si no hay uuid real); presets sin uuid no son favoritable (guard existente, heart oculto/no-op). Evidencia esperada: test VM — `toggleFavorite` ignora item sin `stationUuid`, hace upsert con uuid. <!-- sdd-owner: implementation -->

## Feature — UI sección Curadas + tiles (Req 9, 11, D2)

- [x] Crear constantes de tiles en `feature/radio/.../CuratedTags.kt` (o dentro de `RadioScreens.kt` si el presupuesto <400 lo exige): solo strings de tag (`label → tag`, defaults `lofi/chillout/ambient` sobrescritos por curl), cero `streamUrl` hardcodeados (Req 11 escenario 2). Evidencia esperada: inspección — `grep -rn streamUrl feature/radio/.../CuratedTags.kt` vacío. <!-- sdd-owner: implementation -->
- [x] Renderizar sección "Curadas" en `feature/radio/src/main/java/com/fonamp/feature/radio/RadioScreens.kt` (`DiscoverScreen`, encima de filtro/tabs, `testTag("curated-section")`): fila de tiles (`testTag("curated-tag-<tag>")`, tap → `onOpenSelection(BrowseQuery(tag=…))` existente con `StationQuery` + `DirectoryCache` 24h + `withMirrorFallback`) + lista de presets reutilizando mismo row play + heart (`station-play-<uuid>` / `station-fav-<uuid>`, `SourceBadgeKind.RADIO`); cablear `onPlay = viewModel::playStation`, `onToggleFavorite = viewModel::toggleFavorite`; sin control de name-search (Req 6/12 intacto). Evidencia esperada: `RadioScreensTest`/`RadioUiScaffoldTest` — sección visible, tiles presentes, row curado con play + heart. <!-- sdd-owner: implementation -->
- [x] Verificar tile tap delega a `source.browse(BrowseQuery(tag="<pinned>"))` con argumento capturado en fake `Source` (Req 11 escenario 1, cache + mirror heredados sin tocar `RadioBrowserSource`/`StationQuery`/cache). Evidencia esperada: test VM/fake-source con captor de `BrowseQuery(tag=…)` en verde. <!-- sdd-owner: implementation -->

## Favoritas sin migración + gates (Req 10, D3–D4)

- [x] Reutilizar `FavoriteStation`/`FavoriteDao`/`FavoritesViewModel` sin entidad ni migración nueva: curada favoritada aparece en `FavoritesUiState.Content`, `remove` + undo (10s) restauran; no tocar `core/database/*` (schema), `core/player/*`, `provider/api/*`, `core/network/*`, permisos ni navegación. Evidencia esperada: tests `feature/radio` + `core/database` existentes en verde + inspección — sin fichero de migración nueva. <!-- sdd-owner: implementation -->
- [x] Correr gates finales: `./gradlew test` verde + `./scripts/audit-gates.sh` sin trackers/permisos nuevos (allowlist v1 intacta), PR con evidencia curl + diff ≤400 líneas o propuesta de cadena. Evidencia esperada: logs de ambos comandos + diff stat en PR. <!-- sdd-owner: implementation -->
