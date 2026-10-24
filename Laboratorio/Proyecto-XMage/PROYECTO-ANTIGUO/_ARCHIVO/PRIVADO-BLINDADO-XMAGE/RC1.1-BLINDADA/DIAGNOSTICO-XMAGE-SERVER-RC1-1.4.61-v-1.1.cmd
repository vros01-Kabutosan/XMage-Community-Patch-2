@echo off
setlocal EnableExtensions
cd /d "%~dp0"
title Diagnostico XMage Server RC1 1.4.61

set "ROOT=%~dp0"
set "JAVA=%ROOT%java\jre1.8.0_201\bin\java.exe"
set "SERVER=%ROOT%xmage\mage-server"
set "LOGDIR=%ROOT%logs"
set "LOG=%LOGDIR%\server-diagnostic-%RANDOM%.log"

if not exist "%LOGDIR%" mkdir "%LOGDIR%" >nul 2>&1

(
  echo ================================================================
  echo XMAGE SERVER DIAGNOSTIC RC1 1.4.61
  echo Date: %DATE% %TIME%
  echo Root: %ROOT%
  echo Java: %JAVA%
  echo Server: %SERVER%
  echo ================================================================
  echo.
  echo [CHECK] Java executable
  if exist "%JAVA%" (echo OK: existe) else (echo ERROR: no existe)
  echo.
  echo [CHECK] Server files
  if exist "%SERVER%\lib\mage-server-1.4.61.jar" (echo OK: mage-server JAR encontrado) else (echo ERROR: falta mage-server-1.4.61.jar)
  if exist "%SERVER%\config\config.xml" (echo OK: config\config.xml encontrada) else (echo AVISO: no se ve config\config.xml en xmage\mage-server)
  echo.
  echo [CHECK] Java version used by this test
  "%JAVA%" -version
  echo.
  echo [CHECK] Puerto 17171 antes de arrancar
  netstat -ano | findstr ":17171"
  if errorlevel 1 echo OK: no aparece ningun proceso escuchando en 17171
  echo.
  echo [START] Ejecutando mage.server.Main con el classpath oficial del servidor
  echo [START] Si el servidor arranca, deja esta ventana abierta
  echo.
) > "%LOG%" 2>&1

if not exist "%JAVA%" goto :fail
if not exist "%SERVER%\lib\mage-server-1.4.61.jar" goto :fail

pushd "%SERVER%"
"%JAVA%" -cp "lib\*" mage.server.Main >> "%LOG%" 2>&1
set "RC=%ERRORLEVEL%"
popd

(
  echo.
  echo [END] Server process exit code: %RC%
  echo [CHECK] Puerto 17171 despues de cerrar el proceso
  netstat -ano | findstr ":17171"
  echo.
  echo Log completo: %LOG%
) >> "%LOG%" 2>&1

echo.
echo El diagnostico ha terminado o el servidor se ha cerrado.
echo Log generado:
echo %LOG%
echo.
echo Adjunta ese archivo para identificar el error exacto.
pause
exit /b %RC%

:fail
(
  echo.
  echo [FATAL] No se puede iniciar la prueba: falta Java o el JAR del servidor.
  echo Log completo: %LOG%
) >> "%LOG%" 2>&1
echo.
echo ERROR: faltan archivos necesarios. Revisa el log:
echo %LOG%
pause
exit /b 1
