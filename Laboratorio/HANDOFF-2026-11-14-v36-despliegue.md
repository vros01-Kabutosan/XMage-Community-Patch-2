# HANDOFF 2026-11-14 — despliegue v36 (import .dck UTF-8) en la instalación oficial

**Tarea:** implementar en `J:\MTG\Instalacion-XMage` el fix que faltaba (las cartas que el
cliente no veía al importar los mazos descargados) y dejar el punto exacto para el siguiente worker.
**Estado:** DESPLIEGUE DEL CORE OK Y VERIFICADO · **PENDIENTE:** reconstruir la base de cartas del
cliente oficial (requiere cerrar el cliente, confirmado por Kabutosan).
**Worker anterior:** Perch (fix v36) → Hermes (despliegue + handoff) · **Fecha:** 2026-11-14

## Contexto mínimo

Los `.dck` descargados son UTF-8; `PlainTextDeckImporter` los leía con el charset por defecto de
la JVM (`windows-1252` en Windows), así que todo nombre con acento llegaba corrupto y la carta no
resolvía. Perch corrigió eso (commit `e642db13bd6`, 2 ficheros, +5/−2) y lo verificó en el
laboratorio, pero **no lo desplegó** a la instalación oficial. Kabutosan lo pidió el 2026-11-14.

## Hecho y verificado (con evidencia)

| Paso | Resultado | Evidencia |
|---|---|---|
| Fix en fuente | commit `e642db13bd6` en `upgrade/official-1.4.63V1` | `git -C XMage-1.4.62V1-src show --stat e642db13bd6` |
| Build | `Mage/target/mage-1.4.63.jar` SHA256 `2975b2af5116df09e18db3b036dbec15447882cb5faf0e43c2da191c38042d6b` | `sha256sum` |
| Test lab (core v36 + base lab) | `main=60 side=15` | `$SCRATCH/lab2.out` (ImportTest, Amulet Titan) |
| Test instalación **antes** (core v35) | `main=0 side=0`, `MjAlnir` | `$SCRATCH/inst2.out` |
| Backup | `Instalacion-XMage\xmage\backup-jars-1.4.63-v36-20261114\cliente\mage-1.4.63.jar` (`f5d9cd7d…106eb9`) | `sha256sum` |
| Despliegue | copiado a `Instalacion-XMage\xmage\mage-client\lib\mage-1.4.63.jar` → SHA `2975b2af…` | `sha256sum` |
| Test instalación **después** (jar desplegado + base compatible) | `main=60 side=15` | `$SCRATCH/inst5.out` |
| Servidor 17171 | intacto: PID 25548, jar `mage-1.4.63.jar` SHA `f5d9cd7d…` (sin tocar) | `Get-NetTCPConnection` + `sha256sum` |

## Punto exacto de continuación

1. **Confirmar con Kabutosan si cierra el cliente oficial** (PID 19660, `javaw` launcher PID 24356).
2. Con el cliente cerrado, forzar la reconstrucción de la base de cartas:
   ```bash
   cd "J:/MTG/Instalacion-XMage/xmage/mage-client"
   mkdir -p "db/backup-20261114"
   mv db/cards.h2.mv.db db/backup-20261114/   # la base activa está vacía (135 KB)
   # rm -f db/cards.h2.lock.db                # si queda lock huérfano
   ```
3. Arrancar el cliente oficial (`startClient.bat`) y dejar que complete el escaneo
   (≈591 sets / 94787 cards; tarda unos minutos). Comprobar que `db/cards.h2.mv.db` crece a cientos de MB.
4. Verificar en la GUI: Deck Editor → LOAD → `sample-decks\Descargados`, abrir un mazo con acentos
   (p. ej. `2026-11-09_MTGTop8_09_Amulet_Titan_910595_0c07753fb2.dck`) y ver las 60 + 15.

## Pendiente (por este orden)

