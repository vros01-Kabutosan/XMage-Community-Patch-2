# CHECKPOINT - 2026-10-31 (Migración Oficial XMage 1.4.63V1)

## 1. Resumen Ejecutivo de la Migración

- **Versión upstream oficial:** `xmage_1.4.63V1` (Commit upstream: `79aec614fc2`, 31 de Octubre de 2026).
- **Rama de trabajo del fork:** `upgrade/official-1.4.63V1` (commit `68852b5e00c`).
- **Protocolo aplicado:** Laboratorio aislado (`LAB-UI-1.4.62-V2`, puerto 17191) -> Smoke Test 100% OK -> Backup oficial previo -> Despliegue en `J:\MTG\Instalacion-XMage`. Servidor oficial (puerto 17171) nunca tocado.

---

## 2. Novedades Integradas de la Versión Oficial 1.4.63V1

1. **268 Cartas Nuevas y Sets Actualizados:**
   - Base de datos local ampliada de 589 a 591 sets y de 93.420 a 94.787 cartas indexadas.
   - Sets añadidos/actualizados: `FDN`, `TMT` (Teenage Mutant Ninja Turtles), `TRK` (Star Trek), `FIN` (Final Fantasy), `ECL`, etc.
2. **Sistema de Valoración de Draft de 17Lands:**
   - 34 archivos `.csv` de ratings oficiales integrados en `Mage/src/main/resources/ratings/`.
   - Motor de sugerencias de draft automático y selector para bots (`RatingDraftPickSuggester`, `DraftPickSuggest`).
3. **Mejoras en el Motor y Animaciones:**
   - Refactorización asíncrona de animación de eliminación de permanentes (`Plugins.instance.onRemoveCard(...).thenRunAsync(...)`).
   - Fixes de soporte de descarga de imágenes en Scryfall y Gatherer.

---

## 3. Preservación Íntegra de Todos los Mods de Kabutosan (Solo Sumar, Nunca Restar)

Todos los mods existentes se trasladaron y verificaron sin pérdida ni alteración:

1. **FeedbackPanel.java:** Protección de ancho de chat (`PREFERRED_WIDTH = 1024`).
2. **GamePanel.java:**
   - Barra de fases centrada y compacta bajo la mano.
   - Píldoras de resaltado (amarillo activo, verde skip).
   - Fila de accesos directos F4/F5/F7/F9/F10/F11 junto a CONCEDE.
   - Recuperación y autocuración de divisores.
   - Integrado con la nueva API de cancelación de animaciones upstream `Animation.cancelRunningAnimations(this.gameId)`.
3. **HelperPanel.java:** Superficie de decisión centrada, insignia de turno compacta.
4. **GraveyardCardsDialog.java:** Ventana clon de búsqueda y visualización no modal de cementerio.
5. **MageServerImpl.java / Main.java:** Optimización de inicialización inmediata de tipos de juego (`markGameTypesLoadFinished`).
6. **DragCardGrid.java (v1.6 + v1.7):**
   - Selector moderno de edición con iconos, ordenación y previsualización asíncrona.
   - Barra de tierras básicas rápidas (`{W}`, `{U}`, `{B}`, `{R}`, `{G}`, `{C}`).
   - Atajos de teclado completos (`+`, `-`, `Supr`, `E`, `S`, `Ctrl+A`, `Esc`).
   - Ajuste de cantidad por rueda de ratón (`Ctrl + Wheel` y sobre el contador).
   - Desglose en vivo de la composición del mazo (criaturas, tierras, hechizos).
7. **Diálogos de Soporte UI:** `Cards.java`, `CardArea.java`, `MageDialog.java`, `HoverButton.java`, `KeyboundButton.java`, `CardInfoWindowDialog.java`, `ShowCardsDialog.java`, `DeckDownloaderPane.java`, `AiDeckLibrary.java`.

---

