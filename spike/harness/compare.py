"""Spike 0.3: alle Vergleichsarten in einem Lauf, mit der 15-Prozentpunkte-Schwelle.

  python compare.py                                  # Hash-Platzhalter: nur Plumbing, KEIN Go/No-Go
  python compare.py --model st:google/embeddinggemma-2-270m
  python compare.py --corpus ../corpus_real          # eigene Belege statt synthetischer Daten

Drei Verfahren, jeweils Recall@3:
  1. Volltext + Felder (Baseline, baseline_search.py)
  2. Embedding allein: alle Belege per Kosinus ranken
  3. Feldfilter + Embedding: genau die Logik der App (SearchEngine.kt):
     Haendlerwort filtern, findet das nichts -> alle Belege; Monat filtern,
     findet das nichts -> Monat ignorieren; Rest per Kosinus ranken.
     Aendert sich SearchEngine.kt, muss app_candidates() hier nachgezogen werden.

Welcher der beiden Embedding-Werte fuer die 15-pp-Schwelle zaehlt, entscheidet das Produkt;
das Skript zeigt beide.

Eigener Korpus (Ordner mit zwei Dateien, Schema wie spike/corpus):
  receipts.json  [{"id", "merchant", "date": "YYYY-MM-DD", "text": "roher OCR-Text"}, ...]
  queries.json   [{"id", "text", "target": "<Beleg-id>"}, ...]
Als "text" gehoert der rohe Text nach der Texterkennung, nicht bereinigte Stichwoerter.
"""
import argparse
import datetime
import json
import math
import re
from pathlib import Path

import baseline_search
import embedding_search
from metrics import MONTH_TO_NUM, normalize, recall_at_k

HERE = Path(__file__).resolve().parent
THRESHOLD_PP = 15.0
QUERY_PREFIX = "task: search result | query: "  # Pflicht laut Modellkarte (siehe Embed.kt)
STOPWORDS = {"der", "die", "das", "vom", "von", "im", "bei", "den"}
MONTHS = {name: f"{num:02d}" for name, num in MONTH_TO_NUM.items()}


def unit(vec):
    n = math.sqrt(sum(v * v for v in vec)) or 1.0
    return [v / n for v in vec]


def load_embedder(spec, dim):
    """-> (doc_vecs(list[str]), query_vec(str), Beschriftung). Vektoren sind L2-normiert."""
    if spec == "hash":
        return ((lambda texts: [embedding_search.embed(t, dim) for t in texts]),
                (lambda t: embedding_search.embed(t, dim)),
                "Hash-Platzhalter (NICHT EmbeddingGemma)")
    if spec.startswith("st:"):
        try:
            from sentence_transformers import SentenceTransformer
        except ImportError:
            raise SystemExit("Fehlt: pip install sentence-transformers torch "
                             "(und Hugging-Face-Zugang, siehe EMBEDDING_SPIKE_MODELL.md)")
        model = SentenceTransformer(spec[3:])

        def enc(texts):
            # Erst auf dim kuerzen (Matryoshka), dann neu normieren - wie die App (EMBED_DIM).
            return [unit(list(map(float, v))[:dim]) for v in model.encode(texts, normalize_embeddings=True)]

        return enc, (lambda t: enc([t])[0]), f"{spec[3:]} (dim {dim})"
    raise SystemExit(f"Unbekanntes Modell '{spec}'. Erlaubt: hash | st:<huggingface-name>")


def app_candidates(query, receipts):
    """Spiegel der Kandidatenwahl in SearchEngine.kt."""
    tokens = normalize(query).split(" ")
    month = next((MONTHS[t] for t in tokens if t in MONTHS), None)
    word = next((t for t in tokens if len(t) > 2 and t not in MONTHS and t not in STOPWORDS), None)
    cands = [r for r in receipts if word and word in r["merchant"].lower()] or receipts
    if month:
        cands = [r for r in cands if r["date"][5:7] == month] or cands
    return cands


