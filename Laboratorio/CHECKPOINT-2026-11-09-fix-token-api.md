# CHECKPOINT 2026-11-09 — FIX bug de partida: NoSuchMethodError en Token.putOntoBattlefield

**Petición del usuario (Kabutosan):** arreglar el error "SERVER - game error" que le tira la
partida; autorización explícita para trabajar directamente en la instalación oficial
(`J:\MTG\Instalacion-XMage`) porque es donde juega.

## Síntoma

- Partida Two Player Duel vs Computer 2 (bot) el 09/11/2026, inicio 20:42, crash 20:46:32
  (turno 7 del bot). Cliente 1.4.63-V1 build 2026-11-02 10:12, servidor 1.4.63-V1 build
  2026-10-31 13:52 (arrancado hoy 19:54 por XMageLauncher).
- Diálogo "Error type: SERVER - game error" y partida cerrada con "technical winner:
  Kabutosan (decided by life)".
- Evidencia: `LAB-UI-1.4.62-V2/capturas/20261109-fix-token-api/`
  (`gamelog-error-204632.html`, `captura-error-usuario-2046.png`).

```
game thread error: java.lang.NoSuchMethodError:
mage.game.permanent.token.Token.putOntoBattlefield(
  ILmage/game/Game;Lmage/abilities/Ability;Ljava/util/UUID;ZZ
  Ljava/util/UUID;Ljava/util/UUID;ZLjava/util/List;)Z
```

El fallo saltó al crear el token Construct de **Urza's Saga** (capítulo I) en el
servidor: el efecto que ejecutaba la creación llamaba a una firma de `Token` que el
core desplegado no tiene.

## Diagnóstico (cadena completa)

1. El gamelog HTML del cliente (`gamelogs\20261109_204210_*.html`) da el error exacto.
2. `javap` sobre `Token.class` del core desplegado (`mage-server\lib\mage-1.4.63.jar`,
   build 01/11 01:25): la interfaz declara la API **nueva** del upgrade 1.4.63
   (`Set<UUID> attackedDefenders`, commit upstream 79aec614fc2).
3. Escáner de constant-pool (`scan_putob.py`, en capturas) sobre todos los jars
   desplegados: el **propio core** contiene 4 clases compiladas con la API **vieja**
   (pre-upgrade, `UUID attackedPlayer, UUID attachedTo`):
   - `mage/abilities/effects/common/CreateTokenEffect.class` ← el más usado del motor
   - `mage/abilities/keyword/EmbalmEffect.class`
   - `mage/abilities/keyword/EternalizeEffect.class`
   - `mage/constants/RoleType.class`
   Cualquier carta que pase por ellos (tokens con CreateTokenEffect, embalm, eternalize,
   roles) tumbaba la partida. El jar de cartas (`mage-sets`) era correcto (144 refs, todas
   API nueva).
4. Causa raíz A — **jars mixtos por build incremental**: `mvn package` **sin `clean`**
   recicló `target/classes` de la migración del 31/10 (código pre-upgrade) porque el delta
   oficial dejó mtimes desalineados; el compilador incremental no recompiló esas clases y
   el despliegue del 01/11 (v22 h2-lock + v23 serialver) las arrastró al jar del core.
5. Causa raíz B — **hueco de source**: `Mage.Server` no compila desde source
   (`package mage.db does not exist` en `FeedbackServiceImpl`). Upstream tiene
   `mage.db` en **Mage.Common** (`EntityManager` + `model/Feedback` + `model/Log`),
   pero el delta del upgrade no lo trajo y la regla global `**/db` del `.gitignore`
   (pensada para carpetas de base de datos) se comía el paquete fuente `mage/db`,
   así que nunca pudo comprometerse al repo. Los jars antiguos solo "funcionaban"
   con las clases mage/db recicladas de `target/classes`.

## Fix aplicado

1. **Source** (rama `upgrade/official-1.4.63V1`, commit **`2b1a7a5c59e` (v31)**, push OK):
   - Restaurados 4 ficheros verbatim del commit upstream `79aec614fc2`:
     `Mage.Common/src/main/java/mage/db/{EntityManager,EntityManagerTest}.java` y
     `mage/db/model/{Feedback,Log}.java`.
   - `.gitignore`: negación específica `!Mage.Common/src/main/java/mage/db/` +
     `!Mage.Common/src/main/java/mage/db/**` (patrón igual al existente
     `!Mage/src/main/java/mage/target`). Aditivo, no se toca código existente.
