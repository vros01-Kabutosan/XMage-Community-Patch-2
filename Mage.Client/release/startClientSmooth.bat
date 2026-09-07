@ECHO OFF
setlocal EnableExtensions
cd /d "%~dp0"
if not exist "..\logs" mkdir "..\logs"
set "LOG=..\logs\client-smooth-%RANDOM%.log"

set "JAVA_EXE=..\..\java\jre1.8.0_201\bin\java.exe"
if not exist "%JAVA_EXE%" set "JAVA_EXE=java"

>"%LOG%" echo XMage smooth launcher
>>"%LOG%" echo Hardware Java2D enabled; Xms=1G Xmx=8G; G1GC
>>"%LOG%" "%JAVA_EXE%" -version

"%JAVA_EXE%" -Xms1g -Xmx8g -XX:+UseG1GC -XX:MaxGCPauseMillis=50 -Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 -Dsun.java2d.d3d=true -Dsun.java2d.noddraw=false -Dsun.java2d.uiScale=1.5 -jar ".\lib\mage-client-1.4.61.jar" >>"%LOG%" 2>&1
set "RC=%ERRORLEVEL%"
>>"%LOG%" echo Exit code=%RC%
echo Log: "%CD%\%LOG%"
if not "%RC%"=="0" echo El cliente termino con error. Usa el arranque normal.
pause
exit /b %RC%
