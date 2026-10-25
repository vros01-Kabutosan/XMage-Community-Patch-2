# CHECKPOINT UI 1.4.62 V2 - 2026-10-25 02:58

Pausa pedida por Kabutosan ("mañana seguimos"). Retomar por el punto 5.

## 1. Estado del laboratorio ahora mismo

| Proceso | PID | Puerto / HWND | Nota |
|---|---|---|---|
| Servidor OFICIAL (NO TOCAR) | 33968 | 17171 | Sigue intacto, no se ha reiniciado |
| Servidor laboratorio | 39764 | 17191 | Activo |
| Cliente laboratorio | 55768 | hwnd 2426976 | Activo, partida turno 1 Upkeep, build de diagnóstico |

- Cliente lanzado con: `-Djava.util.prefs.userRoot=/Software/XMageLabUIv2 -Dxcp.layout.dump=true -Dxcp.debug.graveyard=true`.
- Jar del laboratorio: `LAB-UI-1.4.62-V2/cliente/lib/mage-client-1.4.62.jar` (build de diagnóstico, NO es el build final).
- Logs: `cliente-lab.log` (los anteriores rotados como `cliente-lab.log.prev*`).
- Capturas nuevas: `capturas/31-cliente-reiniciado.png`, `32-newmatch.png`, `32b-derecha.png`, `33-layout-arreglado.png`.

## 2. Cambios aplicados en esta sesión (fuente en Laboratorio)

1. `Mage.Client/.../game/FeedbackPanel.java`
   - `PREFERRED_WIDTH = 1024` sustituye al ancho preferido `Short.MAX_VALUE` en `refreshLayoutSize()`.
   - Se mantiene máximo `Short.MAX_VALUE`, mínimo ancho 0.
2. `Mage.Client/.../game/GamePanel.java`
   - `setGUISize()`: `feedbackPanel` y `pnlShortCuts` ya no fijan 32767 de ancho preferido; usan `FeedbackPanel.PREFERRED_WIDTH`.
   - `restoreSplitter()`: ahora espera a que el splitter tenga ancho (>0, máx. 25 reintentos), relaja mínimos si el divisor queda pinzado y reaplica una segunda vez.
   - Nuevo `normalizeSplitterLocation()`: ignora divisores guardados degenerados (un lado con menos del 35% de la proporción esperada) y usa la proporción por defecto.
3. `Mage.Client/.../game/HelperPanel.java`
   - `getRequiredHeight()` limitado a un tercio de la altura de pantalla (mínimo 120 px).
4. Instrumentación TEMPORAL (quitar antes del build final):
   - `xcpDebugGraveyardOpened`, bloque `XCPGRAVE` en el timer y condición `xcp.debug.graveyard` en `GamePanel.java`.
   - `d()` ampliado con `min=`, dumps `fb=` y `battlefield=`.
   - Todo el bloque `xcpStartDebugLayoutDump`/`XCPDUMP` es solo diagnóstico.

## 3. Evidencia de verificación (antes / después)

- Antes: `splitBC.right = 102x1988`, `div=3128`, chat ilegible.
- Preferencia guardada que causaba el pinzamiento: `gamepanel/Divider/Locations/Battlefield/And/Chats = {"3840_x_1988":3128}`.
- Log del arreglo: `Ignoring degenerate saved divider 3128 (3248 px wide, right proportion 0.0314...)`.
- Después (XCPDUMP 02:55): `div=2598`, `splitBC.right = 632x1988` (proporción 0.8 correcta).
- Captura `33-layout-arreglado.png`: chat ancho y legible, barra de fases debajo de la mano (Upkeep resaltado), superficie de decisión centrada con botón Done y badge "Turn 1 - Kabutosan".
- Ventana de cementerio abierta por el trigger: "Kabutosan's graveyard (0) - 0 card types", usando `CardArea` + `MageDialog` y la misma fórmula `pack + clamp + keepInsideDesktop` que `ShowCardsDialog` (ventana de elección/búsqueda en biblioteca).

## 4. Qué queda validado y qué no

- [x] Chat ya no se aplasta (632 px, legible).
- [x] Fases debajo de la mano.
- [x] La ventana de cementerio abre, se centra y usa el clon de la ventana de búsqueda.
- [ ] Ver la ventana de cementerio CON cartas reales (la automatización de clics dentro del campo de juego no registra; hover y menús sí funcionan).
- [ ] Confirmar que al cerrar el cliente de forma limpia la preferencia quede guardada como 2598 (si se mata el proceso sigue 3128, pero la autocuración la ignora igual).
- [ ] Quitar todo el código XCP (dumps + trigger cementerio) y recompilar.
- [ ] Repetir smoke final sin flags de diagnóstico.
- [ ] Tarea #7: endurecer `waitForGameTypesLoaded` (MageServerImpl:975-996) para no bloquear 180 s.
- [ ] Desplegar a `J:\MTG\Instalacion-XMage` con backup previo, limpiar/ordenar Laboratorio, sincronizar git y documentar.

## 5. Cómo retomar mañana

