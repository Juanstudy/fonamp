# Proposal — `curated-radio-stations` (v0.0.3)

## Problem statement (requerido)

El descubrimiento temático (lofi, chill, ambient) está enterrado en el tab de tags del Radio Discover. Hoy el usuario solo ve el índice de países ("radios mundiales") + el índice de tags sin curaduría, con un flujo de 3 taps (Discover → Stations → play) y sin ningún acceso directo a lo que realmente escucha.

Evidencia (ver `explore.md`):
- Fuente única: directorio comunitario `radio-browser.info` (~30k estaciones, mirrors de1/de2/nl1). No hay radios hardcodeadas.
- La app solo usa 5 endpoints (`countries`, `tags`, `bycountry`, `bytag`, click-count). Todo lo temático ya existe en el servidor pero sin entrada visible.
- `lofi` casi seguro existe como tag (`lofi` / `lo-fi` / `chillout` / `ambient` — lista exacta a confirmar por curl en apply).
- `omarchy` (distro Linux de DHH) casi seguro NO existe en el directorio ni como tag, país ni nombre — solo viable como preset manual con URL aportada, o fuera de alcance con evidencia del curl.

Sin este cambio, el usuario fundador no encuentra su caso de uso principal (lofi/chill para programar) y concluye que "solo hay radios mundiales".

## Intent

Agregar a Radio Discover una sección "Curadas" con dos vías de entrada complementarias: presets hardcodeados (cubre lo que el directorio no tiene, caso omarchy) + tiles por tag precableados (cero mantenimiento, reutiliza cache 24h + mirror fallback). Play + heart idénticos a estaciones normales. Sin tocar player, permisos, ni scope de búsqueda.

## Scope

### In — A. Presets hardcodeados ("Curadas")

- Nueva lista fija en código: `CuratedStation(name, streamUrl, country?, tags?, stationUuid?)` + proveedora (`browseCurated()` o equivalente) en `provider/radio` que mapea presets → `AudioItem` reutilizando `toAudioItem` / `radioMediaItem`.
- Sección "Curadas" en Discover (`feature/radio`: `RadioViewModel` expone estado `curated`, `RadioScreens` suma fila/sección) con mismo row play + heart (reutiliza `playStation` / `toggleFavorite`).
- Favoritar una curada reutiliza `FavoriteStation` en Room, sin migración.
- Incluye vía de entrada para lo que el directorio no tiene: si el curl de apply confirma que `omarchy` no existe en el directorio, el preset manual con URL aportada por el mantenedor es la única forma de incluirla.

### In — B. Tiles por tag precableados

- Tiles "Lofi" / "Chill" / "Ambient" (lista exacta: `lofi`, `chillout`, `ambient` por defecto, a confirmar por curl en apply entre variantes `lofi` / `lo-fi` / `chill` / `chillout` / `ambient`) → `browse(BrowseQuery(tag=...))` existente.
- Reutiliza path completo: `StationQuery` + `DirectoryCache` 24h + `withMirrorFallback`. Cero mantenimiento de URLs.

### Criterio de decisión omarchy (apply, con red local)

```bash
BASE=https://de1.api.radio-browser.info
curl -s "$BASE/json/stations/bytag/lofi?order=votes&reverse=true&limit=5" | python3 -c "import json,sys; [print(r['name'],'|',r.get('url_resolved')) for r in json.load(sys.stdin)]"
curl -s "$BASE/json/stations/byname/omarchy?limit=5" | head -c 1000
curl -s "$BASE/json/stations/bytag/omarchy?limit=5" | head -c 500
```

- Si `bytag/<tag>` devuelve ≥3 streams estables (https, MP3/AAC, votos altos) → tile por tag.
- Si `byname/omarchy` + `bytag/omarchy` devuelven 0 filas → omarchy solo como preset manual con URL, o fuera de alcance con la evidencia del curl adjunta al PR. Sin URL no hay feature.

## Options considered

