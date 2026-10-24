# Informe forense de informes y logs locales — Recuperación V3.6 (Proyecto XMage)

- **Fecha de ejecución:** martes 2026-10-13 (≈20:40–21:00, hora local).
- **Modo:** estrictamente **solo lectura** (Get-ChildItem / Select-String / Get-Content y lectura de ficheros). No se modificó, movió ni borró nada; no se ejecutó ningún script del proyecto ni ningún proceso de build/test/t Git. **Única escritura en disco: este fichero.**
- **Raíz analizada:** `J:\MTG\Proyecto-XMage`.
- **Límites aplicados:** ficheros >2 MB ignorados (solo listados por nombre/tamaño cuando procedía); recursión con profundidad máx. 4–5 según pase; en BACKUPS/INSTALACIONES/LEGADO/PROYECTO-ANTIGUO solo nombres con profundidad ≤2.
- **Niveles de evidencia usados en este informe:** `[LECTURA]` = verificado leyendo el fichero/carpeta en esta sesión; `[CRUZADA]` = tomado de los informes hermanos de hoy (`git-local.md`, `github-remoto.md`, ver §2.2), no re-verificado aquí; `[DECLARADO]` = afirmado en el encargo, sin respaldo local encontrado.

## 0. Nota de alcance (interpretación)

«Las 8 carpetas» se interpretaron como: **CI-FIX-V3.3, ROOM-FIX-V3.4, VERIFY-FIX-V3.5, CERT-V3.6, T2-FIX-V3.6, _LOGS, LOGS, .agents** (BACKUPS se lista aparte en la tarea 4; INSTALACIONES y SCRIPTS no son carpetas de informes — de SCRIPTS solo existe `solution.md`, 4 285 B, 2026-09-28 02:49). Se añadió una mirada ligera de nombres a carpetas auxiliares para no perder evidencia: `_XMAGE-SMOKE`, `_XMAGE-TALLER`, `PROYECTO-ANTIGUO\_ARCHIVO`, `.kilo`, `.zwork`.

## 1. Inventario de informes (*.md, *.txt, *.json < 2 MB)

**Totales por carpeta:** CI-FIX-V3.3: 17 | ROOM-FIX-V3.4: 14 | VERIFY-FIX-V3.5: 14 | CERT-V3.6: 16 | T2-FIX-V3.6: 15 | _LOGS: 3 | LOGS: 0 | .agents: 146 → **total 225**.

**Las 15 más recientes (todas son ficheros del propio repositorio con fecha de checkout de wt-t2-fix, NO informes de ejecución):**

| # | Fecha modificación | Tamaño | Ruta |
|---|---|---|---|
| 1 | 2026-10-10 16:32:05 | 8 354 B | `T2-FIX-V3.6\wt-t2-fix\readme.md` |
| 2 | 2026-10-10 16:32:05 | 94 B | `T2-FIX-V3.6\wt-t2-fix\config\deck-downloader\VERSION_DECKS_V2_2.txt` |
| 3 | 2026-10-10 16:32:05 | 1 269 B | `T2-FIX-V3.6\wt-t2-fix\Utils\readme.txt` |
| 4 | 2026-10-10 16:32:05 | 8 086 B | `T2-FIX-V3.6\wt-t2-fix\Utils\mtg-sets-data.txt` |
| 5 | 2026-10-10 16:32:04 | 3 016 B | `T2-FIX-V3.6\wt-t2-fix\Utils\keywords.txt` |
| 6 | 2026-10-10 16:32:04 | 2 364 B | `T2-FIX-V3.6\wt-t2-fix\PROJECT-MOD.md` |
| 7 | 2026-10-10 16:32:01 | 51 B | `T2-FIX-V3.6\wt-t2-fix\Mage.Tests\JustLands.txt` |
| 8 | 2026-10-10 16:31:41 | 291 B | `T2-FIX-V3.6\wt-t2-fix\Mage.Server\release\server.msg.txt` |
| 9 | 2026-10-10 16:31:41 | 238 B | `T2-FIX-V3.6\wt-t2-fix\Mage.Server\release\config\readme.txt` |
| 10 | 2026-10-10 16:31:41 | 1 715 B | `T2-FIX-V3.6\wt-t2-fix\Mage.Server\release\config\init.example.txt` |
| 11 | 2026-10-10 16:31:41 | 172 B | `T2-FIX-V3.6\wt-t2-fix\Mage.Server\config\dev_readme.txt` |
| 12 | 2026-10-10 16:31:40 | 238 B | `T2-FIX-V3.6\wt-t2-fix\Mage.Client\release\config\readme.txt` |
| 13 | 2026-10-10 16:31:40 | 172 B | `T2-FIX-V3.6\wt-t2-fix\Mage.Client\config\dev_readme.txt` |
| 14 | 2026-10-10 16:31:40 | 1 099 B | `T2-FIX-V3.6\wt-t2-fix\LICENSE.txt` |
| 15 | 2026-10-10 16:31:40 | 4 335 B | `T2-FIX-V3.6\wt-t2-fix\.devcontainer\xmage-dev-tools\devcontainer.json` |

