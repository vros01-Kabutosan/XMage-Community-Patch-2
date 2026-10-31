# CHECKPOINT - 2026-10-31 (Migración Oficial XMage 1.4.63V1)

## 1. Resumen Ejecutivo de la Migración

- **Versión upstream oficial:** `xmage_1.4.63V1` (Commit upstream: `79aec614fc2`, 31 de Octubre de 2026).
- **Rama de trabajo del fork:** `upgrade/official-1.4.63V1` (commit `68852b5e00c`).
- **Protocolo aplicado:** Laboratorio aislado (`LAB-UI-1.4.62-V2`, puerto 17191) -> Smoke Test 100% OK -> Backup oficial previo -> Despliegue en `J:\MTG\Instalacion-XMage`. Servidor oficial (puerto 17171) nunca tocado.

---

## 2. Novedades Integradas de la Versión Oficial 1.4.63V1

1. **268 Cartas Nuevas y Sets Actualizados:**
   - Base de datos local ampliada de 589 a 591 sets y de 93.420 a 94.787 cartas indexadas.
   - Sets añadidos/actualizados: `FDN`, `TMT` (Teenage Mutant Ninja Turtles), `TRK` (Star Trek), `FIN` (Final Fantasy), `ECL`, etc.
2. **Sistema de Valoración de Draft de 17Lands:**
   - 34 archivos `.csv` de ratings oficiales integrados en `Mage/src/main/resources/ratings/`.
   - Motor de sugerencias de draft automático y selector para bots (`RatingDraftPickSuggester`, `DraftPickSuggest`).
3. **Mejoras en el Motor y Animaciones:**
   - Refactorización asíncrona de animación de eliminación de permanentes (`Plugins.instance.onRemoveCard(...).thenRunAsync(...)`).
   - Fixes de soporte de descarga de imágenes en Scryfall y Gatherer.

---

## 3. Preservación Íntegra de Todos los Mods de Kabutosan (Solo Sumar, Nunca Restar)

Todos los mods existentes se trasladaron y verificaron sin pérdida ni alteración:

1. **FeedbackPanel.java:** Protección de ancho de chat (`PREFERRED_WIDTH = 1024`).
2. **GamePanel.java:**
   - Barra de fases centrada y compacta bajo la mano.
   - Píldoras de resaltado (amarillo activo, verde skip).
   - Fila de accesos directos F4/F5/F7/F9/F10/F11 junto a CONCEDE.
   - Recuperación y autocuración de divisores.
   - Integrado con la nueva API de cancelación de animaciones upstream `Animation.cancelRunningAnimations(this.gameId)`.
3. **HelperPanel.java:** Superficie de decisión centrada, insignia de turno compacta.
4. **GraveyardCardsDialog.java:** Ventana clon de búsqueda y visualización no modal de cementerio.
5. **MageServerImpl.java / Main.java:** Optimización de inicialización inmediata de tipos de juego (`markGameTypesLoadFinished`).
6. **DragCardGrid.java (v1.6 + v1.7):**
   - Selector moderno de edición con iconos, ordenación y previsualización asíncrona.
   - Barra de tierras básicas rápidas (`{W}`, `{U}`, `{B}`, `{R}`, `{G}`, `{C}`).
   - Atajos de teclado completos (`+`, `-`, `Supr`, `E`, `S`, `Ctrl+A`, `Esc`).
   - Ajuste de cantidad por rueda de ratón (`Ctrl + Wheel` y sobre el contador).
   - Desglose en vivo de la composición del mazo (criaturas, tierras, hechizos).
7. **Diálogos de Soporte UI:** `Cards.java`, `CardArea.java`, `MageDialog.java`, `HoverButton.java`, `KeyboundButton.java`, `CardInfoWindowDialog.java`, `ShowCardsDialog.java`, `DeckDownloaderPane.java`, `AiDeckLibrary.java`.

---

## 4. Evidencia de Laboratorio y Smoke Test (Puerto 17191)

