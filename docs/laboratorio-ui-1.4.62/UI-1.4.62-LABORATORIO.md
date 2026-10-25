# UI 1.4.62 — laboratorio y despliegue (2026-10-25)

Documento para cualquier worker/IA que retome el fork de Kabutosan. El entorno completo
vive en `J:\MTG\Laboratorio` (fuente, laboratorio aislado, capturas y parches).

## Mods incluidos en esta rama (`upgrade/official-1.4.62V1`)

| Archivo | Cambio |
|---|---|
| `Mage.Client/.../game/FeedbackPanel.java` | Ancho preferido del chat fijo (1024) en vez de `Short.MAX_VALUE`; evita el panel aplastado. |
| `Mage.Client/.../game/GamePanel.java` | Usa `FeedbackPanel.PREFERRED_WIDTH`; restauración robusta del splitter y autocuración de divisores degenerados; fases debajo de la mano; barra de fases solo con nombres (44 px) y atajos F4/F5/F7/F9/F10/F11 de vuelta junto a CONCEDE (v1.4). |
| `Mage.Client/.../game/HelperPanel.java` | Altura máxima de la superficie de decisión = 1/3 de pantalla (mín. 120 px). |
| `Mage.Client/.../dialog/GraveyardCardsDialog.java` | Clon de la ventana de búsqueda/elección de biblioteca (`ShowCardsDialog`): `CardArea`, mismo `pack`/clamp/posicionamiento, no modal y persistente. |
| `Mage.Server/.../MageServerImpl.java` + `Main.java` | `markGameTypesLoadFinished()`: evita el bloqueo de 180 s cuando no hay plugins de game types. |

Todos los cambios son aditivos; no se eliminó ningún mod ni configuración previa.

## Compilar y probar

```bash
cd J:/MTG/Laboratorio/XMage-1.4.62V1-src
C:/Users/vros0/apache-maven-3.9.6/bin/mvn.cmd -B -q -DskipTests -pl Mage.Client,Mage.Server package

cd J:/MTG/Laboratorio/LAB-UI-1.4.62-V2
./montar-lab.sh   # laboratorio aislado en el puerto 17191 y prefs /Software/XMageLabUIv2
./lanzar-lab.sh
```

Smoke final verificado con partida real (Kabutosan vs Computer 2, Modern): chat ancho,
fases debajo de la mano, elección de jugador inicial, mulligan y ventana de cementerio
abierta en vivo. Evidencia: `J:\MTG\Laboratorio\LAB-UI-1.4.62-V2\capturas\` (79, 82, 83, 84).

### v1.4 — teclas F de vuelta junto a CONCEDE (2026-10-25 13:3x)

Petición de Kabutosan: las teclas "F" salen de la barra de fases y vuelven al lado de
CONCEDE; la barra de fases se queda debajo de la mano (le parece bien ahí).

- `GamePanel.java`: se elimina la fila centrada de CONCEDE (`pnlConcedeCenter`); CONCEDE
  vuelve a `pnlShortCuts`. `applyPhaseFusionHiddenShortcuts()` pasa a
  `applyPhaseFusionShortcutVisibility()` y muestra los seis botones F (Cancel Skip sigue
  solo por teclado F3 y menú de fases). La barra de fases baja de 56 a 44 px y las celdas
  ya no pintan la tecla (`PhaseSummaryCell(phaseName, null)`).
- Evidencia: `capturas/86-v14-en-juego.png` (mulligan + layout), `87-v14-keep.png`
  (turno del oponente, fila F junto a CONCEDE) y `88-v14-f5.png` (clic real en TO END
  STEP F5: la partida pasó de Upkeep a Precombat Main y el botón quedó con foco).
- Desplegado en la instalación el 2026-10-25 (jar `9230b8ca…dfacc`); el cliente oficial
  se cerró para desbloquear el archivo y no se tocó el servidor.

### v1.5 — resaltado de fase ajustado al texto (2026-10-25 tarde)

Kabutosan reportó que las fases se solapaban con las ventanas de selección y pidió una
ventana amarilla/verde ajustada a la letra, con bordes redondeados minimalistas.

- `PhaseSummaryCell`: el texto se centra con `FontMetrics` (antes `drawString` empezaba
  en `getWidth()/2` y las fases largas invadían la celda siguiente) y se recorta con
  `clipRect`; si no cabe, baja la fuente en pasos de 0,5 pt hasta 12 pt.
- El resaltado deja de ser un borde de celda completa: se pinta una píldora
  (`RoundRectangle2D`) ajustada al texto, borde de 1 px con antialiasing y relleno tenue.
  Amarillo `#FFD640` = fase activa; verde `#5AE65A` = objetivo del skip; el resto, sin caja.
- `updatePhaseSummary()` ya no asigna `LineBorder` a las celdas; usa
  `setHighlightColor()`.
- Evidencia: `capturas/89-v15-arranque.png` y `capturas/90-v15-fases.png`.
- Desplegado: jar `f25c7bd1…4c80`, backup `backup-jars-1.4.62-v15-20261025/`.

## Despliegue en la instalación

- v1.5 cliente: `J:\MTG\Instalacion-XMage\xmage\mage-client\lib\mage-client-1.4.62.jar`
  SHA256 `f25c7bd1b2941e84319963f9e42eb3e20177e0e3ccc6ac2b662e5fc844834c80`
  (contiene `setHighlightColor`; backup en
  `xmage/backup-jars-1.4.62-v15-20261025/cliente/`).
- v1.4 cliente: SHA256 `9230b8ca1fb4017c411413c62381b8e4e9882f095538bf3dcbebb6edf49dfacc`
  (backup en `xmage/backup-jars-1.4.62-v14-20261025/cliente/`).
- Servidor: `xmage\mage-server\lib\mage-server-1.4.62.jar`
  SHA256 `f89bf09248a4057bb98d648eb5bb8a49c2438d74243010e17018b4a5b728f0ad`
  (sin cambios; el servidor 17171 no se reinició).
- Cliente anterior (build 11:31): SHA256 `d538279279a526c919fa3300379ccf04b6b1b653b725b29b3c903c60e5a6fc7a`
  (backup en `xmage/backup-jars-1.4.62-final-20261025/`).
- Se verificó que el resto de jars `mage-*` de la instalación son compatibles (listados de
  clases idénticos; solo metadatos de compresión distintos).

## Pendientes

- Autorización de Kabutosan para borrar la clave basura `HKCU\Software\JavaSoft\Prefs\HKEY_CURRENT_USER`.
- Validar el cementerio con cartas reales (mazo de prueba con descartes/mill).
- Primer arranque de confirmación desde el lanzador oficial.
