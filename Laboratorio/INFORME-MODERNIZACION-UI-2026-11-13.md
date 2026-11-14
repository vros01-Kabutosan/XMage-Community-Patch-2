# INFORME TÉCNICO v3 — UI DE SERIE: qué se tocaría y cómo quedaría

**Proyecto:** XMage (fork Kabutosan, rama `upgrade/official-1.4.63V1`) · **Módulo:** `Mage.Client`
**Fecha:** 13/11/2026 · **Alcance:** solo investigación — **ningún archivo modificado**
**Regla:** NO se cuenta nada que la fork ya modernizado (campo, fases, pila, decisiones, cementerio,
editor v1.7, descargador, marcador de match…). Solo **XMage de serie** verificado contra el tag
oficial `xmage_1.4.63V1` (48/293 ficheros del cliente difieren del oficial → 84 % de la UI es 100 % de serie).

Cada mejora lleva: **Se toca** (ficheros concretos + tamaño del cambio) y **Quedaría** (resultado
visible/funcional). Riesgo: ● bajo · ●● medio · Nada se activa sin smoke en laboratorio.

---

## Resumen: ficheros que se tocarían (21 de 293)

| Fase | Fichero | Cambios | Tamaño |
|---|---|---|---|
| 0 | `util/TransformedImageCache.java` | B1 escalado con hints | S (8 líneas) |
| 0 | `game/FeedbackPanel.java` | E4 fix F2 por actionCommand | S |
| 0 | `game/HelperPanel.java` | E4 reactivar blink (línea comentada de serie) | S |
| 0 | `arcane/ManaSymbols.java` + `arcane/SvgUtils.java` | B3 precarga + caché CSS | M |
| 0 | `game/PlayerPanelExt.java` | D3 remove playerLives | S |
| 0 | `util/ImageHelper.java` | D3 cache soft | S |
| 0 | `components/ext/dlg/DialogManager.java` | D3 ConcurrentHashMap | S |
| 0 | `game/GamePanel.java` | D3 stop resizeTimer en cleanUp | S |
| 0 | `util/gui/countryBox/CountryItemRenderer.java` | E5 renderer de tema | S |
| 0 | `chat/ChatPanelBasic.java` | E5 hora HH:mm | S |
| 0 | `deckeditor/CardSelector.java` | E2 búsqueda incremental + sorter | M |
| 0 | `table/TablesPanel.java` | E1 tooltips por celda | S |
| 1 | `MageFrame.java` | D1 connect en worker · E6 combo mazos | M |
| 1 | `themes/ThemeType.java` + pintores de serie | A3 tokens de color | L (aditivo) |
| 1 | diálogos de serie (Connect, Tournament, Draft, TablePlayer…) | C1 fuentes + Tahoma fuera | M |
| 1 | recursos `fonts/*.ttf` + `ModernCardResourceLoader` | C1 fuente empaquetada | S |
| 1 | `components/KeyBindButton.java` | E4 conflictos + multi-modificador | M |
| 1 | `dialog/DownloadImagesDialog.java` | E5 Stop + progreso + aviso fin | M |
| 1 | `dialog/ConnectDialog.java` | E5 servidores recientes + validación inline | M |
| 1 | `dialog/PreferencesDialog.java` | E5 buscador de opciones + iconos pestañas | M |
| 1 | `tournament/TournamentPanel.java` | E3 RowSorter en clasificación | S |
| 2 | `util/gui/GuiDisplayUtil.java` + `pom.xml` | A1 FlatLaf opcional | L (con switch y fallback) |
| 2 | `game/BattlefieldPanel.java` | B5 optimized drawing condicional | M |
| 2 | `util/SoftValuesLoadingCache.java` | B4 techo en MB | S |
| 2 | `cards/ImageCache.java` | B2 sin mutar entry | S |
| 2 | `dialog/MageDialog.java` + `components/MageUI.java` | D2 CompletableFuture | M |
| 2 | `MageFrame.java`, `mage/client/table/*`, `collection/viewer/*` | C2/E7 escala real + libro theme-aware | L |

---

# BLOQUE A — Look & Feel y temas

### A1 · FlatLaf opcional con Nimbus intacto de default — Riesgo ●● · Fase 2
- **Se toca:** `GuiDisplayUtil.java:430-457` (seleccionar L&F por preferencia nueva, try/catch →
  fallback Nimbus), `ThemeType.reload()` (mapear claves `nimbus*` → claves FlatLaf 1:1),
  `PreferencesDialog` (selector), `pom.xml` (+1 dependencia Apache-2.0). Después, retirar
  `MageSynthStyleFactory`, `TranslucentSynthSytle`, painter `MageJDesktop` y `ColorPane` (4 workarounds
  solo si el L&F nuevo está estable).
