# Inventario de limpieza — 2026-10-25

Autorizada por Kabutosan (confirmación explícita para borrar solo duplicados y
regenerables). No se tocó la instalación oficial salvo lectura/verificación.

## Borrado (~780 MB)

| Ruta | Tamaño | Motivo | Dónde sigue disponible |
|---|---:|---|---|
| `LAB-UI-FASES-CEMENTERIO/cliente/lib` | 152 MB | Dependencias estándar + jars 1.4.61 duplicados | `Instalacion-XMage/xmage/backup-jars-1.4.61-ui-20261024` (6 jars, 175 MB) |
| `LAB-UI-FASES-CEMENTERIO/servidor/lib` | 96 MB | Dependencias estándar + jars 1.4.61 duplicados | Mismo backup 1.4.61 |
| `LAB-UI-FASES-CEMENTERIO/cliente/db` | 154 MB | `cards.h2.mv.db` regenerable | Se reconstruye al primer arranque desde `Mage.Sets` |
| `LAB-UI-FASES-CEMENTERIO/servidor/db` | 294 MB | `cards.h2.mv.db` + stats de pruebas regenerables | Se reconstruye al primer arranque desde `Mage.Sets` |
| `XMage-1.4.62V1-src/{Mage,Mage.Client,Mage.Common,Mage.Server,Mage.Server.Console}/target` | 84 MB | Artefactos Maven regenerables | `mvn -pl Mage.Client,Mage.Server package`; jars ya copiados a `lib/` |
| `_archivo/20261025/untitled-project` | 62 KB | Repositorio git vacío, sin objetos ni commits | No aplica |

Conservado del laboratorio archivado: `capturas/` (2 imágenes), `cliente-lab.log`,
`servidor-lab.log`, `cliente/config`, `servidor/config`, readmes y assets menores
(15 MB en total).

## Conservado

- `LAB-UI-1.4.62-V2/` — 782 MB, con 127 capturas de evidencia.
- `XMage-1.4.62V1-src/` — 845 MB, fuente + historia git de la rama de trabajo.
- `Proyecto-XMage/` — 2.5 GB, archivo histórico 1.4.61; no se usa para probar.
- `parches/` — 100 KB, parche consolidado + backups `.bak-v13`/`.bak-v14`.
- `_archivo/20261025/lab-logs` y `logs` — evidencia de builds y pruebas anteriores.
- Backups de instalación: 5 carpetas (`backup-jars-1.4.61`, `1.4.62`, `final`, `v14`,
  `v15` + `backup-prefs-4k`), 461 MB; ninguno se borró.

## Movido (sin borrar)

- `Laboratorio/proyecto Xmage/` → `_archivo/20261025/proyecto-Xmage-legacy/` (89 MB):
  herramienta MandarinJuice y backup del juego Pragmata, ajeno a XMage.
- `Laboratorio/v36-recuperacion/` → `_archivo/20261025/v36-recuperacion/` (20 MB):
  informes y herramientas de recuperación V3.6.

## No tocado

- `J:\MTG\.git` (~6.2 GB): historia local con ~122.000 archivos; no se publica porque
  su historia no está relacionada con `origin/main`. No se reescribe ni se fuerza push.
- `Instalacion-XMage/fuente-xmage` (~430 MB): clon fuente separado con 3 commits
  propios; se conserva por si el lanzador lo usa.
- Servidor oficial (17171) y su base de datos: intactos.
- Jar desplegado: SHA256 `f25c7bd1b2941e84319963f9e42eb3e20177e0e3ccc6ac2b662e5fc844834c80`
  sin cambios tras la limpieza.

## GitHub

- Rama `upgrade/official-1.4.62V1` publicada hasta `87c7f2b7`, con `AGENTS.md` y
  `docs/laboratorio-ui-1.4.62/` (LEEME, CHECKPOINT, parche y guía).
- `main` remoto sin cambios: el push directo quedó bloqueado por la política de
  seguridad. No se hizo force-push.
