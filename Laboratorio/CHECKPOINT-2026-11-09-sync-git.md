# CHECKPOINT - 2026-11-09 (Sincronización git del proyecto con el remoto XMage-Community-Patch-2)

## 1. Contexto

Kabutosan pidió: «coge mi instalación y tal como está ahora sincroniza el git de mi
repositorio https://github.com/vros01-Kabutosan/XMage-Community-Patch-2». No hubo cambios
de código ni de la instalación en esta sesión: la sincronización es de estado git y
documentación. `Instalacion-XMage` y el servidor oficial (17171) no se tocaron; sin build
ni deploy.

## 2. Auditoría previa (hallazgos)

- Dos repos locales comparten el mismo remoto:
  1. `J:\MTG\Laboratorio\XMage-1.4.62V1-src` (código del fork): rama `upgrade/official-1.4.62V1`
     (97550128868) y rama activa `upgrade/official-1.4.63V1` (05b8c76cf2a, último commit de código
     = UI Mod v30, cementerio/búsqueda de biblioteca) — **ya idénticas al remoto**, árbol limpio.
     No hizo falta subir nada de código.
  2. `J:\MTG` (proyecto/docs, rama `main`): 26 commits propios (2026-10-24 → 2026-11-02) **sin
     relación de historia** con el `main` del remoto (2 commits de 2026-09-06, autor «v r»:
     instantánea del árbol fuente + documentación de estructura; misma instantánea conservada en
     la rama `backup/pre-cleanup-main-20260920`).
- Estado pendiente en `main` local antes de la sincronización:
  - 6 documentos modificados (LEEME, 3 checkpoints, propuesta battlefield, taste.md).
  - Sin trackear: checkpoints 2026-11-01 y 2026-11-02-graveyard-v30, parches v20/v21/v22/v23/v30,
    carpeta `mods/`, `.kilo/` y `acl-recovery-20261101/`.
  - 121.974 ficheros legado dados de baja en disco el 2026-11-01 (reorganización
    `reorganizar-2026-11-01.ps1`; archivados en `_archivo/20261101/`) que seguían trackeados en
    `Laboratorio/Proyecto-XMage` (T2-FIX-V3.6, VERIFY-FIX-V3.5, ROOM-FIX-V3.4, PROYECTO-ANTIGUO).
- Integridad verificada: ningún blob ≥90 MB alcanzable desde `main`; `rev-list` exit 0 en todas
  las pruebas. Nota técnica: los ~122k ficheros legado colapsan a ~42k objetos únicos porque los
  tres árboles son copias casi idénticas del mismo fuente (`rev-list` cuenta objetos deduplicados,
  `ls-tree` cuenta rutas).

## 3. Acciones ejecutadas

1. `.gitignore`: añadidas reglas para `.kilo/` (worktrees de herramienta) y
   `Laboratorio/acl-recovery-*/` (recuperaciones internas). No se versionan a partir de hoy.
2. Commit de contenido `1af3f8be601` — «sync: estado actual del proyecto»: mods/, parches nuevos
   (v20, v21, v22, v23, v30), checkpoints 2026-11-01 y 2026-11-02-graveyard-v30 y actualización de
   LEEME/checkpoints/propuesta pendientes desde la sesión v28-v30.
3. Commit de baja del legado `bb92a54c6fd` — 121.974 ficheros de `Laboratorio/Proyecto-XMage`
   fuera del árbol. El contenido sigue consultable en `_archivo/20261101/` y la historia git lo
   conserva (solo sumar, nunca restar).
4. Merge `ddfd96b6e4b` — `git merge -s ours --allow-unrelated-histories origin/main`: el `main`
   histórico del remoto (6e870991089) queda como ancestro del merge (no se pierde nada y el push
   es fast-forward, sin force) y el árbol resultante es fiel al disco.
5. Commit de documentación de esta misma sesión: este checkpoint + sección de LEEME.
6. `git push -u origin main` → `main` en GitHub pasa a reflejar el estado actual del proyecto.

## 4. Verificación

- `git log --graph`: `ddfd96b6e4b` tiene como padres `bb92a54c6fd` (historia real del proyecto) y
  `6e870991089` (main histórico del remoto). `git merge-base HEAD origin/main` = 6e870991089 ✔.
- Estado de trabajo limpio tras cada commit (`git status --porcelain` = 0 entradas).
- Tras el push: `git ls-remote origin` debe mostrar `main` = commit de documentación, y las ramas
  de código sin cambios (`upgrade/official-1.4.62V1` = 97550128868, `upgrade/official-1.4.63V1` =
  05b8c76cf2a).

## 5. Reglas aplicadas y notas para futuras sesiones

- Backup natural: la instantánea del `main` remoto sigue alcanzable en la rama
  `backup/pre-cleanup-main-20260920` y como ancestro del merge. Nada fue borrado del remoto.
- `Instalacion-XMage/` sigue sin versionarse por decisión del proyecto (binarios pesados); su
  estado se documenta en los checkpoints y los jars desplegados tienen backup en
  `Instalacion-XMage\xmage\backup-jars-*`.
- Los 9 worktrees de `.kilo/` (sesiones de otra herramienta) apuntan al main histórico del remoto;
  quedan ignorados por git y no se tocan.
- La rama de trabajo del código sigue siendo `upgrade/official-1.4.63V1` en
  `Laboratorio\XMage-1.4.62V1-src` (v30 desplegado y documentado en el checkpoint 2026-11-02).
