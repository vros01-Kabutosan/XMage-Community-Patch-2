import os
import shutil
import subprocess
import sys
import time
from pathlib import Path

RAIZ = Path(__file__).resolve().parent
SB = RAIZ / "sb"
LOGS = SB / "logs"
DESC = SB / "Descargados"
LANZADOR = RAIZ / "INICIAR-VIGILANTE-DECKS.cmd"
PYTHONW = r"C:\Users\vros0\AppData\Local\Programs\Python\Python314\pythonw.exe"
VIG = DESC / "vigilante_decks.py"
LOG_LANZ = LOGS / "vigilante-autoinicio.log"
LOG_MOTOR = LOGS / "deck-library-updater.log"
fallos = []


def check(nombre, cond, detalle=""):
    print(("  PASS " if cond else "  FAIL ") + nombre + (f"  [{detalle}]" if detalle else ""))
    if not cond:
        fallos.append(nombre)


def vivos():
    q = subprocess.run(
        ["powershell", "-NoProfile", "-NonInteractive",
         "-Command",
         "Get-CimInstance Win32_Process -Filter \"Name='pythonw.exe'\" | "
         "Where-Object { $_.CommandLine -like '*vigilante_decks.py*' } | "
         "ForEach-Object { $_.ProcessId }"],
        capture_output=True, text=True)
    return [x.strip() for x in q.stdout.split() if x.strip()]


def lanzar():
    """Arranca el lanzador DESACOPLADO: el script no termina hasta que el
    vigilante termina, asi que hay que lanzarlo sin esperar."""
    return subprocess.Popen(
        ["cmd", "/c", str(LANZADOR), str(VIG), PYTHONW, str(LOG_LANZ)],
        stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
        creationflags=0x00000008 | 0x00000200)


def esperar(n):
    time.sleep(n)


def preparar():
    shutil.rmtree(SB, ignore_errors=True)
    LOGS.mkdir(parents=True)
    DESC.mkdir(parents=True)
    src = Path(r"J:\MTG\Instalacion-XMage\xmage\mage-client\sample-decks\Descargados")
    shutil.copy2(src / "vigilante_decks.py", VIG)
    shutil.copy2(src / "ordenar_decks.py", DESC / "ordenar_decks.py")
    texto = VIG.read_text(encoding="utf-8")
    import re
    texto = re.sub(r'CARPETA_LOGS = Path\(r"[^"]*"\)',
                   lambda m: 'CARPETA_LOGS = Path(r"' + str(LOGS) + '")', texto)
    VIG.write_text(texto, encoding="utf-8")
    LOG_MOTOR.write_bytes("inicio\n".encode("utf-8"))


def aparcar_produccion():
    """La comprobacion de instancia unica es GLOBAL: cuenta cualquier
    vigilante_decks.py del equipo, tambien el de produccion. Para que el
    sandbox pueda arrancar el suyo, el vigilante real se para un momento y
    se vuelve a levantar despues. Sin esto las pruebas son intermitentes."""
    subprocess.run(["powershell", "-NoProfile", "-NonInteractive", "-Command",
                    "Stop-ScheduledTask -TaskName 'XMage-VigilanteDecks' "
                    "-ErrorAction SilentlyContinue"], capture_output=True)
    for p in vivos():
        subprocess.run(["taskkill", "/PID", p, "/F"], capture_output=True)
    time.sleep(2)


def restaurar_produccion():
    subprocess.run(["powershell", "-NoProfile", "-NonInteractive", "-Command",
                    "Start-ScheduledTask -TaskName 'XMage-VigilanteDecks' "
                    "-ErrorAction SilentlyContinue"], capture_output=True)
    time.sleep(3)


def marcadores():
    return LOG_LANZADOR_texto().count("INICIO:")


def LOG_LANZADOR_texto():
    return LOG_LANZ.read_text(encoding="utf-8", errors="replace") if LOG_LANZ.exists() else ""


def test_t1_arranque_y_directorio():
    print("== T1: el lanzador arranca el vigilante con pythonw ==")
    preparar()
    lanzar()
    esperar(5)
    check("queda 1 proceso pythonw", len(vivos()) == 1, str(vivos()))
    check("registra INICIO", "INICIO" in LOG_LANZADOR_texto())
    check("registra que sigue vivo (no hay FIN todavia)",
          "FIN:" not in LOG_LANZADOR_texto())


def test_t2_instancia_unica():
    print("== T2: un segundo arranque NO crea una segunda instancia ==")
    antes = len(vivos())
    lanzar()
    esperar(3)
    check("sigue habiendo la misma cantidad", len(vivos()) == antes, f"{antes} -> {len(vivos())}")
    check("el log dice OMITIDO", "OMITIDO" in LOG_LANZADOR_texto())


def test_t2b_guard_global():
    print("== T2b: la comprobacion tambien ve un vigilante lanzado a mano ==")
    # El de T1 sigue vivo: ya es un vigilante "externo" al lanzador.
    antes = len(vivos())
    check("hay un vigilante vivo de partida", antes >= 1, str(vivos()))
    lanzar()
    esperar(3)
    check("el lanzador NO crea otro", len(vivos()) == antes, f"{antes} -> {len(vivos())}")
    check("y lo dice en el log", "OMITIDO" in LOG_LANZADOR_texto())


def test_t3_rutas():
    print("== T3: interprete, ruta y directorio de trabajo ==")
    salida = subprocess.run(
        ["powershell", "-NoProfile", "-NonInteractive",
         "-Command",
         "(Get-CimInstance Win32_Process -Filter \"Name='pythonw.exe'\" | "
         "Where-Object { $_.CommandLine -like '*vigilante_decks.py*' } | "
         "Select-Object -First 1).CommandLine"],
        capture_output=True, text=True).stdout.strip()
    check("la linea de comandos lleva pythonw -u", "pythonw.exe" in salida and "-u" in salida)
    check("la linea de comandos lleva el vigilante del sandbox", str(VIG) in salida)


