# Laboratorio XMage — UI 1.4.63 (actualizado 2026-11-01)

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
| `Proyecto-XMage/` | Archivo histórico 1.4.61 (legado, solo consulta). No se usa para probar. |
| `_archivo/20261025/` | Material antiguo retirado del flujo activo (logs viejos, labs anteriores, restos de recuperación) y limpieza autorizada del 2026-10-25. Detalle exacto en `_archivo/20261025/INVENTARIO-LIMPIEZA-2026-10-25.md`. |

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
