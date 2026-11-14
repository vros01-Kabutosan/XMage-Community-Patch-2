# CHECKPOINT 2026-11-14 — Despliegue del fix v36 (import .dck UTF-8) en la instalación oficial

**Petición de Kabutosan:** «faltaba implementar lo del otro worker a la instalación mía, las cartas
que no veía el cliente… el cliente tiene que poder ver todas las cartas cuando se descargan las
decks porque la consigna es muy clara: solo acepta 60/15».

## Qué se desplegó

El fix de Perch (commit `e642db13bd6`, `upgrade/official-1.4.63V1`, 2 ficheros +5/−2) que faltaba
en la instalación oficial: `PlainTextDeckImporter` lee en UTF-8 y `CardNameUtil` corrige el
mojibake `Ã¶` → `o`. La instalación estaba en v35 (core `f5d9cd7d…106eb9`).

| # | Elemento | Valor |
|---|---|---|
| 1 | Jar desplegado | `Instalacion-XMage\xmage\mage-client\lib\mage-1.4.63.jar` |
| 2 | SHA256 desplegado | `2975b2af5116df09e18db3b036dbec15447882cb5faf0e43c2da191c38042d6b` |
| 3 | SHA256 backup (v35) | `f5d9cd7d256ca51f418207d86cf19bf4288a694f2546f7ea49ec3e32ff106eb9` |
| 4 | Backup | `Instalacion-XMage\xmage\backup-jars-1.4.63-v36-20261114\cliente\mage-1.4.63.jar` |
| 5 | Servidor 17171 | **intacto** (PID 25548, core `f5d9cd7d…`, sin tocar) |

La copia del jar no requirió cerrar el cliente (no estaba bloqueado para escritura); el cliente
en ejecución conserva el jar viejo en memoria hasta que se reinicie.

## Verificación (evidencia propia, ImportTest con el JRE del cliente)

| Escenario | Resultado |
|---|---|
| Lab: core v36 + base lab | `main=60 side=15` (`lab2.out`) |
| Instalación **antes**: core v35 + base oficial | `main=0 side=0`, `MjAlnir, Hammer of Thor` (`inst2.out`) |
| Instalación **después**: jar desplegado + base compatible | `main=60 side=15` (`inst5.out`) |

Mazo usado: `Descargados\Modern\2026-11-09\2026-11-09_MTGTop8_09_Amulet_Titan_910595_0c07753fb2.dck`.

## Hallazgo adicional: la base de cartas activa del cliente está vacía

El cliente oficial (PID 19660) usa `J:\MTG\Instalacion-XMage\xmage\mage-client\db\cards.h2.mv.db`,
que pesa **135 KB** (vacía; una base poblada son ~300 MB). La base poblada (307 MB, ago/2026) está
en `mage-client\lib\db\` y no es la que usa el cliente. El `cards.h2.lock.db` de `db/` coincide con
el arranque del cliente (14:45), o sea que esa es la base activa.

**Acción pendiente (requiere confirmación):** cerrar el cliente, mover la base vacía a
`db\backup-20261114\` y rearrancar para que el cliente la reconstruya (~591 sets / 94787 cartas).
Sin este paso, el cliente seguirá sin ver cartas aunque el jar ya sea el bueno.

## Cartas que siguen sin resolver (28, ajenas al importador)

No existen en XMage o están excluidas; no son arreglables desde el importador:
`SecretsOfStrixhaven` (5, en lista `unfinished`, **implementadas**), `RealityFracture` (6),
`StarTrek` (9), `TheHobbitEternal` (4), `MarvelSuperHeroes` (2), `Salt Vampire` (TRK).
Solo las 5 de SOS son recuperables sin implementar efectos (quitarlas de `unfinished`), pero es un
cambio de comportamiento que requiere decisión de Kabutosan.

## Rollback

Copiar el jar del backup (con el cliente cerrado) y rearrancar. Base de cartas: no hay que restaurar
nada; el cliente la reconstruye.

## Ruido en el árbol (artefactos de Perch, no se versionan)

`J:\MTG\$null` (fichero de 0 bytes), `J:\MTG\.perch\` (manual/MCP de Perch), `J:\MTG\db\` (base H2
creada por el ImportTest de Perch en la raíz). No se han borrado ni versionado.