- **Quedaría:** toda la UI de serie (diálogos, tablas, preferencias, conexión, torneo, draft, libro)
  con aspecto actual plano y **modo oscuro real** (`FlatDarkTheme`) con conmutador en preferencias;
  los `JInternalFrame` translúcidos dejarían de necesitar parches. Máquina con problema → un clic a
  Nimbus y todo vuelve a ser exactamente como hoy.

### A2 · Temas por fichero — Riesgo ● · Fase 2
- **Se toca:** `ThemeType.java` (añadir carga opcional de properties/JSON desde `~/.xmage/themes/`
  fusionada en `reload()`; el enum queda como presets).
- **Quedaría:** tema nuevo o tweak propio sin recompilar (compartible entre instalaciones); sin
  fichero, comportamiento idéntico al actual.

### A3 · Tokens semánticos para los colores hardcodeados de serie — Riesgo ● · Fase 1
- **Se toca:** `ThemeType` (interfaz `ThemePalette` con ~10 tokens) y sustitución puntual en
  ficheros de serie: `TablesPanel.java:254-257,401,1076,1098` (verde fosforito asientos, viewport
  blanco, toolbar azul), `PlayersChatPanel.java:74-94` (blanco puro sobre negro),
  `components/ability/MageScrollbarUI.java:62,131-133` (barra Metal negra → seguir el tema),
  `MageBook.java:524` + `CollectionViewerPanel.java:64-178` (avisos y `Color.white`),
  `MageRoundPane.java:23-25`, y **unificación de la rareza hoy triple** (`CardRenderer.java:414`,
  HTML en `GuiDisplayUtil.java:309`, renderers Modern/Retro → una sola paleta).
- **Quedaría:** mismo look el día 1 (los defaults son los colores actuales) pero al elegir tema
  oscuro o claro, lobby, chat de jugadores, libro, tooltips y barra de稀有 rarezas se tiñen solos;
  cambiar el color de «rare» se hace en un sitio.

---

# BLOQUE B — Pipeline de imagen (100 % de serie)

### B1 · `getScaledInstance(SCALE_SMOOTH)` → `Graphics2D` + interpolación — Riesgo ● · Fase 0 ★ el cambio estrella
- **Se toca:** `TransformedImageCache.java:98-105` (método `resizeImage`, ~8 líneas) y el mismo patrón
  en `MageScrollButton.java:37-40`, `TablesPanel.java:615-639` (iconos lobby) e
  `ImageManagerImpl.java:450,471`. Las cachés no se tocan.
- **Quedaría:** cartas al zoom **nítidas y sin el ghosting/apantallado** que produce el algoritmo de
  los 90; textos de cartas pequeñas legibles; sin el micro-corte del primer repintado (el trabajo
  perezoso de AWT caía a veces en EDT). Sin banderas nuevas ni dependencias.

### B2 · No mutar entradas cacheadas — Riesgo ● · Fase 1
- **Se toca:** `ImageCache.java:317-333,345-361` (devolver la transformada sin `data.setImage(...)`).
- **Quedaría:** fin del artefacto posible «imagen ya reducida re-reducida» al cambiar tamaños de mesa
  (calidad constante entre zoom in/out) y线程-safe con el pool de precarga.

### B3 · SVG Batik: quitar disco y raster del hilo de pintura — Riesgo ● · Fase 0
- **Se toca:** `ManaSymbols.java:420` (precargar al cambiar prefs los tamaños que usa
  `GUISizeHelper`, no on-demand en `draw()`), `SvgUtils.java:96-215` (cachear el `data:URI` del CSS
  por (css, settings) en vez de escribir/leer fichero temporal cada carga).
- **Quedaría:** al agrandar la mesa, cambiar tema o abrir unAbilityPicker con símbolos nuevos,
  **cero tirones**: ya no se rasteriza ni se toca el disco en medio del repintado.

### B4 · Cachés con techo en MB — Riesgo ● · Fase 2
- **Se toca:** `SoftValuesLoadingCache.java:20-26` (Weigher de Guava por bytes, máximo tipo
  `-Xmx`×0.25 configurable). Las 8 cachés se benefician sin tocar sus llamadas.
