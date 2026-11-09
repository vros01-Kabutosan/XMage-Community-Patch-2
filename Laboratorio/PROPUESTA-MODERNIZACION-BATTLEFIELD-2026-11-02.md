# Propuesta y Hoja de Ruta: Modernización Integral del Campo de Batalla (*Battlefield*)

> **Documento guardado para la sesión de trabajo:** 2026-11-02  
> **Objetivo:** Plan maestro de modernización visual, ergonomía, interactividad y fluidez para el campo de batalla de XMage 1.4.63V1 (`Mage.Client`).  
> **Estado:** Fase de investigación completa. Listo para ejecución modular siguiendo el protocolo obligatorio de `AGENTS.md` (laboratorio $\rightarrow$ smoke $\rightarrow$ backup $\rightarrow$ instalación oficial).

---

## 1. Mapa de Clases y Puntos de Inyección en el Código

| Módulo / Función | Archivo Fuente | Métodos Clave | Objetivo de la Mejora |
|---|---|---|---|
| **Auras y Resplandores** | `org/mage/card/arcane/CardPanelRenderModeImage.java` | `createImage(Key key)`, `CardSizes` | Sustituir rectángulos planos de 1998 por halos de resplandor suave (*Soft Bloom Glow*) con degradados radiales. |
| **Fuerza / Resistencia y Daño** | `org/mage/card/arcane/CardPanelRenderModeImage.java` | `updatePTTexts()`, `ptText1..3` | P/T con colorimetría semántica: Verde (+ buff), Rojo (- debuff), y daño acumulado visible `X/Y (-D)`. |
| **Mareo de Invocación** | `org/mage/card/arcane/CardPanelRenderModeImage.java` | `overlayPanel`, `ImageManagerImpl` | Reemplazar el sprite tosco que tapa el arte por bruma etérea o icono elegante en esquina con tooltip. |
| **Giro de Cartas (*Tap*)** | `org/mage/card/arcane/CardPanel.java` | `TAPPED_ANGLE`, `getAnimationSettings()` | Ángulo de giro de 30°/45° con sombreado tenue (*dimming*) para no invadir columnas ni solapar vecinos. |
| **Contadores en Criaturas** | `org/mage/card/arcane/CardPanelRenderModeImage.java` | `getCounterImageWithAmount()`, `updateCounters()` | Badges vectoriales nítidos con antialiasing (+1/+1 verde, -1/-1 carmesí, lealtad amatista, escudos/palabras clave). |
| **Territorio y Línea de Frente** | `org/mage/plugins/card/CardPluginImpl.java` | `sortPermanents()`, `BattlefieldPanel.java` | Separador de horizonte sutil entre Vanguardia (criaturas) y Retaguardia (tierras/recursos). |
| **Agrupamiento de Tierras** | `org/mage/plugins/card/CardPluginImpl.java` | `createStacks()`, `Row` | Apilado inteligente en abanico de tierras básicas idénticas (`x4 {R}`) para liberar hasta un 50% de espacio horizontal. |
| **Auras y Equipos Conectados** | `org/mage/plugins/card/CardPluginImpl.java` | `attachmentSpacingY`, `AttachmentLayoutInfos` | Desplazamiento lateral escalonado o badge de anexos para no ocultar el texto ni el P/T de la criatura. |
| **Flechas de Combate / Objetivos** | `mage/client/util/gui/Arrow.java` y `ArrowBuilder.java` | `getArrow()`, `paintComponent()` | Reemplazar polígonos rígidos por arcos cinemáticos Bézier (`CubicCurve2D`) con gradientes y cabezas fluidas. |
| **Emparejamiento de Bloqueo** | `mage/client/combat/CombatManager.java` | `displayArrows()`, `drawBlockers()` | Badges gemelos de color (*Combat Pairing Rings*) para identificar de un vistazo atacantes y sus bloqueadores. |
| **Panel de Jugador (HUD)** | `mage/client/game/PlayerPanelExt.java` | `update()`, `avatar`, `lifeLabel`, `zonesPanel` | Anillo pulsante de prioridad (*Breathing Ring*), vidas grandes con números flotantes `+3`/`-4`, maná flotante brillante. |
| **Caja Central de Decisiones** | `mage/client/game/HelperPanel.java` | `setGUISize()`, `buttonGrid`, `btnLeft..btnSpecial` | Botones estilizados con atajos claros (`[Espacio] Hecho`, `[Esc] Cancelar`) y modo de anclaje no obstructivo. |
| **Marcos de Ventanas Internas** | `mage/client/dialog/MageDialog.java` | `MageDialog()`, `BasicInternalFrameUI` | Look & feel oscuro uniforme (*Dark Slate / Glassmorphism*), bordes finos de acero/oro y sombras suaves exteriores. |
| **Selector de Modos y Colores** | `mage/client/dialog/PickChoiceDialog.java` | `ChoiceCellRenderer`, `listChoices` | Tarjetas interactivas con iconos de maná en lugar de la lista gris de texto plano. |
| **Zoom Rápido Flotante** | `mage/client/cards/BigCard.java` / `CardPanel.java` | `mouseMoved()`, `mouseWheelMoved()` | Previsualización emergente al pulsar botón central o hover prolongado sin mirar al panel lateral lejano. |

