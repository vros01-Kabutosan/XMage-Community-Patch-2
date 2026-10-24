@echo off
setlocal EnableExtensions EnableDelayedExpansion
title XMage RC1.3 - Rollback base funcional pre-T - v-1.0.0

set "ACTIVE_ROOT=J:\mtg\xmage"
set "CLIENT_LIB=%ACTIVE_ROOT%\client\lib"
set "ARCHIVE_ROOT=J:\mtg\_ARCHIVO"
set "LOG_ROOT=J:\mtg\_LOGS"
set "KNOWN_GOOD=%ARCHIVE_ROOT%\trigger-indicator-before-activation_20260827_140747"
set "SOURCE_CLIENT=%KNOWN_GOOD%\mage-client-before.jar"
set "SOURCE_COMMON=%KNOWN_GOOD%\mage-common-before.jar"
set "STAMP="
for /f "delims=" %%T in ('powershell -NoProfile -Command "Get-Date -Format yyyyMMdd_HHmmss"') do set "STAMP=%%T"
if not defined STAMP set "STAMP=manual"
set "LOG_DIR=%LOG_ROOT%\rollback_pre-T_RC1.3-v-1.0.0_%STAMP%"
set "BACKUP_DIR=%ARCHIVE_ROOT%\before-rollback-to-pre-T_%STAMP%"
set "TRANSCRIPT=%LOG_DIR%\rollback_transcript.log"
set "ROLLBACK_STARTED=0"

if not exist "%LOG_DIR%\." mkdir "%LOG_DIR%"
if not exist "%BACKUP_DIR%\." mkdir "%BACKUP_DIR%"
call :LOG "START rollback urgente a la base funcional anterior a la T"
call :LOG "Instalacion objetivo=%ACTIVE_ROOT%"
call :LOG "Fuente fija de recuperacion=%KNOWN_GOOD%"

if not exist "%ACTIVE_ROOT%\." goto FAIL_ACTIVE
if not exist "%CLIENT_LIB%\." goto FAIL_ACTIVE
if not exist "%CLIENT_LIB%\mage-client-1.4.61.jar" goto FAIL_ACTIVE
if not exist "%CLIENT_LIB%\mage-common-1.4.61.jar" goto FAIL_ACTIVE
if not exist "%SOURCE_CLIENT%" goto FAIL_SOURCE
if not exist "%SOURCE_COMMON%" goto FAIL_SOURCE

powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; Add-Type -AssemblyName System.IO.Compression; Add-Type -AssemblyName System.IO.Compression.FileSystem; $z=[IO.Compression.ZipFile]::OpenRead('%SOURCE_CLIENT%'); $n=@($z.Entries | ForEach-Object {$_.FullName}); $z.Dispose(); if($n -notcontains 'menu/preferences.png'){throw 'La copia pre-T no contiene menu/preferences.png'}; if($n -notcontains 'menu/connect.png'){throw 'La copia pre-T no contiene menu/connect.png'}; 'KNOWN_GOOD_RESOURCES=OK'" > "%LOG_DIR%\source_verify.log" 2>&1
if errorlevel 1 goto FAIL_SOURCE

call :LOG "Cerrando solo procesos Java de la instalacion objetivo."
powershell -NoProfile -ExecutionPolicy Bypass -Command "$root='J:\mtg\xmage'; foreach($p in (Get-CimInstance Win32_Process -ErrorAction SilentlyContinue)){if($p.Name -in @('java.exe','javaw.exe') -and $p.CommandLine -and $p.CommandLine.IndexOf($root,[StringComparison]::OrdinalIgnoreCase) -ge 0){Stop-Process -Id $p.ProcessId -Force -ErrorAction Stop; 'STOPPED '+$p.ProcessId+' '+$p.Name}}" > "%LOG_DIR%\stop_xmage_processes.log" 2>&1
if errorlevel 1 goto FAIL_STOP

call :LOG "Guardando una copia de seguridad de la version actual antes del rollback."
powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; Copy-Item -LiteralPath '%CLIENT_LIB%\mage-client-1.4.61.jar' -Destination '%BACKUP_DIR%\mage-client-before-rollback.jar' -Force; Copy-Item -LiteralPath '%CLIENT_LIB%\mage-common-1.4.61.jar' -Destination '%BACKUP_DIR%\mage-common-before-rollback.jar' -Force; if(-not (Test-Path -LiteralPath '%BACKUP_DIR%\mage-client-before-rollback.jar')){throw 'No se guardo la JAR actual del cliente'}; if(-not (Test-Path -LiteralPath '%BACKUP_DIR%\mage-common-before-rollback.jar')){throw 'No se guardo la JAR actual de common'}" > "%LOG_DIR%\backup_current.log" 2>&1
if errorlevel 1 goto FAIL_BACKUP
set "ROLLBACK_STARTED=1"

