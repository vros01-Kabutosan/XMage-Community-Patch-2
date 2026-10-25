# UI 1.4.62 — laboratorio y despliegue (2026-10-25)

Documento para cualquier worker/IA que retome el fork de Kabutosan. El entorno completo
vive en `J:\MTG\Laboratorio` (fuente, laboratorio aislado, capturas y parches).

## Mods incluidos en esta rama (`upgrade/official-1.4.62V1`)

| Archivo | Cambio |
|---|---|
| `Mage.Client/.../game/FeedbackPanel.java` | Ancho preferido del chat fijo (1024) en vez de `Short.MAX_VALUE`; evita el panel aplastado. |
| `Mage.Client/.../game/GamePanel.java` | Usa `FeedbackPanel.PREFERRED_WIDTH`; restauración robusta del splitter y autocuración de divisores degenerados; fases debajo de la mano. |
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

## Despliegue en la instalación

- `J:\MTG\Instalacion-XMage\xmage\mage-client\lib\mage-client-1.4.62.jar`
  SHA256 `d538279279a526c919fa3300379ccf04b6b1b653b725b29b3c903c60e5a6fc7a`
- `J:\MTG\Instalacion-XMage\xmage\mage-server\lib\mage-server-1.4.62.jar`
  SHA256 `f89bf09248a4057bb98d648eb5bb8a49c2438d74243010e17018b4a5b728f0ad`
- Backups previos: `xmage/backup-jars-1.4.62-final-20261025/`.
- Se verificó que el resto de jars `mage-*` de la instalación son compatibles (listados de
  clases idénticos; solo metadatos de compresión distintos).

## Pendientes

- Autorización de Kabutosan para borrar la clave basura `HKCU\Software\JavaSoft\Prefs\HKEY_CURRENT_USER`.
- Validar el cementerio con cartas reales (mazo de prueba con descartes/mill).
- Primer arranque de confirmación desde el lanzador oficial.
