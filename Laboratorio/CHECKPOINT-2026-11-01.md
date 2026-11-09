# CHECKPOINT - 2026-11-01 (Fix Bloqueo H2 y Compatibilidad de Serialización Remota v2.3)

## 1. Resumen Ejecutivo

- **Incidencia 1:** El cliente no iniciaba (`NullPointerException` en `CardRepository` por `SocketTimeoutException: connect timed out: 10.18.180.53:65247`). Proceso zombi retenía la base de datos `cards.h2.mv.db` con una IP obsoleta y el cliente no terminaba sus hilos de JavaFX al cerrar.
- **Incidencia 2:** Al abrir el cliente y pulsar "Connect to server" en `localhost:17171`, aparecía el error:
  `Remote task error: Probably the server version is not compatible with the client or connection blocked by ISP/VPN.` (`EOFException: end of file` en `server.connectUser`).
- **Causa raíz 2:** `MageVersion` y `MageVersionException` implementan `Serializable` sin declarar `serialVersionUID`. Al añadirse métodos auxiliares (`getMajor()`, etc.), el hash autogenerado de la JVM cambió de `-5872491576857560039L` a `-5180031096599588334L`, provocando que el servidor oficial rechazara la deserialización del objeto versión.
- **Solución implementada:**
  1. H2 AUTO_SERVER configurado a `127.0.0.1` (`h2.bindAddress`) con sistema de autocuración de bloqueos huérfanos (`checkAndFixStaleH2Lock`) y terminación garantizada en `MageFrame.doClientShutdownAndExit()`.
  2. Fijados los `serialVersionUID` canónicos de `MageVersion` (`-5872491576857560039L`) y `MageVersionException` (`9200806506119372910L`).
- **Resultado:** El cliente oficial (PID 35580) inicia en 9 segundos, limpia cualquier lock huérfano automáticamente, y conecta de forma transparente a `localhost:17171` (`Logging: DONE` como usuario `Kabutosan`). El servidor oficial no requirió reinicio ni alteración.

---

## 2. Commits y Parches

- **Commit 1:** `e8dade024e1` - `Fix H2 AUTO_SERVER bindAddress localhost, stale lock auto-healing, and client shutdown cleanup`
- **Commit 2:** `57326e1763d` - `Fix serialVersionUID compatibility on MageVersion and MageVersionException (v2.3)`
- **Parches generados:**
  - `Laboratorio/parches/2026-11-01-h2-lock-zombie-fix-v22.patch`
  - `Laboratorio/parches/2026-11-01-serialver-compat-v23.patch`
- **Backups realizados:**
  - `Instalacion-XMage/xmage/backup-jars-1.4.63-20261101_h2_lock_zombie_v22/`
  - `Instalacion-XMage/xmage/backup-jars-1.4.63-20261101_serialver_compat_v23/`

---

## 3. Verificación y Evidencias

- **Laboratorio (`LAB-UI-1.4.62-V2`, puerto 17191):**
  - Servidor arrancado y verificado.
  - Test de conexión programático con `MageClient`: `CONNECT_SUCCESS=true`, `Logging: DONE`.
- **Servidor Oficial (`localhost:17171`):**
  - Test de compatibilidad de serialización: `CONNECT_SUCCESS=true`.
  - Arranque del cliente oficial interactivo (PID 35580):
    ```
    INFO  2026-11-01 01:54:22,328 Auto-connecting to localhost
    INFO  2026-11-01 01:54:23,136 Connect: connecting to server localhost:17171
    INFO  2026-11-01 01:54:23,229 Connect: DONE
    INFO  2026-11-01 01:54:23,230 Logging: as username Kabutosan to server localhost:17171
    INFO  2026-11-01 01:54:24,255 Logging: DONE
    ```


---

## 4. Despliegue en Servidor Oficial (v22+v23) — COMPLETADO 2026-11-01

- **Problema detectado:** Los jars del cliente se habían desplegado (v22+v23), pero los jars del servidor oficial (`mage-1.4.63.jar`, `mage-common-1.4.63.jar`) seguían con la versión antigua del 31/10/2026. Trabajo a medias.
- **Acción realizada:**
  - Backup: `Instalacion-XMage/xmage/backup-jars-1.4.63-20261101_server_deploy_v22_v23/servidor/` (contiene `mage-1.4.63.jar` y `mage-common-1.4.63.jar` antiguos).
  - Desplegado `Mage/target/mage-1.4.63.jar` → `mage-server/lib/mage-1.4.63.jar` (v22: DatabaseUtils H2 lock fix).
  - Desplegado `Mage.Common/target/mage-common-1.4.63.jar` → `mage-server/lib/mage-common-1.4.63.jar` (v23: serialVersionUID fix).
- **Verificación:** Tamaños y timestamps coinciden con el build del laboratorio. `javap` confirma `serialVersionUID` en `MageVersion`. Servidor no estaba en ejecución (puerto 17171 libre).
