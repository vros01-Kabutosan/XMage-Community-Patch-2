@echo off
setlocal EnableExtensions
cd /d "%~dp0"
title Preparar XMage Launcher oficial - RC1 1.4.61

set "ROOT=%~dp0"
set "JAVA17=%ROOT%java\jre17"
set "JAVA_ALIAS=%ROOT%java\jre"
set "CLIENT=%ROOT%client"
set "SERVER=%ROOT%server"
set "XMAGE=%ROOT%xmage"
set "CLIENT_ALIAS=%XMAGE%\mage-client"
set "SERVER_ALIAS=%XMAGE%\mage-server"

if not exist "%ROOT%installed.properties" (
  echo ERROR: este CMD debe estar en la raiz del clon XMage.
  pause
  exit /b 1
)
if not exist "%JAVA17%\bin\java.exe" (
  echo ERROR: no se encuentra java\jre17\bin\java.exe.
  pause
  exit /b 2
)
if not exist "%CLIENT%\lib\mage-client-1.4.61.jar" (
  echo ERROR: no se encuentra client\lib\mage-client-1.4.61.jar.
  pause
  exit /b 3
)
if not exist "%SERVER%\lib\mage-server-1.4.61.jar" (
  echo ERROR: no se encuentra server\lib\mage-server-1.4.61.jar.
  pause
  exit /b 4
)

if not exist "%ROOT%java" mkdir "%ROOT%java" >nul 2>&1
if not exist "%JAVA_ALIAS%\bin\java.exe" (
  echo Creando alias oficial java\jre ...
  mklink /J "%JAVA_ALIAS%" "%JAVA17%" >nul 2>&1
  if not exist "%JAVA_ALIAS%\bin\java.exe" (
    echo El enlace no se pudo crear; copiando la JVM como alternativa...
    robocopy "%JAVA17%" "%JAVA_ALIAS%" /E /COPY:DAT /R:1 /W:1 >nul
    if errorlevel 8 (
      echo ERROR: no se pudo preparar java\jre.
      pause
      exit /b 5
    )
  )
)

if not exist "%XMAGE%" mkdir "%XMAGE%" >nul 2>&1
if not exist "%CLIENT_ALIAS%\lib\mage-client-1.4.61.jar" (
  echo Creando alias oficial xmage\mage-client ...
  mklink /J "%CLIENT_ALIAS%" "%CLIENT%" >nul 2>&1
  if not exist "%CLIENT_ALIAS%\lib\mage-client-1.4.61.jar" (
    echo El enlace no se pudo crear; copiando el cliente como alternativa...
    robocopy "%CLIENT%" "%CLIENT_ALIAS%" /E /COPY:DAT /R:1 /W:1 >nul
    if errorlevel 8 (
      echo ERROR: no se pudo preparar xmage\mage-client.
      pause
      exit /b 6
    )
  )
)

if not exist "%SERVER_ALIAS%\lib\mage-server-1.4.61.jar" (
  echo Creando alias oficial xmage\mage-server ...
  mklink /J "%SERVER_ALIAS%" "%SERVER%" >nul 2>&1
  if not exist "%SERVER_ALIAS%\lib\mage-server-1.4.61.jar" (
    echo El enlace no se pudo crear; copiando el servidor como alternativa...
    robocopy "%SERVER%" "%SERVER_ALIAS%" /E /COPY:DAT /R:1 /W:1 >nul
    if errorlevel 8 (
      echo ERROR: no se pudo preparar xmage\mage-server.
      pause
      exit /b 7
    )
  )
)

echo.
echo Estructura compatible preparada correctamente:
echo   java\jre\bin\java.exe
echo   xmage\mage-client\lib\mage-client-1.4.61.jar
echo   xmage\mage-server\lib\mage-server-1.4.61.jar
echo.
echo Abriendo el lanzador oficial...
call "%ROOT%INICIAR_XMAGE_LAUNCHER.bat"
exit /b %ERRORLEVEL%