Notas del inventario `[LECTURA]`:
- **LOGS** no contiene ningún .md/.txt/.json; solo `LOGS\mageclient-raiz.log` (6 524 B, 2026-10-04 10:31, fuera de las extensiones pedidas).
- **_LOGS** es material de 2026-10-01 (smoke de fases: `smoke-fase-bar-20261001-030003.log`, capturas PNG, .class de prueba). Nada ≥ 2026-10-09.
- **.agents**: 146 ficheros, todos de 2026-09-17 a 2026-10-01 (skills de agentes e `INSTALLATION-LOG.md` 2026-10-01 01:45). Nada ≥ 2026-10-09.
- **CERT-V3.6** y **T2-FIX-V3.6** solo contienen los worktrees (`wt-sixpr` checkout 16:21:34–16:21:51; `wt-t2-fix` checkout 16:31:40–16:32:05 del 10-10). Sin logs, sin certificados, sin informes propios. El único fichero ≥2 MB en ambas es `Utils\mtg-cards-data.txt` (13 303 684 B, dato del repo).

## 2. Lectura de informes con fecha >= 2026-10-09

### 2.1 Resultado principal

**No existe ningún informe de ejecución genuino (.md/.txt/.json) con fecha ≥ 2026-10-09 en CERT-V3.6, T2-FIX-V3.6, _LOGS, LOGS ni .agents** — solo boilerplate de checkout de los worktrees (resultado vacío, verificado por listado completo de ambas carpetas V3.6 con cualquier extensión <2 MB).

### 2.2 Lo que sí se leyó y aporta evidencia

