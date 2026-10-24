@echo off
setlocal EnableExtensions
title SMOKE visual directo T RC1.3 v-1.0.1.8

set "SCRIPT=%~dp0LANZAR-SMOKE-DIRECTO-T-RC1.3-v-1.0.1.8.ps1"
if not exist "%SCRIPT%" (
    exit /b 1
)

start "" /b powershell.exe -NoLogo -NoProfile -WindowStyle Hidden -ExecutionPolicy Bypass -File "%SCRIPT%" %*
if errorlevel 1 (
    exit /b 1
)
exit /b 0
