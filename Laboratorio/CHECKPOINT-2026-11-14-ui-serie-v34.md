# CHECKPOINT 2026-11-14 — UI de serie v34 (Fase 1 del informe: tokens, fuentes, orden y detalles)

**Petición del usuario (Kabutosan):** "continúa con todas las fases respetando los protocolos".
Este checkpoint aplica la **Fase 1** del `INFORME-MODERNIZACION-UI-2026-11-13.md` (v33 fue la Fase 0).
Todo vuelve a verificarse contra el árbol oficial `xmage_1.4.63V1` antes de tocar: los 8 ficheros
afectados están sin cambios de fork en las líneas modificadas (DraftPanel/ConnectDialog/KeyBindButton/
TournamentPanel/PlayersChatPanel no están en los 48 ficheros tocados; ThemeType solo tenía diferencias
de whitespace; TablesPanel solo las líneas v33; DownloadPicturesService intacto).

## Cambios aplicados (8 ficheros, +210/−55)

| # | Fichero | Cambio | Efecto |
|---|---|---|---|
| 1 | `themes/ThemeType.java` | tokens semánticos derivados (`isDarkTheme`, `getSeatAvailable*`, `getLobbyViewportTint`, `getFilterBarInk`, `getOverlayBackground/Foreground`, `getTimerDanger/WarningColor`). Sin tocar los enum-constants: los temas claros devuelven los valores históricos exactos | cero cambio visual con temas claros; los temas oscuros reciben equivalentes legibles |
| 2 | `table/TablesPanel.java` | verde de asientos, viewport, tinta de filtros y `jPanelTop` pasan a tokens; banner "Message of the Day" usa `dialogFont` bold | tema-consistente; texto escalable |
| 3 | `table/PlayersChatPanel.java` | fondo/foreground del overlay de usuarios y del pane "System" por tokens; fuente Arial 14 fija → `chatFont` | idem |
| 4 | `tournament/TournamentPanel.java` | `MageTableRowSorter` en clasificación y partidas (con fix view↔model en la acción Watch), fuentes Tahoma/Arial → `dialogFont`, refresco 2 s → 2-3 s aleatorio (como el lobby) | ordenable por puntos; sin olas de peticiones simultáneas |
| 5 | `draft/DraftPanel.java` | 17× Tahoma 10 → `dialogFont`; timer Tahoma 36 → fuente escalable (dialogFont×2.2, mín 24); rojo/naranja del timer por tokens | draft legible a cualquier escala |
| 6 | `dialog/ConnectDialog.java` | **servidores recientes** (TODO oficial): se guardan al conectar con éxito (MRU, máx 8, clave `recentServers`) y se ofrecen en popup con click-derecho sobre el campo del servidor (sin tocar el layout); botón Connect con `dialogFont` bold | conectar en 2 clics |
| 7 | `components/KeyBindButton.java` | los conflictos de atajo ahora **preguntan** (qué acción ocupa la tecla) en vez de borrar en silencio | rebind seguro |
| 8 | `org/mage/plugins/card/images/DownloadPicturesService.java` | aviso de bandeja + parpadeo al terminar una descarga larga de imágenes (TODO oficial) | fin de descarga visible |

## Correcciones al informe tras verificar (no se tocó nada de esto)

- **Stop de descargas**: ya funcionaba — el servicio engancha `doStopAndClose()` al botón (línea 315
  del servicio); el TODO del `DownloadImagesDialog` es código GEN muerto. El informe v1/v2 pedía
  implementar lo que ya existe.
- **Progreso ilegible**: el string kilométrico era el placeholder de diseño del `.form`; el runtime
  pinta "%d of %d … [MB left]" con barra real. Sin nada que arreglar.
- **F2/connect-EDT**: reconfirmado en el smoke de hoy (`Connect: connecting … =>[SwingWorker-pool-4-thread-3]`).
- **B2 (mutación ImageCache)**: falso positivo — la clave de la cache ya incluye el tamaño, la
  mutación no afecta a otros tamaños.

## Build

`mvn -B -DskipTests -pl Mage.Client clean package` → BUILD SUCCESS (26 s) tras arreglar un import
falta (`PreferencesDialog` en PlayersChatPanel). Solo cliente (ningún fichero de servidor tocado).

## Verificación en laboratorio (LAB-UI-1.4.62-V2, puerto 17191, prefs `XMageLabUIv2`)

- `montar-lab.sh` OK; arranque con `Start-Process` (scratchpad `launch-lab.ps1`).
- Servidor escuchando a los 15 s; `KabutosanLab connected to server with restored session`;
  `Connect: DONE`; cero excepciones en err.log (solo el warning conocido de JavaSoft prefs).
- Captura **`capturas/128-v34-lobby-lab.png`**: título `Client: 1.4.63-V1 (build: 2026-11-14 12:39)`
  (jar v34), lobby idéntico a v33 con el tema claro — confirma el "cero cambio visual" de los tokens.
- Probes `javap` sobre el jar del lab: tokens presentes en `ThemeType`; `setRowSorter`×2 en
  `TournamentPanel`; `readRecentServers/rememberRecentServer/initRecentServersPopup` en
  `ConnectDialog`; `JOptionPane.showConfirmDialog` en `KeyBindButton.applyNewKeycode`;
  `MageTray.displayMessage/blink` en `DownloadPicturesService.finishDownloadInSwingThread`;
  `getTimerDangerColor/getTimerWarningColor` en `DraftPanel`.
- `parar-lab.sh` (solo los 2 PIDs del lab; servidor oficial 17171 intacto).

## Pruebas manuales pendientes

- Torneo: clicar cabeceras de clasificación para ordenar por puntos; botón Watch sigue funcionando
  con la tabla ordenada (fix view↔model).
- Connect: conectar a un servidor, cerrar, reabrir el diálogo y click-derecho en el campo servidor.
- Preferencias→Hotkeys: reasignar F4 a donde ya está F5 → debe preguntar.
- Draft con tema oscuro (Carbon Fiber): timer rojo/naranja legibles.

## Parche

`Laboratorio\parches\2026-11-14-ui-serie-v34.patch` (8 ficheros, +210/−55).

## Despliegue

Pendiente de confirmación (mismo procedimiento que v33: cerrar cliente oficial, backup
`backup-jars-1.4.63-v34-<fecha>\cliente\`, copiar jar, servidor intacto).

## Rollback

`git apply -R Laboratorio\parches\2026-11-14-ui-serie-v34.patch` + rebuild; instalación desde el backup.