```bash
# 1) Comprobar procesos (el 33968 es el servidor oficial: NO tocarlo)
powershell -NoProfile -Command "Get-Process java | Select-Object Id,StartTime,MainWindowHandle"

# 2) Recompilar el cliente
cd "J:/MTG/Laboratorio/XMage-1.4.62V1-src"
C:/Users/vros0/apache-maven-3.9.6/bin/mvn.cmd -B -q -DskipTests -pl Mage.Client package

# 3) Copiar SOLO al laboratorio y relanzar cliente (servidor 17191 puede seguir vivo)
cp Mage.Client/target/mage-client-1.4.62.jar "J:/MTG/Laboratorio/LAB-UI-1.4.62-V2/cliente/lib/"
# lanzar con los flags que hagan falta (ver lanzar-lab.sh)

# 4) Ver diagnóstico de layout
grep -a "XCPDUMP\|XCPGRAVE\|degenerate" "J:/MTG/Laboratorio/LAB-UI-1.4.62-V2/cliente-lab.log" | tail -5
```

## 6. Reglas vigentes

- No tocar el servidor oficial PID 33968 / puerto 17171 ni su base de datos bloqueada.
- Solo sumar: no borrar mods, funciones ni configuración existente; backup antes de sobrescribir.
- El build final NO debe llevar `XCPDUMP`, `XCPGRAVE` ni `xcp.*`.
- Pedir autorización antes de borrar la clave basura del registro `HKCU\Software\JavaSoft\Prefs\HKEY_CURRENT_USER`.
- Al terminar: en `J:\MTG` solo `Laboratorio` e `Instalacion-XMage`; documentar para otros workers/IA.

## 7. Cierre final (2026-10-25 13:15)

Hecho:

- Build final sin instrumentación XCP (client+server, 12:31).
- Smoke completo repetido en el laboratorio con partida real `74d2477e-b5a1-4501-9055-95c27e281961`
  (Kabutosan vs Computer 2, Modern): layout correcto, chat ancho, fases bajo la mano.
- Elección de jugador inicial resuelta clicando el panel del jugador; mulligan y turno 1 OK.
- Ventana de cementerio abierta EN VIVO: `Kabutosan's graveyard (0) - 0 cards`
  (captura `capturas/84-cementerio.png`), mismo marco/fórmula que `ShowCardsDialog`.
- Desplegado a `J:\MTG\Instalacion-XMage`:
  - `mage-client-1.4.62.jar` SHA256 `d5382792...a6fc7a`
  - `mage-server-1.4.62.jar` SHA256 `f89bf092...28f0ad`
  - backups en `xmage/backup-jars-1.4.62-final-20261025/`.
- Laboratorio ordenado: `LEEME.md` nuevo, parches en `parches/`, material viejo movido a
  `_archivo/20261025/` (nada borrado).
- Parche consolidado: `parches/2026-10-25-UI-final.patch`.

Pendiente:

- Autorización de Kabutosan para borrar la clave basura del registro.
- Validar cementerio con cartas reales (mazo de prueba con descartes).
- Arranque de confirmación desde el lanzador oficial (los jars desplegados son byte a byte
  los mismos que pasaron el smoke).

## 8. v1.4 — teclas F de vuelta junto a CONCEDE (2026-10-25 13:40)

Petición de Kabutosan: sacar las teclas "F" de la barra de fases y devolverlas al lado de
CONCEDE; la barra de fases se queda debajo de la mano porque le gusta cómo queda.

Cambios (solo `GamePanel.java`, aditivos sobre los mods existentes):

- Eliminada la fila centrada de CONCEDE (`pnlConcedeCenter`); CONCEDE vuelve a
  `pnlShortCuts`, junto a los seis botones F.
- `applyPhaseFusionHiddenShortcuts()` → `applyPhaseFusionShortcutVisibility()`: muestra
  TO NEXT TURN / TO END STEP / TO MAIN STEP / TO YOUR TURN / SKIP STACK / TO PRIOR END.
  Cancel Skip sigue oculto; disponible por F3 y por el menú de fases.
- Barra de fases: 56 → 44 px y celdas solo con el nombre (`PhaseSummaryCell(phaseName, null)`).

Evidencia del smoke en laboratorio (servidor 17191, cliente con prefs aisladas):

- `capturas/86-v14-en-juego.png`: layout nuevo durante el mulligan.
- `capturas/87-v14-keep.png`: turno del oponente con la fila F junto a CONCEDE.
- `capturas/88-v14-f5.png`: clic real en TO END STEP F5 → Upkeep a Precombat Main;
  el botón quedó con foco. Atajos clicables confirmados.

Despliegue (autorizado por Kabutosan; solo se cerró el cliente oficial, PID 16936):

- `J:\MTG\Instalacion-XMage\xmage\mage-client\lib\mage-client-1.4.62.jar`
  SHA256 `9230b8ca1fb4017c411413c62381b8e4e9882f095538bf3dcbebb6edf49dfacc`
  (verificado: el jar contiene `applyPhaseFusionShortcutVisibility`).
