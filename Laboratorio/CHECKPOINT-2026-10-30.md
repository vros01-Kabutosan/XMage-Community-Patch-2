# CHECKPOINT UI 1.4.62 V2 - 2026-10-30 (v1.6 Selector de Edición)

## 1. Estado y despliegue

| Componente | Versión | SHA256 | Estado |
|---|---|---|---|
| Servidor OFICIAL (NO TOCAR) | 1.4.62 | `f89bf09248a4057bb98d648eb5bb8a49c2438d74243010e17018b4a5b728f0ad` | Intacto, puerto 17171 no tocado |
| Cliente OFICIAL | 1.4.62 v1.6 | `a1c057a8517eb1ff4169c7f0e1646614685a29c6949130b09f1f6ba4035d2251` | Desplegado en `Instalacion-XMage/xmage/mage-client/lib/` |
| Backup previo v1.5 | 1.4.62 v1.5 | `f25c7bd1b2941e84319963f9e42eb3e20177e0e3ccc6ac2b662e5fc844834c80` | Preservado en `backup-jars-1.4.62-v16-20261030/cliente/` |
| Laboratorio | 1.4.62 v1.6 | `a1c057a8517eb1ff4169c7f0e1646614685a29c6949130b09f1f6ba4035d2251` | Montado y probado con `montar-lab.sh` / `lanzar-lab.sh` |

## 2. Mejoras técnicas implementadas en el Selector de Edición (v1.6)

Archivo modificado: [`Mage.Client/src/main/java/mage/client/cards/DragCardGrid.java`](file:///J:/MTG/Laboratorio/XMage-1.4.62V1-src/Mage.Client/src/main/java/mage/client/cards/DragCardGrid.java#L1645-L2100)

1. **Símbolos oficiales de expansión**: Se renderiza el icono oficial de la edición coloreado por rareza mediante `ManaSymbols.getSetSymbolImage(set, rarity)` con caché interna para máxima velocidad de scroll.
2. **Año y cronología clara**: Cada entrada incluye su año de salida (`ExpansionSet.getReleaseYear()`) y rareza con color semántico.
3. **Modos de ordenación configurables**: Desplegable con 4 criterios:
   - Más reciente primero (Fecha ↓) — predeterminado.
   - Más clásica primero (Fecha ↑).
   - Código de edición (A-Z).
   - Nombre de edición (A-Z).
4. **Búsqueda enriquecida**: Filtro en tiempo real por código, nombre, año (ej. `2024`), número de carta o rareza.
5. **Previsualización asíncrona (`SwingWorker`)**: La carga de imágenes se ejecuta fuera del hilo EDT de Swing con cancelación de tareas obsoletas, eliminando cualquier tirón al navegar la lista.
6. **Soporte de doble cara (DFCs / MDFCs)**: Detección automática con botón "Ver reverso/anverso" y alternancia por teclado (barra espaciadora).
7. **Confirmación ágil**: Doble clic sobre cualquier elemento o pulsación de la tecla `ENTER` confirman y cierran inmediatamente el diálogo. Cierre limpio con tecla `ESC`.
8. **Alcance de sustitución configurable**:
   - Opción de reemplazar todas las copias en la zona actual.
   - Opción de reemplazar únicamente la carta seleccionada.
   - Casilla para propagar la edición al mazo complementario (Maindeck <-> Sideboard) si contiene copias del mismo nombre.
9. **Optimización con `CardInfo.getCardNumberAsInt()`**: Ordenación numérica directa sin sobrecarga de regex repetitiva, con fallback seguro ante nulos.

## 3. Pruebas y Smoke

- Unit test añadido: [`DragCardGridTest.java`](file:///J:/MTG/Laboratorio/XMage-1.4.62V1-src/Mage.Client/src/test/java/mage/client/cards/DragCardGridTest.java) — ejecuta y valida ordenación y tolerancia a nulos (OK, 0 fallos).
- Smoke en el laboratorio (`LAB-UI-1.4.62-V2`):
  - Montaje aislado con `./montar-lab.sh` exitoso.
  - Servidor de laboratorio en puerto 17191 arrancado y verificado.
  - Cliente de laboratorio ejecutado y verificado con base de datos completa de 589 sets y 93.992 cartas.
  - Parada limpia con `./parar-lab.sh`.
- Parche generado en `Laboratorio/parches/2026-10-30-selector-edicion-v16.patch`.