- **Quedaría:** RAM estable en sesiones largas con muchos sets pesados; adiós al patrón de serie de
  «capturar OutOfMemoryError y rezar».

### B5 · `isOptimizedDrawingEnabled()==false` → condicional — Riesgo ●● · Fase 2
- **Se toca:** `BattlefieldPanel.java:387-388` (devolver `true` cuando no haya subcapas solapadas —
  la fork ya redujo los repaints inútiles; esto recorta el coste de los que quedan).
- **Quedaría:** partidas grandes (EDH, 40+ tokens, auras) con **menos CPU por repintado**; animaciones
  de ataque/bloqueo más fluidas en equipos modestos. Verificar z-order de iconos en laboratorio.

---

# BLOQUE C — Fuentes, iconos y DPI

### C1 · Fuente empaquetada + fuera Tahoma/Arial fijos — Riesgo ● · Fase 1
- **Se toca:** `resources/fonts/inter-*.ttf` (nuevo, licencia SIL) usando
  `ModernCardResourceLoader.loadFont()` (ya escrito, sin usar); ~30 sustituciones mecánicas de
  `new Font("Tahoma"…)` → `GUISizeHelper.dialogFont` en ficheros de serie: `ConnectDialog.java:241`,
  `TablesPanel.java:1662`, `TablePlayerPanel.java:105`, `TournamentPanel.java:321,368-381`,
  `DraftPanel.java:728,824-985`, `PreferencesDialog.java:2850`; y `MageFrame.java:1073`
  «Segoe UI 48» → fuente themed.
- **Quedaría:** idéntico render de texto en Windows/Linux/macOS (hoy Arial/Tahoma/Segoe no existen
  fuera de Windows y cada SO improvisa un fallback); **todo el texto de diálogos respeta por fin el
  tamaño de GUI configurado** (hoy Tahoma ignora el escalado: ilegible en 4K, diminuto en portátiles).

### C2 · Escala real de pantalla + iconos SVG de UI — Riesgo ●● · Fase 2
- **Se toca:** `PreferencesDialog.findBestPreset():417-468` (leer
  `getDefaultTransform().getScaleX()` en lugar del `getScreenResolution()` que siempre da 96);
  conversión progresiva de `resources/menu|buttons` (PNGs a 1 resolución) a SVG reutilizando el
  pipeline Batik ya usado en iconos de carta; `MageFrame.java:1052-1054` preferred size de toolbar
  derivado de escala.
- **Quedaría:** elegir sola la escala buena en 125/150/175 % (portátiles actuales) sin iconos
  borrosos ni re-muestreados; toolbar y botones proporcionales en cualquier pantalla.

---

# BLOQUE D — Threading y memoria

### D1 · `performConnect` fuera del EDT — Riesgo ● · Fase 1 ★ el fix más notorio
- **Se toca:** `MageFrame.java` rutas de login (`:925`/`:1709` rama) — mover
  `SessionHandler.connect()` (TCP+SSL+login, segundos) a worker con `invokeLater` del resultado y
  botón «Cancelar» real en el diálogo.
- **Quedaría:** al conectar o reconectar, **la ventana deja de congelarse**: se puede mover,
  cancelar, y se ve progreso en lugar del fantasma «Application not responding» de Windows.

### D2 · Modalidad y esperas bloqueantes — Riesgo ● · Fase 1-2
- **Se toca:** `MageDialog.java:155-157` (modalidad basada en `wait()` sobre EDT) y
  `MageUI.java:79-103` (`getComponent()` con `wait()`).
- **Quedaría:** dos ventanas modales solapadas ya no pueden bloquear el cliente; desaparece el
  deadlock latente de registro de componentes.

### D3 · Fugas y races de 1-5 líneas — Riesgo ● · Fase 0
- **Se toca y queda:**
  | Qué | Fichero | Quedaría |
  |---|---|---|
  | `playerLives` sin remove | `PlayerPanelExt.java:95` | RAM plana tras 50 partidas |
  | `images`/`backgrounds` sin eviction | `ImageHelper.java:28-29` | fondos de tema no se acumulan toda la sesión |
  | DCL sobre HashMap | `DialogManager.java:24-39` | fin del race al abrir el primer diálogo por partida |
  | `resizeTimer` sin stop | `GamePanel.cleanUp()` | sin callback fantasma tras cerrar la partida |
  | `foundErrors` sin límite | `EDTExceptionHandler.java:18` | cap de 200 entradas |
  | AWTEventListener sin remover | `MageFrame.java:302` | limpieza correcta al cerrar |