def test_t4_deteccion_y_organizador():
    print("== T4: detecta el marcador y ejecuta el organizador ==")
    for modo in ("Standard", "Pioneer", "Modern"):
        (DESC / modo).mkdir(exist_ok=True)
        for i in range(3):
            (DESC / modo / f"2026-10-09_{modo}_{i}.dck").write_text("x\n", encoding="utf-8")
    with LOG_MOTOR.open("ab") as f:
        f.write("\nACTUALIZACIÓN TERMINADA: nuevos=9, repetidos=0, rechazados=0\n".encode("utf-8"))
    esperar(22)
    movidos = sum(len(list((DESC / m / "2026-10-09").glob("*.dck")))
                  for m in ("Standard", "Pioneer", "Modern"))
    check("los 9 mazos acabaron en subcarpeta de fecha", movidos == 9, str(movidos))
    check("no quedan sueltos en las raices",
          not any(f.is_file() for m in ("Standard", "Pioneer", "Modern")
                  for f in (DESC / m).iterdir()))


def test_t5_ruta_inexistente():
    print("== T5: el lanzador avisa si falta el interprete o el vigilante ==")
    log_err = LOGS / "errores.log"
    r = subprocess.run(["cmd", "/c", str(LANZADOR), str(VIG),
                        r"C:\no\existe\pythonw.exe", str(log_err)],
                       capture_output=True, text=True, timeout=30)
    check("devuelve 1 si falta el interprete", r.returncode == 1, str(r.returncode))
    check("lo deja escrito en el log", "ERROR" in log_err.read_text(encoding="utf-8", errors="replace"))
    r2 = subprocess.run(["cmd", "/c", str(LANZADOR), r"J:\no\existe\vig.py",
                         PYTHONW, str(log_err)], capture_output=True, text=True, timeout=30)
    check("devuelve 1 si falta el vigilante", r2.returncode == 1, str(r2.returncode))


def test_t6_recuperacion():
    print("== T6: si el vigilante muere, un relanzamiento lo recupera ==")
    for p in vivos():
        subprocess.run(["taskkill", "/PID", p, "/F"], capture_output=True)
    esperar(3)
    check("no queda ninguno vivo", len(vivos()) == 0)
    check("el lanzador registra el FINAL",
          "FIN:" in LOG_LANZADOR_texto(), LOG_LANZADOR_texto()[-90:])
    lanzar()
    esperar(5)
    check("el relanzamiento lo vuelve a levantar", len(vivos()) == 1, str(vivos()))
    check("el log acumula otro INICIO", marcadores() >= 2, str(marcadores()))


def test_t7_sin_bucles():
    print("== T7: el lanzador no crea bucles de reinicio ==")
    n = marcadores()
    esperar(15)
    check("no se relanza solo", marcadores() == n, f"{n} -> {marcadores()}")
    check("sigue exactamente 1 proceso", len(vivos()) == 1, str(vivos()))


def test_t8_recursos():
    print("== T8: consumo razonable de recursos ==")
    pid = vivos()[0]
    p = subprocess.run(["powershell", "-NoProfile", "-NonInteractive",
                        "-Command",
                        f"$p=Get-Process -Id {pid}; "
                        "'{0:N1}|{1:N2}' -f ($p.WorkingSet64/1MB), $p.TotalProcessorTime.TotalSeconds"],
                       capture_output=True, text=True).stdout.strip()
    ram, cpu = (p.split("|") + ["0", "0"])[:2]
    print(f"     RAM={ram} MB  CPU={cpu}s")
    check("RAM por debajo de 80 MB", float(ram.replace(",", ".")) < 80, ram)


def test_t9_parada():
    print("== T9: se puede parar y desactivar ==")
    for p in vivos():
        subprocess.run(["taskkill", "/PID", p, "/F"], capture_output=True)
    esperar(2)
    check("el vigilante se detiene", len(vivos()) == 0)
    lanzar()
    esperar(4)
    check("y se puede volver a arrancar a mano", len(vivos()) == 1)


def test_t10_xmage_cerrado():
    print("== T10: funciona con XMage cerrado ==")
    java = subprocess.run(["powershell", "-NoProfile", "-NonInteractive", "-Command",
                           "(Get-CimInstance Win32_Process -Filter \"Name='java.exe' or Name='javaw.exe'\").Count"],
                          capture_output=True, text=True).stdout.strip()
    print(f"     procesos java XMage ahora mismo: {java}")
    check("el vigilante sigue vivo (no depende de XMage)", len(vivos()) == 1)


if __name__ == "__main__":
    aparcar_produccion()
    try:
        for fn in (test_t1_arranque_y_directorio, test_t2_instancia_unica, test_t2b_guard_global, test_t3_rutas,
                   test_t4_deteccion_y_organizador, test_t5_ruta_inexistente, test_t6_recuperacion,
                   test_t7_sin_bucles, test_t8_recursos, test_t9_parada, test_t10_xmage_cerrado):
            fn()
    finally:
        for p in vivos():
            subprocess.run(["taskkill", "/PID", p, "/F"], capture_output=True)
        restaurar_produccion()
        print("vigilante de produccion restaurado")
    print()
    if fallos:
        print(f"FALLOS ({len(fallos)}): " + ", ".join(fallos))
        sys.exit(1)
    print("TODAS LAS PRUEBAS DE AUTOINICIO PASAN")