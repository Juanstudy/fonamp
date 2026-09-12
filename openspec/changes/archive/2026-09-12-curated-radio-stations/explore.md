# Explore — `curated-radio-stations` (v0.0.3)

> Pregunta del usuario: "revisar el client de radio porque no hay solamente radios mundiales sino también radios como omarchy o lofi; decir de dónde salen y agregarlas".
> Alcance de esta fase: solo exploración, sin implementar.

## 1. De dónde salen las estaciones (evidencia)

**Directorio único: `radio-browser.info` (comunitario, ~30k estaciones), no hay "radios mundiales" hardcodeadas.**

| Capa | Archivo | Evidencia |
|---|---|---|
| Servidores | `core/network/.../MirrorPolicy.kt` | `DE1=https://de1.api.radio-browser.info/`, `DE2`, `NL1`, opcional `BOOTSTRAP=all.api...`. Orden `de1 → de2 → nl1`, sticky-winner + reset ante fallo total. |
| HTTP | `core/network/.../RadioBrowserClient.kt` | Retrofit + kotlinx.serialization + OkHttp (10s connect/read, ~12s call, disk-cache 10MB opcional). `withMirrorFallback`: rota en Offline/Timeout/5xx, falla rápido en 4xx. Errores tipados `NetworkError → SourceError`. |
| Endpoints v1 | `core/network/.../RadioBrowserApi.kt` | Solo 5 llamadas: `GET json/countries`, `GET json/tags`, `GET json/stations/bycountry/{country}`, `GET json/stations/bytag/{tag}`, `POST json/url/{uuid}` (click-count). **Sin** `tops/random/byname/search`. Comentario explícito: "Genres resolve through the tags endpoint". |
| Query mapping | `RadioBrowserClient.fetchStations(StationQuery)` | `country → stationsByCountry`, `genre → stationsByTag`, `tag → stationsByTag`, vacío → `emptyList`. Filtra filas sin `stationuuid` o sin URL resuelta (`url_resolved ?: url`). |
| Contrato | `provider/api/.../Source.kt`, `BrowseQuery.kt` | `Source{id="radio-browser", kind=RADIO}` con `browse/query`, `search(q)`, `streamOf`. `BrowseQuery` vacío = índice; con un campo = lista de estaciones. |
| Source impl | `provider/radio/.../RadioBrowserSource.kt` | `browse()` vacío → índice 24h desde `DirectoryCache`; filtrado → `StationQuery` + cache por `stationKey(country/genre/tag)`. `search(q)` = **filtro en memoria sobre `cache.allStations()`**, cero red, sin name-search (radio Req 6). `streamOf` → `radioMediaItem` + click-count fire-and-forget (nunca bloquea playback). |
| Cache | `core/network/.../DirectoryCache.kt` | TTL 24h, memory-first + JSON write-through. `allStations()` = corpus de búsqueda (dedupe por uuid). `clear()` no toca Room. |
| UI | `feature/radio/.../RadioViewModel.kt`, `RadioScreens.kt` | Discover con 2 segmentos (Countries, GenresTags), filtro de texto **local** por keystroke sin red, flujo 3-taps (Discover → Stations → play). Filas índice (`country:`/`tag:`, `streamUri=""`) no reproducibles por contrato. Favoritas en Room (`FavoriteDao`) solo estaciones. |
| Specs | `openspec/specs/radio/spec.md` Req 1–6, `docs/01-mvp.md` §3.4 | País/género/tag + filtro local + 1-tap play + favoritas locales + cache 24h/offline. **Exclusiones v1**: sin tops/random/name-search. |

Por eso el usuario "solo ve radios mundiales": la UI solo expone el índice `countries + tags` del directorio. Todo lo temático (lofi, jazz, ambient, etc.) ya está en el directorio pero enterrado dentro del tab "Genres & tags" sin curaduría.

## 2. ¿Existen "omarchy" o "lofi" en ese directorio?

**Respuesta corta: `lofi` casi seguro sí (por tag); `omarchy` casi seguro no (ni tag ni país ni nombre).** El directorio es comunitario y orientado a género/tag/ciudad, no a distros Linux.

- `lofi`: el directorio usa tags libres (`lofi`, `lo-fi`, `chillout`, `chill`, `ambient`, `study`, `jazz`). `fetchStations(StationQuery(tag="lofi"))` → `GET json/stations/bytag/lofi` debería devolver filas. Si `lofi` viene vacío, probar variantes `lo-fi`, `chillout`, `chill`.
- `omarchy`: Omarchy es la distro Arch/Hyprland de DHH — no es un género, país ni tag del directorio. Probabilidad ~nula como `tag=omarchy` o `country=omarchy`. Como **nombre de estación** solo existiría si alguien subió una stream llamada así; se verifica con name-search (endpoint existe en el servidor pero **no está cableado en la app** por Req 6).
- `search("lofi")` actual en la app **no prueba nada**: solo filtra lo ya cacheado en `allStations()`. Vacío ≠ no existe en el servidor.

### Cómo verificarlo (sin cambiar código)

