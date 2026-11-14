# HANDOFF — plantilla de continuación entre workers

> **Norma del proyecto (Kabutosan):** cualquier tarea que quede a medias deja un
> `HANDOFF-<fecha>-<slug>.md` en `J:\MTG\Laboratorio\` con el **punto exacto** de
> continuación, para que el siguiente worker, harness o modelo (Perch, Hermes, DSH,
> CommandCode, lo que sea) lo retome sin repetir trabajo ni re-descubrir el contexto.
>
> Reglas de oro del handoff:
> 1. **Punto exacto, no narración.** Fichero:línea, comando, SHA256, PID, puerto.
>    «Terminé el análisis» no es un punto; «el parche está en `parches\X.patch` y el
>    jar `2975b2af…` está verificado en lab, falta copiarlo a `mage-client\lib`» sí.
> 2. **Evidencia verificable.** Cada afirmación importante lleva comando o ruta de log.
> 3. **Pendiente = siguiente acción**, con el chequeo que la valida.
> 4. **Rollback** = comando exacto o copia de seguridad a restaurar.
> 5. **No rehacer lo ya hecho.** El handoff dice qué está probado y por qué.

## Copia y pega esto para crear un handoff nuevo

```markdown
# HANDOFF <fecha> — <slug>

**Tarea:** <una frase>
**Estado:** EN CURSO / BLOQUEADO / LISTO-PARA-DESPLEGUE / DESPLEGADO
**Worker anterior:** <modelo/harness>   **Fecha:** <aaaa-mm-dd hh:mm>

## Contexto mínimo
- <qué se pidió y por qué, 2-3 líneas, sin re-contar la historia>

## Hecho y verificado (con evidencia)
- <cambio/compilación/prueba> → evidencia: <ruta de log, SHA256, captura>

## Punto exacto de continuación
1. <primer comando o paso que ejecuta el siguiente worker, copiable tal cual>
2. <siguiente>

## Pendiente (por este orden)
- [ ] <acción> → se da por buena si <chequeo concreto>
- [ ] <acción>

## Riesgos / prohibido
- <p. ej. "no tocar el servidor 17171 ni su base de datos">

## Rollback
- <comando o carpeta de backup>

## Artefactos
- Parche: <ruta>
- Jar: <ruta>  SHA256: <hash>
- Evidencia: <rutas de logs/capturas>

## Prompt listo para el siguiente worker
> <el prompt completo, autocontenido, para pegar en cualquier harness/modelo>
```

## Índice de handoffs

| Handoff | Tarea | Estado |
|---|---|---|
| `HANDOFF-2026-11-14-v36-despliegue.md` | Desplegar el fix v36 (import .dck UTF-8) en la instalación oficial | ver ficha |
