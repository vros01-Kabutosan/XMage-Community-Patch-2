@echo off
setlocal EnableExtensions
cd /d "%~dp0"
title Reparar Java 8 XMage RC1 1.4.61

set "ROOT=%~dp0"
set "SOURCE=J:\MTG\java\jre1.8.0_201"
set "TARGET=%ROOT%java\jre1.8.0_201"
set "LOGDIR=%ROOT%logs"
set "LOG=%LOGDIR%\java8-repair-%RANDOM%.log"
set "BACKUP_NAME=jre1.8.0_201-java17-backup-%RANDOM%"

if not exist "%LOGDIR%" mkdir "%LOGDIR%" >nul 2>&1

(
  echo ================================================================
  echo XMAGE JAVA 8 REPAIR RC1 1.4.61
  echo Date: %DATE% %TIME%
  echo Root: %ROOT%
  echo Source: %SOURCE%
  echo Target: %TARGET%
  echo ================================================================
  echo.
  echo [CHECK] Source Java 8
  if exist "%SOURCE%\bin\java.exe" (echo OK: source found) else (echo ERROR: source not found)
  echo.
  echo [CHECK] Source Java version
  "%SOURCE%\bin\java.exe" -version
  echo.
  echo [CHECK] Launcher metadata
  if exist "%ROOT%installed.properties" findstr /b /c:"java.version=" /c:"xmage.version=" /c:"xmage.launcher.version=" "%ROOT%installed.properties"
  echo.
) > "%LOG%" 2>&1

if not exist "%SOURCE%\bin\java.exe" goto :fail_source
if not exist "%ROOT%installed.properties" goto :fail_root

if exist "%TARGET%\bin\java.exe" (
  echo [BACKUP] Moving existing target to java\%BACKUP_NAME% ... >> "%LOG%"
  ren "%TARGET%" "%BACKUP_NAME%" >> "%LOG%" 2>&1
  if errorlevel 1 goto :fail_backup
  echo Existing target preserved at: %ROOT%java\%BACKUP_NAME% >> "%LOG%"
)

if not exist "%ROOT%java" mkdir "%ROOT%java" >> "%LOG%" 2>&1
mkdir "%TARGET%" >> "%LOG%" 2>&1

echo [COPY] Copying real Java 8 runtime... >> "%LOG%"
robocopy "%SOURCE%" "%TARGET%" /E /COPY:DAT /DCOPY:DAT /XJ /R:1 /W:1 /MT:16 /TEE "/LOG+:%LOG%"
if errorlevel 8 goto :fail_copy

echo. >> "%LOG%"
echo [VERIFY] Target Java version after repair >> "%LOG%"
"%TARGET%\bin\java.exe" -version >> "%LOG%" 2>&1
if errorlevel 1 goto :fail_verify

(
  echo.
  echo [SUCCESS] Real Java 8u201 installed at:
  echo %TARGET%
  echo [SUCCESS] Previous runtime preserved at:
  echo %ROOT%java\%BACKUP_NAME%
  echo [LOG] %LOG%
) >> "%LOG%"

echo.
echo Reparacion completada correctamente.
echo Java 8 real instalada en:
echo %TARGET%
echo.
echo La Java anterior se ha conservado en:
echo %ROOT%java\%BACKUP_NAME%
echo.
echo Log generado:
echo %LOG%
echo.
echo Ahora puedes ejecutar INICIAR_XMAGE_LAUNCHER.bat.
pause
exit /b 0

:fail_source
echo [FATAL] No se encontro la Java 8 fuente: %SOURCE% >> "%LOG%"
echo ERROR: no se encontro la Java 8 fuente en %SOURCE%
echo Log: %LOG%
pause
exit /b 1

:fail_root
echo [FATAL] No se encontro installed.properties en la raiz. >> "%LOG%"
echo ERROR: ejecuta este CMD en la raiz del clon XMage.
echo Log: %LOG%
pause
exit /b 2

:fail_backup
echo [FATAL] No se pudo conservar el runtime anterior. No se copio nada nuevo. >> "%LOG%"
echo ERROR: no se pudo mover el runtime anterior; no se modifico.
echo Log: %LOG%
pause
exit /b 3

:fail_copy
echo [FATAL] Robocopy no pudo completar la copia de Java 8. >> "%LOG%"
echo ERROR: la copia de Java 8 fallo. El log contiene el detalle.
echo Log: %LOG%
pause
exit /b 4

:fail_verify
echo [FATAL] La Java copiada no supera la comprobacion de version. >> "%LOG%"
echo ERROR: la Java 8 copiada no se pudo verificar.
echo Log: %LOG%
pause
exit /b 5
