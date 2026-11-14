# Laboratorio XMage — UI 1.4.63 (actualizado 2026-11-09)

> **Norma permanente para cualquier worker/IA:** todo cambio se prueba primero en este
> laboratorio (puerto 17191) y solo se despliega en `J:\MTG\Instalacion-XMage` cuando el
> smoke pasa. Reglas completas en `J:\MTG\AGENTS.md`. No hace falta repetirlas en cada prompt.

Este directorio contiene TODO lo necesario para reproducir, probar y desplegar el fork
modificado de XMage 1.4.63V1 de Kabutosan. La instalación oficial vive aparte, en
`J:\MTG\Instalacion-XMage`.

## Estructura

| Ruta | Qué es |
|---|---|
| `XMage-1.4.62V1-src/` | Fuente completa del fork (git, rama `upgrade/official-1.4.63V1`). Aquí se compila. |
| `LAB-UI-1.4.62-V2/` | Laboratorio ejecutable aislado: `cliente/` + `servidor/` (puerto 17191), scripts, logs y `capturas/` con la evidencia de los smokes. |
| `parches/` | Parches consolidados de los mods (`2026-10-31-layout-4k-fix-v19.patch`, `2026-10-31-migracion-1.4.63V1.patch`, etc.). |
| `Proyecto-XMage/` | Legado 1.4.61 retirado del disco el 2026-11-01 y archivado en `_archivo/20261101/` (la carpeta quedó vacía). Su baja del árbol git se registró el 2026-11-09; la historia lo conserva. |
| `_archivo/20261025/` | Material antiguo retirado del flujo activo (logs viejos, labs anteriores, restos de recuperación) y limpieza autorizada del 2026-10-25. Detalle exacto en `_archivo/20261025/INVENTARIO-LIMPIEZA-2026-10-25.md`. |
| `_archivo/20261101/` | Reorganización del 2026-11-01 (script `reorganizar-2026-11-01.ps1`): legado `Proyecto-XMage/`, config del repo raíz, caches de herramientas y worktrees huérfanos. No se versiona. |

## Mods activos (todos aditivos)

Fuente: `XMage-1.4.62V1-src`. Parches en `parches/`.

1. `Mage.Client/src/main/java/mage/client/game/FeedbackPanel.java`
   - Ancho preferido del panel de chat = `PREFERRED_WIDTH` (1024) en vez de `Short.MAX_VALUE`.
   - Evita que el chat se aplaste al restaurar el divisor.
2. `Mage.Client/src/main/java/mage/client/game/GamePanel.java`
   - `setGUISize()` usa `FeedbackPanel.PREFERRED_WIDTH` para feedback y shortcuts.
   - `restoreSplitter()` espera a que el splitter tenga ancho real antes de restaurar.
   - `normalizeSplitterLocation()` descarta divisores guardados degenerados y garantiza un ancho mínimo de chat indestructible (380 px en 4K).
   - En `applySplitterLocation`: no resetear el tamaño mínimo de `splitChatAndLogs`.
   - Altura de campos de batalla flexibilizada al 50% de la altura visible, sin forzar mínimo estático.
   - La barra de fases queda debajo de la mano (hand height correcto).
   - v1.4: la barra de fases muestra solo nombres (44 px) y los atajos F4/F5/F7/F9/F10/F11 vuelven a la fila inferior.
   - v1.5: texto de fase centrado con `FontMetrics` y píldora de resaltado.
3. `Mage.Client/src/main/java/org/mage/plugins/card/CardPluginImpl.java`
   - v19: Gutters dinámicos calculados con `MageLayer.getAnimationOuterSpace(cardWidth, cardHeight)` para evitar que las cartas del campo se coloquen en coordenadas negativas o se corten en el borde superior.
4. `Mage.Client/src/main/java/mage/client/plugins/adapters/MageActionCallback.java`
   - v19: Margen inferior ampliado en `HAND_CARDS_MARGINS` (10 px) para proteger la caja de fuerza/resistencia en la mano.
5. `Mage.Client/src/main/java/org/mage/card/arcane/MageLayer.java`
   - v19: `getAnimationOuterSpace` hecho `public static` para cálculo de layout compartido.
6. `Mage.Client/src/main/java/mage/client/MageFrame.java`
   - v19: Soporte de propiedad `-Dxmage.prefs.node` para aislamiento de preferencias en el laboratorio.
