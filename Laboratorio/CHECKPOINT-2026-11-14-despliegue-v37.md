# CHECKPOINT 2026-11-14 — v36 desplegado, base del cliente reconstruida y v37 (5 lecciones de SOS)

**Petición de Kabutosan:** «faltaba implementar lo del otro worker a la instalación mía, las cartas
que no veía el cliente… el cliente tiene que poder ver todas las cartas cuando se descargan las
decks porque la consigna es muy clara: solo acepta 60/15».

## 1. Despliegue del fix v36 ( UTF-8 )

El fix de Perch (`e642db13bd6`, 2 ficheros, +5/−2: `PlainTextDeckImporter` en UTF-8 y
`CardNameUtil` `Ã¶`→`o`) estaba verificado en laboratorio pero **no desplegado**.

| Elemento | Valor |
|---|---|
| Jar | `Instalacion-XMage\xmage\mage-client\lib\mage-1.4.63.jar` |
| SHA256 desplegado | `2975b2af5116df09e18db3b036dbec15447882cb5faf0e43c2da191c38042d6b` |
| Backup (v35) | `backup-jars-1.4.63-v36-20261114\cliente\mage-1.4.63.jar` (`f5d9cd7d…106eb9`) |
| Servidor 17171 | intacto (PID 25548, core `f5d9cd7d…`, sin tocar) |

Evidencia: ImportTest sobre `2026-11-09_MTGTop8_09_Amulet_Titan_910595_0c07753fb2.dck`:
antes `main=0 side=0` (`MjAlnir`), después `main=60 side=15`.

## 2. Hallazgo: la base de cartas activa del cliente estaba vacía

`mage-client\db\cards.h2.mv.db` pesaba **135 KB** (vacía; una base poblada son ~300 MB) y el propio
cliente la tenía abierta (lock de las 14:45). La base poblada de agosto (307 MB) estaba en
`mage-client\lib\db\` y no era la que usaba el cliente. Por eso el cliente «no veía las cartas»
independientemente del importador.

**Arreglo (con confirmación de Kabutosan):** cliente y launcher cerrados; base vacía movida a
`db\backup-20261114\`; cliente rearrancado. Reconstruyó la base sola:

```
INFO 16:59:10 DB: creating cards database (it can take few minutes)...
INFO 16:59:12 DB: need to add 591 new sets
INFO 16:59:23 DB: need to add 94797 new cards
INFO 16:59:29 Done.
INFO 16:59:34 Client start up time: 24 seconds
INFO 16:59:41 Connect: DONE / Logging: DONE (Kabutosan)
```
`db\cards.h2.mv.db` → **318 MB**. Servidor 17171 intacto.

## 3. v37 — 5 lecciones de SecretsOfStrixhaven

De las 28 cartas que no resolvían, 5 estaban **implementadas pero excluidas** por la lista
`unfinished` del set. Kabutosan autorizó habilitarlas.

- `Mage.Sets\src\mage\sets\SecretsOfStrixhaven.java`: `unfinished` pasa a lista vacía (commit `dc5abcb7c2f`).
- Build limpio `mvn -B -q -DskipTests -pl Mage.Sets -am clean package` → OK.
- Bytecode verificado con `javap`: `Arrays.asList()` sin argumentos.
- Artefacto: `mage-client\lib\mage-sets-1.4.63.jar` → SHA256
  `342f137130d16608d5c5e1b9bbca03ef80b7197cb4dda0d07c752c48266148fb`.
- Backup: `backup-jars-1.4.63-v37-20261114\cliente\mage-sets-1.4.63.jar` (`481114db…46b7`).
- Servidor 17171 intacto (sus jars de sets/core no se tocan).

### Verificación v37

| Prueba | Resultado |
|---|---|
| Base del lab reconstruida con v37 (`CardScanner.scan`) | 94797 cartas, 591 sets, 275 MB |
| BulkTest 2811 `.dck` de `Descargados` | **2648 completos 60+15** (antes 2519, **+129**); incompletos 163 (antes 292); cartas perdidas 580 (antes 725); nombres no encontrados **24** (antes 28) |
| `.dck` de prueba `sos5b.dck` (8 copias) | `main=8` |
| Instalación oficial (base reconstruida + jar v37) | `sos5b main=8`; Amulet Titan `main=60 side=15` |

Las 4 cartas de SOS que aparecían en mazos reales (Decorum Dissertation, Improvisation Capstone,
Germination Practicum, Restoration Seminar) ya resuelven. `Echocasting Symposium` estaba en la
lista pero no aparecía en ningún mazo descargado.

## 4. Queda sin resolver (24 cartas, ajenas al importador)

No existen en XMage: `RealityFracture` (6), `StarTrek` (9), `TheHobbitEternal` (4),
`MarvelSuperHeroes` (2), `Salt Vampire` (TRK) y demás de esos sets. No son arreglables sin
implementar las cartas upstream.

**Aviso de asimetría:** el servidor oficial 17171 no tiene el fix de sets; si se sube un mazo con
esas 5 cartas a un torneo/partida en el servidor oficial, el servidor puede no conocerlas. El
servidor no se toca por norma del proyecto.

## 5. Rollback

- Core: `cp backup-jars-1.4.63-v36-20261114\cliente\mage-1.4.63.jar mage-client\lib\` → `f5d9cd7d…`.
- Sets: `cp backup-jars-1.4.63-v37-20261114\cliente\mage-sets-1.4.63.jar mage-client\lib\` → `481114db…`.
- Base: `db\backup-20261114\` conserva la base vacía previa (por si acaso); el cliente reconstruye
  la base solo si falta.

## 6. Evidencia

`LAB-UI-1.4.62-V2\capturas\20261114-v37-sos\`: `mageclient-log-cola.txt`, `scan-v37.out`,
`bulk-v37-final.out`, `import-oficial-post.txt`. Parche: `parches\2026-11-14-habilitar-sos-v37.patch`
(aplica en reversa verificado con `git apply --check -R`).
