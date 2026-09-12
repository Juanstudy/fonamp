# Design — `curated-radio-stations` (v0.0.3)

## Context

Proposal adds a "Curadas" section to Radio Discover with two complementary
entries (A: hardcoded presets incl. the omarchy escape hatch, B: pre-wired tag
tiles `lofi`/`chillout`/`ambient`), play + heart identical to normal stations,
no player/DB/permission changes. Spec delta pins Req 7–12:

- Req 7: fixed preset list (`CuratedStation` + provider e.g. `browseCurated()`)
  mapping to `AudioItem` with `sourceId="radio-browser"`, identity preserved,
  click-count fire-and-forget.
- Req 8: blank/malformed `streamUrl` filtered before state/UI, no crash.
- Req 9: "Curadas" section on initial Discover, same row play + heart via
  `playStation`/`toggleFavorite`, same mini-player + source badge, ≤2 taps.
- Req 10: favorites via existing `FavoriteStation`, no migration, same undo,
  rows survive section removal.
- Req 11: Lofi/Chill/Ambient tiles → existing `browse(BrowseQuery(tag=…))`
  with `StationQuery` + 24h `DirectoryCache` + `withMirrorFallback`, zero
  maintained URLs (only tag strings pinned).
- Req 12: tag list + omarchy inclusion resolved **only** by apply-time curl
  evidence; no invented URLs; no name-search wired into the app (Req 6 intact).

Code facts constraining this design (read before writing):

- `provider/radio/RadioBrowserSource` (id `radio-browser`) owns
  `StationDto.toAudioItem()` (private) and `streamOf` → `radioMediaItem(item)`
  with `recordClick` fire-and-forget. Index rows use `stableId`
  `country:`/`tag:` prefixes + empty `streamUri` = navigation nodes, never
  playable. `browse(BrowseQuery())` = index, `browse(filtered)` = station
  list via `StationQuery` + `DirectoryCache` + `RadioBrowserClient`.
- `provider/api`: `AudioItem` (plain data, `sourceId`/`stableId`/`streamUri`/
  `stationUuid`/…), `BrowseQuery(country?, genre?, tag?)`, `Source`
  (`browse`/`search`/`streamOf`), `radioMediaItem` (mediaId `radio:<uuid>`).
  No `downloadOf`. Player sees only `MediaItem`.
- `feature/radio/RadioViewModel`: plain class (test scope injected), states
  `Loading/Empty/Discover/Stations/Error`; `Discover` carries
  `countries/tags/segment/query/visible/offline/favorites/refreshing`.
  `playStation` refuses blank `streamUri`; `toggleFavorite` refuses rows
  without `stationUuid`; favorites via `FavoriteDao.upsert/delete`,
  `nowMs` injected. `loadIndex`/`loadStations` split fresh rows by
  `stableId` prefix. `feature/radio` must not import provider internals
  (duplicated `StationDto.toAudioItem` documented in VM file).
- `feature/radio/RadioScreens`: `DiscoverScreen` (filter + `Countries` /
  `Genres & tags` tabs + `discover-list`), `StationsScreen` (station rows
  `station-play-<uuid>` / `station-fav-<uuid>` + heart), `FavoritesScreen`
  (undo snackbar). `DiscoverRoute` maps `BrowseQuery(country|tag)` from row
  `stableId` prefix. No name-search control anywhere (Req 6).
- `feature/radio/FavoritesViewModel`: `FavoriteStation ⇄ AudioItem` mapping
  (`toAudioItem` with `sourceId="radio-browser"`), blank-URL play refused,
  `toggleMediaItem` refuses metadata fabrication. Curated favorites reuse
  exactly this path.

## Decisions (tradeoffs required by `openspec/config.yaml`)

### D1. Dónde vive la lista de presets — `provider/radio` como `CuratedStations` + `browseCurated()`, viajando como `List<AudioItem>` con `sourceId="radio-browser"`; `provider/api` y `BrowseQuery`/`Source` intactos

