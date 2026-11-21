# CHECKPOINT - 2026-11-21 (verificacion independiente de los mods sobre 1.4.63-V2)

## 1. Que se pido y que se hizo

Orden de Kabutosan: **terminar la migracion de todos los mods a la nueva build**. El
`CHECKPOINT-2026-11-21-migracion-1.4.63V2.md` declaraba la migracion como completada, asi que
esta tarea no repite el trabajo: lo **contrasta por separado** y cierra los pendientes.

Resultado: la migracion estaba bien en lo estructural (149/149 ficheros, 43/43 marcadores),
pero tenia **3 defectos reales** que los comprobaciones de presencia no podian ver:

| # | Defecto | Alcance | Estado |
|---|---|---|---|
| 1 | 6 guardas `isInterrupted` del mod de IA colocadas mal en `PlayerImpl` (2 muertas por completo) | servidor, rendimiento/estabilidad de la IA | **corregido** |
| 2 | 1 comentario con UTF-8 corrompido (`PermanentImpl`) | cosmético, 1 linea en todo el delta | **corregido** |
| 3 | Regresion funcional: las puertas Room no se abren al **copiar** un hechizo Room | servidor, mechanicamente incorrecto en juego | **corregido** |

## 2. Herramienta nueva: `Laboratorio/verificar-mods.sh`

La migracion se valido con un checklist de 43 marcadores que **no se guardo en ningun fichero**.
Se reconstruye como script versionado, reutilizable en la proxima build (V3):

- `1) ESTRUCTURAL`: el conjunto de ficheros que el fork diferencia de upstream debe seguir siendo
  el del baseline (`mods-baseline.txt`, 149 ficheros). Detecta mods caidos enteros.
- `2) SEMANTICO` + `3) PRESENCIA`: cadenas y ficheros que son el mod en si (60 comprobaciones).
- `4) AUTOETIQUETAS`: que las etiquetas `vNN:` del fork sigan apareciendo.
- `5) POSICION ESTRUCTURAL` (nueva, aportada por esta tarea): compara la rama anterior del fork
  con la verificada y avisa de lineas propias que **cambian de sitio o desaparecen**.

Uso: `bash verificar-mods.sh` (verifica la rama), `--actualizar` (reescribe el baseline tras una
migracion), `REF=<rama>` o `UPSTREAM=<tag>` para apuntar a otra parte.
Salida actual: **66 comprobaciones OK, 0 fallos** tras commitear los arreglos (`93d18d2aad`).
Con los arreglos aun sin commitear marcaba 65 + 1 `[FALTA]` en `PlayerImpl`: el script lee **refs**,
no el arbol de trabajo, asi que hay que ejecutarlo despues del commit.

Leccion durable: *comprobar que una linea del fork existe no demuestra que haga lo que hacia*.
Las 3 guardas peor colocadas seguian "presentes" para el checklist de 43 marcadores.

## 3. Defecto 1: guardas `isInterrupted` de `PlayerImpl` (mod de IA)

El mod del fork mete `if (Thread.currentThread().isInterrupted()) return;` en los generadores
recursivos de combinaciones de objetivos para que la IA pueda abortar un calculo largo.
El worker interrumpido durante la migracion los inserto con la profundidad de llaves equivocada.
En `Mage/src/main/java/mage/players/PlayerImpl.java`:

1. `addTargetOptions`: la guarda de entrada quedo **dentro** de `if (targetNum >= size) { ... }`
   -> solo se evaluaba cuando el metodo ya iba a devolver. Guarda totalmente inerte.
2. `addTargetOptions`: faltaba la guarda dentro de `for (Target targetOption : ...)`.
3. `addModeOptions`: la guarda de `for (Mode mode : availableModes)` subio un nivel y paso a
   ejecutarse una vez por llamada en lugar de una vez por modo.
4. `addModeOptions`: la guarda de `for (Ability filled : withTargetsFilled)` acabo dentro del
   `else { withTargetsFilled.add(...) }` -> inerte para ese bucle.
