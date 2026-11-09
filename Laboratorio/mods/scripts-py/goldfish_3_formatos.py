#!/usr/bin/env python3
"""Descarga 25 mazos 60+15 de MTGGoldfish en Forge y XMage por HTTP directo.

Actualizado para ejecucion 100% autonoma sin Selenium, sin abrir navegadores
y sin requerir resolucion manual de Cloudflare ni intervencion humana.
"""
import importlib.util
import os
import re
import shutil
import subprocess
import sys
import time
from urllib.parse import urljoin

DEPENDENCIAS = {
    "requests": "requests",
    "bs4": "beautifulsoup4",
}
faltan = [paquete for modulo, paquete in DEPENDENCIAS.items()
          if importlib.util.find_spec(modulo) is None]
if faltan:
    subprocess.check_call([sys.executable, "-m", "pip", "install", *faltan])

import requests
from bs4 import BeautifulSoup

BASE = "https://www.mtggoldfish.com"
FORMATS = {"Standard": "standard", "Pioneer": "pioneer", "Modern": "modern"}
TOP_N = 25
HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, "MTGGoldfish")
LOG = os.path.join(HERE, "goldfish_3_formatos.log")
LOCK_PATH = os.path.join(HERE, "goldfish_3_formatos.lock")

SESSION = requests.Session()
SESSION.headers.update({
    "User-Agent": (
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
        "(KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36"
    ),
    "Accept-Language": "en-US,en;q=0.9",
})

class Tee:
    def __init__(self, *streams):
        self.streams = streams

    def write(self, text):
        for s in self.streams:
            s.write(text)
            s.flush()

    def flush(self):
        for s in self.streams:
            s.flush()

def get_soup(url, referer=None):
    headers = {"Referer": referer} if referer else {}
    print(f"GET {url}", flush=True)
    r = SESSION.get(url, headers=headers, timeout=30)
    r.raise_for_status()
    return BeautifulSoup(r.text, "html.parser")

def clean_output():
    if os.path.isdir(ROOT):
        shutil.rmtree(ROOT, ignore_errors=True)
    for engine in ("Forge", "XMage"):
        for fmt in FORMATS:
            os.makedirs(os.path.join(ROOT, engine, fmt), exist_ok=True)
    print("LIMPIANDO", ROOT, flush=True)

def tournament_urls(fmt):
    soup = get_soup(f"{BASE}/tournaments/{fmt}#paper")
    out = []
    seen = set()
    for a in soup.select('a[href^="/tournament/"]'):
        u = urljoin(BASE, a.get("href", "").split("#", 1)[0])
        if u not in seen:
            seen.add(u)
            out.append(u)
    return out

def deck_links(event_url):
    soup = get_soup(event_url)
    out = []
    seen = set()
    for a in soup.select('a[href*="/deck/"]'):
        m = re.search(r"/deck/(\d+)", a.get("href", ""))
        if not m or m.group(1) in seen:
            continue
        did = m.group(1)
        seen.add(did)
        name = a.get_text(" ", strip=True) or "MTGGoldfish Deck"
        out.append((did, urljoin(BASE, a.get("href").split("#", 1)[0]), name))
    return out

