"""Baseline-Monatserkennung: normalize() entfernt Umlaute, aus "März" wird "marz"."""
import unittest

from baseline_search import search


class BaselineMonthTest(unittest.TestCase):
    def test_umlaut_month_ranks_matching_receipt_first(self):
        # Sonst gleiche Belege; bei Gleichstand entscheidet die ID (R1 vor R2).
        receipts = [
            {"id": "R1", "merchant": "Alpha", "date": "2025-06-10", "text": "xxxxxxxx yyyy"},
            {"id": "R2", "merchant": "Beta", "date": "2025-03-10", "text": "xxxxxxxx yyyy"},
        ]
        self.assertEqual(search("Einkauf im März", receipts)[0], "R2")
        self.assertEqual(search("Einkauf im Maerz", receipts)[0], "R2")


if __name__ == "__main__":
    unittest.main()
