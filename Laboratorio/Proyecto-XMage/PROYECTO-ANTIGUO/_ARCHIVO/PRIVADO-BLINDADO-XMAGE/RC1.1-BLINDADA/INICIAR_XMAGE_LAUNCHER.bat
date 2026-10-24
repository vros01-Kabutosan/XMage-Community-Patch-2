@echo off
setlocal EnableExtensions
cd /d "%~dp0"
title XMage Community Patch RC1 1.4.61 - Official Launcher

set "LOGDIR=%~dp0logs"
if not exist "%LOGDIR%" mkdir "%LOGDIR%"
set "LOG=%LOGDIR%\launcher-%RANDOM%.log"

>"%LOG%" echo [RC1] Official launcher start
>>"%LOG%" echo [RC1] Installation: %CD%
>>"%LOG%" echo [RC1] Launcher: %CD%\XMageLauncher-0.3.8.jar

set "JAVA_EXE=%~dp0java\jre17\bin\java.exe"
set "JAVA_AVAILABLE="
if exist "%JAVA_EXE%" set "JAVA_AVAILABLE=1"
if not defined JAVA_AVAILABLE (
  set "JAVA_EXE=java"
  where java >nul 2>&1
  if not errorlevel 1 set "JAVA_AVAILABLE=1"
)
if not defined JAVA_AVAILABLE (
  >>"%LOG%" echo [RC1] ERROR: Java 17 o superior no disponible.
  echo No se encuentra Java 17 o superior ni java en el PATH.
  echo Log: %LOG%
  pause
  exit /b 1
)

if not exist "%~dp0XMageLauncher-0.3.8.jar" (
  >>"%LOG%" echo [RC1] ERROR: falta XMageLauncher-0.3.8.jar
  echo Falta XMageLauncher-0.3.8.jar.
  echo Log: %LOG%
  pause
  exit /b 2
)

>>"%LOG%" "%JAVA_EXE%" -version
echo Abriendo XMage Launcher oficial 0.3.8...
echo Log: %LOG%
"%JAVA_EXE%" -Xms256m -Xmx1g -jar "%~dp0XMageLauncher-0.3.8.jar" >>"%LOG%" 2>&1
set "RC=%ERRORLEVEL%"
>>"%LOG%" echo [RC1] Launcher exit code: %RC%
if not "%RC%"=="0" echo El launcher ha terminado con codigo %RC%. Revisa el log.
pause
exit /b %RC%
