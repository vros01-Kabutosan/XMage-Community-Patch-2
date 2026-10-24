@echo off
rem ---------------------------------------------------------------------------
rem Lanzador del vigilante de mazos de XMage.
rem
rem Lo invoca la tarea programada "XMage-VigilanteDecks" al iniciar sesion.
rem No modifica el vigilante: solo lo ejecuta y deja rastro en un log.
rem
rem Este script NO termina hasta que el vigilante termina, a proposito: asi la
rem tarea programada queda en estado "En ejecucion" mientras el vigilante viva,
rem lo que hace que su politica de instancia unica sea fiable y que el
rem historial de la tarea refleje la salud del vigilante.
rem
rem Los tres valores de abajo se pueden sustituir por otros (util para pruebas)
rem sin tocar el resto del script:
rem   %1 = ruta del vigilante_decks.py
rem   %2 = ruta del pythonw.exe
rem   %3 = ruta del log de diagnostico
rem ---------------------------------------------------------------------------

set "COMPROBAR=%~dp0comprobar-vigilante.ps1"

set "VIGILANTE=%~1"
set "PYTHONW=%~2"
set "LOG=%~3"

if "%VIGILANTE%"=="" set "VIGILANTE=J:\MTG\Instalacion-XMage\xmage\mage-client\sample-decks\Descargados\vigilante_decks.py"
if "%PYTHONW%"=="" set "PYTHONW=C:\Users\vros0\AppData\Local\Programs\Python\Python314\pythonw.exe"
if "%LOG%"=="" set "LOG=J:\MTG\Instalacion-XMage\xmage\mage-client\config\deck-downloader\vigilante-autoinicio.log"

if not exist "%PYTHONW%" (
    >>"%LOG%" echo %DATE% %TIME% ERROR: no existe el interprete %PYTHONW%
    exit /b 1
)
if not exist "%VIGILANTE%" (
    >>"%LOG%" echo %DATE% %TIME% ERROR: no existe el vigilante %VIGILANTE%
    exit /b 1
)

rem --- Instancia unica -------------------------------------------------------
rem La politica "IgnoreNew" de la tarea ya cubre dos arranques simultaneos de
rem la tarea; esta comprobacion cubre ademas una instancia lanzada a mano.
rem Aqui no se termina nada: si ya hay un vigilante vivo, simplemente no se
rem arranca otro, para no interferir con el que ya esta trabajando.
if exist "%COMPROBAR%" (
    powershell -NoProfile -NonInteractive -ExecutionPolicy Bypass -File "%COMPROBAR%"
    if not errorlevel 1 (
        >>"%LOG%" echo %DATE% %TIME% OMITIDO: ya hay un vigilante en ejecucion
        exit /b 0
    )
)

>>"%LOG%" echo %DATE% %TIME% INICIO: vigilante de mazos (pidWatcher en ejecucion)

rem El vigilante corre en primer plano de este script: se espera a que termine
rem para conocer su codigo de salida. pythonw no abre ventana de consola.
"%PYTHONW%" -u "%VIGILANTE%"
set "CODIGO=%ERRORLEVEL%"

>>"%LOG%" echo %DATE% %TIME% FIN: el vigilante termino con codigo %CODIGO%
exit /b %CODIGO%