---

## 2. Fases de Implementación Propuestas (Por Bloques de Valor)

### Bloque A: Impacto Visual Inmediato y Claridad de Mesa (Prioridad 1)
1. **Halos de Selección y Estados Dinámicos en Mesa:**
   - Sustituir los recuadros planos de `createImage()` en `CardPanelRenderModeImage.java` por resplandores degradados suaves (*Bloom*).
   - Colores premium: Cian brillante para cartas jugables en mano/mesa, Dorado para objetivos elegibles, Carmesí para atacantes/bloqueadores.
2. **P/T Inteligente y Daño Marcado:**
   - Verde esmeralda para valores bufados, Rojo para reducidos, y visualización de daño acumulado `3/4 (-2)`.
3. **Rediseño del Mareo de Invocación:**
   - Eliminar el sprite antiguo del centro; sustituir por niebla mágica translúcida o icono en la esquina superior.

### Bloque B: Combate Cinemático y Flechas Fluidas (Prioridad 2)
1. **Curvas Bézier en `Arrow.java`:**
   - Curvatura suave y degradado translúcido hacia la punta de la flecha.
2. **Badges de Emparejamiento de Bloqueadores:**
   - Distintivo visual compartido por cada grupo de combate en `CombatManager.java` para evitar el "efecto tela de araña" cuando hay muchas criaturas atacando y bloqueando.

### Bloque C: Paneles de Jugador y Ventana de Decisiones (Prioridad 3)
1. **Anillo de Prioridad y Contador de Vidas en `PlayerPanelExt.java`:**
   - Anillo luminoso pulsante que deja claro quién tiene el turno y la prioridad.
   - Efecto de flotación de números al variar la vida.
2. **HelperPanel Moderno:**
   - Botones con estilo moderno (verde esmeralda, carmesí, oro) y atajos de teclado explícitos.

### Bloque D: Ventanas Modernas y Diálogos de Juego (Prioridad 4)
1. **Marcos Oscuros en `MageDialog.java`:**
   - Cabecera y bordes oscuros integrados con la estética de la nueva Pila Flotante.
2. **Modernización de `PickChoiceDialog.java`:**
   - Selector visual de colores, modos y cartas en lugar de listas grises estándar.

---

## 3. Protocolo de Ejecución para Mañana

Cuando el usuario dé la orden de iniciar cualquiera de estos bloques:
1. Implementar en `XMage-1.4.62V1-src/`.
2. Compilar con Maven (`-pl Mage.Client,Mage.Server package`).
3. Montar y probar en Laboratorio aislado `LAB-UI-1.4.62-V2` (puerto 17191).
4. Hacer backup del jar oficial en `Instalacion-XMage\xmage\backup-jars-...`.
5. Desplegar en instalación oficial `Instalacion-XMage\xmage\mage-client\lib`.
6. Generar parche en `Laboratorio/parches/`, actualizar `LEEME.md` y hacer commit/push en `upgrade/official-1.4.63V1`.

