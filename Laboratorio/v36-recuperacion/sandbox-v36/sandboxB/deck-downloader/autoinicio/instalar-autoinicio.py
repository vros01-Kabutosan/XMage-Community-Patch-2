#!/usr/bin/env python3
"""Instala y desinstala el autoinicio del vigilante de mazos de XMage.

Crea (o quita) la tarea programada "XMage-VigilanteDecks", que arranca
vigilante_decks.py con pythonw al iniciar sesion. No modifica el vigilante
ni ningun otro componente de XMage.

    py -3 instalar-autoinicio.ps1        (desde PowerShell)
    py -3 instalar-autoinicio.py         instalacion
    py -3 instalar-autoinicio.py --desinstalar

Se ejecuta con el usuario actual y sin privilegios de administrador.
"""

from __future__ import annotations

import argparse
import shutil
import subprocess
import sys
from pathlib import Path

AQUI = Path(__file__).resolve().parent
NOMBRE_TAREA = "XMage-VigilanteDecks"

VIGILANTE = Path(
    r"J:\MTG\Instalacion-XMage\xmage\mage-client\sample-decks\Descargados\vigilante_decks.py"
)
PYTHONW = Path(
    r"C:\Users\vros0\AppData\Local\Programs\Python\Python314\pythonw.exe"
)
LOG = Path(
    r"J:\MTG\Instalacion-XMage\xmage\mage-client\config\deck-downloader"
    r"\vigilante-autoinicio.log"
)


def powershell(script: str) -> tuple[int, str]:
    resultado = subprocess.run(
        ["powershell", "-NoProfile", "-NonInteractive", "-Command", script],
        capture_output=True, text=True, encoding="utf-8", errors="replace",
    )
    return resultado.returncode, (resultado.stdout or "") + (resultado.stderr or "")


def instalar() -> int:
    for ruta, que in ((VIGILANTE, "vigilante_decks.py"),
                      (PYTHONW, "pythonw.exe")):
        if not ruta.is_file():
            print(f"ERROR: no existe {que} en {ruta}")
            return 1

    print(f"Vigilante : {VIGILANTE}")
    print(f"Interprete: {PYTHONW}")
    print(f"Log       : {LOG}")

    codigo, salida = powershell(
        "$ErrorActionPreference='Stop';"
        f"$a = New-ScheduledTaskAction -Execute '{AQUI / 'INICIAR-VIGILANTE-DECKS.cmd'}'"
        f" -WorkingDirectory '{VIGILANTE.parent}';"
        "$t = New-ScheduledTaskTrigger -AtLogOn -User $env:USERNAME;"
        "$s = New-ScheduledTaskSettingsSet -MultipleInstances IgnoreNew"
        " -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries"
        " -ExecutionTimeLimit ([TimeSpan]::Zero) -RestartCount 3"
        " -RestartInterval (New-TimeSpan -Minutes 5) -StartWhenAvailable -Hidden;"
        f"Register-ScheduledTask -TaskName '{NOMBRE_TAREA}' -Action $a -Trigger $t"
        f" -Settings $s -Description 'Arranca el vigilante de mazos de XMage al iniciar sesion'"
        " -Force | Out-Null;"
        f"(Get-ScheduledTask -TaskName '{NOMBRE_TAREA}').State"
    )
    if codigo != 0:
        print("ERROR: no se pudo crear la tarea")
        print(salida)
        return 1

    print(f"\nTarea '{NOMBRE_TAREA}' creada. Estado: {salida.strip()}")

    codigo, salida = powershell(f"Start-ScheduledTask -TaskName '{NOMBRE_TAREA}'")
    if codigo != 0:
        print("AVISO: no se pudo arrancar la tarea ahora mismo")
        print(salida)

    import time
    time.sleep(5)
    _, salida = powershell(
        "(Get-CimInstance Win32_Process -Filter \"Name='pythonw.exe'\" |"
        " Where-Object { $_.CommandLine -like '*vigilante_decks.py*' }).ProcessId"
    )
    pids = [x for x in salida.split() if x.strip()]
    if pids:
        print(f"Vigilante en marcha, PID {', '.join(pids)}")
    else:
        print("AVISO: el vigilante no aparece en la lista de procesos")
        print(f"Revisa {LOG}")

    print("\nPara desinstalar:")
    print(f"  py -3 {Path(__file__).name} --desinstalar")
    return 0


def desinstalar() -> int:
    powershell(f"Stop-ScheduledTask -TaskName '{NOMBRE_TAREA}' -ErrorAction SilentlyContinue")
    codigo, salida = powershell(
        f"Unregister-ScheduledTask -TaskName '{NOMBRE_TAREA}'"
        " -Confirm:$false -ErrorAction SilentlyContinue; 'ok'"
    )
    print(f"Tarea '{NOMBRE_TAREA}' desinstalada.")

    _, salida = powershell(
        "(Get-CimInstance Win32_Process -Filter \"Name='pythonw.exe'\" |"
        " Where-Object { $_.CommandLine -like '*vigilante_decks.py*' }).ProcessId"
    )
    pids = [x for x in salida.split() if x.strip()]
    if pids:
        print(f"AVISO: el vigilante sigue vivo (PID {', '.join(pids)}).")
        print("       Stop-ScheduledTask no lo mata; el lanzador nunca termina")
        print("       procesos a proposito. Para pararlo:")
        print(f"       taskkill /PID {pids[0]} /F")
    print("\nEl vigilante sigue disponible para arrancarlo a mano:")
    print(f"  py -3 \"{VIGILANTE}\"")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--desinstalar", action="store_true",
                        help="Quita la tarea programada")
    args = parser.parse_args()
    return desinstalar() if args.desinstalar else instalar()


if __name__ == "__main__":
    sys.exit(main())