def fetch_deck_data(did, event_url):
    headers = {
        "Referer": event_url,
        "X-Requested-With": "XMLHttpRequest",
    }
    w_url = f"{BASE}/widgets/deck/js?deckId={did}"
    r = SESSION.get(w_url, headers=headers, timeout=30)
    if r.status_code != 200:
        raise ValueError(f"HTTP {r.status_code} en widget para mazo {did}")

    start_idx = r.text.find('elem.innerHTML = "') + len('elem.innerHTML = "')
    end_idx = r.text.rfind('";')
    if start_idx == -1 or end_idx == -1 or end_idx <= start_idx:
        raise ValueError(f"No se pudo extraer innerHTML para mazo {did}")

    raw_html = r.text[start_idx:end_idx]
    html = raw_html.replace(r"\'", "'").replace(r'\"', '"').replace(r'\/', '/').replace(r'\n', '\n')
    soup = BeautifulSoup(html, "html.parser")

    field = soup.select_one('input[name="deck_input[deck]"]')
    if not field:
        raise ValueError(f"El widget no contiene deck_input[deck] para mazo {did}")
    raw = field.get("value", "")

    name_field = soup.select_one('input[name="deck_input[name]"]')
    name = name_field.get("value") if name_field else None
    if not name:
        h = soup.select_one("h1.title") or soup.select_one("h1")
        name = h.get_text(" ", strip=True) if h else f"MTGGoldfish_{did}"
    name = re.sub(r"\s+by\s+.+$", "", name).strip()

    main = []
    side = []
    is_side = False
    for line in raw.splitlines():
        line = line.strip()
        if not line:
            continue
        if line.lower() == "sideboard" or line.lower().startswith("sideboard"):
            is_side = True
            continue
        m = re.match(r"^(\d+)\s+(.+)$", line)
        if not m:
            continue
        card = re.sub(r"\s*\[[^]]+\].*$", "", m.group(2)).strip()
        card = re.sub(r"\s*<[^>]+>\s*$", "", card).strip()
        (side if is_side else main).append((int(m.group(1)), card))

    return name, main, side

def safe_stem(name, did, index):
    stem = re.sub(r"[^A-Za-z0-9 _-]", "", name).strip().replace(" ", "_") or "MTGGoldfish"
    return f"{index:02d}_{stem}_{did}"

def write_deck(fmt, stem, name, main, side):
    fpath = os.path.join(ROOT, "Forge", fmt, stem + ".dck")
    xpath = os.path.join(ROOT, "XMage", fmt, stem + ".txt")
    with open(fpath, "w", encoding="utf-8") as f:
        f.write(f"[metadata]\nName={name}\n[Main]\n")
        for q, c in main:
            f.write(f"{q} {c}\n")
        f.write("[Sideboard]\n")
        for q, c in side:
            f.write(f"{q} {c}\n")
    with open(xpath, "w", encoding="utf-8") as f:
        for q, c in main:
            f.write(f"{q} {c}\n")
        f.write("\n")
        for q, c in side:
            f.write(f"{q} {c}\n")

def run():
    clean_output()
    total = 0
    print("MTGGoldfish: descargando por HTTP directo (sin Selenium, 100% autonomo)...", flush=True)
    for fmt, slug in FORMATS.items():
        ok = 0
        processed = set()
        try:
            events = tournament_urls(slug)
        except Exception as e:
            print(f"[{fmt}] error listando torneos: {e}", flush=True)
            continue
        for event in events:
            if ok >= TOP_N:
                break
            try:
                links = deck_links(event)
            except Exception as e:
                print(f"[{fmt}] evento omitido {event}: {e}", flush=True)
                continue
            for did, url, link_name in links:
                if ok >= TOP_N:
                    break
                if did in processed:
                    continue
                processed.add(did)
                try:
                    name, main, side = fetch_deck_data(did, event)
                    mq = sum(q for q, _ in main)
                    sq = sum(q for q, _ in side)
                    if mq != 60 or sq != 15:
                        print(f"[{fmt}] omitido {did}: no es 60+15 ({mq}+{sq})", flush=True)
                        continue
                    write_deck(fmt, safe_stem(name, did, ok + 1), name, main, side)
                    ok += 1
                    total += 1
                    print(f"[{fmt}] {did}: {mq}+{sq} ({name}) -> OK {ok}/{TOP_N}", flush=True)
                except Exception as e:
                    print(f"[{fmt}] omitido {did}: {e}", flush=True)
                time.sleep(0.2)
        print(f"{fmt}: {ok} exportados", flush=True)
    print("TOTAL", total, flush=True)

if __name__ == "__main__":
    old = sys.stdout
    with open(LOG, "w", encoding="utf-8") as log:
        sys.stdout = Tee(old, log)
        try:
            run()
        except Exception as e:
            print("ERROR GLOBAL", repr(e), flush=True)
        finally:
            print("LOG", LOG, flush=True)
            sys.stdout = old
