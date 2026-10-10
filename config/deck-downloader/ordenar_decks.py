#!/usr/bin/env python3
# -*- coding: utf-8 -*-
r"""
Organiza decks de MTG por fecha dentro de cada carpeta de modo
(Standard, Modern, Pioneer).

Guárdalo en: J:\MTG\xmage\client\sample-decks\Descargados
y haz doble clic, o ejecútalo desde ahí.

Si no le pasas ninguna carpeta como argumento, usa la carpeta
donde está este propio .py (es decir, Descargados).
"""

import argparse
import re
import shutil
import sys
from datetime import date, datetime
from pathlib import Path

MODOS_POR_DEFECTO = ["Standard", "Modern", "Pioneer"]
DATE_RE = re.compile(r"(?<!\d)(\d{4})[-_./](\d{1,2})[-_./](\d{1,2})(?!\d)")
ISO_FOLDER_RE = re.compile(r"^\d{4}-\d{2}-\d{2}$")
JUNK = {"desktop.ini", "thumbs.db", ".ds_store"}


def parse_date(texto, ydm=False):
    """
    Busca una fecha AAAA-MM-DD en el nombre.
    Si el "mes" es imposible (p.ej. 2026-19-08), prueba día/mes invertidos.
    Con --ydm se prioriza AAAA-DD-MM (por si tu mod lo escribe invertido
    y el día es <= 12, que si no sería ambiguo).
    """
    for anio, a, b in DATE_RE.findall(texto):
        anio, a, b = int(anio), int(a), int(b)
        pares = [(b, a), (a, b)] if ydm else [(a, b), (b, a)]
        for mes, dia in pares:
            try:
                return date(anio, mes, dia)
            except ValueError:
                continue
    return None


def nombre_unico(dest_dir, nombre, planificados):
    """Evita pisar archivos si ya existe otro con el mismo nombre."""
    dest = dest_dir / nombre
    if not dest.exists() and dest not in planificados:
        return dest
    stem = Path(nombre).stem
    suf = Path(nombre).suffix
    i = 1
    while True:
        cand = dest_dir / f"{stem}_{i}{suf}"
        if not cand.exists() and cand not in planificados:
            return cand
        i += 1