---

# BLOQUE E — UX de serie en las pantallas de uso diario

### E1 · Lobby de mesas — Riesgo ● · Fase 0-1 ★ (la pantalla más usada tras el juego)
- **Se toca:** `TablesPanel.java` (colores → tokens A3: verde `156,240,146` `:254-257`, viewport
  `:401,1076`, tinta toolbar `:1098`, Tahoma banner `:1662`; tooltips por celda en los renderers
  `:167-243`; iconos SVG en los ~30 toggles `:1105-1369`), `TablesTableModel.java` (ocultar por
  defecto «Min Rating/Quit %/Deck Type» con menú «mostrar columnas»; booleanos reales en vez de
  `"YES"`), `TablesButtonColumn.java:62-68` (Join=verde, Watch=azul, Replay=gris + icono),
  `util/gui/TableUtil.java:26-44` (filtros persistidos por actionCommand, no por posición),
  `PlayersChatPanel.java` (color de serie desde tema).
- **Quedaría:** lobby limpio y coherente con el tema (claro u oscuro), botones de acción reconocibles
  de un vistazo, celdas con tooltip («creada hace X por Y, formato, rating»), caja de filtro rápido
  de texto libre sobre mesas además de los toggles, configs de columnas que sobreviven a
  reordenaciones. La lista de jugadores deja de ser texto blanco sobre negro fijo.

### E2 · Deck builder y gestión de mazos — Riesgo ● · Fase 0-1
- **Se toca:** `deckeditor/CardSelector.java:98` (buscar **mientras se escribe** con debounce ~200 ms,
  no solo con ENTER) + montar `MageTableRowSorter` (ya existe en el repo) en `mainTable`;
  `MageFrame` combo de mazos con buscador (`FastSearchUtil` existente); diálogo nuevo «Manage decks»
  (tabla crear/duplicar/renombrar/borrar/importar/exportar); extender el patrón de buscador de la
  fork (cementerio/biblioteca) al resto de ventanas de zona (TODO `CardArea.java:40`).
- **Quedaría:** escribir «shrouded» y ver la lista filtrarse al instante; clicar cabecera CMC/rareza
  para ordenar; encontrar un mazo entre 200 escribiendo en el combo; exportar/importar sin cazar
  menús contextuales.

### E3 · Torneo y draft — Riesgo ● · Fase 1
- **Se toca:** `TournamentPanel.java` (`MageTableRowSorter` en `tablePlayers`/`tableMatches` — hoy no
  ordenan; campos `JTextField`+`EtchedBorder` `:325-355` → labels themed; Tahoma → dialogFont;
  refresco `:616` con randomize como el lobby), `DraftPanel.java` (16 labels Tahoma `:824-985` →
  dialogFont + **avatar reutilizando `MyAvatar`**; timer Tahoma 36 `:728` → fuente escalable y color
  del tema; mesa `BevelBorder.RAISED` `:904-906` → themed).
- **Quedaría:** clasificación de torneo ordenable por puntos/rival con un clic; draft donde se ve
  quién pasa qué (avatar + nombre legible a cualquier escala); el timer en ámbar/rojo del tema en vez
  de rojo/quemado fijos.

### E4 · Teclado y confirmaciones — Riesgo ● · Fase 0-1
- **Se toca:** `FeedbackPanel.java:350-355` (fix F2: comparar `actionCommand`/`FeedbackMode`, no
  `getText()` en inglés), `HelperPanel.java:110-111` (descomentar y testear el blink/balloon oficial
  que la fork no tocó), `KeyBindButton.java:41-57` (conflicto → preguntar en vez de borrar en
  silencio; multi-modificador), scoping de los `WHEN_IN_FOCUSED_WINDOW` de juego (TODO oficial
  `AbilityPicker.java:436/521` «F9 works from lobby»), `DragCardGrid.java:1570,1689` (usar el diálogo
  de usuario themed en vez de `JOptionPane ==1`).
- **Quedaría:** F2 confirma con servidores en español; F4-F11 dejan de colarse mientras navegas el
  lobby; rebinding sin perder secretos otra tecla; «te toca decidir» también parpadea en la bandeja
  si la ventana está de fondo; confirmaciones con el aspecto del cliente, no de ventana nativa suelta.

