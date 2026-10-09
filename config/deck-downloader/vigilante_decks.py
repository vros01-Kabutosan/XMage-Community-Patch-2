#!/usr/bin/env python3
# -*- coding: utf-8 -*-
r"""
vigilante_decks.py - Ordena los decks automáticamente al terminar el descargador.

Cómo funciona:
 - Vigila los logs del descargador (por defecto todos los *.log de la
   carpeta deck-downloader).
 - Cuando ve la línea "ACTUALIZACIÓN TERMINADA", espera unos segundos
   (para que reposen los últimos archivos) y lanza ordenar_decks.py --no-pausa.
 - Se queda residente hasta Ctrl+C.

Uso: doble clic, o  python vigilante_decks.py
"""

import subprocess
import sys
import time
from datetime import datetime
from pathlib import Path

AQUI = Path(__file__).resolve().parent
SCRIPT_ORDENAR = AQUI / "ordenar_decks.py"

# Carpeta donde el descargador escribe sus logs
CARPETA_LOGS = Path(r"J:\MTG\Instalacion-XMage\xmage\mage-client\config\deck-downloader")

# Si sabes el nombre exacto del log del motor, ponlo aquí,
# p.ej. LOG_DESCARGADOR = CARPETA_LOGS / "decks.log"
# Con None, vigila todos los .log de la carpeta (recomendado).
LOG_DESCARGADOR = None

MARCADOR = "ACTUALIZACIÓN TERMINADA"
INTERVALO_SEG = 3     # frecuencia de comprobación del log
RETARDO_SEG = 10      # pausa tras detectar el fin, antes de ordenar
COOLDOWN_SEG = 60     # separación mínima entre dos ordenaciones


def hora():
    return datetime.now().strftime("%H:%M:%S")


def decodificar(datos):
    try:
        return datos.decode("utf-8")
    except UnicodeDecodeError:
        return datos.decode("cp1252", errors="replace")


def logs_a_vigilar():
    if LOG_DESCARGADOR is not None:
        return [LOG_DESCARGADOR] if LOG_DESCARGADOR.exists() else []
    try:
        return sorted(CARPETA_LOGS.glob("*.log"))
    except OSError:
        return []


def lanzar_ordenador():
    print(f"[{hora()}] >>> Lanzando ordenar_decks.py...")
    try:
        subprocess.run([sys.executable, str(SCRIPT_ORDENAR), "--no-pausa"], cwd=str(AQUI))
        print(f"[{hora()}] <<< Ordenación terminada.")
    except Exception as e:
        print(f"[{hora()}] Error al lanzar el ordenador: {e}")


def main():
    print("=" * 62)
    print(" VIGILANTE DE DECKS - se activa al terminar la descarga")
    print(f" Logs vigilados : {CARPETA_LOGS}")
    print(f" Marcador       : {MARCADOR!r}")
    print(" Ctrl+C para parar.")
    print("=" * 62)

    posiciones = {}   # log -> último byte leído
    colas = {}        # log -> cola de texto (por si una línea queda partida)
    ultima_ejecucion = 0.0

    # Arranca desde el FINAL de los logs actuales: solo reacciona a lo nuevo
    for log in logs_a_vigilar():
        try:
            posiciones[str(log)] = log.stat().st_size
            colas[str(log)] = ""
        except OSError:
            pass

    print(f"[{hora()}] Listo. Esperando el próximo 'ACTUALIZACIÓN TERMINADA'...")

    while True:
        time.sleep(INTERVALO_SEG)
        ahora = time.time()

        for log in logs_a_vigilar():
            clave = str(log)
            try:
                size = log.stat().st_size
            except OSError:
                continue

            pos = posiciones.get(clave)
            if pos is None:                      # log nuevo aparecido en marcha
                posiciones[clave] = size
                colas[clave] = ""
                continue
            if size == pos:                      # sin novedades
                continue
            if size < pos:                       # log rotado/truncado
                print(f"[{hora()}] Log rotado: {log.name}")
                pos = 0

            with open(log, "rb") as f:
                f.seek(pos)
                nuevo = f.read()
            posiciones[clave] = size

            texto = colas.get(clave, "") + decodificar(nuevo)

            if MARCADOR in texto:
                colas[clave] = ""
                if ahora - ultima_ejecucion < COOLDOWN_SEG:
                    continue
                print(f"[{hora()}] Descarga terminada detectada ({log.name}). "
                      f"Esperando {RETARDO_SEG} s...")
                time.sleep(RETARDO_SEG)
                lanzar_ordenador()
                ultima_ejecucion = time.time()
                print(f"[{hora()}] Vigilando de nuevo hasta la próxima actualización...")
            else:
                colas[clave] = texto[-120:]


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\nVigilante detenido.")
    finally:
        if "--no-pausa" not in sys.argv:
            try:
                input("\nPulsa Enter para cerrar...")
            except (EOFError, OSError):
                pass