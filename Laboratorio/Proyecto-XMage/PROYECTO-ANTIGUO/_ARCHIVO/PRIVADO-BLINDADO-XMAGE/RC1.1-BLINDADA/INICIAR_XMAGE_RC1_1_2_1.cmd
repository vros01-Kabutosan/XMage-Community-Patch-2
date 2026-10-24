@echo off
setlocal EnableExtensions EnableDelayedExpansion
cd /d "%~dp0"
title XMage RC1 1.4.61 - arranque secuencial 1.2.1

set "ROOT=%~dp0"
set "LOGDIR=%ROOT%logs"
if not exist "%LOGDIR%" mkdir "%LOGDIR%" >nul 2>&1
set "LOG=%LOGDIR%\sequential-launch-%RANDOM%.log"
set "SERVERLOG=%LOGDIR%\server-live-%RANDOM%.log"
set "LAUNCHER_OUT=%LOGDIR%\launcher-live-%RANDOM%.log"
set "LAUNCHER_ERR=%LOGDIR%\launcher-live-errors-%RANDOM%.log"
set "JAVA=%ROOT%java\jre1.8.0_201\bin\java.exe"
set "LAUNCHER=%ROOT%XMageLauncher-0.3.8.jar"
set "PS=%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe"
set "UIFLAG=-Dsun.java2d.uiScale=1.5"
set "JAVA_TOOL_OPTIONS=%UIFLAG%"

(
  echo ================================================================
  echo XMAGE RC1 1.4.61 - ARRANQUE SECUENCIAL v1.2.1
  echo Date: %DATE% %TIME%
  echo Root: %ROOT%
  echo Java: %JAVA%
  echo Launcher: %LAUNCHER%
  echo Mandatory UI flag: %UIFLAG%
  echo ================================================================
) > "%LOG%" 2>&1

echo.
echo [XMAGE] Script iniciado correctamente.
echo [XMAGE] Log: %LOG%
echo [XMAGE] UI blindada: %UIFLAG%
>>"%LOG%" echo [XMAGE] Script iniciado.

if not exist "%JAVA%" goto error_java
if not exist "%LAUNCHER%" goto error_launcher
if not exist "%ROOT%installed.properties" goto error_properties
if not exist "%ROOT%xmage\mage-server\lib" goto error_server

echo [1/4] Java 8u201 detectado.
>>"%LOG%" echo [1/4] Java 8u201 detectado.
"%JAVA%" -version 2>>"%LOG%"

echo [2/4] Comprobando el puerto 17171...
>>"%LOG%" echo [2/4] Comprobando el puerto 17171...
netstat -ano | findstr /R /C:":17171 .*LISTENING" >nul 2>&1
if not errorlevel 1 (
  echo [XMAGE] El servidor ya esta escuchando. Se reutiliza.
  >>"%LOG%" echo [XMAGE] El servidor ya estaba escuchando; no se inicia otro.
  goto wait_server
)

echo [3/4] Iniciando servidor XMage con Java 8...
>>"%LOG%" echo [3/4] Iniciando servidor XMage con Java 8...
start "XMage Server RC1 1.4.61" /D "%ROOT%" /B "%ComSpec%" /d /c ""%JAVA%" -Xms512m -Xmx4g -Djava.net.preferIPv4Stack=true -cp "%ROOT%xmage\mage-server\lib\*" mage.server.Main >> "%SERVERLOG%" 2>&1"
if errorlevel 1 goto error_start
>>"%LOG%" echo Server log: %SERVERLOG%

:wait_server
echo [XMAGE] Esperando a que el servidor abra 17171...
>>"%LOG%" echo [XMAGE] Esperando a que el servidor abra 17171...
set /a SECONDS=0
:wait_loop
netstat -ano | findstr /R /C:":17171 .*LISTENING" >nul 2>&1
if not errorlevel 1 goto server_ready
set /a SECONDS+=2
echo     Esperando... !SECONDS! / 180 segundos
>>"%LOG%" echo     Esperando... !SECONDS! / 180 segundos
if !SECONDS! GEQ 180 goto error_timeout
timeout /t 2 /nobreak >nul
goto wait_loop

