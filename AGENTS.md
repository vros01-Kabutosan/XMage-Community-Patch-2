# Normas del proyecto MTG / XMage — Kabutosan

Estas normas aplican a cualquier modelo, IA o worker que trabaje en este repositorio.
No hace falta repetirlas en cada prompt: están aquí de forma permanente.

## Protocolo obligatorio: primero laboratorio, después instalación

1. **Todo cambio se compila y se prueba primero en el laboratorio**
   `J:\MTG\Laboratorio\LAB-UI-1.4.62-V2` (cliente y servidor aislados, puerto 17191,
   prefs en `HKCU\Software\XMageLabUIv2`).
2. **Solo si el smoke pasa en el laboratorio** se despliega en la instalación oficial
   `J:\MTG\Instalacion-XMage`.
3. **Nunca tocar el servidor oficial** (puerto 17171 y su base de datos). Para copiar
   jars bloqueados, cerrar únicamente el cliente oficial y pedir/confirmar antes.
4. **Backup antes de sobrescribir**: los jars desplegados se guardan en
   `Instalacion-XMage\xmage\backup-jars-<version>-<fecha>\`.
5. **Solo sumar, nunca restar**: no borrar mods, funciones ni configuración existente.
6. **Documentar y sincronizar**: dejar evidencia (capturas/logs) en
   `Laboratorio\LAB-UI-1.4.62-V2\capturas`, actualizar `LEEME.md` y
   `CHECKPOINT-<fecha>.md`, regenerar `Laboratorio\parches\` y hacer commit/push en la
   rama de trabajo del fork.

## Entorno

- Fuente: `J:\MTG\Laboratorio\XMage-1.4.62V1-src` (rama `upgrade/official-1.4.62V1`).
- Build: `C:/Users/vros0/apache-maven-3.9.6/bin/mvn.cmd -B -q -DskipTests -pl Mage.Client,Mage.Server package`.
- Laboratorio: `LAB-UI-1.4.62-V2/montar-lab.sh`, `lanzar-lab.sh`, `parar-lab.sh`.
- Instalación oficial: `J:\MTG\Instalacion-XMage\xmage\mage-client\lib` y `mage-server\lib`.

## Reglas adicionales

- Responder en español.
- Si un arreglo no funciona, volver al último estado bueno conocido y explicarlo.
- No borrar la clave `HKCU\Software\JavaSoft\Prefs\HKEY_CURRENT_USER` sin autorización
  expresa de Kabutosan.
