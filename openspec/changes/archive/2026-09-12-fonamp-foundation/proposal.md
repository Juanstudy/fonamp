# Proposal — Fonamp: media hub personal (visión + slice v1)

## Problem statement
Quien escucha música 24/7, suma radio y podcasts, y descarga música, vive saltando entre 3-4 apps (player local, app de radio, app de podcasts, descargador) sin colección unificada, sin descargas integradas y con anuncios/trackers en casi todas. No existe un hub personal open source que una todo en un solo player liviano.

## Target user
Oyente intensivo (música 24/7 + radio/podcasts ocasionales + descargas) que quiere una sola app, customizable, sin anuncios y sin trackers. Usuario inicial y dueño del producto: el propio autor.

## Product principles (constraints permanentes)
1. **Customizable** — temas, orden de tabs/home, EQ/audio, comportamiento (sleep timer, velocidad).
2. **Expandible** — toda fuente de audio implementa el contrato `Source` (browse/search/tracks/download); agregar una fuente es días, no semanas.
3. **Ligera** — ExoPlayer + Room + Retrofit y nada más. Allowlist de dependencias: todo lo que entra se justifica. Sin SDKs de analytics, sin Firebase, sin ads.
4. **Segura** — permisos mínimos y a pedido, cero trackers, descargas en almacenamiento propio.
5. **Sin anuncios** — nunca un SDK de ads; constraint arquitectónico, no solo promesa.

## Product outcome (visión completa)
Fonamp abre en tu colección (local + descargas + suscripciones), con tabs configurables para Radio y Podcasts, un player único en fondo + notificación, descargas offline integradas (URLs y episodios), y providers externos como fuentes más.

## Slices (cada uno un change SDD propio y shipeable)
- **v1 fundación + local + radio** — estructura multi-módulo, contrato `Source`, local vía MediaStore, descubrir radio por país/género/tag, favoritas, fondo + notificación. (Repite el prototipo, pero bien.)
- **v2 podcasts** — directorio + suscripciones, feed de nuevos, descarga de episodios offline, velocidad de reproducción.
- **v3 descargas** — pegar URL y bajar audio, carpeta observada de archivos, gestión de cola de descargas (WorkManager).
- **v4 providers externos** — Navidrome/Jellyfin/Plex, streaming público, Spotify último por costo/DRM. Cada uno con proposal propia.

## Scope — slice v1 (este change)
1. Scaffold multi-módulo: `app`, `core/player`, `core/network`, `core/database`, `provider/api`, `feature/library`, `feature/radio`, `feature/settings`, design system + tema base.
2. Contrato `Source` + implementaciones `LocalSource` y `RadioBrowserSource`.
3. Local: MediaStore auto, canciones/artistas/álbumes.
4. Radio: países/tags con filtro, estaciones, play en 1 tap, favoritas.
5. Player único Media3 en fondo + notificación; error handling de streams.
6. Settings base: tema claro/oscuro (primera pieza de customizable).

## Non-goals v1 (explícito)
- Sin podcasts, sin descargas de URLs, sin providers externos.
- Customization avanzada (orden de tabs, EQ, sleep timer) → v2+; v1 solo tema.
- Sin Android Auto, sin widgets, sin historial.
- Sin iOS, sin backend propio.

## Success criteria v1
- Abrir → colección local sin configurar; descubrir y reproducir radio en ≤3 taps.
- Suena en fondo con pantalla apagada, controles en notificación.
- Agregar una fuente mock implementando `Source` toma <1 día (prueba de expandibilidad).
- APK debug razonable (<40 MB), cero permisos salvo audio + internet + foreground, `./gradlew test` verde.

## Risks
- Scope creep del "all in one" → defensa: slices con non-goals por fase.
- Descargas de URLs (v3) con zona gris legal/ToS → en v3 se evalúa qué fuentes se soportan.
- Radio Browser inestable → caché + fallback multi-mirror (ya validado en prototipo).
- Referencia: prototipo `~/Projects/cliamp-android` (archivado, 11/11 verify PASS) como fuente de patrones, no de código.
