# CHECKPOINT 2026-11-14 — Import de .dck: cartas que faltaban (v36)

**Petición de Kabutosan:** «si tiene la orden la descarga de decks de 60/15 faltan algunas cartas,
averigua por qué y arréglalo con los protocolos correspondientes».

## Resumen

Los .dck **sí están completos** (60+15 verificados en los 150 de MTGTop8 y en los 2811 de la
biblioteca). Lo que fallaba es la **importación en XMage**: cartas con acento en el nombre se
perdían al leer el fichero. Dos bugs de serie, ambos en `Mage` (no en el fork).

## Causa raíz 1 — el Scanner leía en CP1252 (el grande)

`PlainTextDeckImporter.importDeck()` usaba `new Scanner(f)`, que toma el **charset por defecto de
la JVM**. En Windows eso es `windows-1252`, y los .dck son UTF-8. Resultado: `Mjölnir` llegaba
como `MjÃ¶lnir` y ningún nombre con acento resolvía.

Medido en esta máquina (`DpiProbe`/`FixTest`, JRE 1.8.0_201 del cliente):
`default charset = windows-1252`.

## Causa raíz 2 — el normalizador mapeaba mal el mojibake

`CardNameUtil.normalizeCardName()` tenía `.replace("Ã¶", "A")`. El mojibake de `ö` (UTF-8 `C3 B6`
leído como CP1252) es `Ã¶`, y su letra real es **`o`**, no `A`. Eso convertía `MjÃ¶lnir` en
`MjAlnir` — una carta que no existe. Corregido a `"o"`.

## Cambios (2 ficheros, +5/−2)

| # | Fichero | Cambio |
|---|---|---|
| 1 | `Mage/.../decks/importer/PlainTextDeckImporter.java` | `new Scanner(f)` → `new Scanner(f, "UTF-8")` |
| 2 | `Mage/.../decks/CardNameUtil.java` | `.replace("Ã¶", "A")` → `.replace("Ã¶", "o")` |

## Verificación en laboratorio (LAB-UI-1.4.62-V2, 17191, prefs `XMageLabUIv2`)

1. `montar-lab.sh` → OK. Cliente arranca, construye la base (`591 sets`, `94787 cards`),
   `Connect: DONE`, `Logging: DONE`, cero excepciones.
2. **Prueba de importación real** (`tools/importtest`, JRE del cliente, base del lab):
   - `ImportTest` sobre `2026-11-09_MTGTop8_09_Amulet_Titan_910595`: **main=60 side=15**, sin
     cartas perdidas. Antes del fix ese mismo mazo daba `main=0 side=0` y 35 errores.
   - `BulkTest` sobre los **2811 .dck** de `sample-decks\Descargados`:
     **2519 completos 60+15**, 292 incompletos, 725 cartas perdidas.
   - `FixTest`: `Mjölnir, Hammer of Thor` → `Mjolnir, Hammer of Thor` por las dos rutas
     (charset por defecto y UTF-8).
3. `parar-lab.sh` → procesos del lab detenidos. **Servidor oficial 17171 intacto** (PID 25548).

## Lo que queda y NO es arreglable desde el importador (28 cartas)

Tras el fix, las 28 cartas que siguen sin resolverse **no existen en XMage** o están marcadas
como no terminadas. No es un bug de importación: es cobertura de sets.

- **`SecretsOfStrixhaven` (SOS)**: `Decorum Dissertation` (69), `Improvisation Capstone` (56),
  `Germination Practicum` (13), `Restoration Seminar` (1). Están declaradas en la lista
  `unfinished` del propio set, así que no entran en la base de cartas.
- **`RealityFracture` (FRA)**: `Seasoned Cryomancer` (69), `Uldaros Theorix` (23),
  `Jace's Machinations` (21), `Sanctum Lurker` (5), `Extrapolate the Impossible` (3),
  `Null Summoner` (3).
- **`StarTrek` (TRK)**: `Georgiou, Terran Emperor` (6), `Genesis Device` (5), `Jadzia Dax,
  Living Legacy` (3), `Deep Space 9`, `Khan, Vengeful Heart`, `Lieutenant Commander Data`,
  `The Holodeck`, `Storied Veteran`, `Q, Stacking the Deck`.
- **`TheHobbitEternal` (HOC)**: `Gollum, Riddle Master` (19), `Elrond, Moon-Reader` (2),
  `Azog, Moria's Ruin` (2), `Woodland Weavemaster`.
- **`MarvelSuperHeroes` (MSH)**: `Political Triumph` (12), `Kid Loki` (4).
- **`Salt Vampire`** (TRK, 4).

Causa: MTGTop8 publica listas de sets que XMage aún no tiene implementados al completo. Se
resolverá cuando el upstream implemente esas cartas; el importador ya las reporta por nombre.

## Nota sobre `MjAlnir`

Sigue apareciendo **1 vez** en el informe porque ese .dck concreto tiene el nombre ya corrompido
en disco (`MjAlnir` en vez de `Mjölnir`), no en el origen. Es un fichero generado antes del fix;
se regenera en la próxima descarga.

## Build

`mvn -B -q -DskipTests -pl Mage.Client,Mage.Server package` → OK.

## Parche y rollback

`Laboratorio\parches\2026-11-14-import-dck-utf8-v36.patch` (2 ficheros, +5/−2).
Rollback = `git apply -R` + rebuild.

## Pendiente

- Desplegar en la instalación oficial (requiere cerrar el cliente oficial; el servidor 17171 y su
  base de datos no se tocan).
- Regenerar los .dck de `Descargados` para limpiar el `MjAlnir` residual.
