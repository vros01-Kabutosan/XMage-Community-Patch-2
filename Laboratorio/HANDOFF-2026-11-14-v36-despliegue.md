# HANDOFF 2026-11-14 — v36 (UTF-8) + base del cliente + v37 (5 lecciones SOS)

**Tarea:** implementar en `J:\MTG\Instalacion-XMage` lo que faltaba (cartas que el cliente no veía
al importar los mazos descargados, 60/15) y dejar el punto exacto para el siguiente worker.
**Estado:** DESPLIEGUE Y VERIFICADO (core v36 + sets v37 + base reconstruida). Solo quedan las 24
cartas que no existen en XMage (ajenas al importador).
**Workers:** Perch (fix v36) → Hermes (despliegue, base, v37) · **Fecha:** 2026-11-14

## Contexto mínimo

Los `.dck` son UTF-8; el importador los leía en CP1252 (charset por defecto) y el normalizador
mapeaba mal el mojibake `Ã¶`. Perch lo corrigió (commit `e642db13bd6`) sin desplegarlo. Además, la
base de cartas activa del cliente oficial estaba vacía (135 KB). Y 5 cartas de SOS estaban
implementadas pero excluidas por la lista `unfinished`.

## Hecho y verificado (evidencia en `LAB-UI-1.4.62-V2\capturas\20261114-v37-sos\`)

| Paso | Artefacto / resultado |
|---|---|
| Core v36 desplegado | `mage-client\lib\mage-1.4.63.jar` SHA `2975b2af…42d6b`; backup `backup-jars-1.4.63-v36-20261114\cliente\` (`f5d9cd7d…106eb9`) |
| ImportTest antes (v35) | `main=0 side=0`, `MjAlnir` |
| ImportTest después (v36) | Amulet Titan `main=60 side=15` |
| Base oficial reconstruida | `db\cards.h2.mv.db` 135 KB → **318 MB**, log `need to add 94797 new cards`, `Connect: DONE` |
| Sets v37 desplegado | `mage-client\lib\mage-sets-1.4.63.jar` SHA `342f1371…48fb`; backup `backup-jars-1.4.63-v37-20261114\cliente\` (`481114db…46b7`) |
| BulkTest lab v37 (2811 mazos) | **2648 completos 60+15** (antes 2519, +129); nombres no encontrados 28 → **24** |
| Instalación oficial post | `sos5b main=8`; Amulet Titan `main=60 side=15` |
| Servidor 17171 | **intacto** (PID 25548; core `f5d9cd7d…`, sets `481114db…`) |

Commits: `e642db13bd6` (v36, Perch) y `dc5abcb7c2f` (v37), rama `upgrade/official-1.4.63V1`,
pusheados a `origin`. Documentación: `LEEME.md` puntos 26-27, `CHECKPOINT-2026-11-14-despliegue-v36.md`
y `CHECKPOINT-2026-11-14-despliegue-v37.md`. Parches: `parches\2026-11-14-import-dck-utf8-v36.patch` y
`parches\2026-11-14-habilitar-sos-v37.patch`.

## Punto exacto de continuación

No hay trabajo obligatorio pendiente. Lo que queda es opcional y está documentado:

1. (Opcional) Regenerar los `.dck` de `sample-decks\Descargados` que contengan `MjAlnir` (1 fichero)
   para limpiar el nombre corrupto en disco.
2. (Opcional, requiere decisión) Las 24 cartas restantes no existen en XMage (`RealityFracture`,
   `StarTrek`, `TheHobbitEternal`, `MarvelSuperHeroes`, `Salt Vampire`): habría que implementarlas
   o esperar al upstream.
3. (Aviso) El servidor oficial no tiene el sets v36/v37: si se sube un mazo con las 5 lecciones o con
   nombres con acento a un torneo en 17171, el servidor puede no conocerlas. El servidor no se toca
   por norma del proyecto.

## Riesgos / prohibido

- **No tocar el servidor oficial 17171** ni su base de datos.
- No borrar `HKCU\Software\JavaSoft\Prefs\HKEY_CURRENT_USER`.
- No desplegar subsets de jars: core y sets se despliegan juntos y del mismo build.

## Rollback

- Core: `cp backup-jars-1.4.63-v36-20261114\cliente\mage-1.4.63.jar mage-client\lib\` → `f5d9cd7d…`.
- Sets: `cp backup-jars-1.4.63-v37-20261114\cliente\mage-sets-1.4.63.jar mage-client\lib\` → `481114db…`.
- Base: `db\backup-20261114\` guarda la base vacía previa; el cliente la reconstruye solo.

## Prompt listo para el siguiente worker

> Retoma el handoff `J:\MTG\Laboratorio\HANDOFF-2026-11-14-v36-despliegue.md` del proyecto XMage.
> Estado: el fix v36 (import `.dck` UTF-8) y el sets v37 (5 lecciones de SecretsOfStrixhaven) están
> desplegados y verificados en `J:\MTG\Instalacion-XMage`, y la base de cartas del cliente se
> reconstruyó (318 MB, 94797 cartas). Artefactos: `mage-client\lib\mage-1.4.63.jar` SHA256
> `2975b2af5116df09e18db3b036dbec15447882cb5faf0e43c2da191c38042d6b` y `mage-sets-1.4.63.jar` SHA256
> `342f137130d16608d5c5e1b9bbca03ef80b7197cb4dda0d07c752c48266148fb`; backups en
> `backup-jars-1.4.63-v36-20261114\cliente\` y `backup-jars-1.4.63-v37-20261114\cliente\`. El servidor
> oficial 17171 no se toca. No queda trabajo obligatorio: solo opcional (regenerar el `.dck` con
> `MjAlnir`, o implementar las 24 cartas que faltan). Reglas: responde en español, no toques el
> servidor 17171 ni su base, y si cambias algo deja parche + CHECKPOINT + LEEME + handoff nuevos.