### E5 · Diálogos de serie (conexión, descargas, preferencias, bandera, chat) — Riesgo ● · Fase 0-1
- **Se toca:** `ConnectDialog.java` (dropdown de **servidores recientes** — el TODO oficial `:138` —
  validación inline en rojo+tooltip en vez de 4 `JOptionPane` `:570-583`, quitar/rehabilitar botones
  muertos Main/US/Other/Check, centrar en pantalla), `DownloadImagesDialog.java` (**Stop real**
  (TODO `:434`), progreso en «1 234/5 678 · 34 % · ETA» legible en vez del string kilométrico `:391`,
  **aviso sonoro+bandeja al terminar**, retirar u ocultar la pestaña Custom muerta),
  `PreferencesDialog.java` (buscador de opciones sobre las 9 pestañas + iconos de pestaña con el
  patrón que ya usa el descargador), `CountryItemRenderer.java:36-55` (heredar
  `DefaultListCellRenderer` — azul/amarillo de 2004 fuera), `ChatPanelBasic.java:31` (hora `HH:mm`
  explícita — hoy cambia según el locale del SO).
- **Quedaría:** arrancar = conectar en 2 clics al servidor de siempre sin popups; ver cuánto falta
  real de una descarga y poder cortarla; encontrar «mostrar PT en cartas» escribiendo en preferencias;
  la hora del chat igual en todas tus máquinas.

### E6 · Drag & drop export — Riesgo ● · Fase 1
- **Se toca:** `TransferHandler` de export en `CardSelector`/`DragCardGrid` (listas como texto .dck
  / copia al portapapeles).
- **Quedaría:** arrastrar tu mazo desde XMage al escritorio (fichero .dck), al Discord o al block de
  notas sin pasar por el diálogo Exportar. (El drop-in de ficheros ya funciona.)

### E7 · Colección «libro» — Riesgo ●● · Fase 2
- **Se toca:** `MageBook.java:648-662,524` (lienzo fijo 950×700/1250×900 → escala por
  `GUISizeHelper`; `Color(150,0,0)` → errorForeground), `CollectionViewerPanel.java:64-178`
  (`Color.white` → themed), indicador «página X/Y».
- **Quedaría:** el libro se sigue pareciendo a un libro (metáfora de serie intacta) pero legible en
  4K y en tema oscuro, con las páginas navegables visualmente.

---

# BLOQUE F — Accesibilidad (todo de serie)

- **Se toca:** `TablesPanel.java:1105-1369` y `FeedbackPanel` (`setFocusable(false)` retirado o
  borde de foco propio — el patrón correcto ya existe en la fork: `RoundedDecisionButton`),
  `GUISizeHelper.java:32` (`MIN_FONT_SIZE` 7 → 10), contrastes `PlayerPanelExt.java:61` y gris
  `#646464` de `DragCardGrid` → tokens A3, rareza/rated con icono o texto además de color
  (`GuiDisplayUtil.java:309`).
- **Quedaría:** Tab/Shift+Tab navegan el lobby viéndose dónde estás; ninguna fuente por debajo de
  ~10 px; textos AA-compliant en los dos temas; daltónicos distinguen rareza sin depender del tono.

---

# Cómo quedaría la app en conjunto (recorrido)

**Arrancar** → ventana que no se congela al conectar (D1), selector de servidor en 2 clics (E5),
bandera con combo decente (E5). **Lobby** → tablas themed, botones Join con color e icono, filtro
rápido, tooltips por celda (E1). **Construir mazo** → búsqueda instantánea, columnas ordenables,
mazos gestionables y arrastrables fuera (E2/E6). **Jugar** → sin cambios visuales (la fork ya lo
modernizó); por debajo: zoom nítido (B1), menos CPU en mesas grandes (B5), RAM estable (B4/D3),
F2 que responde en español (E4) y aviso visual si te toca decidir (E4). **Torneo/draft** → orden por
puntos, avatares, texto escalable (E3/C1). **Descargar sets** → progreso legible, cancelable, avisa
al acabar (E5). **Temas** → mismo catálogo actual por defecto; opcionalmente FlatLaf claro/oscuro
con un switch y Nimbus de red de seguridad (A1/A2/A3). Todo reversible, todo verificado de serie,
nada del trabajo previo de la fork se repite ni se pisa.

---

*v3 — añade «se toca / quedaría» a cada hallazgo del v2 (escopo solo-DE-SERIE por diff vs tag oficial
`xmage_1.4.63V1` + blame + verificación en árbol oficial). Ningún archivo, jar ni preferencia modificado.*
