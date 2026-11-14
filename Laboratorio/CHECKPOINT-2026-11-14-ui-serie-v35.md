# CHECKPOINT 2026-11-14 — UI de serie v35 (Fase 2 del informe: FlatLaf opcional, overrides de tema, opt-ins y capilar)

Continuación del plan por fases (v33 = Fase 0, v34 = Fase 1, este = **Fase 2**). 7 ficheros, +218/−16.

## Cambios aplicados

| # | Fichero | Cambio | Riesgo |
|---|---|---|---|
| 1 | `Mage.Client/pom.xml` | dependencia `com.formdev:flatlaf:3.5.4` + **maven-shade-plugin** que la mete DENTRO de `mage-client-1.4.63.jar` (355 clases verificadas dentro del jar) → sin jars nuevos en lib/, lab ni instalación | bajo (additivo) |
| 2 | `util/gui/GuiDisplayUtil.java` | `refreshThemeSettings()` ahora lee la preferencia nueva `lafSelection` (default `"Nimbus"`): Nimbus sigue siendo el camino exacto de siempre; si el usuario elige FlatLaf se carga **por reflexión** (`FlatLightLaf`/`FlatDarkLaf`) con fallback automático a Nimbus si la clase no estuviera. Con FlatLaf activo se mapean los colores del ThemeType a claves FlatLaf (`Panel.background`, `Table.background`, `Button.background`, `ToolTip.background`, selección, etc.) | bajo (opt-in) |
| 3 | `dialog/PreferencesDialog.java` | nueva fila **"Look and feel:"** en la pestaña GUI Theme (Nimbus (default) / FlatLaf modern / FlatLaf dark), guardado en `lafSelection` con el mismo ciclo saveTheme→loadTheme→reload; se aplica al instante (el aviso de restart de colores sigue igual) | bajo |
| 4 | `themes/ThemeType.java` + GuiDisplayUtil | **A2 temas por fichero**: `~/.xmage/theme-overrides.properties` opcional (`NombreTema.control=49,51,56` etc.) fusionado en `refreshThemeSettings()`; recargado en cada `reload()`; sin fichero, comportamiento idéntico | bajo (additivo) |
| 5 | `game/BattlefieldPanel.java` | **B5**: `isOptimizedDrawingEnabled()` pasa a opt-in por flag `-Dxmage.battlefield.optimized=true` (default = comportamiento histórico `false`). No se activa por defecto porque su verificación real exige partidas con iconos/flechas solapadas (smoke de lobby no cubre z-order) | nulo por default |
| 6 | `util/SoftValuesLoadingCache.java` | **B4**: tope de entradas configurable `-Dxmage.cache.maxEntries` (default 3000 = actual) | nulo por default |
| 7 | `deckeditor/collection/viewer/MageBook.java` | **E7 parcial**: caption/info del libro escalan con `dialogFont` (antes 25f/17f fijos). La geometría del lienzo se queda como está: va ligada a los artwork fijos `book_*.jpg` y tocarla sin prueba visual rompería el layout | bajo |

## Bug cazado durante el smoke (corregido en el mismo v35)

La primera verificación aplicaba **FlatLaf Light** cuando la preferencia decía `FlatLaf dark`:
el código comprobaba `contains("Dark")` con D mayúscula. Corregido a `toLowerCase().contains("dark")`
y re-verificado (log `Look and feel applied: FlatLaf Dark`).

## Smoke en laboratorio (LAB-UI-1.4.62-V2, 17191, prefs `XMageLabUIv2`) — doble ruta

1. **Ruta default (Nimbus, sin tocar nada)**: `montar-lab.sh` + arranque normal → `Using GUI theme: Default`,
   `Connect: DONE`, cero excepciones; lobby idéntico a v34 (captura `129-v35-nimbus-lab.png`).
   *Nota de evidencia*: el script de captura tenía el nombre 128 fijo y sobreescribió
   `128-v34-lobby-lab.png` con esta misma escena (visualmente idéntica por definición del default);
   se copia a 129 y se deja constancia.
2. **Ruta opt-in (FlatLaf dark)**: siembra `lafSelection="FlatLaf dark"` vía API de Preferences
   (`LafSeed`, el `reg add` directo NO es legible por Java — anotado para futuros smokes) →
   `Look and feel applied: FlatLaf Dark` ×2, `Connect: DONE`, cliente vivo, cero excepciones →
   captura `130-v35-flatlaf-dark-lab.png` (cabeceras/titles/toolbar oscuras; la tabla sale clara
   porque el **tema de colores** sigue siendo Default: L&F y tema son independientes; para UI
   totalmente oscura: FlatLaf dark + tema Carbon Fiber, probable por Kabutosan en manual).
3. Limpieza: `LafSeed clear` (prefs restauradas), `parar-lab.sh` (solo PIDs del lab; 17171 intacto).

## Build final para despliegue

`mvn -B -q -DskipTests -pl Mage.Client clean package` → OK,
`mage-client-1.4.63.jar` SHA256 `B2517155FF708DA9999F24987E3D5749B33CAC7870C1AE7FE1BBB02F3FD04E85`
(con FlatLaf sombreado dentro).

## Pendientes manuales (opt-in, no bloquean el despliegue)

- Probar FlatLaf modern/dark en uso real (partidas, editor, cementerio): los 4 workarounds Synth
  siguen activos solo bajo Nimbus; bajo FlatLaf el pintado custom de cartas/mesa no cambia
  (pintura propia), pero conviene un vistazo a diálogos internos translúcidos.
- Probar `-Dxmage.battlefield.optimized=true` en una partida EDH antes de plantearse activarlo.
- Documentar `theme-overrides.properties` si Kabutosan lo usa (ejemplo: `Carbon Fiber.info=...`).

## Parche y rollback

`Laboratorio\parches\2026-11-14-ui-serie-v35.patch` (7 ficheros, +218/−16); rollback = apply -R + rebuild.
El pom añade shade: si algún día se quisiera deshacer FlatLaf del todo, basta con el revert del parche.

## Despliegue (2026-11-14 — cliente oficial cerrado, no hubo nada que cerrar)

- Backup del jar saliente: `Instalacion-XMage\xmage\backup-jars-1.4.63-v35-20261114\cliente\mage-client-1.4.63.jar` (22.572.170 bytes).
- Desplegado `mage-client-1.4.63.jar` v35 (con FlatLaf sombreado dentro): SHA256 `B2517155FF708DA9999F24987E3D5749B33CAC7870C1AE7FE1BBB02F3FD04E85` (MATCH).
- Servidor 17171 y su base de datos: sin tocar. No se añadió ningún jar nuevo a la instalación.