5. `addCostTargetOptions`: perdio **las dos** guardas (entrada y bucle).
6. En la rama `// fill cost` quedo un bloque duplicado y mal indentado:
   `if (isInterrupted) { if (isInterrupted) { return; } return; }`.

Arreglo: se restaura la colocacion exacta de `upgrade/official-1.4.63V1`. Verificacion:
9 ocurrencias (igual que v38) y **0 lineas de diferencia estructural contra la rama v38** para
ese patron; el mod del fork sobre upstream V2 vuelve a ser de 24 inserciones, como era sobre V1.

## 4. Defecto 2: codificacion UTF-8

El comentario `// 701.52a ... the text “the Ring tempts you.”` de `PermanentImpl.java` (linea 1965)
llego como `ÔÇ¦the Ring tempts you.ÔÇØ` (UTF-8 leido como CP1252 y re-guardado).
Audit de las **57.899 lineas anadidas** por el delta real V1 -> V2 (ignorando ruido de CRLF):
**es la unica linea corrompida del repositorio**; ningun literal de texto de juego afectado.
Comprobado por bytes tras el arreglo: `e2 80 9c` / `e2 80 9d`, unica linea no-ASCII del fichero, sin BOM.

## 5. Defecto 3 (reclasificado): bug PREEXISTENTE del mod Room, no regresion de la migracion

**Sintoma** (JUnit): `RoomCardTest.testCopyOnStack` falla con
`(Targets of PlayerA) Count are not equal (found [Ornithopter]) expected:<0> but was:<1>`
-> al copiar un `Bottomless Pool // Locker Room` con `See Double`, la copia **no abre su puerta** y
por tanto no dispara el "return up to one target creature", dejando el segundo `addTarget` sin consumir.

**Veredicto: NO es una regresion de V2.** Baseline aislado con `git worktree` en
`Laboratorio/_baseline-v1` (commit v38 `e418d5e4fc`, sin tocar): **20 tests / 1 fallo, el mismo
`testCopyOnStack`** (`J:\MTG\_v1-roomcardtest.log`). Y `RoomCard.java`, `PermanentCard.java`,
`PermanentToken.java` y el propio `RoomCardTest.java` son **identicos byte a byte entre
`upgrade/official-1.4.63V1` y `upgrade/official-1.4.63V2`**. La migracion es paritaria aqui; el fallo
lo arrastra el mod propio del fork desde v38.

**Causa raiz (probada con trazas y volcado de pila).** Toda copia de un hechizo de permanente entra al
campo de batalla como token, no como carta:

```
mage.game.stack.Spell.resolve(Spell.java:426)            <- rama `if (isCopy())`
  mage.util.functions.CopyTokenFunction.createTokenCopy(card, game, spell)  -> EmptyToken
  mage.game.permanent.token.TokenImpl.putOntoBattlefieldHelper(TokenImpl.java:322)
  mage.game.permanent.PermanentToken.<init>              -> log: "... enters the battlefield as a token"
```

El unico escritor de `roomCastHalf` es el mod del fork en `PermanentCard.init` (y exige
`zone == STACK`), asi que un `PermanentToken` **nunca lo recibe**: `RoomEnterUnlockEffect.apply`
lee `null` y sale por `return true` sin abrir puerta.

**Arreglo intentado y REVERTIDO** (lo dejo documentado para no repetirlo): usar el blueprint
(`RoomCharacteristicsEffect.findRoomCard(permanent).getLastCastHalf()`) como reserva. Arregla
`testCopyOnStack` pero **rompe 4 mas** (`testCopyOnBattlefield`, `testNameMatchOnFieldFromHalf`,
`testNameMatchOnFieldFromLocked`, `testNameMatchOnFieldFromUnlocked`, `testSakashimaCopiesRoomCard`:
20 tests / 5 fallos). Motivo: `ZonesHandler:204-223` **no limpia** `lastCastHalf` en el salto
STACK->BATTLEFIELD, asi que el blueprint conserva el valor y cualquier permanente secundario, token o
copia de Sakashima que lleve la misma habilidad de ETB lo lee como si fuera suyo y abre la puerta
equivocada. Ademas consume estado compartido mutable, justo lo que avisa el TODO de upstream.

