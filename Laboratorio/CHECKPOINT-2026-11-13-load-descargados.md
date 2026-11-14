# CHECKPOINT 2026-11-13 — LOAD del Deck Editor abre en la carpeta de mazos descargados (v32)

**Petición del usuario (Kabutosan):** "haz que cuando entre en decks editor y abra en load me
vaya directamente a la carpeta de los mazos descargados".

Criterios elegidos en la misma sesión:

1. **Siempre** en `sample-decks\Descargados` (ignorando la última carpeta recordada).
2. En la **raíz** de `Descargados` (sin entrar en el formato ni en la fecha más reciente).
3. **Solo** el botón LOAD del Deck Editor (SAVE, Import/Export, selector de mazo de la mesa y
   torneos quedan como estaban).

## Qué pasaba antes

`DeckEditorPanel.btnLoadActionPerformed` leía la preferencia `lastDeckFolder` (clave
`"lastDeckFolder"` del nodo de prefs del cliente) y hacía `setCurrentDirectory(new File(ruta))`.
Esa clave guarda en realidad la **ruta canónica del último archivo** tocado, y la comparten
SAVE del editor, `NewPlayerPanel` (elegir mazo al crear/unirse a partida) y `NewTournamentDialog`.
Resultado: el LOAD abría donde fuera la última vez que se tocó un mazo en cualquier pantalla,
y si la ruta ya no existía caía en "Mis documentos".

## Cambio aplicado (source, rama `upgrade/official-1.4.63V1`)

- `Mage.Client/src/main/java/mage/client/deckeditor/DeckEditorPanel.java`
  - import `mage.client.util.AiDeckLibrary`
  - en `btnLoadActionPerformed`: si `AiDeckLibrary.downloadedDeckRoot()` es directorio, ese es el
    directorio inicial del `fcSelectDeck`; si no, se mantiene el comportamiento viejo con
    `lastDeckFolder`.
- `Mage.Client/src/main/java/mage/client/util/AiDeckLibrary.java`
  - `downloadedDeckRoot()` pasa de `private` a `public` (con javadoc) para compartir la resolución
    de ruta con el editor. Era la misma lógica que ya usa el pool de mazos de la IA
    (`sample-decks\Descargados\<formato>`), así que no se duplica ni se hardcodea nada.

Se sigue escribiendo `lastDeckFolder` al terminar el LOAD (no se toca), para no romper el
comportamiento de las otras pantallas que la consumen.

Parche: `Laboratorio\parches\2026-11-13-load-folder-descargados-v32.patch` (8 inserciones, 2 supresiones).

## Build

```
C:/Users/vros0/apache-maven-3.9.6/bin/mvn.cmd -B -q -DskipTests -pl Mage.Client clean package
```

`clean` incluido (disciplina del 2026-11-09: nada de builds incrementales para desplegar).
Solo se modificó Mage.Client, así que el resto de jars mage no se recompilaron ni se movieron.

## Verificación en laboratorio (LAB-UI-1.4.62-V2, puerto 17191, prefs `XMageLabUIv2`)

1. **Resolución de rutas** (JVM con `user.dir` = carpeta del cliente, código real de
   `AiDeckLibrary.downloadedDeckRoot()`):
   - lab: `J:\MTG\Laboratorio\LAB-UI-1.4.62-V2\cliente\sample-decks\Descargados` (existe)
   - oficial: `J:\MTG\Instalacion-XMage\xmage\mage-client\sample-decks\Descargados` (existe)
