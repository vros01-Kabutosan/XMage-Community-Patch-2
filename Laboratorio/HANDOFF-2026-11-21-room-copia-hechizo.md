# HANDOFF 2026-11-21 — room-copia-hechizo

**Tarea:** que una **copia de un hechizo Room** abra su puerta al resolver (hoy no la abre).
**Estado:** BLOQUEADO (espera decisión de Kabutosan: arreglar ahora o mantener paridad con v38)
**Worker anterior:** Qoder Desktop (verificación de la migración 1.4.63V2)   **Fecha:** 2026-11-21 22:45

## Contexto mínimo

Al verificar la migración V1→V2 apareció `RoomCardTest.testCopyOnStack` en rojo. Resultó **no ser una
regresión de la migración**: el mismo test falla igual en v38. Es un defecto que el fork arrastra desde
su mod de Rooms (v3.4), conviviendo con un test rojo en la rama. Es un arreglo **de mejora**, no de
migración, y toca el motor, por eso se saca del alcance y se documenta aquí.

## Hecho y verificado (con evidencia)

1. **Paridad V1 = V2.** `git worktree` limpio en `J:\MTG\Laboratorio\_baseline-v1` (rama
   `upgrade/official-1.4.63V1`, commit `e418d5e4fc`): `RoomCardTest` = **20 tests / 1 fallo
   (`testCopyOnStack`)** → log `J:\MTG\_v1-roomcardtest.log`. La rama V2, con el código v38 intacto,
   idéntico. `RoomCard.java`, `PermanentCard.java`, `PermanentToken.java` y `RoomCardTest.java` son
   **byte a byte iguales** entre `upgrade/official-1.4.63V1` y `upgrade/official-1.4.63V2`
   (`git diff --ignore-cr-at-eol --name-only upgrade/official-1.4.63V1 upgrade/official-1.4.63V2 -- <ficheros>` = vacío).
2. **Causa raíz probada.** Toda copia de un hechizo de permanente entra como **token**, no como carta:
   pila capturada con `Thread.dumpStack()` en `PermanentToken.<init>` →
   `mage.game.stack.Spell.resolve(Spell.java:426)` (rama `if (isCopy())` de `Spell.java:418`) →
   `CopyTokenFunction.createTokenCopy(card, game, spell)` → `EmptyToken` →
   `TokenImpl.putOntoBattlefieldHelper(TokenImpl.java:322)` → `new PermanentToken(...)`.
   El log de partida lo dice: `Bottomless Pool // Locker Room [e2b] enters the battlefield as a token`.
   El **único** escritor de `roomCastHalf` es el mod del fork en `PermanentCard.java:104-109`
   (`if (card instanceof RoomCard && game.getState().getZone(card.getId()) == Zone.STACK)`
   → `setRoomCastHalf(...)`), y un `PermanentToken` **no pasa por ahí**.
   `RoomEnterUnlockEffect.apply` (`RoomCard.java:138-141`) lee `null` y sale por `return true`
   sin abrir puerta → no dispara el "return up to one target creature" → el test se queda con el
   segundo `addTarget("Ornithopter")` sin consumir.
3. **Arreglo descartado (revertido).** Volver a leer el blueprint como reserva
   (`RoomCharacteristicsEffect.findRoomCard(permanent).getLastCastHalf()`): arregla
   `testCopyOnStack` pero sube a **20 tests / 5 fallos** (log `J:\MTG\_v2-roomcardtest-current.log`):
   `testCopyOnBattlefield`, `testNameMatchOnFieldFromHalf`, `testNameMatchOnFieldFromLocked`,
   `testNameMatchOnFieldFromUnlocked`, `testSakashimaCopiesRoomCard`. Motivo: `ZonesHandler.java:204-223`
   **no limpia** `lastCastHalf` en el salto STACK→BATTLEFIELD, así que el blueprint conserva el valor y
   cualquier permanente secundario / token / copia de Sakashima con la misma habilidad de ETB lo lee
   como propio. WIP guardado en `J:\MTG\_wip-patches\room-fallback-wip.patch` (no aplicar tal cual).
4. **El dato correcto sí existe, y es por-objeto.** Trazas del árbol de trabajo:
   `tokenCtor from=EmptyToken tokenName=Bottomless Pool // Locker Room
   copySource=BottomlessPoolLockerRoom/8382d9ba zone=OUTSIDE lastCastHalf=LeftSplit`
   → `CopyTokenFunction.java:153` (`target.setCopySourceCard(sourceObj)`) deja en el token la **propia
   instancia copia** del `RoomCard`, y `RoomCard.java:42-45` (`RoomCard(RoomCard card)`) copia
   `lastCastHalf`. Ojo: su zona ya es `OUTSIDE`, así que **no sirve** una guarda `zone == STACK`.

## Punto exacto de continuación

Transporte explícito del medio por el token (4 ficheros, ~15 líneas):

1. `Mage/src/main/java/mage/game/permanent/token/TokenImpl.java`: campo nuevo
   `private SpellAbilityType roomCastHalf;` + getter/setter, y copiarlo en el constructor de copia
   (`protected TokenImpl(final TokenImpl other)`), que es lo que usa `EmptyToken.copy()`.
