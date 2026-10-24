# XMage Community Patch — proyecto de Kabutosan

Este repositorio contiene una versión modificada y jugable de XMage. La base oficial se conserva como referencia, pero la rama principal del proyecto es `main`.

## Estructura de ramas

- `main`: versión modificada que se debe compilar y probar.
- `official-base`: referencia local de la base oficial de XMage.
- `archive/official-main-20260906`: copia recuperable de la base oficial anterior.
- `archive/mod-history-20260906`: copia recuperable del historial modificado anterior.
- `upstream/main`: fuente oficial externa de XMage.

No se deben mezclar cambios directamente en `official-base`. Para una modificación nueva se trabaja sobre `main`, se prueba y se crea un commit separado.

## Qué contiene la versión modificada

- Interfaz de juego compacta con panel de decisiones redondeado y contenido centrado.
- Barra de fases y distintivo de turno integrados en la interfaz.
- Carga de la mano serializada en Swing para evitar que las cartas desaparezcan al reordenarlas.
- Protección contra respuestas asíncronas antiguas de carga de imágenes.
- Conservación del mazo seleccionado en el Deck Editor al crear una partida.
- Ajustes de cartas apiladas, indicadores de habilidades y renderizado visual.

La lógica de reglas y el servidor de XMage siguen siendo la base oficial salvo los cambios explícitos del proyecto.

## Regla de trabajo segura

1. Comprobar `git status` antes de tocar nada.
2. Crear una rama de experimento desde `main`.
3. Cambiar una sola funcionalidad.
4. Compilar el cliente y el servidor.
5. Probar arranque, Deck Editor, lobby, mano, reordenación, fases y cartas.
6. Integrar solo si la versión estable sigue funcionando.

La autoría histórica de XMage se conserva deliberadamente. Que aparezcan autores upstream no significa que sean colaboradores actuales de este mod; son la procedencia del código base.

## Recuperación

Para volver al estado modificado anterior a esta reorganización existe `archive/mod-history-20260906`. Para consultar la base oficial anterior existe `official-base` y también `archive/official-main-20260906`.

## Build

El código fuente está en `01-FUENTE`. Las herramientas y validaciones del proyecto se mantienen fuera del repositorio fuente, en las carpetas numeradas del proyecto padre.