```bash
BASE=https://de1.api.radio-browser.info
# 1. ¿Existe el tag lofi / variantes? (ordenar por votos para ver las buenas)
curl -s "$BASE/json/tags/lofi" | head -c 500
curl -s "$BASE/json/tags/lo-fi" | head -c 500
curl -s "$BASE/json/stations/bytag/lofi?order=votes&reverse=true&limit=5" \
  | python3 -c "import json,sys; [print(r['name'],'|',r.get('url_resolved'), '| votes:',r.get('votes')) for r in json.load(sys.stdin)]"

# 2. ¿Existe algo llamado omarchy? (name-search: existe en servidor, NO en la app v1)
curl -s "$BASE/json/stations/byname/omarchy?limit=5" | head -c 1000
curl -s "$BASE/json/tags/omarchy" | head -c 500
curl -s "$BASE/json/stations/bytag/omarchy?limit=5" | head -c 500

# 3. Reproducción del comportamiento actual de la app (lo que la app sí llama)
curl -s "$BASE/json/countries" | head -c 300
curl -s "$BASE/json/tags?order=stationcount&reverse=true&limit=10" | head -c 1000
curl -s "$BASE/json/stations/bytag/chillout?order=votes&reverse=true&limit=3" | head -c 1000
```

Criterio de decisión para la proposal: si `bytag/lofi` devuelve ≥3 streams estables (https, codec MP3/AAC, votos altos), la curada puede ser **query por tag**. Si `byname/omarchy` devuelve 0 filas, "omarchy" no se puede agregar como query — solo como **preset manual** (URL aportada por el usuario/mantenedor) o se descarta con evidencia.

## 3. Contorno de la feature (v0.0.3)

### Opciones evaluadas

| Opción | Qué es | Pro | Contra |
|---|---|---|---|
| **A. Presets hardcodeados** (recomendada para v0.0.3) | Lista fija en código: `CuratedStation(uuid?, name, streamUrl, country, tags, …)` + sección "Curadas/Para vos" en Discover | Determinista, offline-friendly, testeable sin red, permite incluir URLs fuera del directorio (caso omarchy). Review chico. | Requiere mantener URLs; si muere un stream hay que bump. |
| **B. Búsquedas por tag precableadas** | Tiles como "Lofi", "Chill", "Ambient" → `browse(BrowseQuery(tag="lofi"))` existente | Cero mantenimiento de URLs, reutiliza cache 24h + fallback mirrors. | No resuelve omarchy; calidad depende del directorio; tags pueden variar. |
| **C. Favoritas precargadas** | Seed de Room en primera instalación | Aparece en tab existente, sin UI nueva. | Confunde "mío" con "curado", rompe semántica de Req 4 (favoritas = acción del usuario), undo/seed frágil. **Descartada.** |

**Recomendación para proposal: A + B combinadas** — sección "Curadas" con presets fijos (A, incluye lo que el directorio no tiene) + accesos por tag (`lofi`, `chillout`, `ambient`, a confirmar por curl) que reutilizan el path existente (B). C queda fuera.

### Qué cambia por capa (estimación)

- `core/network`: nada obligatorio. Opcional: exponer `stationsByUuids(List<uuid>)` o `byname` solo si los presets A quieren resolverse contra el directorio en vez de URL fija (decisión de proposal; suma superficie API).
- `provider/radio`: nuevo `CuratedStations` (lista + proveedora `browseCurated()` o `BrowseQuery` extendido) que mapea presets → `AudioItem(stationUuid?, streamUri fijo)`. Reutiliza `toAudioItem`/`radioMediaItem`. Tests: preset válido mapea, URL vacía se filtra, click-count no rompe.
- `provider/api`: probablemente nada (si la curada viaja como `List<AudioItem>` del mismo `sourceId="radio-browser"`). Solo cambiaría si se crea un `Source` separado o un `BrowseQuery.curated` — preferir no tocar.
- `feature/radio`: `RadioViewModel` expone estado `curated` (o tercera sección en `Discover`), `RadioScreens` suma fila/sección "Curadas" con mismo row play + heart. Reutiliza `playStation`/`toggleFavorite`. Tests VM + screenshot/Compose.
- `core/database`: nada (favoritar una curada reutiliza `FavoriteStation`; sin migración).
- `app`/DI: registrar proveedora de curadas (Hilt) si no es constante pura.
- Specs/docs: nuevo delta `openspec/changes/curated-radio-stations/specs/` (o extender `specs/radio`), más `proposal.md` + `design.md` + `tasks.md` en el change. Bump versión a `0.0.3` en commit aparte según AGENTS.md (no mezclar con el feat).

### Qué NO cambia (restricciones duras)

- Player intacto: solo consume `MediaItem` vía `streamOf`/`playSingle`; sin cambios en `core/player`, servicio, notificación.
- Sin permisos nuevos (allowlist v1: audio + internet + foreground + notificaciones).
- Sin trackers/SDKs nuevos; release sigue siendo debug sin firma; gate `<40MB` sigue monitoreado.
- Sin tops/random/name-search salvo decisión explícita de proposal que modifique Req 6 (si se cablea `byname` para verificar omarchy, es cambio de scope, no gratis).
- Sin tocar `LocalSource`, settings, ni navegación entre features.

## 4. Riesgos y preguntas abiertas para proposal

1. **Streams muertos**: los presets A se pudren. Mitigación: elegir https + votos altos + `url_resolved`, test que valide formato, documentar cadencia de revisión.
2. **Omarchy no existe en el directorio** (esperado): ¿la aporta el usuario como URL o se declara fuera de alcance con evidencia del curl? Sin URL no hay feature.
3. **Tag lofi fragmentado** (`lofi` vs `lo-fi` vs `chillout`): el curl decide la lista exacta de tags precableados.
4. **Tamaño de review**: mantener el diff ≤400 líneas (ask-on-risk); si crece, partir en cadena preset-UI primero, tag-tiles después.

## 5. Siguiente fase

`proposal` del grafo SDD: problema (descubrimiento temático enterrado), usuario (founder v0.0.3), opciones A+B, non-goals (sin name-search salvo que omarchy lo exija), criterios (curadas visibles en ≤2 taps, play + heart iguales, tests verdes).