2. `Mage/src/main/java/mage/game/stack/Spell.java:418-426` (rama `isCopy()` del `resolve`), justo tras
   `Token token = CopyTokenFunction.createTokenCopy(card, game, this);`:
   ```java
   if (card.getMainCard() instanceof RoomCard && token instanceof TokenImpl) {
       ((TokenImpl) token).setRoomCastHalf(((RoomCard) card.getMainCard()).getLastCastHalf());
   }
   ```
   Revisar si la rama hermana de `Spell.java:348-356` necesita el mismo trato.
3. `Mage/src/main/java/mage/game/permanent/PermanentToken.java:28-33` (ctor `PermanentToken(Token, UUID, Game)`),
   tras `this.token = token.copy();`:
   ```java
   if (this.token instanceof TokenImpl && ((TokenImpl) this.token).getRoomCastHalf() != null) {
       setRoomCastHalf(((TokenImpl) this.token).getRoomCastHalf());
   }
   ```
4. Ejecutar la comparación: antes `20/1`, objetivo `20/0` **sin romper nada** del listado del punto 3.

## Pendiente (por este orden)

- [ ] Decisión de Kabutosan: ¿arreglar ahora (fuera del alcance de la migración) o abrir LAB dedicado?
      → se da por buena con un «arregla» o «deja en pendiente» explícito.
- [ ] Si se arregla: aplicar los 3 pasos de arriba en `J:\MTG\Laboratorio\XMage-1.4.62V1-src`
      → bueno si `mvn -B -pl Mage.Tests -am test -Dtest='RoomCardTest' -Dsurefire.failIfNoSpecifiedTests=false`
      da `Tests run: 20, Failures: 0`.
- [ ] Smoke de laboratorio en `17191` partiendo un `Bottomless Pool // Locker Room` con `See Double`
      (capturas en `LAB-UI-1.4.62-V2\capturas\`).

## Riesgos / prohibido

- `TokenImpl` y `Spell` son motor puro: un fallo aquí afecta a **todas** las copias de hechizos de
  permanente (Fetchland-style, Clever Impersonator, Sakashima, Strionic…). No basta con el test de Room:
  pasar también `org.mage.test.AI.basic.CopyAITest`, `testCopyOnBattlefield` y la batería de split cards.
- **No** reintroducir la lectura del blueprint sin limpiar el estado compartido antes (ver punto 3).
- No tocar el servidor oficial (puerto `17171`) ni su base de datos. Todo pasa primero por el lab `17191`.

## Rollback

El árbol está **limpio** en esta área: `git checkout -- Mage/src/main/java/mage/cards/RoomCard.java
Mage/src/main/java/mage/game/permanent/PermanentCard.java Mage/src/main/java/mage/game/permanent/PermanentToken.java`
ya se ejecutó (verificado: `grep -rn ROOMDBG Mage/src/main/java` = 0 ocurrencias). El worktree de
baseline se quita con `git worktree remove J:/MTG/Laboratorio/_baseline-v1`.

## Artefactos

- Baseline V1: `J:\MTG\_v1-roomcardtest.log`, worktree `J:\MTG\Laboratorio\_baseline-v1`.
- Trazas V2: `J:\MTG\_v2-unfixed-iso.log` (copia aislada), `J:\MTG\_v2-stack.log` (pila de creación del
  token), `J:\MTG\_v2-zone.log` (3 tests: `copySource=… zone=OUTSIDE lastCastHalf=LeftSplit`).
- Parche WIP descartado: `J:\MTG\_wip-patches\room-fallback-wip.patch`.
- Análisis completo: `CHECKPOINT-2026-11-21-verificacion-mods-v2.md`, sección 5.

## Prompt listo para el siguiente worker

> En `J:\MTG\Laboratorio\XMage-1.4.62V1-src` (rama `upgrade/official-1.4.63V2`) arregla el defecto
> preexistente del mod de Rooms del fork: al copiar un hechizo Room (`See Double` sobre
> `Bottomless Pool // Locker Room`) la copia entra al campo de batalla como `PermanentToken`
> (`Spell.resolve`, rama `isCopy()`, `Spell.java:418-426`) y por eso nunca recibe `roomCastHalf`, que es
> lo que lee `RoomEnterUnlockEffect` (`RoomCard.java:138`) para abrir la puerta. Sigue el diseño de
> `Laboratorio\HANDOFF-2026-11-21-room-copia-hechizo.md` (campo `roomCastHalf` en `TokenImpl`, escribirlo
> en la rama `isCopy()` desde `((RoomCard) card.getMainCard()).getLastCastHalf()`, leerlo en el ctor de
> `PermanentToken` y llamar a `setRoomCastHalf`). **No** uses el blueprint compartido
> (`findRoomCard(...).getLastCastHalf()`) como reserva: ya se probó y sube los fallos de 1 a 5.
> Cumple el protocolo de `J:\MTG\AGENTS.md`: `mvn -B -pl Mage.Tests -am test -Dtest='RoomCardTest'
> -Dsurefire.failIfNoSpecifiedTests=false` en verde (20/0), después `mvn clean package -DskipTests`,
> smoke en el laboratorio `17191` con capturas, y **nada** en la instalación oficial sin confirmación
> expresa de Kabutosan. Responde en español.