:server_ready
echo [XMAGE] SERVIDOR LISTO en 127.0.0.1:17171
>>"%LOG%" echo [XMAGE] SERVIDOR LISTO en 127.0.0.1:17171
echo [4/4] Abriendo y verificando el lanzador oficial...
>>"%LOG%" echo [4/4] Abriendo y verificando el lanzador oficial...

set "XMAGE_ROOT=%ROOT%"
set "XMAGE_JAVA=%JAVA%"
set "XMAGE_LAUNCHER=%LAUNCHER%"
set "XMAGE_LOG=%LOG%"
set "XMAGE_LAUNCHER_OUT=%LAUNCHER_OUT%"
set "XMAGE_LAUNCHER_ERR=%LAUNCHER_ERR%"
set "XMAGE_UIFLAG=%UIFLAG%"

"%PS%" -NoLogo -NoProfile -ExecutionPolicy Bypass -Command ^
  "$ErrorActionPreference='Stop'; $p=Start-Process -FilePath $env:XMAGE_JAVA -ArgumentList @($env:XMAGE_UIFLAG,'-jar',$env:XMAGE_LAUNCHER) -WorkingDirectory $env:XMAGE_ROOT -WindowStyle Normal -RedirectStandardOutput $env:XMAGE_LAUNCHER_OUT -RedirectStandardError $env:XMAGE_LAUNCHER_ERR -PassThru; Add-Content -LiteralPath $env:XMAGE_LOG -Value ('OFFICIAL_LAUNCHER_PID='+$p.Id) -Encoding UTF8; Start-Sleep -Seconds 3; $p.Refresh(); if($p.HasExited){ Add-Content -LiteralPath $env:XMAGE_LOG -Value ('LAUNCHER_EXIT_CODE='+$p.ExitCode) -Encoding UTF8; throw ('El lanzador oficial termino antes de mostrar la ventana. Codigo='+$p.ExitCode) }; Add-Content -LiteralPath $env:XMAGE_LOG -Value 'LAUNCHER_ALIVE_AFTER_3_SECONDS=YES' -Encoding UTF8; Write-Host ('OFFICIAL_LAUNCHER_PID='+$p.Id)" >> "%LOG%" 2>&1
if errorlevel 1 goto error_launcher_start

echo.
echo ================================================================
echo LANZADOR OFICIAL ABIERTO Y VERIFICADO.
echo Pulsa solamente: Launch Client
echo NO pulses: Launch Client and Server
echo porque el servidor ya esta activo.
echo.
echo Logs:
echo   %LOG%
echo   %SERVERLOG%
echo   %LAUNCHER_OUT%
echo   %LAUNCHER_ERR%
echo ================================================================
>>"%LOG%" echo [END] Arranque secuencial y lanzador verificados.
pause
exit /b 0

:error_java
echo ERROR: No existe Java 8u201 en la carpeta java.
>>"%LOG%" echo ERROR: No existe Java 8u201.
goto fail

:error_launcher
echo ERROR: No existe XMageLauncher-0.3.8.jar.
>>"%LOG%" echo ERROR: No existe XMageLauncher-0.3.8.jar.
goto fail

:error_properties
echo ERROR: No existe installed.properties.
>>"%LOG%" echo ERROR: No existe installed.properties.
goto fail

:error_server
echo ERROR: No existe xmage\mage-server\lib.
>>"%LOG%" echo ERROR: No existe xmage\mage-server\lib.
goto fail

:error_start
echo ERROR: No se pudo iniciar el proceso del servidor.
>>"%LOG%" echo ERROR: No se pudo iniciar el proceso del servidor.
goto fail

:error_timeout
echo ERROR: El servidor no abrio el puerto 17171 en 180 segundos.
>>"%LOG%" echo ERROR: El servidor no abrio el puerto 17171 en 180 segundos.
if exist "%SERVERLOG%" type "%SERVERLOG%"
goto fail

:error_launcher_start
echo ERROR: El lanzador oficial no quedo abierto.
echo Revisa tambien la salida y el error del lanzador.
>>"%LOG%" echo ERROR: El lanzador oficial no quedo abierto.
goto fail

:fail
echo.
echo No se ha modificado ningun archivo del proyecto.
echo Log: %LOG%
if exist "%LAUNCHER_OUT%" echo Launcher salida: %LAUNCHER_OUT%
if exist "%LAUNCHER_ERR%" echo Launcher errores: %LAUNCHER_ERR%
echo.
pause
exit /b 1
