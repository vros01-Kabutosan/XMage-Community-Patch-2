# CHECKPOINT 2026-11-01 — Layout del campo de batalla (scroll prematuro) v24

## 1. Resumen

- **Síntoma reportado:** el campo de batalla (BF) pide desplazamiento vertical con pocas
  cartas. Objetivo: que las cartas se ajusten a la pantalla sin scroll hasta que el BF
  esté realmente lleno.
- **Estado:** código implementado y compilado; laboratorio montado con el jar nuevo
  (auto-conecta a 17191). **Pendiente el smoke visual dentro de una partida.**
  **No desplegado en la instalación oficial** (el protocolo exige smoke previo).

## 2. Causa raíz (verificada en código y prefs)

1. El auto-fit de `CardPluginImpl.sortPermanents` reparte tierras, criaturas y
   no-permanentes en **filas separadas por tipo**: `wrap(lands/creatures/others)` y
   `fillRow(...)` solo rellena huecos con `others`. Con una tierra y una criatura el
   layout ocupa siempre 2 filas; con un artefacto, 3.
2. El bucle baja `cardWidth` de 10 en 10 hasta `cardWidthMin`; al llegar al mínimo
   `allowHeightOverflow` desactiva el control de altura y el contenido se desborda →
   aparece el scrollbar aunque haya pocas cartas.
3. Con los ajustes reales del usuario (`guiCardBattlefieldSize = 60`): tamaño mínimo de
   carta = 222x317 px, máximo = 668x953 px. En un panel de ~350-400 px de alto, dos filas
   al mínimo ya no caben → scroll con ~5-6 permanentes.

## 3. Cambios (commit `4c1d9289155`)

`Mage.Client/src/main/java/org/mage/plugins/card/CardPluginImpl.java`
- **Intento 3 de empaquetado** (`fillRowsWithLeftovers` / `packLeftoverIntoRow`): tras los
  dos intentos originales, coloca las cartas sobrantes (tierras y criaturas incluidas)
  dentro de las filas ya creadas si hay ancho, respetando la alineación derecha de
  `others` (`findFirstOtherStack`). Los intentos 1 y 2 no cambian, así que el
  comportamiento previo se conserva cuando ya cabía.
- **Suelo duro de tamaño** (`CARD_WIDTH_HARD_MIN = 60`, `getCardWidthHardMin()`): el
  auto-fit puede bajar de `cardWidthMin` (hasta la mitad del mínimo configurado, nunca
  menos de 60 px) antes de aceptar scroll. Solo afecta a los casos que antes desbordaban.
- **Instrumentación opcional** `-Dxmage.debug.bflayout=true`: logea panel/visibleRect/
  viewport, playArea, `cardWidth` elegido, mínimo y mínimo duro, filas, altura y estado
  del scrollbar. Desactivada por defecto.

`Mage.Client/src/main/java/mage/client/game/BattlefieldPanel.java`
- `sortLayout()` también exige altura > 0: un layout con altura 0 forzaba el mínimo y
  activaba el scrollbar.

## 4. Laboratorio

- `LAB-UI-1.4.62-V2/lanzar-lab.ps1`: las prefs aisladas se siembran ahora con la API de
  Java (`tools/PrefsSeed.java`), usando las claves reales de `MagePreferences`
  (`serverAddress`, `serverPort`, `autoConnect`, `<addr>/userName`, `connectFlag`), y se
  elimina `-Djava.util.prefs.userRoot=...` que redirigía el nodo y hacía que el cliente
  mostrara el **17171 por defecto**. Verificado: `Auto-connecting to localhost`,
  `Connect: DONE`, `Logging: DONE` en 17191.
- `tools/ui-real.ps1` y `tools/ui.ps1`: utilidades de captura/click para smokes
  automatizados (no forman parte del cliente).
- Jar desplegado en `LAB-UI-1.4.62-V2/cliente/lib/mage-client-1.4.63.jar`
  (hash `81CFA28116D770F4FFA2E77E2D8214CAD5ED3FC17F08AB3839C7ECCB35C4F55B`).

## 5. Cómo completar la verificación (pendiente)

1. Con el laboratorio arrancado (`bash lanzar-lab.sh`), crear una partida 1v1 (vs AI o
   hotseat) con un mazo pequeño.
2. Poner 3 tierras + 2 criaturas + 1 artefacto en el BF: no debe aparecer scrollbar y las
   cartas deben verse lo más grandes posible.
3. Llenar el BF (20+ permanentes): el scrollbar debe aparecer y funcionar.
4. Revisar `LAB-UI-1.4.62-V2/cliente-lab.log`: cada layout imprime una línea
   `Battlefield layout: ...` con `cardWidth`, `rows`, `layoutHeight` y `scrollBarVisible`.
5. Guardar capturas antes/después en `LAB-UI-1.4.62-V2/capturas/`.

Si el smoke pasa: backup de jars oficiales y despliegue de `mage-client-1.4.63.jar` en
`Instalacion-XMage/xmage/mage-client/lib/`.

## 6. Pendiente / no incluido

- **Columna de logs** (chat+logs a la derecha): sigue con el mínimo de 380 px impuesto en
  `GamePanel.applySplitterLocation` / `splitChatAndLogs.setMinimumSize`. Es un fix previo
  deliberado; para permitir estrecharla hay que mover esa protección al restaurado de
  divisores (`normalizeSplitterLocation`) o añadir un botón de ocultar logs. No se tocó en
  esta iteración.
- Los diálogos de la captura del usuario (derrota + "game error") son ajenos a este
  cambio.
