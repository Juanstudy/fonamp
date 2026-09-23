# ODD — Artwork de Emisoras (Radio)

## Objective
Mostrar el artwork (favicon) de las emisoras de radio en las listas de la app y en el reproductor.

## Problem
Actualmente las listas de emisoras muestran un icono genérico de radio (`AppIcons.Radio`) en vez del artwork oficial que provee la API de RadioBrowser, haciendo que la UI se vea estéril y menos reconocible.

## Why
El artwork mejora la experiencia de usuario y el reconocimiento de las estaciones. La API de RadioBrowser ya provee un campo `favicon` que podemos aprovechar.

## Scope
- `core/network`: Agregar `favicon` a `StationDto`.
- `provider/radio`: Mapear `favicon` a `AudioItem.artworkUri`.
- `core/database`: Agregar `artworkUri` a `FavoriteStation` y generar la migración de Room.
- `provider/api`: En `MediaItems.kt`, actualizar `radioMediaItem` para que mapee el `artworkUri` a `MediaMetadata` y así se vea en el `PlayerSheet` y `MiniPlayer`.
- `feature/radio`: Actualizar `RadioScreens.kt` para mostrar el `artworkUri` usando `AsyncImage` de Coil en las listas (Búsqueda, Estaciones por país/tag, Favoritos, Curadas) cuando esté disponible, con un fallback al icono por defecto.

## Constraints
- Coil ya está disponible en el proyecto (`-keep class coil.**`).
- Mantener la regla de "metadata honesta": si no hay favicon, queda en nulo y mostramos el fallback.
- Test de base de datos para la migración de Room.

## Authorized scope
Módulos: `core/network`, `core/database`, `provider/api`, `provider/radio`, `feature/radio`.

## Tasks
- [x] **T1** — DTOs and mapping. `StationDto`, provider `AudioItem` mapping, and `radioMediaItem` now carry the RadioBrowser favicon into Media3 metadata.
- [x] **T2** — Favorite persistence. `FavoriteStation` stores `artworkUri` and Room schema migration v1 → v2 preserves it.
- [x] **T3** — Coil UI. Discover, search, station, favorites, curated surfaces, and player metadata use remote artwork with generic fallback.
- [x] **T4** — Final verification. Unit tests, debug build, release v0.0.13, and documentation review completed.

## Completion evidence
- Shipped in release v0.0.13.
- Current documentation and canonical specs are synchronized in `odd/tasks/documentation-sync.md`.
