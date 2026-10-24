# Forense GitHub remoto — XMage-Community-Patch-2

- Repo: `vros01-Kabutosan/XMage-Community-Patch-2` (https://github.com/vros01-Kabutosan/XMage-Community-Patch-2)
- Fecha de la consulta: 2026-10-13 (UTC), solo lectura. **Cero escrituras remotas.**
- Ruta pedida: `J:\MTG\.zwork\v36-recuperacion\github-remoto.md` — **no escribible**: `.zwork/` está reservada para estado interno del runtime (denegada por la herramienta de archivos y por el shell). Informe depositado en la ruta equivalente sin el prefijo reservado.
- Método: la CLI `gh` está instalada pero **sin autenticar** (`gh auth status` → "You are not logged into any GitHub hosts"; `gh api` rechaza con "please run: gh auth login"). El repo es **PÚBLICO**, por lo que se usó lectura HTTP GET de la API pública de GitHub (api.github.com, mismos datos que sirven las páginas HTML `/pulls`, `/pull/N`, `/actions`). Todas las respuestas HTTP 200.

## 1. Conclusión

**Ningún PR del repo ha sido fusionado jamás** (los 24 PR históricos tienen `merged_at: null`). Los seis PR objetivo (#17, #18, #19, #20, #21, #23) están **ABIERTOS**, no fusionados, y cada uno tiene su check `build` en **FAILURE** sobre su head actual (`mergeable_state: unstable`). Sí existen PR posteriores a los seis: **#22 y #24**, ambos drafts temporales de certificación combinada, hoy **CERRADOS SIN MERGE**. `main` sigue anclado al commit del 2026-09-06 y su CI programado lleva **3 días consecutivos fallando** (11, 12 y 13 de octubre). La afirmación "GitHub Actions GREEN" del checkpoint V3.5 solo se sostiene para el branch combinado `check/v35-full-sixpr-combo` (PR#24), no para los PR individuales ni para `main`.

## 2. Los seis PR objetivo (todos ABIERTOS, ninguno fusionado)

Fuente por PR: `GET https://api.github.com/repos/vros01-Kabutosan/XMage-Community-Patch-2/pulls/<N>` (leído 2026-10-13) y check-runs del head: `GET .../commits/<sha>/check-runs`.

| PR | Estado | Head | Creado (UTC) | Build CI en head | mergeable_state |
|----|--------|------|--------------|------------------|-----------------|
| #17 "Add deck library downloader..." | ABIERTO | `feature/deck-downloader-http-directo-v3@e842ebfc` | 2026-10-09 11:59:47 | **failure** (10-09 12:02:33) | unstable |
| #18 "Point the deck watcher at the engine's real log directory" | ABIERTO | `fix/vigilante-ruta-logs-v3.1@043dfbc2` | 2026-10-09 15:00:17 | **failure** (10-09 15:03:04) | unstable |
| #19 "Start the deck watcher automatically at logon" | ABIERTO | `feat/vigilante-autoinicio-v3.2@bbf7a19c` | 2026-10-10 09:15:51 | **failure** (10-10 09:18:30) | unstable |
| #20 "Restore missing mage.db package and test DB resource (CI build fix V3.3)" | ABIERTO | `fix/xmage-ci-build-v3.3@2d52bfe8` | 2026-10-10 09:42:35 | **failure** (10-10 09:47:17) | unstable |
| #21 "Fix Room spell copies losing the cast half (V3.4)" | ABIERTO | `fix/room-card-copy-stack-v3.4@b8a2ab15` | 2026-10-10 12:10:19 | **failure** (10-10 12:12:30) | unstable |
| #23 "Fix Mage.Verify data drift... (V3.5)" | ABIERTO | `fix/mage-verify-data-v3.5@c7649bed` | 2026-10-10 13:59:16 | **failure** (10-10 14:01:34) | unstable |

Campos de estado en los seis: `state=open`, `draft=false`, `merged=false`, `merged_at=null`, `closed_at=null`, `mergeable=true`. (El `merge_commit_sha` que devuelve la API es un *test merge* que GitHub calcula, no un merge real.) Cada head tiene checks `labeler` (success) y `build` (failure); el `combined_status` REST queda en `pending` porque el check-run `build` no publica estado combinado.

## 3. PR posteriores a los seis

La lista completa `GET .../pulls?state=all&per_page=50&sort=created&direction=desc` devuelve **24 PR en total** (no hay más páginas: 24 < 50). Números > 23:

| PR | Estado real | Detalle | Creado (UTC) | Cerrado (UTC) | CI |
|----|-------------|---------|--------------|----------------|-----|
| #24 | CERRADO SIN MERGE | draft "[TEMPORAL - NO FUSIONAR] Verificacion CI de los 6 PR combinados (#20+#21+V3.5+#17+#18+#19)", head `check/v35-full-sixpr-combo@157908d7` | 2026-10-10 13:59:56 | 2026-10-10 14:13:00 | **success** (run 38057867646, 2026-10-10 13:59:58) |
| #22 | CERRADO SIN MERGE | draft "[TEMPORAL - NO FUSIONAR] Verificacion CI de PR #20 + V3.4 combinados", head `check/room-v34-plus-baseline-v33@58dc2777` | 2026-10-10 12:10:35 | 2026-10-10 12:21:18 | **failure** (run 38051007210, 2026-10-10 12:10:37) |

No existe ningún PR con número > 24. Otros PR abiertos de contexto (anteriores, no parte de los seis): #16 "Consolidate current XMage working line into main" (abierto desde 2026-09-19, actualizado 2026-10-03) y los dependabot #11–#15. Histórico previo: #1–#10 todos cerrados sin merge (los #1–#9 en bloque el 2026-09-06 17:15:28).

## 4. CI (Actions) — últimos 15 runs de 165 totales

Fuente: `GET .../actions/runs?per_page=15` (leído 2026-10-13).

- **main (schedule, workflow "Build and run tests (github)", sha 6e870991):**
  - 2026-10-13 07:33:23 — run 38363396606 — **failure**
  - 2026-10-12 07:09:24 — run 38216185950 — **failure**
  - 2026-10-11 06:56:55 — run 38119717688 — **failure**
- **PR#24 / branch combinado `check/v35-full-sixpr-combo@157908d7`:** run 38057867646 (pull_request) → **success** (2026-10-10 13:59:58); labeler run 38057867683 → success.
- **PR#23 head:** run 38057828264 → failure. **PR#22 head:** run 38051007210 → failure.
- **PR#21 head:** run 38050992003 → failure. **PR#20 head:** run 38042346718 → failure.
- Runs #17/#18/#19: fuera de la ventana de 15; consultados vía check-runs (§2) → todos `build: failure`.
- Mtg Card Fetch Bot (issue_comment): runs 38058729245 (2026-10-10 14:13:02) y 38051647526 (2026-10-10 12:21:20), ambos success — coinciden con los cierres de #24 y #22.

**Nota de contradicción:** el snapshot "V3.5 GREEN" es cierto solo para el build combinado de los seis (PR#24). Los seis PR por separado y `main` están en rojo. No consta si el paso de fallo del combinado #22 a éxito del #24 se debe a corrección real o a condición de carrera; ver §6.

## 5. Estado de `main`

- Fuente: `GET .../branches/main` y `GET .../commits/6e870991...`.
- HEAD: **`6e870991089ed14c45b71d0db28cb0ba3bde803c`** (corto `6e87099`)
- Mensaje: "Document Kabutosan project structure"
- Autor/fecha: v r <vros01-Kabutosan@users.noreply.github.com>, 2026-09-06 17:18:18 UTC.
- `pushed_at` del repo: 2026-10-10 14:13:05 UTC — pero eso refleja pushes a ramas/PR, no a `main`: el HEAD de `main` no se mueve desde el 2026-09-06.
- CI programado de main: failure el 10-11, 10-12 y 10-13 (ver §4).

## 6. Pendiente / límites

- `reviewDecision` y `statusCheckRollup` por GraphQL (lo que pedía el comando `gh pr view`) requieren autenticación: no disponibles sin token. Sustituidos por equivalentes REST (check-runs + combined status), que cubren los mismos hechos.
- No se ha leído el log de por qué el `build` falla en cada PR (los logs de Actions no son accesibles sin token). Candidato para la siguiente pasada si se aporta un `GH_TOKEN`.
- Objetos no consultados por estar fuera de alcance: issues (`open_issues_count: 12`), releases, y el PR#16 por no pertenecer a los seis.
