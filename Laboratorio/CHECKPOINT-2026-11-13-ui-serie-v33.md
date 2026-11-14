# CHECKPOINT 2026-11-13 — UI de serie v33 (limpieza y quick wins del informe de modernización)

**Petición del usuario (Kabutosan):** informe a fondo de qué se puede modernizar en la UI solo sobre
lo que sigue siendo XMage de serie (sin contar los mods previos de la fork) y después "procede con
todos los protocolos".

Este checkpoint implementa la **Fase 0 del informe** (`Laboratorio\INFORME-MODERNIZACION-UI-2026-11-13.md`,
v2/v3 re-escopada con diff contra el tag oficial `xmage_1.4.63V1` + blame por línea).

## Qué se ha tocado (11 ficheros, todos con el código afectado verificado DE SERIE)

| # | Fichero | Cambio | Efecto visible |
|---|---|---|---|
| 1 | `util/TransformedImageCache.java` | `resizeImage` con `Graphics2D` + halving progresivo bilinear + pase final bicubic (sustituye a `getScaledInstance(SCALE_SMOOTH)`, API perezosa de AWT) | cartas e imágenes escaladas nítidas en toda la app; sin micro-cortes en el primer hit |
| 2 | `table/TablesPanel.java` | los 5 iconos del lobby escalan vía `TransformedImageCache` (nuevo helper `scaledIconImage`) | botones New Match/Tourney y estados sin re-muestreo borroso |
| 3 | `components/ability/MageScrollButton.java` | **fix bug real de serie**: los 4 iconos estáticos compartidos se mutaban en el constructor (scrollbars de distintos tamaños se sobreescribían entre sí); ahora caché `ImageIcon[]` por tamaño con el pipeline nuevo | flechas de scroll correctas en AbilityPicker y resto de barras |
| 4 | `org/mage/card/arcane/SvgUtils.java` | caché del `data:URI` del CSS por fichero, invalidado cuando `prepareCss` reescribe (cambio de tema) | sin lecturas de disco + base64 en cada rasterizado SVG |
| 5 | `deckeditor/CardSelector.java` | búsqueda **mientras se escribe** (debounce 250 ms, `javax.swing.Timer`, sin doble filtro con ENTER/Clear, stop en `cleanUp`) | el builder filtra solo tras escribir; ENTER sigue siendo inmediato |
| 6 | `game/FeedbackPanel.java` | **NO tocado** — ver "Reescopado" abajo | — |
| 7 | `game/HelperPanel.java` | reactivadas la burbuja + parpadeo de bandeja al pedir feedback con la ventana de fondo (estaban comentadas en serie) y `stopBlink()` al contestar; eliminados 2 wrappers `new Thread(() -> invokeLater(...))` | aviso visual cuando te toca decidir con el cliente minimizado |
| 8 | `game/PlayerPanelExt.java` | `clearLifeCache()` estático (el mapa `playerLives` solo tenía `put`, crecimiento de sesión) | RAM plana entre partidas |
| 9 | `game/GamePanel.java` | `cleanUp()` ahora detiene `resizeTimer` y llama `PlayerPanelExt.clearLifeCache()` | sin timer/callback fantasma tras cerrar la partida |
| 10 | `components/ext/dlg/DialogManager.java` | `ConcurrentHashMap` + `computeIfAbsent` / `remove` atómico (antes DCL roto sobre `HashMap`) | fin del race al crear el manager por partida |
| 11 | `util/gui/countryBox/CountryItemRenderer.java` | colores de selección del L&F (antes azul/amarillo fijos) + caché de banderas (antes `new ImageIcon(path)` recargaba disco en cada repintado) | combo de bandera themed y fluido |
| 12 | `chat/ChatPanelBasic.java` | hora fija `HH:mm:ss` (antes `DateFormat.getTimeInstance` variaba con el locale del SO) | marcas de tiempo uniformes en todas las máquinas |

## Reescopado tras verificar (nada de esto se ha tocado)

- **F2 con botones traducidos**: los textos OK/Yes/Done los pone el propio cliente según
  `FeedbackMode`; el servidor solo los sobrescribe en diálogos de elección (Mulligan/Keep, targets),
  donde pulsar el botón "izquierdo" por rol sería **incorrecto**. El comportamiento actual es el
  correcto; se retira el item del informe (queda anotado).
- **D1 "conexión en EDT"**: el log del smoke muestra `Connect: connecting to server ...
  =>[SwingWorker-pool-4-thread-3]` — el handshake ya corre en worker de Swing; no hay congelación
  que arreglar. Se retira del informe.