| Opción | Veredicto |
|---|---|
| **A. Presets hardcodeados** — lista fija + sección "Curadas" | **Seleccionada.** Determinista, offline-friendly, testeable sin red, permite URLs fuera del directorio (caso omarchy). Contra: requiere mantener URLs (mitigación: elegir https + votos altos + `url_resolved`, test de formato, cadencia de revisión documentada). |
| **B. Tiles por tag precableados** — `lofi/chillout/ambient` → `browse(tag)` existente | **Seleccionada.** Cero mantenimiento, reutiliza cache + mirrors. Contra: no resuelve omarchy, calidad depende del directorio. Por eso va combinada con A. |
| **C. Seed de favoritas precargadas en Room** | **Descartada.** Confunde "mío" con "curado", rompe semántica de Req 4 (favoritas = acción del usuario), undo/seed frágil. |
| Solo A / solo B | Descartadas por el handoff confirmado: el usuario confirmó A+B. Solo A deja mantenimiento total de URLs; solo B deja fuera omarchy. |

## Non-goals

- Sin name-search en app (Req 6 intacto). El `byname/omarchy` se usa solo como verificación por curl en apply, no se cablea en la app salvo cambio de scope explícito.
- Sin tops / random.
- Sin seed de favoritas (opción C descartada, Req 4 intacto).
- Player intacto (`core/player`, servicio, notificación: sin cambios).
- Sin permisos nuevos (allowlist v1) ni SDKs/trackers nuevos.
- Sin tocar `LocalSource`, settings, ni navegación entre features.
- Sin cambios a `core/network` salvo decisión explícita en design (p. ej. `stationsByUuids` solo si los presets quieren resolverse contra el directorio en vez de URL fija — por defecto URL fija, no tocar).
- Release sigue debug sin firma; gate `<40MB` sigue monitoreado (no duro en v1).
- Bump a `0.0.3` en commit aparte según AGENTS.md (no mezclar feat + bump).

## Affected areas

- `provider/radio`: nuevo `CuratedStations` + tests (preset válido mapea, URL vacía se filtra, click-count no rompe playback).
- `feature/radio`: `RadioViewModel` (estado `curated`) + `RadioScreens` (sección "Curadas" + tiles por tag) + tests VM / Compose.
- `provider/api`: preferir no tocar (la curada viaja como `List<AudioItem>` del mismo `sourceId="radio-browser"`).
- `core/database`: nada (sin migración).
- `core/network`: nada por defecto.
- `app`/DI: registrar proveedora solo si no es constante pura.
- Specs/docs: delta en `openspec/changes/curated-radio-stations/specs/` (o extensión de `specs/radio`), más `design.md` + `tasks.md` del change.

## Success criteria

1. Curadas visibles en ≤2 taps desde Discover (sección "Curadas" + tiles por tag en la pantalla inicial de Radio).
2. Play + heart idénticos a estaciones normales (mismo row, mismo mini-player, favorita persiste en Room con undo).
3. `./gradlew test` verde + `./scripts/audit-gates.sh` sin trackers/permisos nuevos.
4. Omarchy resuelto con evidencia: o preset manual con URL funcional, o declarado fuera de alcance con output del curl en el PR.
5. Diff ≤400 líneas (ask-on-risk); si crece, partir en cadena: presets+UI primero, tag-tiles después.
6. Bump 0.0.3 (`versionCode 3`) en commit aparte `chore: bump to 0.0.3` + línea `Versión actual` de AGENTS.md, tag `v0.0.3` sobre ese commit.

## Risks

1. **Streams muertos (presets A se pudren)** — mitigación: https + votos altos + `url_resolved`, test de formato, documentar revisión.
2. **Omarchy no existe en el directorio (esperado)** — mitigación: flujo de decisión con curl definido arriba; sin URL no se inventa stream.
3. **Tag fragmentado (`lofi` vs `lo-fi` vs `chillout`)** — mitigación: el curl en apply fija la lista exacta; los tiles usan el tag con más votos/streams.
4. **Tamaño de review** — mitigación: presupuesto 400 líneas, estrategia ask-on-risk, cadena diferida si hace falta.

## Rollback

- Revert del commit del feat (la sección "Curadas" es aditiva; no hay migración de DB ni cambio de player). Las favoritas marcadas sobre curadas siguen siendo filas `FavoriteStation` válidas aunque la sección desaparezca.
- Sin flags ni migración que limpiar.