7. `Mage.Client/src/main/java/mage/client/game/HelperPanel.java`
   - `getRequiredHeight()` limitado a 1/3 de la pantalla (mínimo 120 px).
8. `Mage.Client/src/main/java/mage/client/dialog/GraveyardCardsDialog.java`
   - Diálogo de cementerio interactivo por jugador.
9. `Mage.Client/src/main/java/mage/client/deckeditor/DeckDownloaderPane.java`
   - Moderno dashboard minimalista para descarga y actualización de decks con parsing en tiempo real.
10. `Mage.Server/src/main/java/mage/server/MageServerImpl.java`
    - `markGameTypesLoadFinished()`: corta la espera de 180 s cuando no hay plugins de game types.
11. `Mage/src/main/java/mage/cards/repository/DatabaseUtils.java` (v2.2)
    - `checkAndFixStaleH2Lock()`: autocuración y borrado automático de ficheros `.lock.db` huérfanos o con procesos zombis caídos, enlazando H2 `AUTO_SERVER` a `127.0.0.1`.
    - `MageFrame.doClientShutdownAndExit()`: cierre limpio garantizado con `Platform.exit()` y `System.exit(0)`.
12. `Mage.Common/src/main/java/mage/utils/MageVersion.java` y `MageVersionException.java` (v2.3)
    - Declaración explícita de `serialVersionUID` canónico para compatibilidad binaria absoluta cliente-servidor por RMI/Bisocket (`-5872491576857560039L` y `9200806506119372910L`).
13. `Mage.Client/src/main/java/org/mage/plugins/card/CardPluginImpl.java` (v24 + v25 + v26)
    - Auto-fit del campo de batalla: permite bajar del tamaño mínimo configurado (hasta la mitad, nunca menos de 60 px) antes de activar el scroll vertical. Objetivo: no pedir desplazamiento hasta que el campo esté realmente lleno.
    - v25: tierras y permanentes ya no comparten fila ni se rellenan huecos entre ellos.
    - v26: el orden de filas vuelve a ir espejado (jugador local: criaturas arriba y tierras debajo; rival: tierras arriba y criaturas debajo), el auto-fit compara contra la altura real del layout final y busca el tamaño en pasos de 2 px, así las cartas salen al máximo que cabe sin scrollbar.
    - Traza de diagnóstico opcional con `-Dxmage.debug.bflayout=true`.
14. `Mage.Client/src/main/java/mage/client/game/BattlefieldPanel.java` (v24)
    - `sortLayout()` no hace layout mientras el panel no tenga altura (un layout a altura 0 forzaba el tamaño mínimo y activaba el scrollbar).
15. `Mage.Client/src/main/java/mage/client/game/GamePanel.java` (v25)
    - El ancho mínimo de la columna de chat/logs baja de 380 px a ~160 px (arrastre y one-touch), manteniendo la protección contra divisores guardados degenerados al restaurar.
16. `Mage.Client/src/main/java/mage/client/chat/ChatPanelBasic.java` y `mage/client/components/ColorPane.java` (v27)
    - El log de partida se actualiza siempre en el EDT y la pintura HTML tolera el fallo `BoxView.getOffset` del JDK repintando en vez de abortar con el diálogo "CLIENT - unhandled error in GUI".
17. `Mage.Client/src/main/java/mage/client/cards/Cards.java` y `mage/client/game/GamePanel.java` (v28)
    - Modernización integral de la Pila Flotante (*The Stack*):
      - Fondo Obsidian/Slate con doble borde metálico y sombra suave de elevación.
      - Cabecera interactiva con título dorado `PILA DE RESOLUCIÓN (N)`, botón de colapso rápido `[-]`/`[+]` y grip de redimensionamiento visible.
      - Badges visuales LIFO: `#1 PRÓXIMO` (esmeralda brillante) en la carta que va a resolver y `#2`, `#3`... en capas inferiores.
      - Supresión de iconos de permanentes confusos (como `ABILITY_REACH`) en habilidades disparadas de la pila.
      - HUD con información clara de orden, controlador, tipo (`⚡ Habilidad Disparada / Hechizo`) y objetivo dinámico (`🎯 Objetivo: <Nombre>`).
      - Sombras 3D entre cartas apiladas verticalmente para mayor sensación de profundidad.