- [ ] Reconstrucción de la base del cliente oficial → se da por buena si `db/cards.h2.mv.db` pesa
      cientos de MB y el mazo Amulet Titan muestra `main=60 side=15` en la GUI.
- [ ] Regenerar los `.dck` de `Descargados` que contengan `MjAlnir` residual (1 fichero) para limpiar
      el nombre corrompido en disco.
- [ ] (Opcional, requiere decisión) habilitar las 5 cartas de `SecretsOfStrixhaven` que están en la
      lista `unfinished` (`Decorum Dissertation`, `Echocasting Symposium`, `Germination Practicum`,
      `Improvisation Capstone`, `Restoration Seminar`): están implementadas, solo excluidas.
      Implica rebuild de `mage-sets` + despliegue de `mage-sets-1.4.63.jar`. No tocar sin confirmación.
- [ ] (Opcional) `PlainTextDeckImporter.saveFixedDeckFile` escribe con el charset por defecto; si se
      quiere escribir en UTF-8, usar `PrintWriter(out, "UTF-8")`. No es el bug actual.

## Riesgos / prohibido

- **No tocar el servidor oficial 17171** (PID 25548) ni su base de datos.
- No cerrar el cliente oficial sin confirmación de Kabutosan (puede estar en partida).
- No borrar `HKCU\Software\JavaSoft\Prefs\HKEY_CURRENT_USER`.

## Rollback

- Jar: `cp "J:/MTG/Instalacion-XMage/xmage/backup-jars-1.4.63-v36-20261114/cliente/mage-1.4.63.jar" "J:/MTG/Instalacion-XMage/xmage/mage-client/lib/mage-1.4.63.jar"`
  (con el cliente cerrado) → SHA esperado `f5d9cd7d…106eb9`.
- Base de cartas: la base poblada de respaldo está en `mage-client\lib\db\cards.h2.mv.db` (307 MB, ago/2026).
  Si la reconstrucción falla, no hay que copiarla a mano sin verificar versión (el build la recrea).

## Artefactos

- Parche: `Laboratorio\parches\2026-11-14-import-dck-utf8-v36.patch` (2 ficheros, +5/−2).
- Jar desplegado: `Instalacion-XMage\xmage\mage-client\lib\mage-1.4.63.jar` — SHA256 `2975b2af…42d6b`.
- Evidencia de tests: `J:\RelocatedFromC\Users\vros0\AppData\Local\hermes\cache\scratch\importcheck\{lab2,inst2,inst5}.out`.

## Prompt listo para el siguiente worker

> Retoma el handoff `J:\MTG\Laboratorio\HANDOFF-2026-11-14-v36-despliegue.md` del proyecto XMage.
> Contexto: el fix v36 (import de `.dck` en UTF-8) está commiteado en el repo fuente
> (`XMage-1.4.62V1-src`, rama `upgrade/official-1.4.63V1`, commit `e642db13bd6`) y el jar core ya
> está desplegado y verificado en `J:\MTG\Instalacion-XMage\xmage\mage-client\lib\mage-1.4.63.jar`
> (SHA256 `2975b2af5116df09e18db3b036dbec15447882cb5faf0e43c2da191c38042d6b`), con backup en
> `backup-jars-1.4.63-v36-20261114\cliente\`. El servidor oficial 17171 no se toca. Falta, en este orden:
> (1) confirmar con Kabutosan antes de cerrar el cliente oficial; (2) con el cliente cerrado, mover
> `mage-client\db\cards.h2.mv.db` (135 KB, vacía) a `db\backup-20261114\` y arrancar el cliente para que
> reconstruya la base (~94787 cartas); (3) comprobar en la GUI que el mazo Amulet Titan carga 60/15.
> Reglas: responde en español, no toques el servidor 17171 ni su base, no cierres el cliente oficial
> sin confirmación, y deja la evidencia (logs/SHA) en `Laboratorio\capturas` + un CHECKPOINT y un
> LEEME nuevos antes de dar por terminado.
