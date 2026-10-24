# CI de XMage-Community-Patch-2 — step exacto de fallo por run (solo lectura)

- **Fecha:** martes 2026-10-13. **Modo:** HTTP GET público (`api.github.com` + `raw.githubusercontent.com` + páginas HTML públicas), sin token. **Cero escrituras remotas.** Única escritura local: este fichero.
- **Método:** `GET /commits/{sha}` (resolver SHA) → `GET /commits/{sha}/check-runs` (check `build` → run_id) → `GET /actions/runs/{id}` + `GET /actions/runs/{id}/jobs` (jobs y steps) → `GET /check-runs/{id}/annotations` (firma del error) → `GET /actions/runs?event=schedule` (histórico) → YAML del workflow por raw → contraste con evidencia local de surefire (worktree V3.3).
- **Cuota:** la API sin token (60 peticiones/h) se agotó al final del análisis (remaining 0); las descargas raw no consumen cuota.
- **Nota previa:** este informe completa a `github-remoto.md` y `informes-locales.md` (mismo directorio, hoy): aquéllos fijaron el estado abierto/cerrado; este fija el **paso exacto** y la **firma del error**.

---

## 1. Conclusión (resumen)

**(a) Sí — el step fallido es el mismo en los 6 PR, en #22 y en los 3 runs programados de main:** el job `build` falla siempre en el **step 4 «Build and test»**, que ejecuta `mvn test -B -Dxmage.dataCollectors.printGameLogs=false -Dlog4j.configuration=file:${GITHUB_WORKSPACE}/.travis/log4j.properties`. Los steps previos (checkout, Setup JDK 17) pasan en todos. El run #24 no tiene ningún step fallido (success). Confianza: **determinado** (leído en `steps[].conclusion` de la API para los 11 runs).