**Decisión:** nuevo fichero `provider/radio/.../CuratedStations.kt`:

```kotlin
data class CuratedStation(
  val name: String,
  val streamUrl: String,
  val country: String? = null,
  val tags: List<String> = emptyList(),
  val stationUuid: String? = null,
)

object CuratedStations {
  val DEFAULT: List<CuratedStation> = listOf(/* fijado en apply por curl Req 12 */)
  fun browseCurated(presets: List<CuratedStation> = DEFAULT): List<AudioItem>
}
```

`browseCurated()` valida (Req 8: blank o no-`http(s)`-parseable → filtrado),
mapea cada preset válido a `AudioItem(sourceId = RadioBrowserSource.SOURCE_ID,
stableId = stationUuid ?: "curated:<slug(name)>", title = name,
subtitle = (country + tags).join…, streamUri = streamUrl, stationUuid, …)`.
Reutiliza la forma del `toAudioItem` existente; si el mapeo puede
extraerse a función interna compartida sin cambiar signatura pública, se
hace; si no, duplicación mínima documentada (precedente: el VM ya duplica
ese mapeo por la regla `feature ↛ provider internals`).

La VM recibe la lista curada como `List<AudioItem>` ya mapeada (inyectada
como valor o lambda `() -> List<AudioItem>` en el constructor, default =
`CuratedStations::browseCurated`). La curada **no** pasa por `Source.browse`
ni necesita `BrowseQuery` nuevo.

**Tradeoffs:**

| Opción | Pro | Contra | Veredicto |
|---|---|---|---|
| `CuratedStations` + `browseCurated()` en `provider/radio` → `List<AudioItem>` `radio-browser` | Respeta dueño del mapeo radio; testeable sin red; `provider/api` intacto; `stableId`/`stationUuid` preservan identidad Req 7; `streamOf`/`radioMediaItem`/click-count se heredan gratis | Nueva superficie pequeña en `provider/radio` | **Elegida** |
| Constante en `feature/radio` | Cero cambios en provider | Feature fabricaría `AudioItem` radio (rompe regla `feature ↛ provider internals` y duplica ownership del mapeo); mezcla UI con datos | Descartada |
| Extender `BrowseQuery`/`Source` (p.ej. `curated=true`, `stationsByUuids`) | Resolvería presets contra el directorio | Toca `provider/api` + `core/network` contra el non-goal explícito; añade red donde URL fija basta; cambia contrato `Source` | Descartada salvo decisión explícita futura |

`stableId` para presets sin `stationUuid`: `"curated:<slug>"` (slug =
lowercase, no-alnum → `-`). Con `stationUuid` (caso omarchy si el
mantenedor aporta uuid + URL), `stableId = stationUuid` para que
favorito/click-count/mini-player sean indistinguibles de una estación del
directorio. `streamOf` sin cambios: `stationUuid` presente → `recordClick`
fire-and-forget (Req 7 escenario 2); ausente → `radioMediaItem` igual
reproduce (mediaId `radio:<stableId>`).

### D2. Cómo se exponen los tiles por tag y dónde va la sección "Curadas" en Discover

**Decisión:**

- **Tiles:** constantes en `feature/radio`, p.ej.
  `object CuratedTags { val TILES = listOf("lofi" to "Lofi", "chillout" to "Chill", "ambient" to "Ambient") }`
  con los strings exactos fijados en apply por curl (Req 12; default
  `lofi/chillout/ambient`, variante ganadora = más votos/streams).
  Cada tile llama al path existente `openSelection(BrowseQuery(tag = …),
  title)` → `loadStations` → `source.browse` → `StationQuery` +
  `DirectoryCache` 24h + `withMirrorFallback`. Cero URLs (Req 11
  escenario 2: inspección muestra solo tag strings). Sin cambios en
  `RadioBrowserSource`, `StationQuery`, ni cache.
