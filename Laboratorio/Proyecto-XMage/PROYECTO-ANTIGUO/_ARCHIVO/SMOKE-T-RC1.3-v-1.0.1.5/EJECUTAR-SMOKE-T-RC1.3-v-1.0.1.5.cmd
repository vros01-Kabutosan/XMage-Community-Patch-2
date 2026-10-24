@echo off
setlocal EnableExtensions
title SMOKE visual T RC1.3 v-1.0.1.5

set "SCRIPT=%~dp0LANZAR-SMOKE-T-RC1.3-v-1.0.1.5.ps1"
if not exist "%SCRIPT%" (
    echo ERROR: falta LANZAR-SMOKE-T-RC1.3-v-1.0.1.5.ps1 junto a este archivo.
    pause
    exit /b 1
)

echo Iniciando SMOKE visual aislado de T RC1.3 v-1.0.1.5...
echo La instalacion real no se toca.
echo Se usara automaticamente el ultimo SMOKE con preparacion exitosa.
echo.
powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT%" %*
set "RC=%ERRORLEVEL%"
echo.
if not "%RC%"=="0" echo SMOKE ABORTADO. Revisa el log indicado.
if "%RC%"=="0" echo SMOKE FINALIZADO. La instalacion real no se ha tocado.
pause
exit /b %RC%
