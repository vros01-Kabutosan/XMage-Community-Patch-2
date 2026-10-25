# Laboratorio XMage — UI 1.4.62 (estado final 2026-10-25)

> **Norma permanente para cualquier worker/IA:** todo cambio se prueba primero en este
> laboratorio (puerto 17191) y solo se despliega en `J:\MTG\Instalacion-XMage` cuando el
> smoke pasa. Reglas completas en `J:\MTG\AGENTS.md`. No hace falta repetirlas en cada prompt.

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
| `_archivo/20261025/` | Material antiguo retirado del flujo activo (logs viejos, labs anteriores, restos de recuperación) y limpieza autorizada del 2026-10-25. Detalle exacto en `_archivo/20261025/INVENTARIO-LIMPIEZA-2026-10-25.md`. |

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
   - v1.4: la barra de fases muestra solo nombres (44 px) y los atajos F4/F5/F7/F9/F10/F11
     vuelven a la fila inferior, junto a CONCEDE (`applyPhaseFusionShortcutVisibility`).
     Cancel Skip sigue solo por teclado (F3) y menú de fases.
   - v1.5: texto de fase centrado con `FontMetrics` y recortado a su celda (antes
     `drawString` arrancaba en el centro e invadía la celda vecina, p. ej. Combat Damage).
     El resaltado ya no es un borde de celda completa: es una píldora ajustada al texto,
     amarilla para la fase activa y verde para el objetivo del skip, borde de 1 px y
     relleno tenue (`PhaseSummaryCell.setHighlightColor`).
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
- v1.4 (2026-10-25 tarde): `86-v14-en-juego.png` (barra de fases sin teclas debajo de la
  mano), `87-v14-keep.png` (fila F junto a CONCEDE) y `88-v14-f5.png` (clic real en
  TO END STEP F5: pasó de Upkeep a Precombat Main y el botón quedó con foco).
- v1.5 (2026-10-25 tarde): `89-v15-arranque.png` (partida reconectada con la barra nueva)
  y `90-v15-fases.png` (zoom: píldoras ajustadas al texto; Main1 amarilla, Combat End y
  Cleanup verdes; Combat Damage ya no se solapa con la celda siguiente).

Limitación conocida del smoke: el mazo de prueba (`cliente/sample-decks/Descargados/Modern/Smoke Modern.dck`,
60 Mountains) no mete cartas al cementerio, así que la ventana se validó vacía; el
renderizado con cartas es el mismo `CardArea.loadCards()` que usa `ShowCardsDialog`.

## Despliegue en la instalación oficial

### v1.5 (2026-10-25, cliente)

- Cliente: `xmage/mage-client/lib/mage-client-1.4.62.jar`
  - SHA256 `f25c7bd1b2941e84319963f9e42eb3e20177e0e3ccc6ac2b662e5fc844834c80`
  - Verificado: el jar desplegado contiene `setHighlightColor`.
- Backup previo: `xmage/backup-jars-1.4.62-v15-20261025/cliente/`
  (SHA256 `9230b8ca…f49dfacc`, build v1.4).
- El servidor oficial (17171 / su BD) no se reinició ni se modificó.

### v1.4 (2026-10-25, cliente)

- Cliente: `xmage/mage-client/lib/mage-client-1.4.62.jar`
  - SHA256 `9230b8ca1fb4017c411413c62381b8e4e9882f095538bf3dcbebb6edf49dfacc`
  - Verificado: el jar desplegado contiene `applyPhaseFusionShortcutVisibility`.
- Backup previo intacto: `xmage/backup-jars-1.4.62-v14-20261025/cliente/`
  (SHA256 `d5382792…a6fc7a`, la build anterior).
- `xmage/mage-server/lib/mage-client-1.4.62.jar` se deja como estaba (no lo usa el
  servidor y estaba bloqueado por el proceso del servidor). No afecta al cliente.
- El servidor oficial (17171 / su BD) no se reinició ni se modificó.

### Despliegue anterior (2026-10-25, mañana)

- Cliente: `xmage/mage-client/lib/mage-client-1.4.62.jar`
  - SHA256 `d538279279a526c919fa3300379ccf04b6b1b653b725b29b3c903c60e5a6fc7a`
- Servidor: `xmage/mage-server/lib/mage-server-1.4.62.jar`
  - SHA256 `f89bf09248a4057bb98d648eb5bb8a49c2438d74243010e17018b4a5b728f0ad`
- Backups previos (sin borrar nada):
  `xmage/backup-jars-1.4.62-final-20261025/cliente/` y `.../servidor/`.
- No se tocaron `config/`, `db/`, `installed.properties`, `java/` ni el lanzador.

## Limpieza y orden (2026-10-25, autorizada)

Se liberaron ~4,03 GB borrando solo duplicados, cachés y artefactos regenerables; no se
borró ninguna captura, log de evidencia, configuración ni backup:

- Laboratorio archivado 1.4.61 (`_archivo/20261025/LAB-UI-FASES-CEMENTERIO`): se
  retiraron `cliente/lib`, `servidor/lib` (jars 1.4.61 ya preservados en
  `Instalacion-XMage/xmage/backup-jars-1.4.61-ui-20261024`) y las bases
  `cards.h2.mv.db` de cliente/servidor (regenerables desde `Mage.Sets`).
- `XMage-1.4.62V1-src/*/target`: artefactos Maven regenerables (359 MB, ocho módulos).
- `Instalacion-XMage/.../plugins/images/downloading/scryfall_bulk_cards.jsonl`: caché
  descomprimida de Scryfall (2,98 GB); se conserva el `.gz` verificado (401 MB), del que
  se puede reconstruir sin volver a descargarlo.
- `_archivo/20261025/untitled-project`: repositorio git vacío.

También se movieron al archivo `proyecto Xmage/` (herramientas MandarinJuice y backup
de Pragmata, ajenos a XMage) y `v36-recuperacion/` (informes/herramientas V3.6).
`Proyecto-XMage/` se conserva como archivo histórico. El servidor oficial 17171 y su
base de datos no se tocaron.

## Pendientes

1. Borrar la clave basura `HKCU\Software\JavaSoft\Prefs\HKEY_CURRENT_USER` — **requiere
   autorización expresa de Kabutosan**.
2. Validar la ventana de cementerio con cartas reales (hace falta un mazo de prueba con
   descartes/mill; el clon ya está verificado por código y en vivo con la ventana abierta).
3. Primer arranque del cliente/servidor desde el lanzador oficial para confirmar que la
   instalación desplegada arranca con las prefs reales (el cliente oficial se cerró para
   poder copiar la v1.4; el servidor 17171 sigue en marcha).
