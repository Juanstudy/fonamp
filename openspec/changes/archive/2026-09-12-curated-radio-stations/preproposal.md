# Pre-proposal — `curated-radio-stations` (v0.0.3, pendiente de decisión)

> Estado: explore complete (ver `explore.md`). Research NO seleccionado (este runtime no tiene grants de evidencia web/docs; un lane seleccionado fail-closearía a blocked). La verificación por curl de tags (`lofi` vs `lo-fi` vs `chillout`) y de `byname/omarchy` se hace con red local durante apply/verify, no como research lane.

## Contexto confirmado (de explore.md)
- Fuente única: directorio comunitario `radio-browser.info` (mirrors de1/de2/nl1). La app solo usa 5 endpoints (countries, tags, bycountry, bytag, click-count). No hay radios hardcodeadas.
- El usuario "solo ve radios mundiales" porque la UI solo expone el índice countries+tags; lo temático está enterrado en el tab de tags sin curaduría.
- `lofi` casi seguro existe como tag; `omarchy` (distro Linux) casi seguro NO existe en el directorio → solo viable como preset manual con URL aportada, o fuera de alcance con evidencia.
- Recomendación explore: A (presets hardcodeados) + B (tiles por tag precableados). C (seed de favoritas) descartada por romper semántica de Req 4.

## Decisiones pendientes (bloquean proposal)
1. **Alcance**: ¿A+B, solo A (presets), o solo B (tags)?
2. **Omarchy**: ¿el usuario aporta URL manual del stream, o se declara fuera de alcance con evidencia del curl?
3. **Tags exactos** (lofi/lo-fi/chillout/ambient): default = lo decide el curl en apply salvo que el usuario fije lista ahora.

## Reglas duras heredadas
Player intacto, sin permisos/SDKs nuevos, release debug, gate 40MB monitoreado, sin name-search salvo que omarchy lo exija (cambio de scope), bump 0.0.3 en commit aparte según AGENTS.md.

## Decisiones confirmadas por el usuario
- Alcance: A+B (presets + tiles por tag).
- Omarchy: verificar primero (curl en apply; si no existe, preset manual o fuera con evidencia).
- Estado: listo para proposal.