- **Compilación Maven:** 40 módulos compilados exitosamente (`mvn install -DskipTests`).
- **Tests unitarios:** `DragCardGridTest` (3/3 pasados, 0 fallos).
- **Servidor de Laboratorio (PID 36916, puerto 17191):**
  - Sets cargados: 591
  - Cartas cargadas: 94.787
  - Plugins cargados: 17 game types, 21 tourneys, 3 players, 23 cubes, 52 decks (0 errores).
  - Escuchando en `0.0.0.0:17191/?serializationtype=java&maxPoolSize=300`.
- **Cliente de Laboratorio (PID 40384, puerto 17191):**
  - Arrancó en 24s.
  - Conexión y autenticación automática: `Logging: as username Kabutosan to server localhost:17191 -> Logging: DONE`.
- **Parada limpia:** `parar-lab.sh` detuvo los procesos aislados y restauró el puerto original en registro (`17171`).

---

## 5. Despliegue en Instalación Oficial (`J:\MTG\Instalacion-XMage`)

### A. Backup Previo Preservado
- Directorio de respaldo: `J:\MTG\Instalacion-XMage\xmage\backup-jars-1.4.62-pre-1.4.63-20261031\`
- Contiene los jars 1.4.62 completos de cliente y servidor antes del despliegue.

### B. Hashes SHA-256 de los Jars Desplegados

| Jar | Destino | SHA-256 |
|---|---|---|
| `mage-client-1.4.63.jar` | `Instalacion-XMage/xmage/mage-client/lib/` | `ACC3C26BD57A484313DB48BD93AF6A316221A3D11B60BCF41B75BE89CD475B41` |
| `mage-server-1.4.63.jar` | `Instalacion-XMage/xmage/mage-server/lib/` | `6E07169949490C8A2504FC3AA4A7CAC7CD8DB86EFEC29E4B57B4587FEB04638C` |
| `mage-sets-1.4.63.jar` | `Instalacion-XMage/xmage/mage-client/lib/` & `mage-server/lib/` | `57732018718007863645C8D09554C112511671678BE2C0385629E4507C34CDD7` |
| `mage-1.4.63.jar` | `Instalacion-XMage/xmage/mage-client/lib/` & `mage-server/lib/` | `C0D80E53B18F12D75CE2FF5DCEF6146194DAA35E815EBCC9A1D8866160161476` |
| `mage-common-1.4.63.jar` | `Instalacion-XMage/xmage/mage-client/lib/` & `mage-server/lib/` | `26FDDE1E187515F051F1076211C1ED60EEF7208E20E3CA859422DF4A0EE285F5` |
| `mage-player-ai-1.4.63.jar` | `Instalacion-XMage/xmage/mage-server/lib/` | `A8BC7262D9A74E9689408F5254A18D1610EBD26992F7DC57DFD8B5509CE4DF16` |
| `mage-deck-constructed-1.4.63.jar` | `Instalacion-XMage/xmage/mage-client/lib/` & `mage-server/plugins/` | `FBCDAA12CEB00244436B044238BEA8DCDA0472ED38A739C79FD0D6045E163B48` |

- Plugins de servidor: los 27 plugins compilados en 1.4.63 fueron desplegados en `Instalacion-XMage/xmage/mage-server/plugins/`.
- Configuración de servidor: `config.xml` actualizado para referenciar los jars 1.4.63 manteniendo puerto `17171`. Backup previo guardado como `config.xml.backup-1.4.62`.
- Servidor oficial 17171: continuó en ejecución ininterrumpida sin sufrir ninguna alteración.

---

## 6. Sincronización Git y Parches

- **Parche consolidado:** `J:\MTG\Laboratorio\parches\2026-10-31-migracion-1.4.63V1.patch` (2,67 MB).
- **Rama remota:** `origin/upgrade/official-1.4.63V1` (push completado).

---

## 7. Corrección del Aviso de Actualización en el Launcher Oficial

- **Causa raíz:** `J:\MTG\Instalacion-XMage\installed.properties` mantenía la cadena de versión antigua `xmage.version=1.4.62-dev (2026-10-03 20-47)`. El Launcher consultaba `http://xmage.today/config.json` (que reporta `"version": "1.4.63-dev (2026-10-31 09-42)"`) y detectaba una discrepancia, solicitando actualización pese a estar ya migrado.
- **Acción realizada:**
  1. Respaldo de seguridad creado: `installed.properties.backup-1.4.62`.
  2. `installed.properties` actualizado a `xmage.version=1.4.63-dev (2026-10-31 09-42)`.
  3. Eliminación de `.jar` 1.4.62 residuales en `mage-client/lib/` que interferían por orden alfabético con los 1.4.63 (los respaldos permanecen seguros en `backup-jars-1.4.62-pre-1.4.63-20261031`).
