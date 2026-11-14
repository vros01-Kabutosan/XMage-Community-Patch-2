# CHECKPOINT 2026-11-14 — v38: se implementa `Jace's Machinations` (FRA) y el mazo 57/15 queda en 60/15

**Petición de Kabutosan:** al abrir su instalación vio un mazo con **57/15**; pidió implementar la
carta que faltaba («implementala»).

## Diagnóstico del mazo

El mazo es `08_UW_Control_907434.dck` (Standard, MTGTop8; en `config\deck-downloader`), completo en
origen (**60/15**). Faltaban exactamente **3 cartas: las 3 copias de `Jace's Machinations` (FRA 32)**,
que no existía implementada en el fork (sin clase y sin declarar en el set). Los dos
`Malformed layout line` del log son las líneas `LAYOUT MAIN`/`LAYOUT SIDEBOARD` del propio `.dck` y no
afectan al conteo. No era ningún fallo del importador ni de la base.

## Implementación (commit `e418d5e4fcf`, rama `upgrade/official-1.4.63V1`)

| Fichero | Cambio |
|---|---|
| `Mage.Sets/src/mage/cards/j/JacesMachinations.java` | Nueva carta: Instant `{2}{U}`, Rare |
| `Mage.Sets/src/mage/sets/RealityFracture.java` | Declarada en FRA 32 y 337 (`NON_FULL_USE_VARIOUS`) |
| `Mage/.../continuous/ActivateAbilitiesAnyTimeYouCouldCastInstantEffect.java` | Constructor nuevo con **duración** y **filtro del permanente origen** (aditivo; los usos de equipo/meditar siguen igual) |
| `Mage.Tests/.../JacesMachinationsTest.java` | 2 tests: activación de lealtad en turno rival y Empower Jace 8 |

Se reutilizó la mecánica ya existente `EmpowerJaceEffect` (del set) y el efecto `ACTIVATE_AS_INSTANT`,
que ya saltaba la comprobación de turno (`canPlaySorcery`), por lo que «en cualquier turno» funciona.

## Verificación

1. **Tests JUnit** (`mvn -Dtest=JacesMachinationsTest -pl Mage.Tests -am test`): `tests="2" errors="0"`
   (activar la lealtad de Jace Beleren en el turno del oponente; token Jace con 8 contadores).
2. **Laboratorio** (base reconstruida con v38): `FindTest` → `FRA 32` y `FRA 337`; el mazo UW Control
   pasa de `main=57` a **`main=60 side=15`**.
3. **BulkTest** sobre los 2811 mazos de `Descargados`: **2667 completos 60+15** (antes 2648, +19);
   incompletos 144 (antes 163); nombres no encontrados **23** (antes 24).

## Despliegue (solo cliente; servidor 17171 intacto)

| Artefacto | SHA256 | Backup |
|---|---|---|
| `mage-client\lib\mage-1.4.63.jar` | `df3ee87255c7446ff7afea8ec4f082670b8cff92ada368c24859dfbdf143fb0f` | `backup-jars-1.4.63-v38-20261114\cliente\` (`2975b2af…`) |
| `mage-client\lib\mage-sets-1.4.63.jar` | `01bdcb3f6aba07f9621b76b1ff42ed9a81fd9b20ca084b83bad132f71afb8ef8` | `backup-jars-1.4.63-v38-20261114\cliente\` (`342f1371…`) |

Servidor: core `f5d9cd7d…`, sets `481114db…` (sin tocar).

## Pendiente (requiere confirmación de Kabutosan)

El escáner no reescanea la base si no cambia la versión, así que el cliente sigue sin ver la carta
hasta reconstruir `mage-client\db\cards.h2.mv.db` (mover a `db\backup-v38-20261114\` y rearrancar).
La confirmación expiró; el cliente no se tocó.

## Rollback

Copiar los dos jars del backup (con el cliente cerrado): `2975b2af…` (core) y `342f1371…` (sets).

## Evidencia

`LAB-UI-1.4.62-V2\capturas\20261114-v38-jaces\`: `scan-v38b.out`, `bulk-v38.out`,
`jaces-machinations-tests.xml`. Parche: `parches\2026-11-14-jaces-machinations-v38.patch`
(`git apply --check -R` OK).