18. `Mage.Client/src/main/java/org/mage/card/arcane/CardPanelRenderModeImage.java`, `mage/client/util/gui/Arrow.java` y `mage/client/game/HelperPanel.java` (v29)
    - Modernización del Campo de Batalla (*Battlefield*):
      - Halos de resplandor suave (*Soft Bloom*) en cartas jugables (cian eléctrico), seleccionadas (esmeralda), objetivos (ámbar) y combate (carmesí).
      - P/T dinámico con colorimetría semántica (aumentos en verde, reducciones en rojo) y resistencia teñida en rojo-daño ante daño de combate acumulado.
      - Mareo de invocación no obstructivo: ilustración 100% visible, niebla mística basal y badge de vórtice dorado en esquina superior con tooltip.
      - Contadores nítidos antialiased centrados con sombra y texto blanco.
      - Flechas cinemáticas de combate/selección con gradiente longitudinal, sombra suave y reborde luminoso.
      - Botones de acción del HelperPanel con jerarquía cromática (verde para OK/Done, rojo para Cancel/No, ámbar para Special).
19. Mage.Client/src/main/java/mage/client/dialog/GraveyardCardsDialog.java, mage/client/cards/CardArea.java y mage/client/dialog/ShowCardsDialog.java (v30)
    - Unificación y modernización de la vista del Cementerio (*Graveyard*) y Búsqueda de Biblioteca (*Library Search*):
      - Visualización vertical en cascada / columnas idéntica a la vista de búsqueda de biblioteca (loadCardsNarrow / loadCardsMany), eliminando la fila horizontal plana y el espacio vacío inferior cuando hay menos de 10 cartas.
      - Ajuste adaptativo y proporcional de la ventana según el número de columnas (	itleMinW = 280), evitando marcos gigantes innecesarios.
      - Tema oscuro Obsidian (#161A23 / 
ew Color(22, 26, 35)) aplicado al fondo de la ventana, CardArea, JScrollPane y su Viewport, eliminando el fondo gris claro desfasado y unificando el estilo con la mesa de juego.

20. `Mage.Common/src/main/java/mage/db/**` (v31, 2026-11-09)
    - Restauración del paquete fuente `mage.db` (EntityManager + EntityManagerTest + model/Feedback + model/Log) verbatim del delta oficial upstream (`79aec614fc2`). El upgrade 1.4.63 lo dejó fuera y la regla global `**/db` del `.gitignore` lo tenía ignorado, con lo que `Mage.Server` no compilaba desde source (`package mage.db does not exist` en FeedbackServiceImpl) y los jars desplegados dependían de clases recicladas de builds viejos. Aditivo: negación en `.gitignore` + 4 ficheros upstream, sin tocar código existente. Sin él, el feedback del servidor daría ClassNotFoundException en runtime.

21. `Mage.Client/src/main/java/mage/client/deckeditor/DeckEditorPanel.java` y `mage/client/util/AiDeckLibrary.java` (v32, 2026-11-13)
    - El botón **LOAD** del Deck Editor abre siempre `sample-decks\Descargados` (la carpeta de los mazos descargados por el descargador: Modern / Pioneer / Standard), en lugar de la última ruta recordada en la preferencia `lastDeckFolder`.
    - Reutiliza `AiDeckLibrary.downloadedDeckRoot()` (antes `private`, ahora `public`) para que el editor y el pool de mazos de la IA resuelvan la misma carpeta: una única fuente de verdad, sin rutas hardcodeadas.
    - Fallback conservado: si `Descargados` no existe, se vuelve al comportamiento anterior (`lastDeckFolder`). SAVE, Import/Export, el selector de mazo de la mesa y los torneos quedan **sin cambios** (mismo criterio pedido por Kabutosan: solo LOAD del editor).
    - Parche: `parches\2026-11-13-load-folder-descargados-v32.patch`. Evidencia: `LAB-UI-1.4.62-V2\capturas\126-prueba-load-descargados.log`.

