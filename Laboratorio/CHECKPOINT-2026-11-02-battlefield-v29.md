# CHECKPOINT - 2026-11-02 (Modernización Visual, Interactiva y Dinámica del Campo de Batalla v29)

## 1. Resumen Ejecutivo

- **Objetivo:** Modernizar profundamente el campo de batalla (*Battlefield*) de XMage 1.4.63V1:
  - Sustituir los marcos planos de selección/jugabilidad de 1998 por halos suaves (*Soft Bloom Glow*) con degradados radiales y códigos de color modernos (Cian para jugables, Esmeralda para seleccionados, Ámbar para objetivos, Carmesí para combate).
  - Dotar a la fuerza y resistencia (P/T) de colorimetría semántica en tiempo real (Verde esmeralda para aumentos/buffs, Rojo coral para reducciones/debuffs) y resistencia teñida en rojo-daño de advertencia cuando hay daño acumulado en combate.
  - Eliminar el sprite gigante de mareo de invocación que tapaba el arte de la criatura, reemplazándolo por una sutil niebla mística en la base y un badge circular estilizado en la esquina superior derecha con tooltip informativo.
  - Centrar con antialiasing y sombra los números de los contadores en las criaturas (+1/+1, -1/-1, lealtad, otros).
  - Transformar las flechas de combate y selección en arcos cinemáticos con gradiente de origen a destino, sombra exterior suave y borde interior luminoso.
  - Dotar a los botones del `HelperPanel` de jerarquía visual (Verde esmeralda para OK/Done/Yes, Carmesí para Cancel/No, Ámbar para Special/Undo).
- **Alcance:** Mod v29 aplicado sobre `Mage.Client`.
- **Estado del protocolo:**
  1. Compilación Maven exitosa (`BUILD SUCCESS`, código 0).
  2. Laboratorio montado (`montar-lab.sh`) y smoke test verificado en puerto aislado 17191 (`LAB-UI-1.4.62-V2`), con conexión de cliente limpia (`Connect: DONE`, `Logging: DONE`).
  3. Backup del jar oficial creado en `Instalacion-XMage\xmage\backup-jars-1.4.63-20261102_battlefield\`.
  4. Despliegue en instalación oficial completado (`Instalacion-XMage\xmage\mage-client\lib\mage-client-1.4.63.jar`).
  5. SHA-256 del nuevo jar: `A5D5FE4EBA9BCCDBF7374958DFDF6E480F38F8CE59D2ED9F1FB6DEC625EDC55F`.
  6. Servidor oficial (17171) nunca tocado.

---

## 2. Modificaciones Técnicas en el Código Fuente

### A. Halos de Selección y Jugabilidad (*Bloom Aura*) (`CardPanelRenderModeImage.java`)
- Implementado `paintSelectionGlow(Graphics2D g2, Rectangle rectSelection, int cornerSize, Color coreColor, Color outerBloom, Color rimHighlight)`.
- Reemplazo de `g2.fillRoundRect()` planos por halos multicapa con desenfoque suave exterior, núcleo luminoso y reborde antialiased interior de 1.2f:
  - `isSelected`: Verde esmeralda `#10b981` con halo `#34d399` y borde `#6ee7b7`.
  - `isPlayable`: Cian eléctrico `#06b6d4` con halo celeste `#38bdf8` y borde `#a5f3fc`.
  - `isChoosable`: Ámbar/oro `#f59e0b` con halo dorado `#fbbf24` y borde `#fef08a`.
  - `canAttack / canBlock`: Carmesí fuego `#ef4444` con halo `#f87171` y borde `#fecaca`.

### B. P/T Dinámico y Daño Marcado (`CardPanelRenderModeImage.java`)
- En `updatePTTexts()`:
  - Detección de incremento de fuerza (`currP > baseP` $\rightarrow$ Verde `#34d399`) o reducción (`currP < baseP` $\rightarrow$ Rojo `#f87171`).
  - Detección de daño acumulado (`isCardWithDamage()` $\rightarrow$ Resistencia teñida en rojo de advertencia `#fb7185`).
  - Detección de incremento/reducción de resistencia si no hay daño.
  - Planeswalkers: Lealtad teñida en violeta amatista `#d8b4fe`, verde si subió o rojo si bajó respecto a la inicial.
  - Batallas: Defensa teñida en ámbar `#fbbf24`.

### C. Mareo de Invocación No Obstructivo (`CardPanelRenderModeImage.java`)
- Sustitución del sprite completo de `overlayPanel` por un panel vectorial personalizado:
  - Niebla mística etérea en la base de la carta (`GradientPaint` azul-índigo suave con alpha 60).
  - Badge circular de 22-26 px en la esquina superior derecha (`#0f172a`, aro dorado `#f59e0b` e icono de vórtice antialiased).
  - Tooltip: *"Mareo de invocación: Esta criatura no puede atacar ni usar {T} este turno."*
  - El arte de la carta queda 100% visible y libre de tachones.

### D. Centrado y Antialiasing de Contadores (`CardPanelRenderModeImage.java`)
- En `getCounterImageWithAmount()`:
  - Activado `KEY_TEXT_ANTIALIASING` y `KEY_ANTIALIASING`.
  - Centrado preciso del número con `FontMetrics`.
  - Doble pasada (sombra oscura + número blanco nítido) para máxima legibilidad sobre cualquier fondo.

### E. Flechas Cinemáticas de Combate y Selección (`Arrow.java`)
- En `paintComponent()`:
  - Sombra exterior suave (`Color(0,0,0,75)` con trazo de 4 px).
  - Relleno con gradiente cinemático a lo largo de la flecha (origen semitransparente con alpha 120 $\rightarrow$ punta luminosa con alpha 245).
  - Reborde antialiased interior de 1.2 px (`Color(255,255,255,175)`).

### F. Jerarquía Visual en el HelperPanel (`HelperPanel.java`)
- En `RoundedDecisionButton`:
  - Botones positivos (OK, Done, Yes, Hecho, Sí): Gradiente verde esmeralda con borde esmeralda.
  - Botones negativos (Cancel, No, Pass): Gradiente carmesí con borde coral.
  - Botones especiales (Special, Undo): Gradiente ámbar dorado.
  - Botones estándar: Gradiente slate/acero.

---

## 3. Evidencias de Verificación

- Parche: `Laboratorio\parches\2026-11-02-battlefield-visual-modernization-v29.patch` (19.195 bytes).
- Backup: `Instalacion-XMage\xmage\backup-jars-1.4.63-20261102_battlefield\mage-client-1.4.63.jar`.
- Despliegue: `Instalacion-XMage\xmage\mage-client\lib\mage-client-1.4.63.jar`.

