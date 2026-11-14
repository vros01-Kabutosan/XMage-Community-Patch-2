# HANDOFF 2026-11-14 — v36 (UTF-8) + base + v37 (SOS) + v38 (Jace's Machinations)

**Tarea:** implementar en `J:\MTG\Instalacion-XMage` lo que faltaba (cartas que el cliente no veía
al importar los mazos descargados, 60/15) y dejar el punto exacto para el siguiente worker.
**Estado:** v36+v37 DESPLEGADOS; **v38 desplegado y verificado en laboratorio, PENDIENTE reconstruir
la base del cliente** para que la carta nueva se vea (la confirmación expiró; el cliente no se tocó).
**Workers:** Perch (fix v36) → Hermes (despliegue, base, v37, v38) · **Fecha:** 2026-11-14

## Contexto mínimo

Los `.dck` son UTF-8; el importador los leía en CP1252 (charset por defecto) y el normalizador
mapeaba mal el mojibake `Ã¶`. Perch lo corrigió (commit `e642db13bd6`) sin desplegarlo. La base de
cartas activa del cliente oficial estaba vacía (135 KB). 5 cartas de SOS estaban implementadas pero
excluidas como `unfinished`. Y un mazo UW Control (907434) daba 57/15 por las 3 copias de
`Jace's Machinations` (FRA 32), no implementada; Kabutosan pidió implementarla.

## Hecho y verificado

| Paso | Artefacto / resultado |
|---|---|
| Core v36 | `mage-1.4.63.jar` SHA `2975b2af…42d6b`; backup v36 (`f5d9cd7d…`) |
| Sets v37 | `mage-sets-1.4.63.jar` SHA `342f1371…48fb`; backup v37 (`481114db…`) |
| Base oficial reconstruida | 135 KB → **318 MB**, 94797 cartas |
| **v38 core** | `mage-1.4.63.jar` SHA `df3ee872…fb0f`; backup v38 (`2975b2af…`) |
| **v38 sets** | `mage-sets-1.4.63.jar` SHA `01bdcb3f…b8ef8`; backup v38 (`342f1371…`) |
| Tests JUnit v38 | `tests="2" errors="0"` (activación en turno rival + Empower 8) |
| Lab v38 | UW Control **60/15**; BulkTest **2667/2811** (antes 2648); no encontradas 24 → 23 |
| Servidor 17171 | **intacto** (core `f5d9cd7d…`, sets `481114db…`) |

Evidencia: `LAB-UI-1.4.62-V2\capturas\20261114-v37-sos\` y `...\20261114-v38-jaces\`.
Commits: `e642db13bd6` (v36), `dc5abcb7c2f` (v37), `e418d5e4fcf` (v38), rama
`upgrade/official-1.4.63V1` (pusheada). Documentación: `LEEME.md` puntos 26-28,
`CHECKPOINT-2026-11-14-despliegue-v36.md`, `...-v37.md`, `...-v38-jaces-machinations.md`. Parches en
`parches\`.

## Punto exacto de continuación

1. **Pendiente inmediato (requiere confirmación de Kabutosan):** reconstruir la base del cliente
   para que `Jace's Machinations` se vea. Con el cliente cerrado:
   ```bash
   cd "J:/MTG/Instalacion-XMage/xmage/mage-client"
   mkdir -p db/backup-v38-20261114
   mv db/cards.h2.mv.db db/cards.h2.lock.db db/cards.h2.trace.db db/backup-v38-20261114/
   # arrancar el cliente; reconstruye solo y el log muestra "need to add N new cards"
   ```
2. Comprobar que el mazo UW Control carga 60/15 y que `Jace's Machinations` aparece en el editor.
3. (Opcional) Regenerar los `.dck` con `MjAlnir` residual.
4. (Opcional) Las 23 cartas que siguen sin existir (TRK/HOC/MSH y el resto de FRA) son cobertura del upstream.

## Riesgos / prohibido

- **No tocar el servidor oficial 17171** ni su base de datos.
- No borrar `HKCU\Software\JavaSoft\Prefs\HKEY_CURRENT_USER`.
- No desplegar subsets de jars: core y sets se despliegan juntos y del mismo build.

## Rollback

- v38: `cp backup-jars-1.4.63-v38-20261114\cliente\mage-1.4.63.jar mage-client\lib\` (`2975b2af…`)
  y `cp backup-jars-1.4.63-v38-20261114\cliente\mage-sets-1.4.63.jar mage-client\lib\` (`342f1371…`).
- v36/v37: sus propias carpetas `backup-jars-1.4.63-v36-...` y `...-v37-...`.
- Base: `db\backup-20261114\` (vacía previa) y `db\backup-v38-20261114\` (si se reconstruye).

## Prompt listo para el siguiente worker

> Retoma el handoff `J:\MTG\Laboratorio\HANDOFF-2026-11-14-v36-despliegue.md` del proyecto XMage.
> Estado: v36 (UTF-8) y v37 (5 lecciones SOS) desplegados; **v38 (Jace's Machinations, FRA 32)
> desplegado y verificado en laboratorio, pendiente reconstruir la base del cliente oficial**.
> Artefactos v38: `mage-client\lib\mage-1.4.63.jar` SHA256
> `df3ee87255c7446ff7afea8ec4f082670b8cff92ada368c24859dfbdf143fb0f` y `mage-sets-1.4.63.jar` SHA256
> `01bdcb3f6aba07f9621b76b1ff42ed9a81fd9b20ca084b83bad132f71afb8ef8`; backup en
> `backup-jars-1.4.63-v38-20261114\cliente\`. El servidor 17171 no se toca. Primer paso: pedir
> confirmación, cerrar el cliente, mover `db\cards.h2.mv.db` a `db\backup-v38-20261114\` y rearrancar
> para reconstruir la base; luego comprobar que el mazo UW Control carga 60/15. Reglas: responde en
> español, no toques el servidor 17171 ni su base, y deja parche + CHECKPOINT + LEEME + handoff.