22. UI de serie v33 (2026-11-13) — Fase 0 del `INFORME-MODERNIZACION-UI-2026-11-13.md` (11 ficheros, todos con el código afectado verificado contra el tag oficial `xmage_1.4.63V1`):
    - `util/TransformedImageCache.java`: resampling moderno (`Graphics2D` + halving bilinear + bicubic final) en vez de `getScaledInstance(SCALE_SMOOTH)` → nitidez de zoom en toda la app.
    - `table/TablesPanel.java`: iconos del lobby escalados por la caché nueva. `components/ability/MageScrollButton.java`: **fix bug de serie** (los iconos estáticos compartidos se mutaban por cada scrollbar de distinto tamaño) + caché por tamaño.
    - `org/mage/card/arcane/SvgUtils.java`: caché del `data:URI` del CSS invalidada en `prepareCss` → sin disco por cada rasterizado SVG.
    - `deckeditor/CardSelector.java`: búsqueda mientras se escribe (debounce 250 ms; ENTER/Clear siguen inmediatos y sin doble filtro).
    - `game/HelperPanel.java`: reactivada la burbuja + parpadeo de bandeja al pedir feedback con el cliente de fondo (venía comentada de serie) y `stopBlink()` al contestar; limpiados 2 wrappers `new Thread` sobre `invokeLater`.
    - `game/PlayerPanelExt.java` + `game/GamePanel.java`: `clearLifeCache()` llamado en `cleanUp()` (fuga de sesión `playerLives`) y stop de `resizeTimer`. `components/ext/dlg/DialogManager.java`: `ConcurrentHashMap` (DCL roto sobre `HashMap`).
    - `util/gui/countryBox/CountryItemRenderer.java`: colores de selección del L&F + caché de banderas. `chat/ChatPanelBasic.java`: hora fija `HH:mm:ss` (antes dependía del locale del SO).
    - Reescopado tras verificar (NO se tocaron): el F2 con textos traducidos (los roles los fija el cliente; sobrescribirlos sería incorrecto en diálogos de elección) y el "connect en EDT" (el log del smoke muestra que el handshake ya corre en `SwingWorker`).
    - Parche: `parches\2026-11-13-ui-serie-v33.patch`. Evidencia: `LAB-UI-1.4.62-V2\capturas\127c-v33-lobby-lab.png` + `CHECKPOINT-2026-11-13-ui-serie-v33.md`.
    - Desplegado en la instalación el 2026-11-13 (cliente `mage-client-1.4.63.jar` SHA256 `B7235FCA…86C7C`; backup en `xmage\backup-jars-1.4.63-v33-20261113\cliente\`; servidor 17171 sin tocar).

23. UI de serie v34 (2026-11-14) — Fase 1 del informe (8 ficheros, todos verificados de serie):
    - `themes/ThemeType.java`: tokens semánticos derivados (oscuro/claro) sin tocar los presets; temas claros = valores históricos exactos (cero cambio visual).
    - `table/TablesPanel.java` + `table/PlayersChatPanel.java`: asientos, viewport, tinta de filtros, overlay de usuarios y fuente del banner pasan a tokens/`dialogFont`/`chatFont`.
    - `tournament/TournamentPanel.java`: clasificación y partidas ordenables (`MageTableRowSorter`, con fix view↔model en Watch), fuentes escalables, refresco 2-3 s aleatorio.
    - `draft/DraftPanel.java`: 17 Tahoma fijos → `dialogFont`, timer escalable y por tokens de tema.
    - `dialog/ConnectDialog.java`: servidores recientes (MRU 8 en prefs `recentServers`, popup con click-derecho en el campo, guardado al conectar con éxito) + botón Connect con fuente escalable.
    - `components/KeyBindButton.java`: los conflictos de atajo se preguntan en vez de borrar en silencio.
    - `org/mage/plugins/card/images/DownloadPicturesService.java`: aviso de bandeja al terminar descargas largas.
    - Corregido en el informe tras verificar: el Stop de descargas y el progreso YA eran correctos de serie (el TODO del diálogo es código GEN muerto); B2 (mutación ImageCache) era falso positivo.
    - Parche: `parches\2026-11-14-ui-serie-v34.patch`. Evidencia: `LAB-UI-1.4.62-V2\capturas\128-v34-lobby-lab.png` + `CHECKPOINT-2026-11-14-ui-serie-v34.md`.
    - Desplegado en la instalación el 2026-11-14 (cliente `mage-client-1.4.63.jar` SHA256 `E87A1223…47EAC7`; backup en `xmage\backup-jars-1.4.63-v34-20261114\cliente\`; servidor 17171 sin tocar).

24. UI de serie v35 (2026-11-14) — Fase 2 del informe (7 ficheros, +218/−16):
    - **FlatLaf opcional** (`pom.xml` con shade + `GuiDisplayUtil` por reflexión + selector en Preferencias→GUI Theme con Nimbus de default y fallback). FlatLaf va DENTRO del jar cliente (no hay jars nuevos en instalación).
    - **Temas por fichero**: `~/.xmage/theme-overrides.properties` opcional fusionado en `refreshThemeSettings()` (recargado en cada `ThemeType.reload()`).
    - **Opt-ins por flag** sin cambio de default: `-Dxmage.battlefield.optimized=true` (optimized drawing condicional) y `-Dxmage.cache.maxEntries=N` (tope de cachés, default 3000).
    - `MageBook`: caption/info escalan con `dialogFont` (geometría del lienzo intacta a propósito: ligada a los artwork fijos).
    - Bug cazado y corregido en el propio smoke: `FlatLaf dark` se comparaba con `contains("Dark")` (mayúscula) y caía a Light.
    - Smoke doble: ruta Nimbus idéntica a v34 (captura 129) + ruta FlatLaf dark arranca, conecta y pinta oscuro (captura 130; L&F y tema de colores son independientes — para todo oscuro usar FlatLaf dark + Carbon Fiber).
    - Parche: `parches\2026-11-14-ui-serie-v35.patch`. Evidencia: capturas 129/130 + `CHECKPOINT-2026-11-14-ui-serie-v35.md`.

## Disciplina de build y despliegue (2026-11-09)

Aprendido a la dura con el bug `NoSuchMethodError: Token.putOntoBattlefield` (detalle completo
en `CHECKPOINT-2026-11-09-fix-token-api.md`):

- **Para desplegar, SIEMPRE `mvn clean package`** (nunca incremental): el compilador
  incremental recicla `target/classes` viejos cuando los mtimes del delta oficial no están
  alineados y produce jars mixtos (interfaz `Token` API nueva + `CreateTokenEffect`/`Embalm`
  /`Eternalize`/`RoleType` API vieja = crash de partida al crear cualquier token). El comando
  histórico sin `clean` queda PROHIBIDO para despliegues.
- **Los jars mage se despliegan SIEMPRE juntos y del mismo build** (core, common, sets,
  server, player-ai y plugins): nunca un subset. Script oficial:
  `Laboratorio\desplegar-oficial.ps1` (exige servidor parado, crea
  `backup-jars-<version>-<fecha>_<tag>\`, copia 5 lib + 27 plugins).
- Verificación de coherencia post-build y post-despliegue: escáner de constant-pool
  `LAB-UI-1.4.62-V2/capturas/20261109-fix-token-api/scan_putob.py` (compara todas las
  referencias a `Token.putOntoBattlefield` contra los descriptores que declara el core).

## Estado git y sincronización (2026-11-09)

- El remoto `vros01-Kabutosan/XMage-Community-Patch-2` recibe dos repos locales:
  - `XMage-1.4.62V1-src/` (código): ramas `upgrade/official-1.4.62V1` y `upgrade/official-1.4.63V1`
    sincronizadas con GitHub (último commit de código = UI Mod v30).
  - `J:\MTG` (proyecto/docs, rama `main`): sincronizado el 2026-11-09. El `main` histórico del
    remoto (instantánea 2026-09-06) se conservó como ancestro del merge `-s ours`
    (`38bbb2550c7`); el árbol de `main` refleja el disco. La historia se reescribió el mismo día
    para retirar 3 ficheros H2 >100 MB de la era 1.4.61 (SHAs definitivos en el checkpoint
    2026-11-09; cadena original solo local en `backup/main-pre-rewrite-20261109`).
- Bajas registradas en `main` el 2026-11-09: el legado `Proyecto-XMage/` (~122k ficheros ya
  archivados en `_archivo/20261101/`). La historia git conserva todo; detalle en
  `CHECKPOINT-2026-11-09-sync-git.md`.
- Ignorados a partir del 2026-11-09 (no se versionan): `.kilo/` y `Laboratorio/acl-recovery-*/`.
- `Instalacion-XMage/` sigue sin versionarse (binarios pesados); su estado queda documentado en
  los checkpoints y con backups de jars en `Instalacion-XMage\xmage\backup-jars-*`.