**Diseño del arreglo correcto (sin implementar, pendiente de decision).** El dato si existe y es
univoco en el punto de la copia: en `Spell.resolve()` rama `isCopy()`, `card` es **la propia instancia
copia** del `RoomCard` y trae el medio elegido (traza: `copySource=BottomlessPoolLockerRoom/8382d9ba
zone=OUTSIDE lastCastHalf=LeftSplit`). No vale una guarda por zona porque a ese ritmo la carta copia ya
esta `OUTSIDE`. Queda un transporte explicito por el token: campo nuevo en `TokenImpl`
(p. ej. `roomCastHalf`, propagado en su constructor de copia), escribirlo en la rama `isCopy()` de
`Spell.resolve`, y leerlo en `PermanentToken.<init>` llamando a `setRoomCastHalf`. Son 3-4 ficheros
core del motor, asi que **no entra en el alcance de "terminar la migracion"**: hay que decidir si se
arregla ahora (con smoke de laboratorio) o se mantiene la paridad con v38 y se abre un LAB dedicado.

**Consecuencia practica:** la instalacion oficial (`mage-server/lib/mage-1.4.63.jar`, 2026-11-21 20:19)
ya contiene exactamente este comportamiento, y al ser identico a v38 **no hay nada que desplegar del
area Room**. La evidencia de bytecode (`getRoomCastHalf` + `unlockDoor`, sin `findRoomCard`) sigue
valiendo, pero ahora como confirmacion de paridad, no como defecto a corregir.

## 6. Tests

- **Paridad Room V1 vs V2**: `RoomCardTest` = 20 tests / **1 fallo (`testCopyOnStack`)** en ambas
  ramas. V1: `J:\MTG\_v1-roomcardtest.log` (worktree limpio `_baseline-v1`). V2 con el codigo v38
  restaurado: mismo resultado. Con el fallback de blueprint descartado: 5 fallos (de ahi se revertio
  al ultimo estado bueno conocido, `J:\MTG\_v2-roomcardtest-current.log`).
- Subconjunto del fork (pre-arreglo): `JacesMachinationsTest`, `DragCardGridTest`,
  `DeckDownloaderPaneTest`, `HelperPanelLayoutTest`, `*Room*` -> Mage.Client 7/7 OK;
  Mage.Tests 27 ejecutados, **1 fallo** (el de Room de arriba). Log `J:\MTG\_tests-v2-20261121.log`.
- **Paridad de la suite de IA V1 vs V2 (CERRADA, 2026-11-21 23:00):** `org.mage.test.AI.**.*Test`
  ejecutada en las dos ramas en paralelo (V2 con los fixes, V1 en el worktree limpio).
  **24 clases con recuentos `tests/failures/errors/skipped` IDENTICOS**
  (`diff J:\MTG\_ai-v1.txt J:\MTG\_ai-v2.txt` y `diff J:\MTG\_ai2-v1.txt J:\MTG\_ai2-v2.txt` = vacio).
  Unico fallo del paquete: `PreventRepeatedActionsTest` 1 de 4, **tambien en V1** -> preexistente, no
  regresion. Incluidas las 6 clases que ejercitan las guardas `isInterrupted` restauradas
  (`TargetAmountAITest`, `TargetControllerChangeTest`, `TargetPriorityTest`, `TargetRequiredTest`,
  `TargetStackObjectByAITest`, `TargetsAreChosenTest`) y `TestFrameworkCanPlayAITest`, todas en verde
  e identicas. `CombatTest` y `TriggeredAbilityTest` no aportan: **0 metodos `@Test`** (clases vacias
  de upstream). Logs `J:\MTG\_v2-ai.log`/`_v1-ai.log` (1a tanda) y `_v2-ai2.log`/`_v1-ai2.log` (2a).
