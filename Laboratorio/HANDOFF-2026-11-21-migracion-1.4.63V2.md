# HANDOFF 2026-11-21 - Migracion 1.4.63V2 completada; residuales opcionales

**Estado:** MIGRACION COMPLETADA Y DESPLEGADA (commit b43984215b rama upgrade/official-1.4.63V2, pusheada). Smoke lab OK, servidor 17171 y cliente oficial en 1.4.63-V2 verificados.

## Pendientes opcionales (ninguno bloquea)

1. Bajas upstream pendientes de retirar con autorizacion (politica solo sumar): BorgQueenOneWhoIsMany.java, NotTransformedCondition.java, PowerUpAbilityPredicate.java, sonar-project.properties; y los 5 ficheros que el fork ya habia borrado (DownloadLog.java, ScryfallTokensBulkDataTest.java, DownloadPicturesServiceTest.java, Mage.Tests/src/test/data/users-db-sample.h2.mv.db, Utils/data/.gitkeep). Comando cuando se apruebe: git rm <ficheros> + mvn clean package + commit.
2. BulkTest de los 2811 .dck de Descargados en V2 (esperado mejorar respecto a 2667/2811 de v38 por las cartas nuevas upstream).
3. Prueba de partida real con IA (Computer - mad) para ejercicio completo del ComputerPlayer6 migrado.
4. Limpieza futura: mover los backups .before-1.4.61 fuera de mage-server\plugins (el loader los ignora).
5. Opcional: PR de upgrade/official-1.4.63V2 -> upgrade/official-1.4.63V1 o merge fast-forward cuando Kabutosan lo decida.

## Rollback

- Jars: copiar de vuelta Instalacion-XMage\xmage\backup-jars-1.4.63-20261121-203017_pre-V2\ (server lib + plugins + cliente\) con el servidor 17171 parado.
- Codigo: git checkout upgrade/official-1.4.63V1 (v38 intacta).

## Reglas

- No borrar HKCU\Software\JavaSoft\Prefs\HKEY_CURRENT_USER. Responder en espanol. Parches + CHECKPOINT + LEEME al terminar cualquier tarea.