**(b) Encaja con la baseline rota pre-V3.3, no es drift:** 8 de los 10 runs fallidos (PR#17, #18, #19, #21, #23 y los 3 de main) llevan las anotaciones javac **`package mage.db does not exist`** y **`package EntityManager does not exist`** — exactamente el recurso que restaura el PR#20 («Restore missing mage.db package in Mage.Common», fix V3.3). Los 2 runs cuyas ramas **ya contienen** V3.3 (PR#20 y PR#22) **no** muestran esos errores (superan el fallo de recursos y llegan más lejos) pero siguen fallando en el mismo step 4 por una causa **no identificable vía API** (los logs requieren login). El combinado de los 6 PR (#24) es verde. La CI programada de main es roja **desde el 04-sep-2026** (verde hasta el 03-sep) sobre un main congelado → fallo **crónico de baseline**, no regresión reciente. Confianza: **alta**.

**(c) Qué cubre el run #24 (38057867646):** 1 job `build` (ubuntu-latest, Temurin JDK 17) con 3 pasos: `checkout@v6` → `setup-java@v5` (JDK 17 + caché Maven) → «Build and test» (`mvn test -B`, reactor completo de **11 módulos**, incl. Mage.Tests y Mage.Verify; surefire activo, sin excludes ni skipTests en los poms). **Compila todo el reactor** — probado por la anotación javac del propio run sobre `Mage.Tests/src/test/java/org/mage/test/serverside/base/MageTestPlayerBase.java#L227`. **La ejecución de tests NO es certificable por API pública**: los logs piden login («Sign in to view logs» en la página pública del run; la API devuelve 403), no hay artefactos (0), no hay `output.summary` en el check-run, y el pom fija `useFile=false` (no genera ficheros de reporte). Además la duración total del job fue **4m 25s** (página pública: 4m 28s), frente a ~**18 min** de fase de tests de la suite completa en la corrida local de referencia (1.966 clases / **6.806 tests**) → **muy improbable que la suite completa de Mage.Tests llegara a ejecutarse en CI**. El número exacto de tests del run #24 queda **pendiente de logs con token**. Confianza: alta en lo compilación; **baja-media** en el alcance real de tests.

**(d) Drift post-10-oct: ninguno detectado.** main no se mueve (HEAD `6e870991…` desde ~06-sep), los 3 runs programados de 11/12/13-oct fallan con firma de error idéntica, y el YAML del workflow es idéntico en main y en la head de #24. Único riesgo futuro detectado (aviso en las anotaciones): migración del runner `ubuntu-latest` a **Ubuntu 26 a partir del 19-oct-2026** (runner-images#14748) — cambio de entorno, no de código. Confianza: alta.

---

## 2. Tabla maestra run → step fallido

Todas las horas según la API (formato local del intérprete; referencia UTC aproximada). SHA completo resuelto vía `GET /commits/{sha_corto}`.

| run | PR / rama | SHA completo | evento | inicio | conclusión run | step fallido (job `build`) |
|---|---|---|---|---|---|---|
| 37927179229 | #17 `feature/deck-downloader-http-directo-v3` | `e842ebfcd7528b7a929ce6e17877e368f8bcaea9` | pull_request | 10-09 11:59 | failure | **step 4 «Build and test»** |
| 37948433727 | #18 `fix/vigilante-ruta-logs-v3.1` | `043dfbc25e458741d32ca10962e032721ba14820` | pull_request | 10-09 15:00 | failure | **step 4 «Build and test»** |
| 38040774350 | #19 `feat/vigilante-autoinicio-v3.2` | `bbf7a19c531264cb118c11e0e0f41010bc2acd3c` | pull_request | 10-10 09:15 | failure | **step 4 «Build and test»** |
| 38042346718 | #20 `fix/xmage-ci-build-v3.3` | `2d52bfe87c9349ea473986bbf5cb32fddeca045d` | pull_request | 10-10 09:42 | failure | **step 4 «Build and test»** (sin err. de package) |
| 38050992003 | #21 `fix/room-card-copy-stack-v3.4` | `b8a2ab154f7b0e8af526a4072de582746a568bf6` | pull_request | 10-10 12:10 | failure | **step 4 «Build and test»** |
| 38051007210 | #22 `check/room-v34-plus-baseline-v33` | `58dc2777a28cb46e92a07713773b11fdfd34fea1` | pull_request | 10-10 12:10 | failure | **step 4 «Build and test»** (sin err. de package) |
| 38057828264 | #23 `fix/mage-verify-data-v3.5` | `c7649bed9873b6721723b0c652918b72c303319b` | pull_request | 10-10 13:59 | failure | **step 4 «Build and test»** |
| **38057867646** | #24 `check/v35-full-sixpr-combo` | `157908d7ed1db33cab1e4623cbd7e73a1ce192d4` | pull_request | 10-10 13:59 | **success** | **ninguno** (step 4 success) |
| 38119717688 | main (schedule) | `6e870991089ed14c45b71d0db28cb0ba3bde803c` | schedule | 10-11 06:56 | failure | **step 4 «Build and test»** |
| 38216185950 | main (schedule) | `6e870991…` | schedule | 10-12 07:09 | failure | **step 4 «Build and test»** |
| 38363396606 | main (schedule) | `6e870991…` | schedule | 10-13 07:33 | failure | **step 4 «Build and test»** |

Secuencia de steps grabada en los 11 runs (idéntica): `1 Set up job [success]` → `2 Run actions/checkout@v6 [success]` → `3 Setup JDK 17 [success]` → `4 Build and test [failure|success]` → `7 Post Setup JDK 17 [success]` → `8 Post Run actions/checkout@v6 [success]` → `9 Complete job [success]` (numeración 5-6 no poblada por la API). Job único: `build` (id por run: 113808823896, 113880671620, 114180400406, 114184928300, 114209911231, 114209955330, 114229820431, 114229936933, 115182684891, 114701359452, 114411753435).

---

## 3. Evidencia

### 3.1 Workflow: `.github/workflows/maven.yml` — «Build and run tests (github)» (id 345289169)

Idéntico en `main` y en la head de #24 (`157908d7`) (raw verificado en ambos):

```yaml
on:
  push: { branches: ["master", "main"] }
  pull_request: { branches: ["master", "main"] }
  schedule: [{ cron: '0 1 * * *' }]
  workflow_dispatch: {}
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v6
    - name: Setup JDK 17
      uses: actions/setup-java@v5
      with: { java-version: '17', distribution: 'temurin', cache: maven }
    - name: Build and test
      run: mvn test -B -Dxmage.dataCollectors.printGameLogs=false -Dlog4j.configuration=file:${GITHUB_WORKSPACE}/.travis/log4j.properties
```

- El step 4 «Build and test» = **`mvn test`** sobre el reactor completo.
- Reactor (root `pom.xml`, 11 módulos): `Mage, Mage.Common, Mage.Server, Mage.Sets, Mage.Client, Mage.Plugins, Mage.Server.Plugins, Mage.Server.Console, Mage.Tests, Mage.Verify, Mage.Reports`.
- Surefire 3.1.2 (root pom L127-185): sin `skipTests`, sin excludes; `useFile=false` (no escribe ficheros de reporte) + tree-reporter por consola. Los `<skip>true</skip>` de Mage.Tests/Mage.Verify son de `maven-install-plugin` y `maven-deploy-plugin` («empty project for tests»), **no** de surefire.

### 3.2 Firma del error por run (check-run annotations)

Endpoint: `GET /repos/…/check-runs/{check_run_id}/annotations`.

| runs | anotaciones (nivel failure salvo indicación) |
|---|---|
| PR#17 (113808823896), PR#18 (113880671620), PR#19 (114180400406), PR#21 (114209911231), PR#23 (114229820431), main 11-oct (114411753435), main 12-oct (114701359452), main 13-oct (115182684891) | `Process completed with exit code 1.` + **`package EntityManager does not exist` ×2** + **`package mage.db does not exist` ×2** (+ notice de ubuntu-latest) |
| PR#20 (114184928300), PR#22 (114209955330) | `Process completed with exit code 1.` + **warning** `non-varargs call of varargs method with inexact argument type for last parameter` (= `MageTestPlayerBase.java#L227`, fichero de Mage.Tests) — **sin errores de package** |
| PR#24 (114229936933, success) | warning `non-varargs … MageTestPlayerBase.java#L227` + notice ubuntu-latest (página pública del run, sin login) |

Lectura: el error `package mage.db/EntityManager does not exist` es un **fallo de compilación javac por recursos ausentes** (fase compile de `mvn test`, falla temprano), exactamente lo que el PR#20/V3.3 restaura. Los runs con V3.3 incorporado (#20, #22, #24) compilan **hasta Mage.Tests** (la warning javac de MageTestPlayerBase lo prueba) y ya no ven ese error. Los check-runs no traen `output.summary` (verificado en el listado de check-runs de los 8 SHA).

Duraciones medidas (job `build`, started→completed): PR#23 **2m12s** (falla en compilación, coherente), PR#20 **4m37s**, PR#22 **7m19s**, PR#24 **4m25s** (success).

### 3.3 Histórico de runs programados de main (drift)

`GET /actions/runs?event=schedule` → total **45**; muestra 14-sep→13-oct (30) + 30-ago→13-sep (15):

- **Verdes hasta el 03-sep-2026** (sha `105d560e`, «version bump», 08-12): 30-ago, 31-ago, 01-sep, 02-sep, 03-sep = success.
- **Rojos desde el 04-sep-2026** en adelante: 40 fallos consecutivos. Desde el ~06-sep el sha es `6e870991…` («Document Kabutosan project structure») y **no cambia**; los últimos 30 programados (14-sep→13-oct) son todos `failure` sobre ese mismo sha.
- Consecuencia: los fallos de 11/12/13-oct no son una regresión nueva sino la **continuación** de una baseline rota desde el 04-sep. (Anomalía menor no resuelta: los runs del 04-05-sep fallan mostrando el mismo sha `105d560e` que había estado verde el 03-sep — posible force-push o variación ambiental; sin impacto en la conclusión.)

### 3.4 Run #24 (38057867646) — alcance documentado

- Página pública del run (visible sin login): «Triggered via pull request · October 10, 2026 13:59 · Status **Success** · Total duration **4m 28s** · Artifacts **–** · job `build` 4m 25s»; logs tras «Sign in to view logs». Run nº 94 del workflow.
- Compila el reactor completo: anotación javac del propio run en `Mage.Tests/…/MageTestPlayerBase.java#L227`.
- Tests: el paso es `mvn test` (surefire activo en Mage.Tests y Mage.Verify), pero **no hay evidencia API/HTML del número de tests ejecutados**: logs con login (API 403 sin token), 0 artefactos, sin `output.summary`, `useFile=false` (no genera XML en CI).
- Referencia local (NO es CI): `CI-FIX-V3.3\wt-ci-fix\Mage.Tests\target\surefire-reports` (worktree de `fix/xmage-ci-build-v3.3`, corrida local 10-10 ≈11:20–11:38) — agregación de los 1.966 XML: **1.966 clases / 6.806 tests / 1 failure / 0 errors / 125 skipped**; suma de tiempos de test 2,5 min pero ~18 min de muro (init por clase domina). Con ese orden de magnitud, los 4m25s totales del job #24 hacen **improbable** que la suite completa corriera en CI.

### 3.5 Interpretación baseline vs drift

1. **Baseline pre-V3.3 confirmada como causa dominante**: 8/10 runs fallidos con la firma exacta de recursos ausentes (`mage.db`, `EntityManager`).
2. **PR#20 (el fix V3.3) sigue rojo en CI individual**: supera el error de recursos (sin esas anotaciones) pero falla más tarde en el mismo step (4m37s); PR#22 (V3.3+V3.4) igual (7m19s). La causa residual **no es observable** con API pública (logs gated). El combinado de los 6 (#24) es verde → por inferencia, la pieza que cura el fallo residual está en alguno de #17/#18/#19/#23 (hipótesis, **no** verificado).
3. **Sin drift post-10-oct**: main congelado + firma idéntica + YAML idéntico. Riesgo futuro: migración ubuntu-latest→Ubuntu 26 (19-oct).

---

## 4. Límites y pendientes

- **Logs de Actions: requieren login** (confirmado dos vías: API `GET /actions/jobs/{id}/logs` → HTTP **403** sin token; página pública del run → «Sign in to view logs»). Consecuencia: la causa exacta del fallo residual de PR#20/PR#22 y el **número de tests** del run #24 quedan **pendientes de un token** con `actions:read`.
- `run_attempt` de #24 no llegó a leerse (cuota agotada); la página pública muestra un único run success.
- Mage.Verify en CI: sin evidencia directa de ejecución de sus tests (mismo paso `mvn test`, pero no certificable sin logs).
- La ventana 11:20–11:38 de la corrida local de referencia y su equivalencia con tiempos de runner de GitHub son aproximadas.
- Fuera de alcance (no pedido): issues, releases, PR#16, dependabot.

## 5. Fuentes

- API (todas leídas 2026-10-13, GET sin token):
  - `https://api.github.com/repos/vros01-Kabutosan/XMage-Community-Patch-2/commits/{sha}` y `/commits/{sha}/check-runs`
  - `https://api.github.com/repos/vros01-Kabutosan/XMage-Community-Patch-2/actions/runs/{run_id}` y `/actions/runs/{run_id}/jobs`
  - `https://api.github.com/repos/vros01-Kabutosan/XMage-Community-Patch-2/check-runs/{check_run_id}/annotations`
  - `https://api.github.com/repos/vros01-Kabutosan/XMage-Community-Patch-2/actions/runs?event=schedule` (páginas 1-2), `/actions/workflows`, `/actions/runs/38057867646/artifacts`
- Raw: `https://raw.githubusercontent.com/vros01-Kabutosan/XMage-Community-Patch-2/main/.github/workflows/maven.yml` e ídem en `157908d7…`; `pom.xml` raíz (main y `157908d7…`); `Mage.Tests/pom.xml`, `Mage.Verify/pom.xml` en `157908d7…`.
- Página pública del run #24: `https://github.com/vros01-Kabutosan/XMage-Community-Patch-2/actions/runs/38057867646` (Status Success, 4m 28s, Artifacts –, «Sign in to view logs», anotaciones).
- Local (solo lectura): `J:\MTG\Proyecto-XMage\CI-FIX-V3.3\wt-ci-fix\Mage.Tests\target\surefire-reports\TEST-*.xml` (1.966 ficheros agregados).
- Notas hermanas del mismo día: `v36-recuperacion/github-remoto.md`, `v36-recuperacion/informes-locales.md`.