- Fase 1/2 del informe (tokens de color, fuente empaquetada, FlatLaf, optimized-drawing condicional,
  techo MB de cachés, torneo/draft sorters, servidores recientes, Stop de descargas, etc.) quedan
  pendientes como v34+ por pedido explícito de cambios pequeños y verificables por tanda.

## Build

```
C:/Users/vros0/apache-maven-3.9.6/bin/mvn.cmd -B -DskipTests -pl Mage.Client clean package
```

BUILD SUCCESS (24 s) → `Mage.Client/target/mage-client-1.4.63.jar`. Solo se modificó Mage.Client;
servidor y resto de jars sin recompilar (misma disciplina que v32). Nota: primer intento falló por
`Timer` ambiguo en `CardSelector` (`java.util.*` + `javax.swing.*`), resuelto cualificando
`javax.swing.Timer`.

## Verificación en laboratorio (LAB-UI-1.4.62-V2, puerto 17191, prefs `XMageLabUIv2`)

- `montar-lab.sh` OK; lanzamiento con `Start-Process` desvinculado (script del scratchpad replicando
  `lanzar-lab.ps1`; lección v32).
- Servidor escuchando en 17191 a los 32 s; **login automático restaurado**:
  `KabutosanLab connected to server with restored session`, `Connect: DONE`, cero excepciones en
  `cliente-lab-v33.err.log` (solo el warning conocido de `Software\JavaSoft\Prefs`, inofensivo — la
  clave no se ha tocado).
- Capturas: `capturas/127-v33-lobby-lab.png` (pantalla sin traer la ventana),
  `127b` (intento con MainWindowHandle, seguía el navegador delante) y **`127c-v33-lobby-lab.png`**
  (ventana XMage al frente vía EnumWindows+ALT trick; título confirma
  `Client: 1.4.63-V1 (build: 2026-11-13 21:52)` = jar nuevo; lobby completo pintado: toolbar,
  filtros, tablas, iconos New Match/Tourney nítidos).
- Probes `javap` sobre el jar desplegado en el lab: presentes `drawScaled`, `initLiveSearch`,
  `liveSearchTimer`, `lastAppliedSearchQuery`, `clearLifeCache`, `flagIcon`, `scaledIcon`,
  `cssDataURICache` y las llamadas `MageTray.stopBlink/displayMessage/blink` en HelperPanel.
- Laboratorio parado con `parar-lab.sh` (solo los 2 PIDs del lab; oficial intacto, 17171 no tocado).

## Pruebas manuales pendientes (el smoke no clica la UI)

Al abrir el Deck Editor: escribir en el buscador debe filtrar ~0,25 s después de soltar la escritura.
En AbilityPicker, las flechas de scroll deben verse correctas si hay dos barras de tamaños distintos.
Con el cliente minimizado y un feedback pendiente debe parpadear el icono de bandeja y parar al contestar.
El combo de bandera del Connect/Preferences debe usar los colores de selección del tema.

## Parche

`Laboratorio\parches\2026-11-13-ui-serie-v33.patch` (11 ficheros; +~120 / -~70 líneas).

## Despliegue (2026-11-13 — confirmado por Kabutosan)

- Cliente oficial (PID 11352) cerrado: WM_CLOSE no fue atendido (diálogo de cierre pendiente) y se
  forzó con `taskkill /F` dentro de la acción autorizada. Servidor 17171 **intacto** (sigue
  escuchando; no se reinició ni se tocó su base de datos).
- Backup del jar saliente: `Instalacion-XMage\xmage\backup-jars-1.4.63-v33-20261113\cliente\mage-client-1.4.63.jar` (22.565.021 bytes).
- Desplegado `mage-client-1.4.63.jar` v33 en `Instalacion-XMage\xmage\mage-client\lib\`:
  SHA256 `B7235FCAA91B19A9F21F25AC66ECD150B3F109CE1739B1D72C45527F10D86C7C` (idéntico al target;
  solo se movió el jar de cliente, mismo criterio que v32/v14-v16 sobre jars 1.4.63 coherentes).
- Pendiente para el usuario: reabrir el cliente oficial y pasar las 4 pruebas manuales de arriba.

## Rollback

`git apply -R Laboratorio\parches\2026-11-13-ui-serie-v33.patch` en la fuente + rebuild; para la
instalación, copiar el jar desde `Instalacion-XMage\xmage\backup-jars-1.4.63-v33-20261113\cliente\`
(con el cliente oficial cerrado).
