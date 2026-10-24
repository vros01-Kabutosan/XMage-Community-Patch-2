@echo off
setlocal EnableExtensions
cd /d "%~dp0"
title Fix XMage Launcher metadata - RC1 1.4.61 v1.2

set "PROP=%~dp0installed.properties"
if not exist "%PROP%" (
  echo ERROR: no se encuentra installed.properties junto a este CMD.
  echo Coloca este archivo en la carpeta raiz de XMage.
  pause
  exit /b 1
)

if not exist "%PROP%.before-launcher-fix-v1.2.bak" (
  copy /y "%PROP%" "%PROP%.before-launcher-fix-v1.2.bak" >nul
)

powershell.exe -NoProfile -ExecutionPolicy Bypass -Command ^
  "$p = [IO.Path]::GetFullPath($env:PROP); $raw = [IO.File]::ReadAllText($p); $raw = [regex]::Replace($raw, '(?m)^java\.version=.*$', 'java.version=1.8.0_201'); $raw = [regex]::Replace($raw, '(?m)^xmage\.version=.*$', 'xmage.version=1.4.61-dev (2026-08-12 12-34)'); $utf8 = New-Object System.Text.UTF8Encoding($false); [IO.File]::WriteAllText($p, $raw, $utf8)"
if errorlevel 1 (
  echo ERROR: no se pudo actualizar installed.properties.
  pause
  exit /b 2
)

echo.
echo Metadata corregida:
findstr /b /c:"java.version=" /c:"xmage.version=" /c:"xmage.launcher.version=" "%PROP%"
echo.
echo Cierra el lanzador y vuelve a ejecutar INICIAR_XMAGE_LAUNCHER.bat
pause
exit /b 0
