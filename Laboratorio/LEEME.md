# Laboratorio XMage — UI 1.4.62 (estado final 2026-10-25)

Este directorio contiene TODO lo necesario para reproducir, probar y desplegar el fork
modificado de XMage 1.4.62V1 de Kabutosan. La instalación oficial vive aparte, en
`J:\MTG\Instalacion-XMage`.

## Estructura

| Ruta | Qué es |
|---|---|
| `XMage-1.4.62V1-src/` | Fuente completa del fork (git, rama `upgrade/official-1.4.62V1`). Aquí se compila. |
| `LAB-UI-1.4.62-V2/` | Laboratorio ejecutable aislado: `cliente/` + `servidor/` (puerto 17191), scripts, logs y `capturas/` con la evidencia de los smokes. |
| `parches/` | Parches consolidados de los mods (aplicables sobre la fuente oficial 1.4.62V1). |
| `Proyecto-XMage/` | Archivo histórico 1.4.61 (legado, solo consulta). No se usa para probar. |
| `_archivo/20261025/` | Material antiguo retirado del flujo activo (logs viejos, labs anteriores, restos de recuperación). Nada se ha borrado. |

## Mods activos (6 archivos, todos aditivos)

Fuente: `XMage-1.4.62V1-src`. Parche: `parches/2026-10-25-UI-final.patch`.

1. `Mage.Client/src/main/java/mage/client/game/FeedbackPanel.java`
   - Ancho preferido del panel de chat = `PREFERRED_WIDTH` (1024) en vez de `Short.MAX_VALUE`.
   - Evita que el chat se aplaste al restaurar el divisor.
2. `Mage.Client/src/main/java/mage/client/game/GamePanel.java`
   - `setGUISize()` usa `FeedbackPanel.PREFERRED_WIDTH` para feedback y shortcuts.
   - `restoreSplitter()` espera a que el splitter tenga ancho real antes de restaurar.
   - `normalizeSplitterLocation()` descarta divisores guardados degenerados (autocuración).
   - La barra de fases queda debajo de la mano (hand height correcto).
3. `Mage.Client/src/main/java/mage/client/game/HelperPanel.java`
   - `getRequiredHeight()` limitado a 1/3 de la pantalla (mínimo 120 px).
4. `Mage.Client/src/main/java/mage/client/dialog/GraveyardCardsDialog.java`
   - Clon visual de la ventana de búsqueda/elección de biblioteca (`ShowCardsDialog`):
     `CardArea` + `MageDialog` interno no modal + misma fórmula `pack`/clamp/`keepInsideDesktop`
     y mismo posicionamiento. Persistente por jugador (HIDE_ON_CLOSE) y refresca en cada update.
5. `Mage.Server/src/main/java/mage/server/MageServerImpl.java`
   - `markGameTypesLoadFinished()`: corta la espera de 180 s cuando no hay plugins de game types.
6. `Mage.Server/src/main/java/mage/server/Main.java`
   - Llama a `markGameTypesLoadFinished()` tras cargar los game types.

## Compilar

```bash
cd "J:/MTG/Laboratorio/XMage-1.4.62V1-src"
C:/Users/vros0/apache-maven-3.9.6/bin/mvn.cmd -B -q -DskipTests -pl Mage.Client,Mage.Server package
```

Salida:
- `Mage.Client/target/mage-client-1.4.62.jar`
- `Mage.Server/target/mage-server-1.4.62.jar`

## Laboratorio

```bash
cd "J:/MTG/Laboratorio/LAB-UI-1.4.62-V2"
./montar-lab.sh    # recrea cliente/servidor desde la fuente + libs de la instalación
./lanzar-lab.sh    # servidor 17191 + cliente con prefs aisladas (/Software/XMageLabUIv2)
./parar-lab.sh     # detiene ambos
```

- Prefs del cliente de laboratorio: nodo de registro separado `HKCU\Software\XMageLabUIv2`.
- Prefs del servidor de laboratorio: `-Djava.util.prefs.userRoot=/Software/XMageLabUIv2srv`.
- El flag `-Dxcp.layout.dump=true` de `lanzar-lab.sh` es inerte en la build final (la
  instrumentación XCP se retiró); se conserva para no alterar la herramienta.
- Servidor oficial de Kabutosan: **NO TOCAR** (puerto 17171 / su BD). El laboratorio usa 17191.

## Smoke final (2026-10-25, build 11:31)

Partida real: `74d2477e-b5a1-4501-9055-95c27e281961` (Kabutosan vs Computer 2, Modern).
Evidencia en `LAB-UI-1.4.62-V2/capturas/`:

- `79-partida-final.png`: chat ancho y legible (~632 px físicos), barra de fases debajo
  de la mano (Upkeep resaltado), superficie de decisión centrada, sin salida de depuración.
- `82-elegido.png`: elección de jugador inicial resuelta clicando el panel del jugador;
  prompt de mulligan con botones visibles.
- `83-keep.png`: turno 1, mano visible, prompt de Upkeep con Done.
- `84-cementerio.png`: ventana `Kabutosan's graveyard (0) - 0 cards` abierta en partida,
  mismo marco/tamaño/fórmula que la ventana de búsqueda de biblioteca.

Limitación conocida del smoke: el mazo de prueba (`cliente/sample-decks/Descargados/Modern/Smoke Modern.dck`,
60 Mountains) no mete cartas al cementerio, así que la ventana se validó vacía; el
renderizado con cartas es el mismo `CardArea.loadCards()` que usa `ShowCardsDialog`.

## Despliegue en la instalación oficial (hecho)

- Cliente: `xmage/mage-client/lib/mage-client-1.4.62.jar`
  - SHA256 `d538279279a526c919fa3300379ccf04b6b1b653b725b29b3c903c60e5a6fc7a`
- Servidor: `xmage/mage-server/lib/mage-server-1.4.62.jar`
  - SHA256 `f89bf09248a4057bb98d648eb5bb8a49c2438d74243010e17018b4a5b728f0ad`
- Backups previos (sin borrar nada):
  `xmage/backup-jars-1.4.62-final-20261025/cliente/` y `.../servidor/`.
- No se tocaron `config/`, `db/`, `installed.properties`, `java/` ni el lanzador.

## Pendientes

1. Borrar la clave basura `HKCU\Software\JavaSoft\Prefs\HKEY_CURRENT_USER` — **requiere
   autorización expresa de Kabutosan**.
2. Validar la ventana de cementerio con cartas reales (hace falta un mazo de prueba con
   descartes/mill; el clon ya está verificado por código y en vivo con la ventana abierta).
3. Primer arranque del cliente/servidor desde el lanzador oficial para confirmar que la
   instalación desplegada arranca con las prefs reales.