def check_corpus(receipts, queries):
    """-> (Fehler, Warnungen). Faengt Formatfehler ab, bevor eine echte Messung daran scheitert."""
    errors, warnings = [], []
    ids = set()
    for i, r in enumerate(receipts):
        where = f"receipts.json[{i}]"
        if not isinstance(r, dict):
            errors.append(f"{where}: kein Objekt")
            continue
        for key in ("id", "merchant", "date", "text"):
            if not isinstance(r.get(key), str) or not r[key].strip():
                errors.append(f"{where}: Feld '{key}' fehlt oder ist leer")
        rid = r.get("id")
        if rid in ids:
            errors.append(f"{where}: doppelte id '{rid}'")
        ids.add(rid)
        date = r.get("date")
        if isinstance(date, str) and date.strip():
            try:
                if not re.fullmatch(r"\d{4}-\d{2}-\d{2}", date):
                    raise ValueError
                datetime.date.fromisoformat(date)
            except ValueError:
                errors.append(f"{where}: date '{date}' ist kein Kalendertag im Format YYYY-MM-DD")
        text = r.get("text")
        if isinstance(text, str) and text.strip() and len(text) < 25:
            warnings.append(f"{where}: text hat nur {len(text)} Zeichen - roher OCR-Text erwartet, keine Stichwoerter")
    qids = set()
    for i, q in enumerate(queries):
        where = f"queries.json[{i}]"
        if not isinstance(q, dict):
            errors.append(f"{where}: kein Objekt")
            continue
        for key in ("id", "text", "target"):
            if not isinstance(q.get(key), str) or not q[key].strip():
                errors.append(f"{where}: Feld '{key}' fehlt oder ist leer")
        if q.get("id") in qids:
            errors.append(f"{where}: doppelte id '{q.get('id')}'")
        qids.add(q.get("id"))
        if isinstance(q.get("target"), str) and q["target"] not in ids:
            errors.append(f"{where}: target '{q['target']}' ist kein Beleg in receipts.json")
    return errors, warnings


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--model", default="hash", help="hash (Standard) oder st:<name>")
    ap.add_argument("--corpus", default=str(HERE.parent / "corpus"), help="Ordner mit receipts.json und queries.json")
    ap.add_argument("--dim", type=int, default=embedding_search.DIM, help="Vektorlaenge (App: 256)")
    args = ap.parse_args()

    corpus = Path(args.corpus)
    receipts = json.loads((corpus / "receipts.json").read_text(encoding="utf-8"))
    queries = json.loads((corpus / "queries.json").read_text(encoding="utf-8"))
    errors, warnings = check_corpus(receipts, queries)
    if errors:
        shown = "\n  ".join(errors[:10]) + (f"\n  ... und {len(errors) - 10} weitere" if len(errors) > 10 else "")
        raise SystemExit(f"Korpus {corpus} ist fehlerhaft ({len(errors)} Fehler):\n  {shown}")
    for w in warnings[:5]:
        print("Warnung:", w)
    doc_vecs, query_vec, label = load_embedder(args.model, args.dim)

    vecs = dict(zip((r["id"] for r in receipts), doc_vecs([embedding_search.doc_text(r) for r in receipts])))
    cos = lambda a, b: sum(x * y for x, y in zip(a, b))

    def ranked(query, cands):
        qv = query_vec(QUERY_PREFIX + query)
        return [r["id"] for r in sorted(cands, key=lambda r: -cos(qv, vecs[r["id"]]))]

    results = {
        "Volltext + Felder (Baseline)": {q["id"]: (q["target"], baseline_search.search(q["text"], receipts)) for q in queries},
        "Embedding allein": {q["id"]: (q["target"], ranked(q["text"], receipts)) for q in queries},
        "Feldfilter + Embedding (App)": {q["id"]: (q["target"], ranked(q["text"], app_candidates(q["text"], receipts))) for q in queries},
    }
    scores = {name: recall_at_k(res, 3) * 100 for name, res in results.items()}
    base = scores["Volltext + Felder (Baseline)"]

    print(f"Korpus: {corpus}  ({len(receipts)} Belege, {len(queries)} Anfragen)")
    print(f"Modell: {label}\n")
    print(f"{'Verfahren':32} {'Recall@3':>9}  {'Diff. zur Baseline':>18}")
    for name, s in scores.items():
        diff = "" if s == base else f"{s - base:+.1f} pp"
        print(f"{name:32} {s:8.1f}%  {diff:>18}")
    print(f"\nSchwelle: mindestens {THRESHOLD_PP:.0f} Prozentpunkte ueber der Baseline")
    for name in ("Embedding allein", "Feldfilter + Embedding (App)"):
        d = scores[name] - base
        print(f"  {name:30} {d:+.1f} pp  -> {'GO' if d >= THRESHOLD_PP else 'NO-GO'}")
    if args.model == "hash":
        print("\nPLATZHALTER-LAUF: Der Hash ist nicht semantisch. Diese Zahlen sagen nichts ueber EmbeddingGemma 2.")
    if len(queries) < 50 or len(receipts) < 100:
        print(f"\nHinweis: Die Planung sieht >= 100 Belege und 50 Anfragen vor (hier {len(receipts)} / {len(queries)}).")


if __name__ == "__main__":
    main()