2. **Build limpio completo**: `mvn -B clean package -DskipTests -pl <36 módulos
   desplegables> -am` desde `XMage-1.4.62V1-src` (logs `_build-clean-20261109.log` y
   `_build-fase2-20261109.log`, copiados en capturas). Todos los módulos BUILD SUCCESS.
3. **Smoke de laboratorio** (protocolo): `montar-lab.sh` con los jars nuevos → servidor
   de laboratorio en 17191: "Loaded game types: 17, tourneys: 21, players: 3, cubes: 23,
   decks: 52", `Started MAGE server`, **0 errores** en log (`servidor-lab-smoke.log`).
4. **Despliegue en la instalación oficial** (autorizado por el usuario):
   - Backup previo (norma 4): `Instalacion-XMage\xmage\backup-jars-1.4.63-20261109_token_api_fix\`
     (31 jars: 5 de `mage-server\lib` + 26 plugins `mage-*-1.4.63.jar`).
   - Parado el servidor oficial (sin respawn del launcher), copiados **5 lib + 27 plugins**
     (cobertura de nombres 1:1, sin huérfanos), rearrancado con la misma línea de comandos
     que usaba el launcher (`-Xms1g -Xmx2g -Djava.net.preferIPv4Stack=true -cp lib\*
     mage.server.Main`, cwd `mage-server`, salida en `server-console-20261109.log`).
   - Verificación post-despliegue: escáner sobre lo desplegado = **0 referencias a la API
     vieja** (`scan-jars-desplegados-POST.txt`); arranque limpio en 17171 (PID 27044,
     21:32:48), 17 game types, log sin errores.

## Lecciones (normas de build a partir de hoy)

- **NUNCA desplegar jars de un build incremental**: para desplegar siempre
  `mvn clean package` (el comando histórico sin `clean` es la causa de los jars mixtos).
- **NUNCA desplegar un subset de jars mage** (core/servidor/cartas/plugins van siempre
  juntos y del mismo build): script nuevo `Laboratorio\desplegar-oficial.ps1`
  (exige servidor parado, backup automático, copia el set completo).
- El `.gitignore` del fork ignora cualquier carpeta `**/db`; vigilad los paquetes
  fuente que caigan en carpetas `db` (hoy `mage/db`; el patrón de negación queda puesto).

## Rollback

- Parar servidor, copiar de vuelta el contenido de
  `backup-jars-1.4.63-20261109_token_api_fix\` a `mage-server\lib` y `mage-server\plugins`,
  rearrancar. (El fix de source y el commit del fork no se tocan.)

## Pendiente

- ~~**Cliente**: refrescar `mage-client\lib`~~ **HECHO el mismo día 21:45** (con autorización
  del usuario): cliente cerrado (CloseMainWindow no bastó, force kill), backup de sus 4 jars
  en `backup-jars-1.4.63-20261109_token_api_fix\cliente\`, copiados
  `mage-1.4.63.jar` + `mage-common-1.4.63.jar` + `mage-sets-1.4.63.jar` +
  `mage-deck-constructed-1.4.63.jar` del build nuevo (se conserva `mage-client-1.4.63.jar`
  v30 del 02/11 con los mods UI). Cliente relanzado con la línea exacta del launcher
  (PID 37184, log `client-console-20261109.log`): actualizó su base de datos de cartas
  (591 sets nuevos, única vez) y el fix v22 de locks H2 **auto-curó** el lock que dejó el
  cierre forzado; auto-reconexión y login "Kabutosan" OK. Cliente y servidor quedan en el
  mismo build limpio del 09/11.

- **Cliente**: `mage-client\lib` conserva `mage-1.4.63.jar` + `mage-sets-1.4.63.jar` +
  `mage-common-1.4.63.jar` del 01/11-31/10 (los mismos jars mixtos). En el cliente es
  **latente e inofensivo** (el cliente no ejecuta lógica de juego ni crea tokens), pero
  conviene refrescar esos 3 jars con los del build nuevo la próxima vez que el cliente
  esté cerrado (los ficheros están bloqueados con el cliente abierto).
- Aterrizar el commit de docs en `main` del repo raíz (ruleset "XMAGE MAIN PROTECTED"
  exige relajación manual; evidencia empujada en rama `sync/fix-token-api-20261109`).
- El usuario debe re-jugar la partida con Urza's Saga para confirmar en su mesa.