2. **Handler real del LOAD** (harness `LoadProbe`: instancia `DeckEditorPanel`, invoca
   `btnLoadActionPerformed` por reflexión y lee el `JFileChooser` mientras el diálogo está abierto).
   Se sembró `lastDeckFolder` apuntando a **otra** carpeta (`sample-decks\AI`) para comprobar que
   se ignora:

   ```
   EXPECTED ROOT = J:\MTG\Laboratorio\LAB-UI-1.4.62-V2\cliente\sample-decks\Descargados (exists=true)
   CHOOSER DIR   = J:\MTG\Laboratorio\LAB-UI-1.4.62-V2\cliente\sample-decks\Descargados
   DIALOG        = "Abrir"
   DIR CHILDREN  = Modern, Pioneer, Standard, test
   MATCHES ROOT  = true
   ```

   Evidencia: `LAB-UI-1.4.62-V2\capturas\126-prueba-load-descargados.log`.
3. **Bytecode del jar desplegado**: `javap` confirma `public static Path downloadedDeckRoot()` y la
   llamada `invokestatic AiDeckLibrary.downloadedDeckRoot` dentro de `btnLoadActionPerformed`.
4. Cliente del lab arrancado con el jar nuevo (build 2026-11-13 20:56), login automático
   `KabutosanLab@localhost:17191` y lobby operativo (capturas `123`-`125`).

## Notas de la sesión (herramientas)

- El cliente del lab se lanzó primero con `lanzar-lab.ps1` (WMI) y **murió al terminar el comando
  del agente** (`^C` en `cliente-lab.err.log` y `servidor-lab.err.log`). Para lanzamientos desde
  agente, `Start-Process` desvinculado funciona mejor (script temporal en el scratchpad).
- `tools\ui-real.ps1` **no es fiable en este equipo**: la pantalla es 4K (3840x2160) y el script
  trabaja DPI-virtualizado (ventana de 2208x1200), así que sus clics salen desplazados; además
  elige "el primer java con título XMage", que con el cliente oficial abierto apunta a la ventana
  equivocada. `PostMessage`/`SendMessage` tampoco los procesa AWT (eventos sintéticos).
  Queda pendiente, si se quiere automatizar la UI, un driver por PID + DPI-aware.
- El laboratorio quedó **parado** y la preferencia `lastDeckFolder` del nodo `XMageLabUIv2`
  restaurada a su valor original. No se tocó el servidor oficial (17171) ni su base de datos.

## Despliegue en la instalación oficial (2026-11-13, autorizado por Kabutosan)

Opción elegida: **solo el jar del cliente** (el cambio no introduce dependencias nuevas fuera de
`mage-client`; el resto de jars sigue con el build limpio del 09/11).

1. Cerrado el cliente oficial que estaba abierto (PID 36908) — el servidor oficial (17171) y su
   base de datos **no se tocaron**.
2. Backup: `Instalacion-XMage\xmage\backup-jars-1.4.63-20261113-load-descargados\mage-client-1.4.63.jar`
   (02/11/2026, 22.570.356 bytes).
3. Copiado `Mage.Client\target\mage-client-1.4.63.jar` (13/11/2026 21:56, 22.565.021 bytes) a
   `Instalacion-XMage\xmage\mage-client\lib\`.
4. `javap` sobre el jar desplegado: `public static Path downloadedDeckRoot()` presente.
5. Smoke del oficial con el harness del handler real, usando un **nodo de prefs de prueba**
   (`XMageV32Smoke/mage/client`, borrado después) para no alterar las preferencias reales:

   ```
   EXPECTED ROOT = J:\MTG\Instalacion-XMage\xmage\mage-client\sample-decks\Descargados (exists=true)
   CHOOSER DIR   = J:\MTG\Instalacion-XMage\xmage\mage-client\sample-decks\Descargados
   DIALOG        = "Abrir"
   DIR CHILDREN  = Modern, Pioneer, Standard, test
   MATCHES ROOT  = true
   ```

   Log: `parches\evidencia-20261113-v32\smoke-oficial-v32-load-descargados.log`.
6. Cliente oficial relanzado con sus parámetros habituales (`startClient.bat`: `-Xms1g -Xmx3g`,
   G1GC, IPv4) y verificado en el lobby como `Kabutosan@localhost:17171` con la build nueva
   (captura `parches\evidencia-20261113-v32\127-oficial-conectado-tras-v32.png`).