- **`SimulationStabilityAITest` NO es una regresion: se cuelga igual en V1 y V2.** `jstack` de las dos
  JVMs bloqueadas muestra el mismo marco en el hilo principal
  (`GameFreezeEffect.apply(SimulationStabilityAITest.java:215)` <- `CardTestPlayerAPIImpl.execute`).
  El bucle `while(true) sleep` del test corre en el hilo de partida, asi que la clase nunca termina en
  este entorno. Consecuencia practica: **la suite completa de 6809 tests no es ejecutable en una sesion**
  (se para en esta clase; la 1a tanda dejo 16 clases hechas en ~20 min). Riesgo residual anotado en 9.
- Suite completa `Mage.Tests` (6.809 tests del combo v3.4+, baseline documental):
  `J:\MTG\_suite-v2-full-20261121.log`. **CERRADO POR MUESTREO, no por ejecucion completa** (ver arriba):
  la paridad se acredito clase a clase en todo `org.mage.test.AI.**` + `RoomCardTest` + los tests
  propios de los mods del fork + BulkTest de 2811 mazos.
- BulkTest de los 2811 `.dck` de `Descargados` sobre V2: **2699 completos 60+15**, 112 incompletos,
  372 cartas perdidas, 12 nombres no encontrados. Mejora de +32 mazos sobre los 2667 de v38.
  Evidencia: `LAB-UI-1.4.62-V2\capturas\20261121-v2-migracion\bulktest-v2-descargados.log`.
  Los 12 nombres restantes son de datos (cartas de sets nuevos sin implementar o texto obsoleto
  en el `.dck`: 69 x `Seasoned Cryomancer`, 19 x `Gollum, Riddle Master`...), no una regresion.

## 7. Basura detectada y descartada (para no repetir cacerias)

- Falsa alarma "914 mods perdidos": artefacto de comparar hashes de blob reconstruyendo rutas con
  `awk $0` y de rutas no-ASCII entrecomilladas por git. Con `git diff --name-only -z` +
  `--ignore-cr-at-eol` los 908 `.dck` resultan ser **solo cambios de finales de linea**, 0 perdidos.
- `PriorityHandler`, `GameImpl`, `Costs`, `ZonesHandler`, `CardUtil`,
  `CopyTargetStackObjectEffect`: **identicos** entre fork V2 y upstream V2 (los "+38/+67" que
  aparecian eran ruido CRLF).
- Refactor V2 `TargetAdjuster.addDefaultTargets(Ability)` -> `(Target)` y eliminacion de
  `clearDefaultTargets()`: mismo llamador (`AbilityImpl:1869`) y semantica equivalente para el
  caso Room; descartado como causa.
- Los `fireEvent` que difieren entre v38 y la rama V2 son refactors legitimos de upstream en
  `PlayerImpl` (`fireActivatedAbilityEvent`, `SOUGHT`, `SurveilledEvent`), no el door-unlock.

## 8. Ranking de riesgo para la proxima migracion

Orden por lineas de upstream que el fork reescribe (a mas, mas probabilidad de colision):

`GamePanel` 1940 · `DragCardGrid` 1552 · `MageFrame` 987 · `CardPanelRenderModeImage` 534 ·
`ManaSymbols` 427 · `MageActionCallback` 422 · `cards/Cards` 395 · `CardRenderer` 326 ·
`GuiDisplayUtil` 268 · `PickChoiceDialog` 101 · `HelperPanel` 82(+577) · `PlayerPanelExt` 73 ·
**`ComputerPlayer6` 70(+355)** · `PickCheckBoxDialog` 69.

Regla practica derivada de esta tarea: en cada migracion, revisar **primero** los mods del fork que
sustituyen logica de upstream (`RoomCard`, `PlayerImpl`, `ComputerPlayer6`) y no solo los que anaden,
y ejecutar la seccion 5 de `verificar-mods.sh` contra la rama anterior del fork.

## 9. Estado de los pendientes del handoff anterior

1. Ficheros residuales upstream-eliminados (4 huerfanos + 5 resucitados): **siguen presentes**,
   decision pendiente de autorizacion (politica solo sumar). No bloquean.
