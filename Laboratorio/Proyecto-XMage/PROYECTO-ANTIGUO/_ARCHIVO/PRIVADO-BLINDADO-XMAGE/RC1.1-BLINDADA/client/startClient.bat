@ECHO OFF
setlocal EnableExtensions
cd /d "%~dp0"
if not exist "..\logs" mkdir "..\logs"
set "LOG=..\logs\client-%RANDOM%.log"

set "JAVA_EXE=..\java\jre17\bin\java.exe"
set "JAVA_AVAILABLE="
if exist "%JAVA_EXE%" set "JAVA_AVAILABLE=1"
if not defined JAVA_AVAILABLE (
  set "JAVA_EXE=java"
  where java >nul 2>&1
  if not errorlevel 1 set "JAVA_AVAILABLE=1"
)
if not defined JAVA_AVAILABLE (
  >"%LOG%" echo [RC1] ERROR: Java no esta instalado o no esta en PATH.
  >>"%LOG%" echo [RC1] Se necesita Java 17 o superior para JavaFX 11.
  echo ERROR: Java 17 o superior no esta disponible.
  echo Log: "%CD%\%LOG%"
  pause
  exit /b 1
)

>"%LOG%" echo [RC1] Inicio del cliente XMage Community Patch RC1 1.4.61
>>"%LOG%" echo [RC1] Directorio: %CD%
>>"%LOG%" echo [RC1] Memoria: Xms=1G Xmx=4G
>>"%LOG%" "%JAVA_EXE%" -version
"%JAVA_EXE%" -Xms1g -Xmx4g -XX:+UseG1GC -Dfile.encoding=UTF-8 -jar ".\lib\mage-client-1.4.61.jar" >>"%LOG%" 2>&1
set "RC=%ERRORLEVEL%"
>>"%LOG%" echo [RC1] Fin del cliente. Codigo=%RC%
echo Log: "%CD%\%LOG%"
if not "%RC%"=="0" echo El cliente termino con error. Revisa el log.
pause
exit /b %RC%
