# CHECKPOINT UI 1.4.62 V2 - 2026-10-31 (v1.7 Modernización Deck Editor)

## 1. Estado y despliegue

| Componente | Versión | SHA256 | Estado |
|---|---|---|---|
| Servidor OFICIAL (NO TOCAR) | 1.4.62 | `f89bf09248a4057bb98d648eb5bb8a49c2438d74243010e17018b4a5b728f0ad` | Intacto, puerto 17171 no tocado |
| Cliente OFICIAL | 1.4.62 v1.7 | `0d59d9d99832c455c1b29b4271fea39d297768c0cd7a29e7e2448372708fe7ec` | Desplegado en `Instalacion-XMage/xmage/mage-client/lib/` |
| Backup previo v1.6 | 1.4.62 v1.6 | `a1c057a8517eb1ff4169c7f0e1646614685a29c6949130b09f1f6ba4035d2251` | Preservado en `backup-jars-1.4.62-v17-20261031/cliente/` |
| Laboratorio | 1.4.62 v1.7 | `0d59d9d99832c455c1b29b4271fea39d297768c0cd7a29e7e2448372708fe7ec` | Montado y probado con `montar-lab.sh` / `lanzar-lab.sh` |

## 2. Mejoras técnicas implementadas en el Deck Editor (v1.7)

Archivo modificado: [`Mage.Client/src/main/java/mage/client/cards/DragCardGrid.java`](file:///J:/MTG/Laboratorio/XMage-1.4.62V1-src/Mage.Client/src/main/java/mage/client/cards/DragCardGrid.java)

1. **Barra de Tierras Básicas Rápidas (`quickLandsPanel`)**:
   - Acceso inmediato en el área de mazo principal mediante 6 botones compactos con símbolos oficiales de maná: `{W}`, `{U}`, `{B}`, `{R}`, `{G}`, `{C}` (Llanura, Isla, Pantano, Montaña, Bosque, Yermos).
   - Clic izquierdo añade +1 copia respetando la edición/arte preferido ya existente en el mazo. Clic derecho o rueda de ratón hacia abajo retira 1 copia.
   - Tooltip dinámico explicativo (`Añadir tierra básica...`).
2. **Atajos de Teclado Profesionales en el Editor de Mazos**:
   - `+` / `=` / teclado numérico `+`: duplica y añade +1 copia a las cartas seleccionadas.
   - `-` / `_` / teclado numérico `-`: quita 1 copia de las cartas seleccionadas.
   - `Supr` / `Retroceso` (`Delete` / `Backspace`): elimina completamente las cartas seleccionadas del mazo.
   - `E`: abre de inmediato el Selector de Edición moderno (v1.6) para la carta bajo el cursor/foco.
   - `S`: mueve las cartas seleccionadas entre el Mazo Principal y el Banquillo (Sideboard).
   - `Ctrl + A`: selecciona todas las cartas de la zona actual.
   - `Esc`: deselecciona todas las cartas.
   - Detección inteligente de foco: los atajos se inhiben automáticamente cuando el cursor del usuario está en cajas de texto de búsqueda (`JTextComponent`).
3. **Ajuste Rápido de Cantidad con Rueda de Ratón (`Ctrl + Wheel`)**:
   - `Ctrl + Rueda arriba` sobre cualquier carta: añade +1 copia instantáneamente.
   - `Ctrl + Rueda abajo` sobre cualquier carta: quita 1 copia instantáneamente.
   - Sin pulsar Ctrl: la rueda se propaga de forma fluida para el scroll normal de la cuadrícula.
   - Rueda de ratón directa sobre el contador numérico de la pila (`countLabel`): suma/resta copias con un solo gesto sin necesidad de Ctrl.
4. **Menú Contextual Enriquecido (Clic Derecho)**:
   - "Añadir copia (+1)" [Atajo: `+`]
   - "Quitar copia (-1)" [Atajo: `-`]
   - "Mover a Banquillo / Mazo Principal" [Atajo: `S`]
   - "Eliminar del mazo" [Atajo: `Supr`]
   - Separador visual y acceso rápido a "Elegir edición..." [Atajo: `E`], "Seleccionar todas las copias", "Invertir selección" y "Ocultar".
5. **Desglose en Vivo de Composición del Mazo**:
   - Etiqueta informativa en tiempo real: `Mazo Principal: X cartas (Criaturas: C, Tierras: T, Hechizos: H)` con tooltip detallado.
   - Para el banquillo: conteo con límite de formato `Banquillo: S / 15 cartas`.
6. **Modernización de Botones Crípticos de Cabecera**:
   - `M` -> `Stats / Maná`
   - `V` -> `Visibilidad`
   - `Select By` -> `Filtrar...`
   - `Sort` -> `Ordenar`

## 3. Pruebas y Smoke

- **Pruebas unitarias**: [`DragCardGridTest.java`](file:///J:/MTG/Laboratorio/XMage-1.4.62V1-src/Mage.Client/src/test/java/mage/client/cards/DragCardGridTest.java) ampliado con `testQuickLandsConfiguration` y `testDragCardGridSelectionAndParentArea` (3/3 tests pasados con éxito, 0 fallos).
- **Compilación Maven**: `mvn -B -q -DskipTests -pl Mage.Client,Mage.Server package` completado con código de salida 0.
- **Laboratorio aislado (`LAB-UI-1.4.62-V2`)**:
  - `montar-lab.sh` ejecutado con éxito.
  - Servidor de laboratorio arrancado en puerto 17191 y verificado con 589 sets cargados.
  - Cliente de laboratorio ejecutado y verificado en entorno de pruebas aislado (`HKCU\Software\XMageLabUIv2`).
  - Parada limpia con `parar-lab.sh`.
- **Despliegue oficial (`Instalacion-XMage`)**:
  - Backup previo del cliente v1.6 en `backup-jars-1.4.62-v17-20261031/cliente/`.
  - Despliegue de `mage-client-1.4.62.jar` (SHA256: `0d59d9d99832c455c1b29b4271fea39d297768c0cd7a29e7e2448372708fe7ec`).
  - Servidor oficial y su base de datos intactos (puerto 17171 no tocado).
- **Parche generado**: `Laboratorio/parches/2026-10-31-deck-editor-v17.patch`.
