# ODD — Podcasts

## Objective
Implementar soporte para Podcasts en fonamp, permitiendo a los usuarios buscar directorios, suscribirse a canales y reproducir episodios directamente desde feeds RSS.

## Problem
Los usuarios necesitan una aplicación aparte para escuchar podcasts porque fonamp actualmente solo soporta música local y radio de internet. La arquitectura actual permite agregar nuevas fuentes sin modificar el reproductor, por lo que es una oportunidad ideal para integrar podcasts de forma nativa y privada.

## Why
Mantenerse alineado con la visión de un "hub de audio" único y privado. Al consumir RSS directamente, evitamos depender de cuentas de terceros o intermediarios que puedan inyectar tracking.

## Scope
- `provider/api`: Expandir `SourceKind` con `PODCAST`.
- `provider/podcast`: Implementar `PodcastSource` usando la API de iTunes Search o PodcastIndex para la búsqueda, y un parser XML para los feeds RSS.
- `core/database`: Entidad `PodcastSubscription` y migración v2 -> v3 de Room.
- `feature/podcast`: UI de descubrimiento, detalles del podcast con lista de episodios, y vista de suscripciones.
- `app`: Inyectar dependencias y agregar la tab "Podcasts" a la navegación base.

## Constraints
- Todo el parsing y fetching se hace on-device. No hay backend propio.
- Usar el reproductor (Media3) existente sin modificarlo; el provider debe mapear a `MediaItem` correctamente.

## Tasks
- [x] **POD-1** — Módulos: Crear `feature/podcast` y `provider/podcast` configurados en settings.gradle.kts.
- [x] **POD-2** — Base de Datos y API: Agregar enum `PODCAST`, entidad `PodcastSubscription`, DAO y migración de Room.
- [x] **POD-3** — Provider: Implementar `PodcastSource` (API client, XML RSS parser y mapeo a `AudioItem`).
- [x] **POD-4** — UI: Construir pantallas en Jetpack Compose para `feature/podcast`.
- [x] **POD-5** — Integración: Conectar Hilt, Bottom Navigation y testear flujo completo.