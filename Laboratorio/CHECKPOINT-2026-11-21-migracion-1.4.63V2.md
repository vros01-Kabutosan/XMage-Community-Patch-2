# CHECKPOINT - 2026-11-21 (Migracion upstream 1.4.63 V1 -> V2)

## 1. Resumen ejecutivo

- **Objetivo:** migrar el fork XMage de Kabutosan desde xmage_1.4.63V1 (79aec614fc2) al nuevo release oficial xmage_1.4.63V2 (c5a5215fa086), conservando intactos TODOS los mods del fork (v19..v38), sin romper nada y siguiendo J:\MTG\AGENTS.md.
- **Metodo:** superposicion de arbol (el fork no comparte historia git con upstream: merge-base vacio). Nueva rama upgrade/official-1.4.63V2; la rama v38 upgrade/official-1.4.63V1 queda intacta como rollback.
- **Estado final: MIGRACION COMPLETADA + SMOKE OK EN LABORATORIO + DESPLIEGUE OFICIAL VERIFICADO** (cliente y servidor 17171 en V2).

## 2. Que aporta la build V2 (upstream V1 -> V2)

- 745 ficheros, +30.680/-4.839: BD a 595 sets / 94.942 cartas (antes 591/94.799). Cartas nuevas (Star Trek TRK, Marvel MSC, MSD, Tolkien HOB, FRC, set Nauctis, WPN 2027), framework Vengeance/Exhaust, zeroLoyaltyRuleApplies, mustAttackDefenders multi-defensor, refactor MageEditorPane con test propio, fix AI MAD isInGame.
- MAGE_VERSION_RELEASE_INFO = "V2" con validacion estricta activa: cliente y servidor deben estar en la MISMA release info -> despliegue siempre conjunto (confirmado en codigo: MageServerImpl.connectUser + MageVersion.compareTo).
- API Token.putOntoBattlefield (7 firmas) intacta V1<->V2 (leccion 2026-11-09 respetada). Animation.cancelRunningAnimations, ActivateAbilitiesAnyTimeYouCouldCastInstantEffect y EmpowerJaceEffect(int) sin cambios.

## 3. Solapes fork x upstream: 9, todos resueltos

| Fichero | Resolucion |
|---|---|
| MageVersion.java | Base V2 (RELEASE_INFO="V2") + serialVersionUID y getters del fork |
| MageVersionException.java | Sobrevivio al checkout; serialVersionUID del fork verificado |
| PlayerImpl.java | Base V2 (isInGame, startingDeckSize, DrawCardEvent, fireActivatedAbilityEvent) + las 8 guardas isInterrupted del fork (2 estaban mal insertadas por el worker interrumpido; corregidas: sintaxis y return options) |
| Permanent.java / PermanentImpl.java | Merge de 3 vias limpio: V2 (zeroLoyaltyRuleApplies, getPreparedSpellCopyId, mustAttackDefenders) + roomCastHalf y fireEvent inmediato del door unlock del fork |
| ComputerPlayer6.java | Merge de 3 vias limpio: fork completo (transposition table, DEEP_STACK, fallback, mulligan, combate) + hunk upstream player.isInGame() |
| MageEditorPane.java | Base V2 (refactor upstream) + guarda lastBodyRule del fork portada a mano (V2 no la tenia) |
| SuperiorSpiderMan.java | Base V2 (getDefaultCardSideForBattlefield) + fix de imagen del fork (setImageFileName si vacio) |
| JacesMachinations.java | Adoptada la implementacion upstream V2 (referencia); la extension del fork en ActivateAbilitiesAnyTime... se conserva (aditiva, upstream no toco el fichero) |
| RealityFracture.java | Adoptada V2 (superset: incluye Jace s Machinations 32/337 del v38 + Sanctum Lurker + Uldaros Theorix) |

## 4. Preservacion de mods (solo sumar, nunca restar)

- Checklist de 43 marcadores de mods: baseline PRE-migracion 43/43 PRESENTE, re-verificado tras migracion 43/43 PRESENTE (CardNameUtil verificado por diff; marcador mal escrito en el script).
- git diff c5a5215fa086 --name-only = 149 ficheros = exactamente los mods del fork sobre V2 (0 mods perdidos, 0 ficheros no-fork alterados).
- Restaurados del fork tras el checkout: 9 launchers release/start* (auto-cierre + deteccion dinamica de jar), Mage.Client/pom.xml (FlatLaf shade), .gitignore, Utils/*.pl, clean_dbs.sh, AGENTS.md, PROJECT-MOD.md, docs, y los 24 fork-added.
- **Restos documentados (NO borrados, politica solo sumar):** BorgQueenOneWhoIsMany.java, NotTransformedCondition.java, PowerUpAbilityPredicate.java y sonar-project.properties siguen presentes aunque upstream los elimino en V2; compilan y no afectan al runtime. Limpieza opcional futura con autorizacion. Tambien siguen presentes los 5 ficheros que el fork ya habia borrado (DownloadLog, 2 tests, users-db-sample, Utils/data/.gitkeep) por la misma razon.

## 5. Build y verificacion

- **Build:** mvn -B -DskipTests clean package completo, **BUILD SUCCESS** (5:33 min). Log: J:\MTG\_build-v2-migracion-20261121.log. 5 core jars + 27 plugins del mismo build limpio.
- **SHA256 (prefijos) de los jars desplegados:** mage CC913FF867F72275, sets 128DCCA34CDBF7D7, common BEC8B2BC6F1FE93D (identicos en mage-server\lib y mage-client\lib -> coherencia total cliente/servidor).
- **Smoke laboratorio (17191):** servidor 1.4.63-V2 (build 2026-11-21 19:18), 595 sets / 94.942 cartas, 17 game types, 21 tourneys, 3 players, 23 cubes, 52 decks; cliente lab: Connect DONE + Logging DONE; err logs a 0 bytes. Evidencia: LAB-UI-1.4.62-V2\capturas\20261121-v2-migracion\.
- **Despliegue oficial:** backup previo completo (36 jars) en Instalacion-XMage\xmage\backup-jars-1.4.63-20261121-203017_pre-V2\; parada del 17171; copia conjunta 5 lib server + 27 plugins + 5 lib cliente; arranque: Starting MAGE SERVER version 1.4.63-V2 en 17171 con las mismas cifras de carga; cliente oficial V2 conecta y logea sin dialogo de version; installed.properties actualizada (backup installed.properties.backup-pre-V2-20261121).
- Rollback: copiar de vuelta el contenido de la carpeta de backup (mismos nombres) con el servidor parado.

## 6. Git y sincronizacion

- Rama nueva upgrade/official-1.4.63V2, commit b43984215b (1656 ficheros, +57.899/-29.141), **pusheada** a origin.
- Parches: Laboratorio\parches\2026-11-21-migracion-1.4.63V2.patch (consolidado, 3,9 MB) y 2026-11-21-mods-fork-sobre-V2.patch (solo mods sobre V2, 1,7 MB).
- upgrade/official-1.4.63V1 (v38) NO tocada: rollback de codigo.

## 7. Estado de servicios tras la tarea

- Servidor oficial 17171: **V2 en ejecucion** (reinicio planificado; Kabutosan puede reconectar).
- Cliente oficial: relanzado en V2, conectado (Logging DONE 20:32:06).
- Laboratorio 17191: parado limpiamente tras el smoke (parar-lab.sh).
