@echo off
setlocal EnableExtensions
cd /d "%~dp0"
title XMage Launcher oficial - Java 8 - UI lock

set "ROOT=%~dp0"
set "JAVA=%ROOT%java\jre1.8.0_201\bin\java.exe"
set "LAUNCHER=%ROOT%XMageLauncher-0.3.8.jar"
set "LOGDIR=%ROOT%logs\pair-rebuild"
set "LOG=%LOGDIR%\launcher-manual-%RANDOM%.log"
set "OUT=%LOGDIR%\launcher-manual-output-%RANDOM%.log"
set "ERR=%LOGDIR%\launcher-manual-errors-%RANDOM%.log"
set "JAVA_TOOL_OPTIONS=-Dsun.java2d.uiScale=1.5"

if not exist "%LOGDIR%" mkdir "%LOGDIR%" >nul 2>&1
(
  echo ================================================================
  echo XMAGE OFFICIAL LAUNCHER - MANUAL RECOVERY
  echo Date: %DATE% %TIME%
  echo Root: %ROOT%
  echo Java: %JAVA%
  echo Launcher: %LAUNCHER%
  echo UI_LOCK: -Dsun.java2d.uiScale=1.5
  echo ================================================================
) > "%LOG%" 2>&1

if not exist "%JAVA%" goto :fail_java
if not exist "%LAUNCHER%" goto :fail_launcher

echo Abriendo lanzador oficial con Java 8u201 y uiScale=1.5...
echo Log: %LOG%
>>"%LOG%" echo PORT 17171:
netstat -ano | findstr ":17171" >> "%LOG%" 2>&1

set "XMAGE_ROOT=%ROOT%"
set "XMAGE_JAVA=%JAVA%"
set "XMAGE_LAUNCHER=%LAUNCHER%"
set "XMAGE_LOG=%LOG%"
set "XMAGE_OUT=%OUT%"
set "XMAGE_ERR=%ERR%"

powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -Command ^
  "$ErrorActionPreference='Stop'; $p=Start-Process -FilePath $env:XMAGE_JAVA -ArgumentList @('-jar',$env:XMAGE_LAUNCHER) -WorkingDirectory $env:XMAGE_ROOT -RedirectStandardOutput $env:XMAGE_OUT -RedirectStandardError $env:XMAGE_ERR -PassThru; Add-Content -LiteralPath $env:XMAGE_LOG -Value ('OFFICIAL_LAUNCHER_PID='+$p.Id) -Encoding UTF8; Write-Host ('OFFICIAL_LAUNCHER_PID='+$p.Id)"
if errorlevel 1 goto :fail_start

echo Lanzador oficial abierto correctamente.
echo Servidor: si 17171 aparece en el log, pulsa solo "Launch Client".
echo Log: %LOG%
echo Salida launcher: %OUT%
echo Errores launcher: %ERR%
pause
exit /b 0

:fail_java
echo ERROR: falta Java 8u201 en %JAVA%
>>"%LOG%" echo ERROR: falta Java 8u201
echo Log: %LOG%
pause
exit /b 1

:fail_launcher
echo ERROR: falta %LAUNCHER%
>>"%LOG%" echo ERROR: falta el lanzador oficial
echo Log: %LOG%
pause
exit /b 2

:fail_start
echo ERROR: Windows no pudo abrir el lanzador.
>>"%LOG%" echo ERROR: Windows no pudo abrir el lanzador
echo Log: %LOG%
echo Error log: %ERR%
pause
exit /b 3