- **Sección "Curadas":** `RadioUiState.Discover` gana un campo
  `curated: List<AudioItem> = emptyList()` (default preserva constructores
  existentes en tests). `RadioViewModel` lo puebla en `emitDiscover` /
  `emitDiscoverRows` desde el proveedor inyectado (síncrono, sin red) y lo
  re-emite en `onFavorites` para paridad de hearts. Fuera de `Discover`
  (estados `Stations`/`Error`/…) la sección no existe — solo vive en el
  Discover inicial (Req 9).
- **UI:** `DiscoverScreen` renderiza, encima de la fila de filtro/tabs, una
  sección `"Curadas"` (`testTag("curated-section")`) con: (a) fila de tiles
  por tag (cada tile `testTag("curated-tag-<tag>")`, tap → `onOpenSelection
  (BrowseQuery(tag=…))`), y (b) lista de presets curados reutilizando el
  **mismo** row play + heart de `StationsScreen` (extraer `StationRow`
  composable compartido si cabe en presupuesto; si no, duplicar el Row con
  mismos testTags `station-play-<uuid>` / `station-fav-<uuid>` y mismo
  `SourceBadgeKind.RADIO`). `RadioRouteScreen`/`DiscoverRoute` cablean
  `onPlay = viewModel::playStation`, `onToggleFavorite =
  viewModel::toggleFavorite` sin wrappers nuevos. Sin name-search (Req 12
  escenario 3).

**Tradeoffs:**

| Opción | Pro | Contra | Veredicto |
|---|---|---|---|
| `curated` como campo en `Discover` + tiles como constantes de tag en feature | Discover sigue siendo el único estado inicial; `Stations` intacto; tiles reutilizan `browse(tag)` + cache + mirrors sin tocar providers | `Discover` crece un campo (aditivo, con default) | **Elegida** |
| Nuevo `RadioUiState.Curated` separado | Aislamiento | Rompe invariante "Discover es la pantalla inicial única"; más ramas en `RadioRouteScreen`; más tests | Descartada |
| Tiles vía índice de tags existente (sin precablear) | Cero código | No cumple Req 11 (entrada visible en ≤2 taps); el descubrimiento sigue enterrado — el problema original | Descartada |

Orden visual en `DiscoverScreen`: 1) sección "Curadas" (tiles + presets),
2) filtro + tabs + lista existentes. Justificación: el problema es que lo
temático está enterrado; la curaduría debe ser lo primero que se ve. El
filtro local (`setQuery`) sigue operando solo sobre `countries/tags`
(`visible`), **no** sobre `curated` — la sección curada no se filtra (evita
sorpresa: lo curado siempre visible).

### D3. Play/favorite reutilizan `playStation`/`toggleFavorite` sin cambios en player, DB ni DI (salvo registro mínimo)

**Decisión:** cero cambios en `core/player`, `core/database` (Req 10:
sin migración, sin entidad nueva), y `provider/api`. Camino:

- **Play curada:** `playStation(curatedItem)` → guard blank-URL (Req 8 ya
  filtró, doble defensa) → `player.playSingle(source.streamOf(item))` →
  mismo mini-player, mismo badge. `FavoritesViewModel.play` idéntico para
  curadas favoritadas (vía `FavoriteStation.toAudioItem()` existente).
- **Heart curada:** `toggleFavorite(curatedItem)` → guard `stationUuid`
  (presets con uuid) → `FavoriteDao.upsert(FavoriteStation(…))` existente;
  `onFavorites` actualiza hearts en Discover. **Presets sin `stationUuid`
  no son favoritable** (el guard existente los ignora) — por tanto la
  regla de apply es: **todo preset que deba ser favoritable lleva
  `stationUuid`** (generar o pedir al mantenedor; para omarchy manual,
  `stationUuid` sintético estable p.ej. `"curated-omarchy"` si no hay uuid
  real del directorio). `FavoritesScreen` + undo-snackbar (10s) sin cambios.
