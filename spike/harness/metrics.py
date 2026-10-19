"""Gemeinsame Hilfsfunktionen: Normalisierung, Tokenisierung, Recall@k."""
import re
import unicodedata

# "marz": normalize() entfernt den Umlaut, aus "März" wird "marz".
MONTH_TO_NUM = {"januar": 1, "februar": 2, "maerz": 3, "marz": 3, "märz": 3, "april": 4,
                "mai": 5, "juni": 6, "juli": 7, "august": 8, "september": 9,
                "oktober": 10, "november": 11, "dezember": 12}


def normalize(text: str) -> str:
    text = text.lower()
    text = unicodedata.normalize("NFKD", text)
    text = "".join(c for c in text if not unicodedata.combining(c))
    return text


def tokenize(text: str) -> list:
    return re.findall(r"[a-z0-9äöüß]+", normalize(text))


def recall_at_k(results: dict, k: int = 3) -> float:
    """results: {query_id: (target_id, [hit_ids...])} -> Anteil Treffer in Top-k."""
    if not results:
        return 0.0
    hits = sum(1 for target, ranked in results.values() if target in ranked[:k])
    return hits / len(results)
