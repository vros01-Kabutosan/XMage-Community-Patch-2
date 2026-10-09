#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Pruebas del vigilante de decks.

No tocan la biblioteca real: cada prueba construye su propio arbol temporal
con una carpeta de logs y las carpetas Standard/Pioneer/Modern, copia ahi el
vigilante y el organizador, y ejecuta el vigilante como proceso real.

    py -3 tests_vigilante.py                      # usa el vigilante de al lado
    py -3 tests_vigilante.py ruta/al/vigilante.py

La prueba T0 es la regresión que se reparó: comprueba que la ruta de
CARPETA_LOGS existe de verdad y que contiene el marcador que el vigilante
busca. Antes de la reparación fallaba, porque el archivo traía la ruta de
una instalación antigua.
"""

import re
import shutil
import subprocess
import sys
import tempfile
import time
from pathlib import Path

VIGILANTE = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else (
    Path(__file__).resolve().parent.parent / "vigilante_decks.py"
)
ORDENAR = Path(sys.argv[2]).resolve() if len(sys.argv) > 2 else (
    Path(__file__).resolve().parent.parent / "ordenar_decks.py"
)
MARCADOR = "ACTUALIZACIÓN TERMINADA"
fallos = []


def check(nombre, condicion, detalle=""):
    print(("  PASS " if condicion else "  FAIL ") + nombre + (f"  [{detalle}]" if detalle else ""))
    if not condicion:
        fallos.append(nombre)


def escenario():
    """Arbol temporal que imita la instalacion: motor aparte, Descargados aparte."""
    raiz = Path(tempfile.mkdtemp(prefix="vigtest_"))
    motor = raiz / "config" / "deck-downloader"
    descargados = raiz / "sample-decks" / "Descargados"
    motor.mkdir(parents=True)
    for modo in ("Standard", "Pioneer", "Modern"):
        (descargados / modo).mkdir(parents=True)
    return raiz, motor, descargados


def variante(raiz, motor):
    """Copia el vigilante con CARPETA_LOGS apuntando al motor temporal."""
    texto = VIGILANTE.read_text(encoding="utf-8")
    texto = re.sub(
        r'CARPETA_LOGS = Path\(r"[^"]*"\)',
        lambda m: 'CARPETA_LOGS = Path(r"' + str(motor) + '")',
        texto,
    )
    destino = raiz / "vigilante_decks.py"
    destino.write_text(texto, encoding="utf-8")
    return destino


def lanzar(_vig, raiz, motor, descargados):
    shutil.copy2(ORDENAR, descargados / "ordenar_decks.py")
    ejecutable = variante(raiz, motor)
    shutil.copy2(ejecutable, descargados / "vigilante_decks.py")
    log = motor / "deck-library-updater.log"
    log.write_bytes("contenido previo que NO debe disparar\n".encode("utf-8"))
    proc = subprocess.Popen(
        [sys.executable, "-u", str(descargados / "vigilante_decks.py")],
        stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
        text=True, encoding="utf-8", errors="replace", cwd=str(descargados),
    )
    return proc, log


def esperar(proc, segundos):
    limite = time.time() + segundos
    while time.time() < limite:
        if proc.poll() is not None:
            return
        time.sleep(0.2)


def test_ruta_de_produccion_existe():
    """La ruta tal y como viene en el archivo debe existir de verdad."""
    print("== T0: la ruta de configuracion apunta a un directorio real ==")
    texto = VIGILANTE.read_text(encoding="utf-8")
    m = re.search(r'CARPETA_LOGS = Path\(r"([^"]*)"\)', texto)
    check("el archivo declara CARPETA_LOGS", m is not None)
    if not m:
        return
    ruta = Path(m.group(1))
    check("la ruta configurada existe en disco", ruta.is_dir(), str(ruta))
    if not ruta.is_dir():
        return
    logs = list(ruta.glob("*.log"))
    check("hay logs del descargador en esa carpeta", bool(logs), str(ruta))
    if logs:
        alguno = any(
            MARCADOR in l.read_bytes().decode("utf-8", "replace") for l in logs
        )
        check("algun log contiene el marcador que el vigilante busca", alguno)


def test_resolucion_ruta():
    print("== T1: la ruta de logs existe y devuelve logs ==")
    raiz, motor, descargados = escenario()
    try:
        import importlib.util
        v = importlib.util.spec_from_file_location("v", str(variante(raiz, motor)))
        mod = importlib.util.module_from_spec(v)
        v.loader.exec_module(mod)
        (motor / "deck-library-updater.log").write_bytes(b"x\n")
        check("CARPETA_LOGS apunta a un directorio existente", mod.CARPETA_LOGS.is_dir(), str(mod.CARPETA_LOGS))
        check("logs_a_vigilar() ve los logs", len(mod.logs_a_vigilar()) >= 1)
        check("el marcador coincide con el del motor", mod.MARCADOR == MARCADOR)
        check("SCRIPT_ORDENAR apunta junto al vigilante", mod.SCRIPT_ORDENAR.parent == mod.AQUI)
    finally:
        shutil.rmtree(raiz, ignore_errors=True)


def test_detecta_finalizacion():
    print("== T2: detecta 'ACTUALIZACIÓN TERMINADA' y lanza el organizador ==")
    raiz, motor, descargados = escenario()
    proc = None
    try:
        proc, log = lanzar(VIGILANTE, raiz, motor, descargados)
        esperar(proc, 3)
        for modo in ("Standard", "Pioneer", "Modern"):
            (descargados / modo / "2026-10-09_Ejemplo.dck").write_text("x\n", encoding="utf-8")
        with log.open("ab") as f:
            f.write("\nACTUALIZACIÓN TERMINADA: nuevos=3, repetidos=0, rechazados=0\n".encode("utf-8"))
        esperar(proc, 22)
        proc.terminate()
        salida = proc.communicate(timeout=15)[0]
        check("arranca y queda esperando", "Listo. Esperando" in salida)
        check("detecta el final de la descarga", "Descarga terminada detectada" in salida)
        check("lanza ordenar_decks.py", "Lanzando ordenar_decks.py" in salida)
        check("el organizador termina bien", "Ordenación terminada" in salida)
        check("vuelve a vigilar", "Vigilando de nuevo" in salida)
        movidos = list((descargados / "Standard" / "2026-10-09").glob("*.dck"))
        check("el organizador movio los mazos a la subcarpeta de fecha", len(movidos) == 1, str(movidos))
    finally:
        if proc:
            try:
                proc.kill()
            except Exception:
                pass
        shutil.rmtree(raiz, ignore_errors=True)


def test_no_dispara_con_contenido_viejo():
    print("== T3: no reacciona a logs anteriores al arranque ==")
    raiz, motor, descargados = escenario()
    proc = None
    try:
        (motor / "deck-library-updater.log").write_bytes(b"x\n")
        proc, log = lanzar(VIGILANTE, raiz, motor, descargados)
        log.write_bytes("ACTUALIZACIÓN TERMINADA: viejos=99\n".encode("utf-8"))
        esperar(proc, 16)
        proc.terminate()
        salida = proc.communicate(timeout=15)[0]
        check("NO reacciona a contenido ya existente", "Descarga terminada detectada" not in salida)
    finally:
        if proc:
            try:
                proc.kill()
            except Exception:
                pass
        shutil.rmtree(raiz, ignore_errors=True)


def test_tres_formatos():
    print("== T4: Standard, Pioneer y Modern se organizan por igual ==")
    raiz, motor, descargados = escenario()
    proc = None
    try:
        proc, log = lanzar(VIGILANTE, raiz, motor, descargados)
        esperar(proc, 3)
        for modo in ("Standard", "Pioneer", "Modern"):
            for i in range(2):
                (descargados / modo / f"2026-10-09_{modo}_{i}.dck").write_text("x\n", encoding="utf-8")
        with log.open("ab") as f:
            f.write("\nACTUALIZACIÓN TERMINADA: nuevos=6, repetidos=0, rechazados=0\n".encode("utf-8"))
        esperar(proc, 22)
        proc.terminate()
        salida = proc.communicate(timeout=15)[0]
        ok = all(
            len(list((descargados / modo / "2026-10-09").glob("*.dck"))) == 2
            for modo in ("Standard", "Pioneer", "Modern")
        )
        check("los 3 formatos se mueven a su subcarpeta de fecha", ok)
        check("sin errores del organizador", "[ERROR]" not in salida)
        check("no quedan archivos sueltos en la raiz de cada modo",
              not any(f.is_file() for m in ("Standard", "Pioneer", "Modern") for f in (descargados / m).iterdir()))
    finally:
        if proc:
            try:
                proc.kill()
            except Exception:
                pass
        shutil.rmtree(raiz, ignore_errors=True)


def test_sin_duplicados():
    print("== T5: no hay procesado duplicado ==")
    raiz, motor, descargados = escenario()
    proc = None
    try:
        proc, log = lanzar(VIGILANTE, raiz, motor, descargados)
        esperar(proc, 3)
        for i in range(3):
            (descargados / "Standard" / f"2026-10-09_Mazo{i}.dck").write_text("x\n", encoding="utf-8")
        with log.open("ab") as f:
            f.write("\nACTUALIZACIÓN TERMINADA: nuevos=3, repetidos=0, rechazados=0\n".encode("utf-8"))
        esperar(proc, 22)
        proc.terminate()
        salida = proc.communicate(timeout=15)[0]
        d1 = list((descargados / "Standard" / "2026-10-09").glob("*.dck"))
        check("cada mazo aparece exactamente una vez", len(d1) == 3, f"{len(d1)} ficheros")
        check("no hay ficheros _1/_2 de desambiguacion", not any("_1" in f.name or "_2" in f.name for f in d1))
        check("una sola ordenacion disparada", salida.count("Lanzando ordenar_decks.py") == 1)
    finally:
        if proc:
            try:
                proc.kill()
            except Exception:
                pass
        shutil.rmtree(raiz, ignore_errors=True)


def test_log_inexistente():
    print("== T6: directorio de logs inexistente no rompe ==")
    raiz, motor, descargados = escenario()
    proc = None
    try:
        texto = VIGILANTE.read_text(encoding="utf-8")
        texto = re.sub(
            r'CARPETA_LOGS = Path\(r"[^"]*"\)',
            lambda m: 'CARPETA_LOGS = Path(r"' + str(raiz / "no" / "existe" / "nada") + '")',
            texto,
        )
        tmp = raiz / "v2.py"
        tmp.write_text(texto, encoding="utf-8")
        proc = subprocess.Popen([sys.executable, "-u", str(tmp)], stdout=subprocess.PIPE,
                                stderr=subprocess.STDOUT, text=True, encoding="utf-8",
                                errors="replace")
        esperar(proc, 6)
        vivo = proc.poll() is None
        proc.terminate()
        salida = proc.communicate(timeout=10)[0]
        check("sigue vivo aunque la ruta no exista", vivo)
        check("anuncia que esta esperando", "Esperando" in salida)
    finally:
        if proc:
            try:
                proc.kill()
            except Exception:
                pass
        shutil.rmtree(raiz, ignore_errors=True)


def test_rotacion_log():
    print("== T7: log rotado o truncado ==")
    raiz, motor, descargados = escenario()
    proc = None
    try:
        proc, log = lanzar(VIGILANTE, raiz, motor, descargados)
        esperar(proc, 3)
        log.write_bytes("nuevo contenido\n".encode("utf-8"))
        esperar(proc, 5)
        proc.terminate()
        salida = proc.communicate(timeout=10)[0]
        check("no revienta con log reescrito", "Traceback" not in salida)
        check("sigue vivo", "Vigilando" in salida or "Esperando" in salida)
    finally:
        if proc:
            try:
                proc.kill()
            except Exception:
                pass
        shutil.rmtree(raiz, ignore_errors=True)


def test_cancelacion_no_ordena():
    print("== T8: una cancelacion del motor no dispara el organizador ==")
    raiz, motor, descargados = escenario()
    proc = None
    try:
        proc, log = lanzar(VIGILANTE, raiz, motor, descargados)
        esperar(proc, 3)
        for modo in ("Standard", "Pioneer", "Modern"):
            (descargados / modo / f"2026-10-09_{modo}.dck").write_text("x\n", encoding="utf-8")
        with log.open("ab") as f:
            f.write("\nACTUALIZACIÓN CANCELADA. Los mazos guardados permanecen intactos.\n".encode("utf-8"))
        esperar(proc, 18)
        proc.terminate()
        salida = proc.communicate(timeout=15)[0]
        check("no dispara el organizador", "Descarga terminada detectada" not in salida)
        check("no mueve ficheros",
              not any((descargados / m / "2026-10-09").exists() for m in ("Standard", "Pioneer", "Modern")))
    finally:
        if proc:
            try:
                proc.kill()
            except Exception:
                pass
        shutil.rmtree(raiz, ignore_errors=True)


def test_marcador_partido():
    print("== T9: marcador partido entre dos escrituras ==")
    raiz, motor, descargados = escenario()
    proc = None
    try:
        proc, log = lanzar(VIGILANTE, raiz, motor, descargados)
        esperar(proc, 3)
        for modo in ("Standard", "Pioneer", "Modern"):
            (descargados / modo / f"2026-10-09_{modo}.dck").write_text("x\n", encoding="utf-8")
        with log.open("ab") as f:
            f.write("\nACTUALIZACI".encode("utf-8"))
        esperar(proc, 6)
        with log.open("ab") as f:
            f.write("ÓN TERMINADA: nuevos=3\n".encode("utf-8"))
        esperar(proc, 20)
        proc.terminate()
        salida = proc.communicate(timeout=15)[0]
        movidos = sum(len(list((descargados / m / "2026-10-09").glob("*.dck")))
                      for m in ("Standard", "Pioneer", "Modern"))
        check("detecta el marcador partido", "Descarga terminada detectada" in salida)
        check("ordena los 3 mazos", movidos == 3, str(movidos))
        check("sin traceback", "Traceback" not in salida)
    finally:
        if proc:
            try:
                proc.kill()
            except Exception:
                pass
        shutil.rmtree(raiz, ignore_errors=True)


def test_sin_procesos_huerfanos():
    print("== T10: no deja procesos huerfanos ==")
    antes = subprocess.run(["tasklist", "/FI", "IMAGENAME eq python.exe"],
                           capture_output=True, text=True).stdout
    raiz, motor, descargados = escenario()
    proc = None
    try:
        proc, log = lanzar(VIGILANTE, raiz, motor, descargados)
        esperar(proc, 3)
        with log.open("ab") as f:
            f.write("\nACTUALIZACIÓN TERMINADA: nuevos=0\n".encode("utf-8"))
        esperar(proc, 20)
        proc.terminate()
        proc.communicate(timeout=15)
        time.sleep(1.5)
        despues = subprocess.run(["tasklist", "/FI", "IMAGENAME eq python.exe"],
                                 capture_output=True, text=True).stdout
        n_antes, n_despues = antes.count("python.exe"), despues.count("python.exe")
        check("ningun python queda vivo tras terminar", n_despues <= n_antes, f"{n_antes} -> {n_despues}")
    finally:
        if proc:
            try:
                proc.kill()
            except Exception:
                pass
        shutil.rmtree(raiz, ignore_errors=True)


if __name__ == "__main__":
    for fn in (test_ruta_de_produccion_existe, test_resolucion_ruta, test_detecta_finalizacion,
               test_no_dispara_con_contenido_viejo, test_tres_formatos, test_sin_duplicados,
               test_log_inexistente, test_rotacion_log, test_cancelacion_no_ordena,
               test_marcador_partido, test_sin_procesos_huerfanos):
        fn()
    print()
    if fallos:
        print(f"FALLOS ({len(fallos)}): " + ", ".join(fallos))
        sys.exit(1)
    print("TODAS LAS PRUEBAS DEL VIGILANTE PASAN")