- **DI:** si `browseCurated` es `object`/función pura, **cero registro
  Hilt** — la VM lo recibe como valor/lambda con default en constructor y
  el módulo Hilt existente no se toca. Solo si el proyecto exige
  inyectar el proveedor como binding se añade un `@Provides` de una línea;
  prohibido tocar grafo del player o DB.

**Tradeoffs:**

| Opción | Pro | Contra | Veredicto |
|---|---|---|---|
| Reúso total, `stationUuid` obligatorio para favoritable | Req 9/10 por construcción; sin migración; undo gratis | Presets sin uuid no tienen heart (limitación visible) | **Elegida**, con regla "sin uuid ⇒ sin heart" documentada en UI (el heart simplemente no se muestra o es no-op, igual que filas de índice) |
| Sintetizar favorito por URL (`stableId` como clave) | Heart universal | Cambia semántica de `FavoriteStation` (clave = `stationUuid`), migración o doble clave, rompe Req 10 "sin migración" | Descartada |

### D4. Verificación por curl en apply + estrategia de tests

**Curl (apply, con red local, Req 12 — comandos de la proposal, no se
cablean en la app):**

```bash
BASE=https://de1.api.radio-browser.info
curl -s "$BASE/json/stations/bytag/lofi?order=votes&reverse=true&limit=5" \
 | python3 -c "import json,sys; [print(r['name'],'|',r.get('url_resolved')) for r in json.load(sys.stdin)]"
# repetir para variantes: lo-fi, chill, chillout, ambient
curl -s "$BASE/json/stations/byname/omarchy?limit=5" | head -c 1000
curl -s "$BASE/json/stations/bytag/omarchy?limit=5" | head -c 500
```

Criterio: tile ⇔ `bytag/<tag>` con ≥3 streams https + MP3/AAC + votos
altos (variante con más votos gana); omarchy ⇔ URL funcional aportada por
el mantenedor (preset manual) o fuera de alcance con ambos outputs vacíos
adjuntos al PR. Evidencia adjunta al PR obligatoria. `byname` jamás se
cablea en la app.

**Tests (todos unitarios/JVM, `./gradlew test`):**

| Req | Test | Módulo |
|---|---|---|
| 7 | preset válido mapea: `name`/`streamUri`/`sourceId="radio-browser"`/`stationUuid` preservados | `provider/radio` (`CuratedStationsTest`) |
| 7 | click-count no bloquea: `streamOf` devuelve `MediaItem` aunque `click()` falle (fake client que lanza) | `provider/radio` (extender `RadioBrowserSourceTest` o test en `CuratedStationsTest` vía `streamOf`) |
| 8 | URL vacía se filtra; URL malformada (`"not a url"`, `ftp://…` según regla que se fije: solo `http/https` parseable) se filtra, sin crash | `provider/radio` |
| 9 | VM expone `curated` en `Discover`; `playStation` delega a `player.playSingle` con el item curado; `toggleFavorite` hace upsert | `feature/radio` (`RadioViewModelTest`, fakes de `Source`/`FavoriteDao`/`PlayerManager` existentes) |
| 10 | favorita curada persiste (fake DAO), aparece en `FavoritesUiState.Content`, `remove` + `undo` restauran; inspección de schema: sin entidad/migración nueva (assert por ausencia de fichero de migración o test de schema existente en verde) | `feature/radio` + `core/database` tests existentes en verde |
| 11 | tile tap → `source.browse(BrowseQuery(tag="<pinned>"))` (verificar argumento capturado en fake `Source`); inspección: ningún `streamUrl` hardcodeado en `CuratedTags` | `feature/radio` |
| 12 | Sin test de red en CI (curl es manual en apply); test de que no existe control de name-search: `RadioScreensTest` assert de ausencia (o revisión) | `feature/radio` |

Formato URL (regla a fijar en apply): `streamUrl.isNotBlank() &&
runCatching { java.net.URI(it).toURL().protocol in setOf("http","https") }
.getOrDefault(false)`. Compose: `RadioScreensTest`/`RadioUiScaffoldTest`
asserts de `curated-section` visible, tiles por tag presentes, row curado
con play + heart testTags.

