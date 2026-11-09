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
2. Commit de contenido `37bbd859289` — «sync: estado actual del proyecto»: mods/, parches nuevos
   (v20, v21, v22, v23, v30), checkpoints 2026-11-01 y 2026-11-02-graveyard-v30 y actualización de
   LEEME/checkpoints/propuesta pendientes desde la sesión v28-v30.
3. Commit de baja del legado `b29aa7dafcd` — 121.974 ficheros de `Laboratorio/Proyecto-XMage`
   fuera del árbol. El contenido sigue consultable en `_archivo/20261101/` y la historia git lo
   conserva (solo sumar, nunca restar).
4. Merge `38bbb2550c7` — `git merge -s ours --allow-unrelated-histories origin/main`: el `main`
   histórico del remoto (6e870991089) queda como ancestro del merge (no se pierde nada y el push
   es fast-forward, sin force) y el árbol resultante es fiel al disco.
5. Commit de documentación de esta misma sesión: este checkpoint + sección de LEEME.
6. Primer `git push -u origin main` RECHAZADO por el hook de GitHub (GH001, pre-receive): la
   historia arrastraba 3 ficheros generados de la era 1.4.61 por encima del límite de 100 MB por
   fichero.
7. Reescritura de la historia LOCAL de `main` con `git filter-branch --index-filter` (2 pasadas)
   retirando de TODA la historia los 3 H2: `.../RC1.1-BLINDADA/client/db/cards.h2.mv.db` (292 MB),
   `.../RC1.1-BLINDADA/xmage/mage-server/db/cards.h2.mv.db` (293 MB) y
   `.../RC1.1-BLINDADA/xmage/mage-client/db/cards.h2.mv.db` (292 MB). Los SHAs definitivos tras la
   reescritura son los citados en los puntos 2-4; la cadena original queda SOLO en local, en la
   rama `backup/main-pre-rewrite-20261109` (72d35df8f48) y en `refs/original/*`. Esa historia nunca
   llegó a GitHub, así que la reescritura no afecta a nadie más. Los 3 .db eran datos generados de
   la era 1.4.61 (regenerables); no estaban ya en ninguna ruta del disco.
8. Segundo `git push -u origin main` → `main` en GitHub pasa a reflejar el estado actual del
   proyecto (fast-forward, sin force). Verificación previa al push: mayor blob alcanzable desde
   `main` = 79,4 MB (solo warning de GitHub, sin bloqueo); 0 ficheros >100 MB.

## 4. Verificación

- `git log --graph`: `38bbb2550c7` tiene como padres `b29aa7dafcd` (historia real del proyecto) y
  `6e870991089` (main histórico del remoto). `git merge-base HEAD origin/main` = 6e870991089 ✔.
- Estado de trabajo limpio tras cada commit (`git status --porcelain` = 0 entradas).
- Tras el push: `git ls-remote origin` debe mostrar `main` = último commit de documentación de
  esta sesión, y las ramas de código sin cambios (`upgrade/official-1.4.62V1` = 97550128868,
  `upgrade/official-1.4.63V1` = 05b8c76cf2a).

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