call :LOG "Restaurando la base funcional anterior a la T."
copy /b /y "%SOURCE_CLIENT%" "%CLIENT_LIB%\mage-client-1.4.61.jar" > "%LOG_DIR%\restore_client.log" 2>&1
if errorlevel 1 goto FAIL_RESTORE
copy /b /y "%SOURCE_COMMON%" "%CLIENT_LIB%\mage-common-1.4.61.jar" > "%LOG_DIR%\restore_common.log" 2>&1
if errorlevel 1 goto FAIL_RESTORE

powershell -NoProfile -ExecutionPolicy Bypass -Command "$a=(Get-FileHash -LiteralPath '%SOURCE_CLIENT%' -Algorithm SHA256).Hash; $b=(Get-FileHash -LiteralPath '%CLIENT_LIB%\mage-client-1.4.61.jar' -Algorithm SHA256).Hash; $c=(Get-FileHash -LiteralPath '%SOURCE_COMMON%' -Algorithm SHA256).Hash; $d=(Get-FileHash -LiteralPath '%CLIENT_LIB%\mage-common-1.4.61.jar' -Algorithm SHA256).Hash; 'CLIENT_SOURCE='+$a; 'CLIENT_ACTIVE='+$b; 'COMMON_SOURCE='+$c; 'COMMON_ACTIVE='+$d; if($a -ne $b -or $c -ne $d){exit 1}" > "%LOG_DIR%\sha256_verification.log" 2>&1
if errorlevel 1 goto FAIL_VERIFY
call :LOG "PASS: base funcional anterior a la T restaurada y SHA-256 verificados."
call :LOG "Backup de emergencia de la version retirada=%BACKUP_DIR%"
call :LOG "LOG=%LOG_DIR%"
echo.
echo ROLLBACK COMPLETADO CORRECTAMENTE.
echo Se ha restaurado la base funcional anterior a la T.
echo No se ha modificado el servidor ni ninguna rama de Git.
echo Backup de emergencia: %BACKUP_DIR%
echo Logs: %LOG_DIR%
echo Cierra esta ventana y arranca XMage con tu lanzador habitual.
echo.
pause
exit /b 0

:FAIL_ACTIVE
call :FAIL "No se encontro la instalacion activa o sus JARs. No se modifico nada."
exit /b 1
:FAIL_SOURCE
call :FAIL "No se encontro la copia pre-T conocida o no es valida. No se modifico nada."
exit /b 1
:FAIL_STOP
call :FAIL "No se pudieron cerrar los procesos XMage. No se modifico nada."
exit /b 1
:FAIL_BACKUP
call :FAIL "No se pudo guardar la version actual. No se modifico nada."
exit /b 1
:FAIL_RESTORE
call :FAIL "Fallo la restauracion. Se intentara devolver la version anterior."
exit /b 1
:FAIL_VERIFY
call :FAIL "La verificacion SHA-256 fallo. Se intentara devolver la version anterior."
exit /b 1

:FAIL
call :LOG "FAIL: %~1"
if "%ROLLBACK_STARTED%"=="1" (
    call :LOG "RESTORE-FAILSAFE: devolviendo los JAR guardados antes del rollback."
    copy /b /y "%BACKUP_DIR%\mage-client-before-rollback.jar" "%CLIENT_LIB%\mage-client-1.4.61.jar" > "%LOG_DIR%\failsafe_client.log" 2>&1
    copy /b /y "%BACKUP_DIR%\mage-common-before-rollback.jar" "%CLIENT_LIB%\mage-common-1.4.61.jar" > "%LOG_DIR%\failsafe_common.log" 2>&1
)
echo.
echo ROLLBACK FALLIDO. NO SE ENTREGA COMO EXITOSO.
echo %~1
echo Logs: %LOG_DIR%
if exist "%BACKUP_DIR%" echo Backup: %BACKUP_DIR%
echo.
pause
exit /b 1

:LOG
>>"%TRANSCRIPT%" echo [%DATE% %TIME%] %~1
echo %~1
exit /b 0