## File changes

| Fichero | Cambio |
|---|---|
| `provider/radio/.../CuratedStations.kt` **(nuevo)** | `CuratedStation` + `DEFAULT` (fijado en apply) + `browseCurated()` con validación Req 8 y mapeo a `AudioItem` `radio-browser` |
| `provider/radio/.../RadioBrowserSource.kt` | Solo si se extrae mapeo compartido interno; sin cambios de signatura pública ni de `browse`/`search`/`streamOf` |
| `provider/radio/.../CuratedStationsTest.kt` **(nuevo)** | Req 7 + 8 + click-count |
| `feature/radio/.../RadioViewModel.kt` | `Discover.curated: List<AudioItem> = emptyList()`; constructor acepta proveedor curado (default `CuratedStations::browseCurated` vía lambda/valor); poblar en `emitDiscover`/`emitDiscoverRows` + `onFavorites` |
| `feature/radio/.../RadioScreens.kt` | Sección "Curadas" + tiles (`CuratedTags`), reutiliza `StationRow` play + heart; sin name-search |
| `feature/radio/.../CuratedTags.kt` **(nuevo, o dentro de `RadioScreens.kt` si <400 líneas lo exige)** | Constantes de tiles `label → tag` fijadas en apply |
| `feature/radio/.../RadioViewModelTest.kt`, `RadioScreensTest.kt` | Req 9–12 (estado, delegación, tiles, ausencia de search) |
| `openspec/changes/curated-radio-stations/tasks.md` | Desglose en work units ≤400 líneas |
| **No tocar** | `provider/api/*`, `BrowseQuery`, `Source`, `core/player/*`, `core/database/*` (schema), `core/network/*`, permisos, navegación, settings, `LocalSource` |

Estimación: ~250–350 líneas (nuevo fichero + tests + VM/UI), dentro del
presupuesto 400; si el review crece, cadena: PR1 presets + sección
(Req 7–10), PR2 tiles (Req 11–12). `ask-on-risk`: pausar y preguntar antes
de partir o de aceptar `size:exception` — nunca inferir `exception-ok`.

## Contracts

- `browseCurated(): List<AudioItem>` — pura, sin red, sin caché; cada
  elemento: `sourceId="radio-browser"`, `streamUri` https válida no vacía,
  `stableId = stationUuid ?: "curated:<slug>"`. Vacío ⇔ todos filtrados
  (Req 8); nunca lanza.
- `RadioUiState.Discover.curated` — lista ya mapeada, siempre presente en
  Discover inicial (posiblemente vacía); `Stations` no la lleva.
- `BrowseQuery(tag=…)` para tiles — mismo contrato que el índice de tags;
  cache 24h + mirror fallback heredados, sin parámetros nuevos.
- `FavoriteStation` — curadas favoritadas son filas normales; `stationUuid`
  es la clave (sin uuid ⇒ sin heart); sin migración; undo 10s intacto.

## Rollout / rollback

Feat en un commit; bump `0.0.3 (versionCode 3)` + AGENTS.md en commit
aparte `chore: bump to 0.0.3`; tag `v0.0.3` sobre el bump (dispara
`release.yml` debug). Rollback = revert del commit feat (aditivo, sin
migración; favoritas huérfanas siguen válidas). Gates: `./gradlew test` +
`audit-gates.sh` (sin trackers/permisos nuevos); APK debug monitoreado.

## Risks (residual post-design)

1. Presets sin `stationUuid` pierden heart — aceptado y documentado; regla
   de apply lo evita (todo preset favoritable lleva uuid).
2. Streams fijos se pudren — mitigado con https + `url_resolved` de votos
   altos + test de formato + revisión documentada; tiles (cero mantenimiento)
   cubren lofi/chill/ambient aunque un preset caiga.
3. Tag fragmentado — resuelto por curl en apply, no por adivinanza.