- Backup de la build anterior: `xmage/backup-jars-1.4.62-v14-20261025/cliente/`
  (hash `d5382792...a6fc7a`).
- Servidor 17171 y su BD intactos; `mage-server/lib` sin tocar.
- Parche regenerado: `parches/2026-10-25-UI-final.patch` (backup `.bak-v13`).

Pendiente tras v1.4:

- Reabrir el cliente oficial (yo no lo relancé) y confirmar el layout en la instalación.
- Autorización para la clave basura del registro.
- Cementerio con cartas reales (mazo con descartes/mill).

## 9. v1.5 — resaltado de fase sin solapes (2026-10-25 16:30)

Petición de Kabutosan: las fases se solapaban con las ventanas de selección; quería una
ventana amarilla y verde ajustada a la letra de la fase, con bordes redondeados
minimalistas. Norma nueva: el protocolo laboratorio → instalación queda escrito para
siempre en `J:\MTG\AGENTS.md` (y en LEEME) para cualquier worker/IA.

Diagnóstico:

- `PhaseSummaryCell.paintComponent()` dibujaba con `drawString(name, getWidth()/2, y)`,
  es decir el texto empezaba en el centro de la celda y se extendía a la derecha sin
  recorte; "Combat Damage" invadía la celda siguiente y chocaba con su caja.
- El resaltado era un `LineBorder` alrededor de TODA la celda (GridLayout), mucho más
  ancho que el texto; de ahí el aspecto de ventanas solapadas.

Cambios (solo `GamePanel.java`):

- Texto centrado de verdad con `FontMetrics.stringWidth` y recorte `clipRect` a la celda;
  si un nombre no cabe, la fuente baja en pasos de 0,5 pt hasta 12 pt.
- Resaltado nuevo: píldora `RoundRectangle2D` ajustada al texto, borde 1 px con
  antialiasing y relleno tenue. Amarillo `#FFD640` para la fase activa, verde `#5AE65A`
  para el objetivo del skip. Se elimina el `LineBorder` de celda completa.
- `updatePhaseSummary()` usa `PhaseSummaryCell.setHighlightColor()`.

Validación en laboratorio:

- `capturas/89-v15-arranque.png`: partida reconectada con la barra nueva.
- `capturas/90-v15-fases.png`: zoom con Main1 en píldora amarilla, Combat End y Cleanup
  en verde, y todas las fases centradas sin solapes (Combat Damage completo).

Despliegue:

- `mage-client-1.4.62.jar` SHA256 `f25c7bd1b2941e84319963f9e42eb3e20177e0e3ccc6ac2b662e5fc844834c80`
  (verificado `setHighlightColor` en el jar).
- Backup: `xmage/backup-jars-1.4.62-v15-20261025/cliente/` (v1.4, `9230b8ca…`).
- Servidor 17171 y su BD intactos; cliente oficial cerrado porque estaba desconectado
  en el diálogo de reconexión (sin partida en curso).

## 10. Limpieza y sincronización con GitHub (2026-10-25 17:45)

Petición de Kabutosan: sincronizar con GitHub, ordenar y limpiar lo antiguo que no
sirva, sin borrar nada importante. El borrado permanente se confirmó explícitamente.

Limpieza (detalle en `_archivo/20261025/INVENTARIO-LIMPIEZA-2026-10-25.md`):

- Laboratorio archivado 1.4.61: se borraron `cliente/lib` y `servidor/lib` (248 MB;
  jars 1.4.61 preservados en `backup-jars-1.4.61-ui-20261024`) y las bases
  `cards.h2.mv.db` de cliente y servidor (448 MB, regenerables desde `Mage.Sets`).
  Se conservaron capturas (2), logs, configuración, readmes y assets menores: 15 MB.
- `XMage-1.4.62V1-src/*/target`: 359 MB de artefactos Maven regenerables (ocho módulos;
  la primera medición solo había alcanzado cinco).
- `_archivo/20261025/untitled-project`: repositorio git vacío (62 KB).
- Total liberado: ~1,05 GB.
- Movidos al archivo, sin borrar: `proyecto Xmage` (MandarinJuice/Pragmata) y
  `v36-recuperacion` (informes V3.6).
- Sin cambios en `Proyecto-XMage` (2.5 GB, histórico), `parches/` ni en los 5 backups
  de la instalación (461 MB). Servidor 17171 y su BD intactos.

Verificación posterior: el jar desplegado sigue con SHA256 `f25c7bd1…834c80`; el
laboratorio actual conserva sus 127 capturas y sus jars en `lib/`.

GitHub:

- `AGENTS.md` y `docs/laboratorio-ui-1.4.62/` (LEEME, CHECKPOINT, parche y guía)
  publicados en la rama `upgrade/official-1.4.62V1`, commit `87c7f2b7`.
- El push directo a `main` quedó bloqueado por la política de seguridad; no se hizo
  force-push y `main` remoto permanece igual.
- La historia local de `J:\MTG` (~6.2 GB, no relacionada con `origin/main`) no se
  publica ni se reescribe.
