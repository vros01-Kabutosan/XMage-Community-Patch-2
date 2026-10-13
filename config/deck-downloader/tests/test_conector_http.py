#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Pruebas del conector MTGTop8 por HTTP directo (sin Selenium).

No toca la biblioteca real: solo valida funciones puras del conector.

    py -3 test_conector_http.py [ruta/al/conector.py]
"""

import importlib.util
import sys
import unittest
from pathlib import Path


def cargar(ruta):
    spec = importlib.util.spec_from_file_location("conector", ruta)
    modulo = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(modulo)
    return modulo


class ConectorHTTP(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        por_defecto = Path(__file__).resolve().parent.parent / "mtgtop8_3_formatos.py"
        cls.conn = cargar(Path(sys.argv[1]) if len(sys.argv) > 1 else por_defecto)

    def test_no_depende_de_selenium(self):
        fuente = Path(self.conn.__file__).read_text(encoding="utf-8")
        for prohibido in ("import selenium", "undetected_chromedriver", "uc.Chrome"):
            self.assertNotIn(prohibido, fuente)

    def test_parse_export_60_15(self):
        main, side = self.conn.parse_export(
            "4 Lightning Bolt\n56 Mountain\nSideboard\n15 Island\n"
        )
        self.assertEqual(sum(q for q, _ in main), 60)
        self.assertEqual(sum(q for q, _ in side), 15)

    def test_parse_export_acepta_marcadores(self):
        for cabecera in ("Sideboard", "SIDEBOARD:", "SB"):
            texto = "60 Mountain\n%s\n15 Island\n" % cabecera
            main, side = self.conn.parse_export(texto)
            self.assertEqual(
                (sum(q for q, _ in main), sum(q for q, _ in side)), (60, 15)
            )

    def test_parse_export_ignora_comentarios_y_vacios(self):
        texto = (
            "// Deck file created with mtgtop8.com\n\n"
            "60 Mountain\nSideboard\n// x\n15 Island\n"
        )
        main, side = self.conn.parse_export(texto)
        self.assertEqual(
            (sum(q for q, _ in main), sum(q for q, _ in side)), (60, 15)
        )

    def test_parse_export_une_bancos(self):
        main, side = self.conn.parse_export(
            "1 Bolt\n1 Bolt\n58 Mountain\nSideboard\nSB: 15 Island\n"
        )
        self.assertEqual(sum(q for q, _ in main), 60)
        self.assertEqual(sum(q for q, _ in side), 15)

    def test_eventos_desde_select_y_enlaces(self):
        html = (
            '<select><option value="event?e=12&f=MO">Modern</option>'
            '<option value="event?e=13&f=MO">Otro</option></select>'
            '<a href="/event?e=12&f=MO">ver</a>'
            '<a href="/event?e=12&d=5&f=MO">mazo</a>'
        )
        eventos = self.conn.tournament_urls_from_html(html)
        self.assertIn(self.conn.BASE + "/event?e=12&f=MO", eventos)
        self.assertIn(self.conn.BASE + "/event?e=13&f=MO", eventos)
        self.assertTrue(all("d=" not in u for u in eventos))

    def test_html_vacio_no_revienta(self):
        self.assertEqual(self.conn.tournament_urls_from_html(""), [])
        self.assertEqual(self.conn.deck_links_from_html(""), [])

    def test_export_url_por_defecto(self):
        self.assertEqual(
            self.conn.export_url_from_html("", "999"),
            self.conn.BASE + "/mtgo?d=999",
        )

    def test_safe_stem_conserva_identificador(self):
        self.assertEqual(
            self.conn.safe_stem("Izzet Prowess", "1234", 7),
            "07_Izzet_Prowess_1234",
        )

    def test_deck_fingerprint_detecta_duplicados(self):
        filas = [
            (4, "Bolt", {"oracle_id": "a", "name": "Bolt"}),
            (56, "Mountain", {"oracle_id": "b", "name": "Mountain"}),
        ]
        self.assertEqual(
            self.conn.deck_fingerprint(filas, []),
            self.conn.deck_fingerprint(list(reversed(filas)), []),
        )


if __name__ == "__main__":
    unittest.main(argv=[sys.argv[0]], verbosity=2)