## 4. Evidencia de Laboratorio y Smoke Test (Puerto 17191)

- **Compilación Maven:** 40 módulos compilados exitosamente (`mvn install -DskipTests`).
- **Tests unitarios:** `DragCardGridTest` (3/3 pasados, 0 fallos).
- **Servidor de Laboratorio (PID 36916, puerto 17191):**
  - Sets cargados: 591
  - Cartas cargadas: 94.787
  - Plugins cargados: 17 game types, 21 tourneys, 3 players, 23 cubes, 52 decks (0 errores).
  - Escuchando en `0.0.0.0:17191/?serializationtype=java&maxPoolSize=300`.
- **Cliente de Laboratorio (PID 40384, puerto 17191):**
  - Arrancó en 24s.
  - Conexión y autenticación automática: `Logging: as username Kabutosan to server localhost:17191 -> Logging: DONE`.
- **Parada limpia:** `parar-lab.sh` detuvo los procesos aislados y restauró el puerto original en registro (`17171`).

---

## 5. Despliegue en Instalación Oficial (`J:\MTG\Instalacion-XMage`)

### A. Backup Previo Preservado
- Directorio de respaldo: `J:\MTG\Instalacion-XMage\xmage\backup-jars-1.4.62-pre-1.4.63-20261031\`
- Contiene los jars 1.4.62 completos de cliente y servidor antes del despliegue.

### B. Hashes SHA-256 de los Jars Desplegados

| Jar | Destino | SHA-256 |
|---|---|---|
| `mage-client-1.4.63.jar` | `Instalacion-XMage/xmage/mage-client/lib/` | `ACC3C26BD57A484313DB48BD93AF6A316221A3D11B60BCF41B75BE89CD475B41` |
| `mage-server-1.4.63.jar` | `Instalacion-XMage/xmage/mage-server/lib/` | `6E07169949490C8A2504FC3AA4A7CAC7CD8DB86EFEC29E4B57B4587FEB04638C` |
| `mage-sets-1.4.63.jar` | `Instalacion-XMage/xmage/mage-client/lib/` & `mage-server/lib/` | `57732018718007863645C8D09554C112511671678BE2C0385629E4507C34CDD7` |
| `mage-1.4.63.jar` | `Instalacion-XMage/xmage/mage-client/lib/` & `mage-server/lib/` | `C0D80E53B18F12D75CE2FF5DCEF6146194DAA35E815EBCC9A1D8866160161476` |
| `mage-common-1.4.63.jar` | `Instalacion-XMage/xmage/mage-client/lib/` & `mage-server/lib/` | `26FDDE1E187515F051F1076211C1ED60EEF7208E20E3CA859422DF4A0EE285F5` |
| `mage-player-ai-1.4.63.jar` | `Instalacion-XMage/xmage/mage-server/lib/` | `A8BC7262D9A74E9689408F5254A18D1610EBD26992F7DC57DFD8B5509CE4DF16` |
| `mage-deck-constructed-1.4.63.jar` | `Instalacion-XMage/xmage/mage-client/lib/` & `mage-server/plugins/` | `FBCDAA12CEB00244436B044238BEA8DCDA0472ED38A739C79FD0D6045E163B48` |

- Plugins de servidor: los 27 plugins compilados en 1.4.63 fueron desplegados en `Instalacion-XMage/xmage/mage-server/plugins/`.
- Configuración de servidor: `config.xml` actualizado para referenciar los jars 1.4.63 manteniendo puerto `17171`. Backup previo guardado como `config.xml.backup-1.4.62`.
- Servidor oficial 17171: continuó en ejecución ininterrumpida sin sufrir ninguna alteración.

---

## 6. Sincronización Git y Parches

- **Parche consolidado:** `J:\MTG\Laboratorio\parches\2026-10-31-migracion-1.4.63V1.patch` (2,67 MB).
- **Rama remota:** `origin/upgrade/official-1.4.63V1` (push completado).
