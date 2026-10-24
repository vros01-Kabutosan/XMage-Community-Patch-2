@echo off
setlocal EnableExtensions
cd /d "%~dp0"
title XMage Launcher oficial - UI blindada RC1 1.4.61

set "ROOT=%~dp0"
set "PROP=%ROOT%installed.properties"
set "JAVA=%ROOT%java\jre1.8.0_201\bin\java.exe"
set "LOGDIR=%ROOT%logs"
set "LOG=%LOGDIR%\launcher-ui-lock-%RANDOM%.log"
set "UIFLAG=-Dsun.java2d.uiScale=1.5"

if not exist "%LOGDIR%" mkdir "%LOGDIR%" >nul 2>&1

(
  echo ================================================================
  echo XMAGE OFFICIAL LAUNCHER - COMMUNITY UI LOCK
  echo RC1 1.4.61
  echo Date: %DATE% %TIME%
  echo Root: %ROOT%
  echo Java: %JAVA%
  echo Mandatory client UI flag: %UIFLAG%
  echo ================================================================
  echo.
  echo [CHECK] Required files
  if exist "%PROP%" (echo OK: installed.properties) else (echo ERROR: missing installed.properties)
  if exist "%JAVA%" (echo OK: Java 8 runtime) else (echo ERROR: missing Java 8 runtime)
  echo.
  echo [CHECK] Java runtime version
  "%JAVA%" -version
  echo.
  echo [PROPERTIES BEFORE]
  if exist "%PROP%" findstr /b /c:"xmage.client.javaopts=" /c:"java.version=" /c:"xmage.version=" "%PROP%"
  echo.
) > "%LOG%" 2>&1

if not exist "%PROP%" goto :fail_root
if not exist "%JAVA%" goto :fail_java

echo [LOCK] Enforcing xmage.client.javaopts=%UIFLAG% >> "%LOG%"
powershell.exe -NoProfile -ExecutionPolicy Bypass -Command ^
  "$p=$env:PROP; $raw=[IO.File]::ReadAllText($p); $lines=$raw -split '\r?\n',-1; $found=$false; for($i=0;$i -lt $lines.Count;$i++){ if($lines[$i] -match '^xmage\.client\.javaopts='){ $found=$true; $line=[regex]::Replace($lines[$i],'-Dsun\.java2d\.uiScale(?:\\)?=[^\s]+',''); $lines[$i]=$line.TrimEnd()+' -Dsun.java2d.uiScale\=1.5'; } }; if(-not $found){ throw 'xmage.client.javaopts not found' }; [IO.File]::WriteAllText($p,($lines -join [Environment]::NewLine),(New-Object Text.UTF8Encoding($false)))" >> "%LOG%" 2>&1
if errorlevel 1 goto :fail_property

echo. >> "%LOG%"
echo [PROPERTIES AFTER] >> "%LOG%"
findstr /b /c:"xmage.client.javaopts=" /c:"java.version=" /c:"xmage.version=" "%PROP%" >> "%LOG%" 2>&1

set "LAUNCHER="
for /f "delims=" %%F in ('dir /b /a-d /o-d "%ROOT%XMageLauncher-*.jar" 2^>nul') do if not defined LAUNCHER set "LAUNCHER=%ROOT%%%F"
if not defined LAUNCHER goto :fail_launcher

if defined JAVA_TOOL_OPTIONS (
  set "JAVA_TOOL_OPTIONS=%UIFLAG% %JAVA_TOOL_OPTIONS%"
) else (
  set "JAVA_TOOL_OPTIONS=%UIFLAG%"
)

(
  echo.
  echo [RUNTIME LOCK] JAVA_TOOL_OPTIONS=%JAVA_TOOL_OPTIONS%
  echo [RUNTIME LOCK] This environment is inherited by launcher, client and server.
  echo [LAUNCHER] %LAUNCHER%
  echo [LAUNCHER] Starting official XMage launcher...
) >> "%LOG%"

"%JAVA%" -jar "%LAUNCHER%" >> "%LOG%" 2>&1
set "RC=%ERRORLEVEL%"
echo. >> "%LOG%"
echo [END] Launcher exit code: %RC% >> "%LOG%"
echo [LOG] %LOG% >> "%LOG%"

echo.
echo Lanzador oficial iniciado con la interfaz blindada.
echo Opcion obligatoria: %UIFLAG%
echo Log: %LOG%
pause
exit /b %RC%

:fail_root
echo [FATAL] No se encontro installed.properties. >> "%LOG%"
echo ERROR: ejecuta este CMD en la raiz del clon XMage.
echo Log: %LOG%
pause
exit /b 1

:fail_java
echo [FATAL] No se encontro la Java 8 real. >> "%LOG%"
echo ERROR: falta java\jre1.8.0_201\bin\java.exe.
echo Log: %LOG%
pause
exit /b 2

:fail_property
echo [FATAL] No se pudo blindar xmage.client.javaopts. >> "%LOG%"
echo ERROR: no se pudo modificar la opcion del cliente. No se inicia XMage.
echo Log: %LOG%
pause
exit /b 3

:fail_launcher
echo [FATAL] No se encontro XMageLauncher-*.jar. >> "%LOG%"
echo ERROR: falta el lanzador oficial en la raiz.
echo Log: %LOG%
pause
exit /b 4