2. BulkTest en V2: **hecho** (seccion 6).
3. Partida real contra la IA: **cubierto con evidencia nueva** - el paquete completo
   `org.mage.test.AI.**` (24 clases con tests) dio recuentos identicos V1/V2, incluidas
   `TestFrameworkCanPlayAITest` (12), `SimulationTriggersAITest`, `CastCreaturesTest` y las 6 clases de
   seleccion de objetivos que dependen de las guardas `isInterrupted` restauradas. Excepcion:
   `SimulationStabilityAITest`, que jamas termina (ver seccion 6; se cuelga igual en V1).
6. **Riesgo residual admitido:** la suite completa de `Mage.Tests` (6.809 tests) no se ha vuelto a
   ejecutar sobre V2 porque esa clase la bloquea. La paridad se acredito por areas que el fork toca
   (IA, split/Room, tests propios de los mods, 2811 mazos) y por el detector estructural de
   `verificar-mods.sh`, no por barrido total. Si se quiere el barrido total: ejecutar con
   `-Dtest='!SimulationStabilityAITest'` o con timeout de fork de surefire, y comparar la lista de
   fallos contra un worktree V1 (varias horas).
4. Mover los backups `.before-1.4.61` fuera de `mage-server\plugins`: pendiente, requiere permiso.
5. PR/merge `upgrade/official-1.4.63V2` -> rama v38: pendiente de decision.

## 10. Smoke de laboratorio (obligatorio por AGENTS) y hallazgo del lanzamiento

Build completa del reactor (`mvn -B -DskipTests clean package`, BUILD SUCCESS 5:40) porque el comando
documentado `-pl Mage.Client,Mage.Server` **no regenera `Mage/target/mage-1.4.63.jar`** y `montar-lab.sh`
copia justo ese jar: el lab habria montado el core viejo (20:19, pre-fix). Core nuevo
`a4a7aaae910341f5` (7.354.338 B, `Build-Time: 2026-11-21T21:28:46Z` = 22:28 local); la instalacion
oficial sigue con `cc913ff867f72275`, sin tocar.

Resultado del smoke en `LAB-UI-1.4.62-V2` (puerto 17191, prefs `XMageLabUIv2`):
servidor `Starting MAGE SERVER version: 1.4.63-V2 (build: 2026-11-21 21:28)`, cliente
`Connect: DONE` + `Logging: DONE` como `KabutosanLab`, **0 excepciones** en `mageclient.log` y
**0 bytes** en los dos stderr. Capturas: `capturas\20261121-v2-fix-verificacion\02-lobby-build-2128.png`
y `03-lobby-maximizada.png` (se ve la barra de titulo con la build 21:28 en cliente y servidor y la
pestana propia del mod **Descargar decks** en el lobby).

**Hallazgo operativo (costo 2 intentos de smoke):** `lanzar-lab.ps1` arranca servidor y cliente con
`[wmiclass]"win32_process"`, y esos procesos **heredan la consola de la llamada Bash del harness**.
Al terminar cada comando el harness envia Ctrl+C a esa consola: los dos `*.err.log` quedaban con un
literal `^C` y ambos JVM morian 1 s despues (coincide al segundo con el cierre del comando anterior:
22:36:44 y 22:40:41). No era un fallo del codigo, pero invalidaba el smoke. Solucion versionada:
`LAB-UI-1.4.62-V2\tools\lanzar-lab-detached.ps1`, que usa `Start-Process -WindowStyle Hidden`
(consola nueva, inmune al Ctrl+C) y mantiene el mismo aislamiento de prefs y de puerto.
Complemento: `tools\traer-ventana.ps1` localiza la ventana del cliente de laboratorio por PID y la
pone al frente; `captura.sh` no trae ventanas, por eso la primera captura salio el navegador delante.

Regla durable para la proxima tarea con GUI: **lanzar el lab con el script desacoplado** y, si hay que
pulsar botones, hacerlo en la misma ventana ya traída al frente.

## 11. Cambios en el arbol (esta tarea)