- **Verificación:** El Launcher muestra "Up to date" y "Launch Client" directamente; el cliente inicia mostrando `Starting MAGE CLIENT version: 1.4.63-V1 (build: 2026-10-31 13:54)`.

---

## 8. Modernización de Descargadores Python de Decks (Autonomía 100%)

- **Diagnóstico:** De los 3 descargadores (`mtgo_3_formatos_fusionado.py`, `mtgtop8_3_formatos.py`, `goldfish_3_formatos.py`), MTGO y MTGTop8 ya funcionaban por HTTP directo sin navegador. Solo `goldfish_3_formatos.py` utilizaba Selenium y `undetected_chromedriver`, abriendo ventanas de Chrome e interrumpiendo el flujo con llamadas a `input("... pulsa ENTER")`.
- **Solución implementada:**
  1. Se identificó la llamada AJAX interna de MTGGoldfish `/widgets/deck/js?deckId={did}` con encabezados `Referer` y `X-Requested-With`, que entrega el payload completo del mazo (`deck_input[deck]`) con código HTTP 200 directo sin requerir emulación de navegador.
  2. Se reescribió `goldfish_3_formatos.py` eliminando completamente dependencias de Selenium, ChromeDriver y llamadas bloqueantes a consola.
  3. Respaldo previo creado: `goldfish_3_formatos.py.bak-selenium-20261031`.
  4. Sincronización a `fuente-xmage/config/deck-downloader/goldfish_3_formatos.py`.
- **Resultados de prueba:**
  - Descarga autónoma de 25 mazos Standard, 25 Pioneer y 25 Modern (75 mazos totales 60+15).
  - Generación de 75 archivos `.dck` para Forge y 75 archivos `.txt` para XMage.
  - Ejecución verificada e integrada con `deck_library_updater.py --source MTGGoldfish` (0 errores, resolución Scryfall por lotes, incorporación a `sample-decks/Descargados`).

---

## 9. Sincronización y Puesta en Marcha del Servidor Oficial (1.4.63-V1)

- **Causa del aviso de versión en cliente:** El cliente 1.4.63-V1 intentaba conectar al servidor oficial en el puerto 17171, que seguía corriendo en memoria el proceso antiguo 1.4.62 (PID 35328), generando `MageVersionException: Wrong client version`.
- **Acciones ejecutadas con autorización:**
  1. Cierre ordenado del proceso servidor 1.4.62 (PID 35328).
  2. Eliminación de `.jar` residuales 1.4.62 en `mage-server/lib/` y `mage-server/plugins/` (respaldados previamente en `backup-jars-1.4.62-pre-1.4.63-20261031`).
  3. Creación del script de arranque actualizado `startServer-1.4.63.bat`.
  4. Puesta en marcha del servidor oficial con JRE 1.8.0_201 en el puerto 17171.
  5. Carga e indexación completa verificada en `mageserver.log`:
     - `Starting MAGE SERVER version: 1.4.63-V1 (build: 2026-10-31 13:52)`
     - 591 sets y 94.787 cartas indexadas en BD H2.
     - 17 game types, 21 torneos, 23 cubos y 52 mazos cargados.
     - Servidor escuchando y listo en `0.0.0.0:17171/?serializationtype=java&maxPoolSize=300`.



