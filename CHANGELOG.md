# Changelog — fonamp

Formato [Keep a Changelog](https://keepachangelog.com/es-ES/1.0.0/): `Added / Changed / Fixed` por versión. Cada release de GitHub lleva además 3-5 bullets en lenguaje de usuario.

## [Unreleased]

## [0.0.6] — 2026-09-18
### Added
- Pipeline de release firmado con keystore propio y secrets en CI (`release.yml`).
- README completo para GitHub con diseño visual, hero y diagramas de arquitectura.
### Changed
- APK release con R8 y shrink habilitados, bajando a 7.3MB con gate duro `<40MB`.
- Rama `main` protegida con check de CI `build` requerido.

## [0.0.5] — 2026-09-12
### Fixed
- El APK debug bajó de 67.4MB a 35.6MB (−47%): fuera `material-icons-extended`, 6 vectores propios verbatim, cero cambio visual (cierra issue #2).

## [0.0.4] — 2026-09-12
### Added
- Carátulas en la UI con Coil: thumb en mini-player, hero en la sheet, covers en canciones y álbumes, fallback genérico si no hay arte.

## [0.0.3] — 2026-09-12
### Added
- Sección "Curadas" en Descubrir: presets fijos + tiles Lofi/Chill/Ambient con play y favorito idénticos. Omarchy quedó fuera de alcance (no existe en el directorio).

## [0.0.2] — 2026-09-12
### Added
- Pipeline de release por tag (`fonamp-<tag>.apk` debug en GitHub Releases).
- `artworkUri` local expuesto de MediaStore al player (base para carátulas).
- Templates de issues (bug / mejora) y convención de versiones en AGENTS.md.