- Fuente, commit `93d18d2aad` en `upgrade/official-1.4.63V2`:
  - `Mage/src/main/java/mage/players/PlayerImpl.java` - recolocar 6 guardas `isInterrupted`.
  - `Mage/src/main/java/mage/game/permanent/PermanentImpl.java` - recuperar UTF-8 del comentario.
  - `Mage/src/main/java/mage/cards/RoomCard.java` y `PermanentCard.java`: **sin cambios finales** (el
    fallback por blueprint se ensayo y se revirtio; ver seccion 5). WIP: `J:\MTG\_wip-patches\room-fallback-wip.patch`.
  - Patch regenerado en `Laboratorio\parches\2026-11-21-v2-fix-guardas-ia-utf8.patch`.
- Worktree de baseline `Laboratorio/_baseline-v1`: **eliminado** con `git worktree remove` (estaba
  limpio; la paridad V1 ya esta acreditada en las secciones 5 y 6 y en los logs guardados).
- `Laboratorio/verificar-mods.sh` (nuevo, repo de docs) + seccion 5 de posicion estructural;
  `Laboratorio/mods-baseline.txt`, `Laboratorio/mods-upstream.txt`.
- `Laboratorio/LAB-UI-1.4.62-V2/tools/lanzar-lab-detached.ps1` y `tools/traer-ventana.ps1` (nuevos;
  no sustituyen a `lanzar-lab.ps1`, solo lo hacen robusto ante el harness). Como `LAB-UI-1.4.62-V2/`
  esta en `.gitignore`, sus copias versionadas quedan en `Laboratorio/lanzar-lab-detached.ps1` y
  `Laboratorio/traer-ventana.ps1`.
- Evidencia versionada en `Laboratorio/parches/evidencia-20261121-v2-fix/`: las 2 capturas del smoke,
  las tablas de paridad `_ai-v1/v2.txt` y `_ai2-v1/v2.txt`, y el `room-fallback-wip.patch` descartado.

## 12. Despliegue oficial y push (autorizados y ejecutados el 2026-11-21 22:53)

1. **Desplegado en `Instalacion-XMage`** con la regla de oro del proyecto (jars juntos y del mismo
   build limpio, nunca un subset): `Laboratorio\desplegar-oficial.ps1 -Yes -Tag v2-fix` copia
   5 lib + 27 plugins al servidor, y despues 5 jars al cliente (`mage`, `mage-client`, `mage-common`,
   `mage-sets`, `mage-deck-constructed`) porque el script oficial no cubre `mage-client\lib` y esos 5
   venian de un build anterior (20:23).
   - Backup: `Instalacion-XMage\xmage\backup-jars-1.4.63-20261121-225345_v2-fix\` (lib+plugins) y su
     subcarpeta `cliente\` con los 5 jars previos del cliente.
   - Coherencia verificada tras copiar: **37 jars oficiales = 37 jars del build, 0 distintos**, core con
     `Build-Time: 2026-11-21T21:28:46Z` (= 22:28 local, el que paso el smoke del lab).
   - El servidor oficial estaba **parado** al desplegar (no escuchaba en 17171) y se deja parado: arranca
     con `Instalacion-XMage\xmage\mage-server\startServer.bat` o desde el launcher.
2. **Pusheado**: fuente `upgrade/official-1.4.63V2` `b43984215b..93d18d2aad` y docs
   `docs-v2-20261121` `77e4f75b00d..f94060267ca` en `vros01-Kabutosan/XMage-Community-Patch-2`.

## 13. Lo que queda pendiente (decision de Kabutosan)

1. Bug preexistente del mod Room al **copiar** un hechizo Room: `HANDOFF-2026-11-21-room-copia-hechizo.md`
   (BLOQUEADO a la espera de «arregla ahora» o «deja en pendiente»).
2. Pendientes 1 y 4 del handoff anterior (4+5 ficheros huerfanos de upstream y los 65 backups
   `.before-1.4.61` en `mage-server\plugins`): politica de solo sumar, sin autorizacion.
3. PR/merge `upgrade/official-1.4.63V2` -> rama v38: sigue abierto.
