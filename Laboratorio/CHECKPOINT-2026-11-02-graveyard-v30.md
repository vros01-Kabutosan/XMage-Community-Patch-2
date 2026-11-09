# Checkpoint: Unificación Visual del Cementerio y Búsqueda de Biblioteca (v30)
Fecha: 2026-11-02 11:15 CEST
Rama: `upgrade/official-1.4.63V1`

## Resumen del cambio

Kabutosan solicitó que la vista de la ventana del cementerio sea exactamente igual a la vista de búsqueda de biblioteca (columnas apiladas en cascada vertical), eliminando la disposición horizontal plana que ocurría cuando había menos de 10 cartas y el fondo gris claro vacío debajo de las cartas.

### Cambios técnicos implementados

1. **`Mage.Client/.../dialog/GraveyardCardsDialog.java`:**
   - Se sustituyó `cardArea.loadCards(...)` por `cardArea.loadCardsNarrow(showCards, bigCard, gameId)`. Esto garantiza que las cartas se presenten siempre en columnas verticales apiladas idénticas a la búsqueda de biblioteca (`loadCardsMany`), incluso con 1 a 9 cartas en el cementerio.
   - Dimensionamiento adaptativo y proporcional al número de columnas (`titleMinW = 280`), evitando ventanas excesivamente anchas o con grandes áreas vacías.
   - Integración con el tema oscuro de la aplicación: fondo `new Color(22, 26, 35)`.

2. **`Mage.Client/.../cards/CardArea.java`:**
   - Soporte para ancho mínimo personalizado (`customMinWidth`), permitiendo que el cementerio ajuste su ancho de forma compacta según las columnas presentes sin imponer el mínimo por defecto de 480 px.
   - Nuevo método `setDarkTheme(boolean enabled)` que aplica de manera consistente el fondo oscuro Obsidian (`new Color(22, 26, 35)`) a `CardArea`, `JScrollPane` y su `Viewport`, eliminando el fondo gris claro de Swing.
   - Registro dinámico de `cardColumns`.

3. **`Mage.Client/.../dialog/ShowCardsDialog.java`:**
   - Activación de `setDarkTheme(true)` y fondo oscuro `new Color(22, 26, 35)` para que la propia ventana de búsqueda de biblioteca comparta la misma estética moderna y limpia.

---

## Verificación en Laboratorio y Despliegue Oficial

1. **Compilación:**
   - `mvn -B -DskipTests -pl Mage.Client,Mage.Server package` -> **BUILD SUCCESS** en 4.5 s.
2. **Laboratorio aislado (`LAB-UI-1.4.62-V2`, puerto 17191):**
   - Montado con `./montar-lab.sh`.
   - Servidor arrancó en puerto 17191 tras 21 s.
   - Cliente conectó (`Connect: DONE`, `Logging: DONE` como `KabutosanLab`).
   - Procesos de laboratorio detenidos limpiamente.
3. **Despliegue Oficial (`J:\MTG\Instalacion-XMage`):**
   - Backup creado en `Instalacion-XMage\xmage\backup-jars-1.4.63-20261102_graveyard\`.
   - Jar desplegado en `Instalacion-XMage\xmage\mage-client\lib\mage-client-1.4.63.jar`.
   - Hash SHA256 verificado: `91669E22CB89A1455E6E23A1026D6A356C83E393210116CD5E78E2D76E72027B`.
   - Servidor oficial (puerto 17171, PID 21848) respetado sin tocar.
   - Cliente oficial reiniciado con el nuevo jar.
4. **Artefactos:**
   - Parche consolidado en `Laboratorio\parches\2026-11-02-graveyard-library-view-v30.patch`.
   - `LEEME.md` actualizado con el mod v30.