1. **`T2-FIX-V3.6\wt-t2-fix\config\deck-downloader\tests\tests_vigilante.py`** (16 104 B, **2026-10-10 16:32:24**) `[LECTURA]` — el harness **con el diff aplicado**:
   - Diagnóstico de T2 **escrito en el docstring de `entorno_hijo()`**: en consola Windows cp1252, la «ó» de «Ordenación terminada» sale como byte 0xF3; el harness decodifica UTF-8 y la sustituye por U+FFFD → **T2 falla aunque el vigilante y el organizador funcionen bien**. «No toca el producto».
   - Fix = `PYTHONUTF8=1` aplicado a los **2 puntos** donde el harness lanza subprocess (`lanzar()` y `test_log_inexistente()`).
   - Suite completa: **11 pruebas T0–T10** (T0 ruta de producción; T1 resolución de ruta; **T2 detecta «ACTUALIZACIÓN TERMINADA» y lanza el organizador**; T3 contenido viejo; T4 tres formatos; T5 duplicados; T6 log inexistente; T7 rotación; T8 cancelación; T9 marcador partido; T10 procesos huérfanos).
   - La copia de `tests_vigilante.py` en `CERT-V3.6\wt-sixpr\...\tests\` (15 517 B, 16:21:50) **NO tiene el fix** → el merge de 6 ramas es anterior a la reparación.
2. **`T2-FIX-V3.6\wt-t2-fix\PROJECT-MOD.md`** (2 364 B) `[LECTURA]` — brief del proyecto (ramas, reglas de trabajo); sin resultados de ejecución.
3. **Informes hermanos de hoy (fuente cruzada, fuera de Proyecto-XMage):** :zwork-file-citation{path="v36-recuperacion/git-local.md" purpose="source" artifact_kind="document"} (22 949 B, 2026-10-13 20:39) y :zwork-file-citation{path="v36-recuperacion/github-remoto.md" purpose="source" artifact_kind="document"} (7 144 B, 2026-10-13 20:33). Aportan: el diff íntegro del harness (17+/1−, sin commit), el estado git (HEAD `c63b55967d6` detached en wt-sixpr, 2026-10-10 16:21:50; wt-t2-fix sobre `043dfbc25e4` del 2026-10-09 17:00) y el estado remoto de hoy: **los 6 PR (#17, #18, #19, #20, #21, #23) ABIERTOS con check `build` en FAILURE; #24 (combinado `check/v35-full-sixpr-combo`) GREEN pero CERRADO SIN MERGE; #22 FAILURE; `main` con CI programado en FAILURE el 11, 12 y 13-oct**.

### 2.3 Qué se ejecutó / números / gates / pendientes

| Concepto | Evidencia |
|---|---|
| Ejecución maven local con informe | **Solo V3.3**: `CI-FIX-V3.3\wt-ci-fix\Mage.Tests\target\surefire-reports\` — 1 969 ficheros, ventana ≈11:20–11:38, el más reciente 2026-10-10 11:37:56 (`TEST-org.mage.test.serverside.DatabaseCompatibleTest.xml`). `[LECTURA]` |
| Mage.Verify (surefire) | **NO existe** en ninguno de los 5 worktrees → «Mage.Verify 28 PASS» queda `[DECLARADO]` sin respaldo local. `[LECTURA]` |
| Números del checkpoint V3.5 (6 809 PASS / 28 PASS / vigilante 10-11) | `[DECLARADO]` en el encargo; sin fichero local que los certifique. |
| Gates V3.5 | `[DECLARADO]` VERIFY_FIX_PASS / FULL_SUITE_PASS / GITHUB_ACTIONS_GREEN; lo tercero solo se sostiene hoy vía el build del PR#24 combinado `[CRUZADA]`. |
| V3.6 — qué se ejecutó | Checkout de wt-t2-fix (16:31–16:32 del 10-10) y **edición del harness a las 16:32:24** (diff sin commit). `[LECTURA]` |
| V3.6 — pendientes con evidencia | Reejecución de tests Python: **sin evidencia**; certificación Java V3.6: **sin evidencia** (no hay surefire en wt-sixpr ni wt-t2-fix); 6 PR y Actions: comprobados HOY por la fuente remota `[CRUZADA]` (todos en rojo individual; main en rojo). |

## 3. Grep de 'T2', 'PYTHONUTF8', 'test-harness-utf8', 'V3.6'

**Método:** Select-String -SimpleMatch (case-insensitive) sobre **8 877 ficheros** de texto <2 MB (md, txt, json, py, ps1, yml, yaml, log, cmd, bat, ini, cfg, properties, csv, xml), profundidad 5, excluyendo mage-source, INSTALACIONES, BACKUPS, LEGADO, PROYECTO-ANTIGUO.

**Conteos de ficheros con mención:** T2 = 929 (ruido mayoritario: subcadenas tipo «Cat2»/«T2026», p. ej. `tokens-database.txt`; solo ~4-5 ficheros son menciones reales del test) | **PYTHONUTF8 = 4** | **test-harness-utf8 = 0** | **V3.6 = 0**.

**TOP 10 unión por fecha de modificación:**

| Fecha | Términos | Fichero |
|---|---|---|
| 2026-10-10 16:32:24 | T2, PYTHONUTF8 | `T2-FIX-V3.6\wt-t2-fix\config\deck-downloader\tests\tests_vigilante.py` |
| 2026-10-10 16:32:05 | PYTHONUTF8 | `T2-FIX-V3.6\wt-t2-fix\config\deck-downloader\deck_library_updater.py` |
| 2026-10-10 16:32:04 | T2 (ruido) | `T2-FIX-V3.6\wt-t2-fix\Mage\src\main\resources\tokens-database.txt` |
| 2026-10-10 16:21:51 | T2 | `CERT-V3.6\wt-sixpr\config\deck-downloader\autoinicio\tests_autoinicio.py` (contiene `def test_t2_instancia_unica()`) |
| 2026-10-10 16:21:50 | T2 | `CERT-V3.6\wt-sixpr\config\deck-downloader\tests\tests_vigilante.py` («== T2: detecta 'ACTUALIZACIÓN TERMINADA'…») |
| 2026-10-10 16:21:50 | PYTHONUTF8 | `CERT-V3.6\wt-sixpr\config\deck-downloader\deck_library_updater.py` |
| 2026-10-10 16:21:45 | T2 (ruido) | `CERT-V3.6\wt-sixpr\Mage\src\main\resources\tokens-database.txt` |
| 2026-10-10 15:36:26 | T2 (ruido) | `VERIFY-FIX-V3.5\wt-verify-fix\Mage\src\main\resources\tokens-database.txt` |
| 2026-10-10 13:07:15 | T2 (ruido) | `ROOM-FIX-V3.4\wt-room-fix\Mage\src\main\resources\tokens-database.txt` |
| 2026-10-10 11:33:27 | T2 (ruido) | `CI-FIX-V3.3\wt-ci-fix\Mage.Tests\target\surefire-reports\TEST-org.mage.test.sets.BoosterGenerationTest.xml` |

**Lecturas clave:** `V3.6` **no aparece en ningún fichero** → no existe ningún documento etiquetado V3.6 en el árbol. `test-harness-utf8` solo existe como nombre de rama git (`fix/vigilante-test-harness-utf8-v3.6` `[CRUZADA]`), nunca en contenido. `PYTHONUTF8` ya estaba en el producto (`deck_library_updater.py`, 3 copias: wt-t2-fix, wt-sixpr, _XMAGE-SMOKE) y el diff lo añade solo al harness — coherente con el diagnóstico.

## 4. BACKUPS — primer nivel (máx 20) `[LECTURA]`

| Fecha | Tipo | Nombre |
|---|---|---|
| 2026-10-04 03:19 | DIR | `INSTALACION-XMage-1.4.61` |

Es el **único** elemento del primer nivel. (Fuente cruzada: dentro cuelgan 79 subdirectorios fechados, el más reciente `pre-1.4.62-upgrade-20261004`; ver `git-local.md` §6, que además registra un nombre con caracteres corruptos `co11e-au11i-11eplo26-20260911-212359`.)

## 5. Conclusiones

**(a) ¿Hay informe final de V3.6? NO.** Cero menciones de «V3.6» en 8 877 ficheros de texto; ninguno de los 225 informes inventariados pertenece a V3.6. Lo único «escrito» de V3.6 en disco local es el propio código del fix (`tests_vigilante.py`, 2026-10-10 16:32:24) y los dos informes hermanos de hoy en `J:\MTG\v36-recuperacion\` (git/GitHub, no de ejecución).

**(b) ¿Hasta dónde llegó V3.6 según evidencia escrita?** Hasta: (1) creación del worktree `T2-FIX-V3.6\wt-t2-fix` (checkout 2026-10-10 16:31:40–16:32:05, rama `fix/vigilante-test-harness-utf8-v3.6`); (2) redacción/aplicación del fix al fichero a las **16:32:24 del 10-10** — **sin commit** (17+/1−) y **sin evidencia de reejecución**. El merge de las 6 ramas (`c63b55967d6`, HEAD detached en wt-sixpr, 16:21:50) es **anterior** al fix y no lo incluye. Sin certificación Java local de V3.6 (no hay surefire en wt-sixpr ni wt-t2-fix). La comprobación de los 6 PR y de Actions se hizo HOY de forma remota `[CRUZADA]`.

**(c) Estado documentado de T2:** diagnóstico por codificación escrito en el docstring de `entorno_hijo()` (cp1252 → U+FFFD en «Ordenación terminada»); fix = `PYTHONUTF8=1` en los 2 subprocess del harness; suite de 11 pruebas (T0–T10); «10/11 PASS con T2 pendiente» es declaración del encargo `[DECLARADO]` — no hay certificado local de esa ejecución.

**(d) Certificados con fecha:** JAVA (maven): **solo CI-FIX-V3.3** (surefire, 1 969 ficheros, 2026-10-10 ≈11:20–11:37:56; `Mage.Tests`; sin `Mage.Verify`). Python: **ninguno** con fecha en el árbol. V3.4/V3.5/V3.6: sin certificado local.

**(e) Anomalías:**
1. Ausencia total de informe/certificado V3.6 en disco (contraste: V3.3 sí dejó surefire).
2. Diff del harness **sin commit** en wt-t2-fix — trabajo vivo no respaldado en git (también detectado hoy por la fuente git `[CRUZADA]`).
3. El commit más reciente del proyecto (`c63b55967d6`) solo es alcanzable como HEAD detached de wt-sixpr.
4. Los 6 PR están abiertos con `build: failure` individual; el GREEN del checkpoint solo lo sostiene el PR#24 combinado, **cerrado sin merge**; `main` lleva 3 días de CI en rojo (11–13/10).
5. wt-sixpr (la combinación de 6 ramas) **no contiene el fix T2** → certificar desde ahí sería certificar sin la reparación.
6. `tests_autoinicio.py` tiene otro «T2» (`test_t2_instancia_unica`) — riesgo de confusión entre suites.
7. El checkpoint V3.5 (6 809 / 28 / 10-11) no tiene respaldo local en los worktrees (sin surefire de Mage.Verify, sin log del vigilante guardado).
8. `_XMAGE-SMOKE\.git\FETCH_HEAD` con fecha 2026-10-10 18:26:44: alguien hizo fetch de todas las ramas del fork esa tarde, sin informe asociado. La instalación de `INSTALACIONES\XMage-1.4.62-Kabutosan` es del 10-04 (anterior a los scripts vigilante v3.1/v3.2 del 10-09/10-10; no verificable a más profundidad por las reglas del encargo).

## 6. Métodos, límites y no verificado

- **Verificado por lectura directa en esta sesión:** inventarios (§1, §4), contenido del harness T2 y su diff aplicado, ausencia de surefire en 4 worktrees, conteos y top del grep, ausencia de ficheros V3.6, FETCH_HEAD de _XMAGE-SMOKE, PROJECT-MOD.md, `.kilo\agent-manager.json` (284 B, 2026-10-10 17:48; solo estado de editor, sin valor probatorio).
- **Fuente cruzada (no re-verificada aquí):** estado git completo, PRs/Actions remotos, contenido anidado de BACKUPS (ver §2.2).
- **No verificado (y por qué):** ejecución de cualquier test o build (prohibido en este modo forense); contenido de `INSTALACIONES` más allá de profundidad 2 (ahí podrían vivir los logs reales del vigilante — no accesibles bajo las reglas); ficheros >2 MB (solo listados; el mayor relevante, `Utils\mtg-cards-data.txt` 13 MB, es dato del repo); `J:\MTG\.zwork\runs` (estado runtime del día, fuera de alcance); carpetas LEGADO y PROYECTO-ANTIGUO más allá de nombres (material de agosto-septiembre, sin relevancia V3.6 detectable por nombre).