def main():
    p = argparse.ArgumentParser(
        description="Ordena decks por fecha dentro de cada carpeta de modo."
    )
    p.add_argument("carpeta", type=Path, nargs="?", default=None,
                   help="Carpeta a ordenar (por defecto: la carpeta donde está este .py)")
    p.add_argument("--modos", nargs="*", default=MODOS_POR_DEFECTO,
                   help=f"Carpetas de modo a procesar (por defecto: {MODOS_POR_DEFECTO})")
    p.add_argument("--todas-las-subcarpetas", action="store_true",
                   help="Procesar TODAS las subcarpetas de la raíz (si añades más modos)")
    p.add_argument("--dry-run", action="store_true",
                   help="Solo muestra lo que haría, sin mover nada")
    p.add_argument("--copiar", action="store_true",
                   help="Copiar en vez de mover (para probar sin riesgo)")
    p.add_argument("--extensiones", nargs="*", default=None,
                   help="Filtrar extensiones, p.ej. .dck (por defecto: todos los archivos)")
    p.add_argument("--ydm", action="store_true",
                   help="Interpretar fechas ambiguas como AAAA-DD-MM")
    p.add_argument("--usar-fecha-modificacion", action="store_true",
                   help="Si un nombre no tiene fecha, usar la fecha de modificación")
    p.add_argument("--no-pausa", action="store_true",
                   help="No esperar a pulsar Enter al terminar (para automatizar)")
    args = p.parse_args()

    script_path = None
    try:
        script_path = Path(__file__).resolve()
    except NameError:
        pass

    # Si no se pasa carpeta, usar la carpeta donde está el script.
    if args.carpeta:
        root = args.carpeta.expanduser().resolve()
    elif script_path:
        root = script_path.parent
    else:
        root = Path.cwd()

    if not root.is_dir():
        sys.exit(f"La carpeta no existe: {root}")

    ext = None
    if args.extensiones:
        ext = {e.lower() if e.startswith(".") else "." + e.lower()
               for e in args.extensiones}

    # --- Elegir carpetas de modo -------------------------------------------
    subdirs = [d for d in sorted(root.iterdir()) if d.is_dir()]

    if args.todas_las_subcarpetas:
        modos = [d for d in subdirs if not ISO_FOLDER_RE.match(d.name)]
    else:
        modos = []
        for nombre in args.modos:
            hit = next((d for d in subdirs if d.name.lower() == nombre.lower()), None)
            if hit:
                modos.append(hit)
            else:
                print(f"Aviso: no existe la carpeta '{nombre}' dentro de {root}")

    # Si la carpeta indicada no tiene modos pero sí archivos sueltos
    # (p.ej. pasas directamente la carpeta Modern), se procesa tal cual.
    if not modos:
        if any(f.is_file() for f in root.iterdir()):
            modos = [root]
        else:
            sys.exit("No encontré carpetas de modo ni archivos que ordenar.")

    # --- Procesar -----------------------------------------------------------
    accion = "copiar" if args.copiar else "mover"
    planificados = set()
    tot_ok = tot_err = tot_sin = 0

    for modo in modos:
        print(f"\n=== {modo.name} ===")
        grupos = {}
        sin_fecha = []

        for f in sorted(modo.iterdir()):
            if not f.is_file():
                continue
            if script_path and f.resolve() == script_path:
                continue  # nunca tocar el propio script
            if f.name.lower() in JUNK:
                continue
            if ext and f.suffix.lower() not in ext:
                continue

            d = parse_date(f.name, args.ydm)
            if d is None and args.usar_fecha_modificacion:
                try:
                    d = datetime.fromtimestamp(f.stat().st_mtime).date()
                except OSError:
                    d = None

            if d is None:
                sin_fecha.append(f)
            else:
                grupos.setdefault(d, []).append(f)

        if not grupos:
            print("  No hay archivos con fecha reconocible.")

        # Fechas de MÁS NUEVO a MÁS ANTIGUO
        for d in sorted(grupos, reverse=True):
            dest_dir = modo / d.isoformat()

            if dest_dir.exists() and not dest_dir.is_dir():
                print(f"  [ERROR] '{dest_dir.name}' existe y no es una carpeta.")
                tot_err += 1
                continue

            if not args.dry_run:
                dest_dir.mkdir(parents=True, exist_ok=True)

            for f in sorted(grupos[d]):
                dest = nombre_unico(dest_dir, f.name, planificados)
                planificados.add(dest)

                if args.dry_run:
                    print(f"  [DRY] {accion}: {f.name} -> {modo.name}\\{d.isoformat()}\\")
                    tot_ok += 1
                    continue

                try:
                    if args.copiar:
                        shutil.copy2(f, dest)
                    else:
                        shutil.move(str(f), str(dest))
                    print(f"  [OK] {f.name} -> {d.isoformat()}\\")
                    tot_ok += 1
                except Exception as e:
                    print(f"  [ERROR] {f.name}: {e}", file=sys.stderr)
                    tot_err += 1

        if sin_fecha:
            tot_sin += len(sin_fecha)
            print("  Se quedan sin mover (sin fecha en el nombre):")
            for f in sin_fecha:
                print(f"   - {f.name}")

    print(f"\nResumen: {tot_ok} procesados, {tot_err} errores, {tot_sin} sin fecha.")
    if args.dry_run:
        print("Esto fue un simulacro. Quita --dry-run para ejecutarlo de verdad.")


if __name__ == "__main__":
    try:
        main()
    finally:
        # Si lo lanzas con doble clic, la ventana no se cierra sola.
        if "--no-pausa" not in sys.argv:
            try:
                input("\nPulsa Enter para cerrar...")
            except (EOFError, OSError):
                pass