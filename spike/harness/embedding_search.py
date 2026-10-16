"""Spike 0.3: Embedding-Suche.

Stufe 1 (jetzt, ohne Modell): Hashing-Trick-Embeddings (char-Trigramme ->
fixe Dimension, L2-normalisiert, Kosinus). Prueft das Plumbing:
Vektorablage als BLOB, Kosinus im Code, Recall@3-Messung, Latenz/RAM-Groessenordnung.

Stufe 2 (echtes Modell, auf Geraet/Rechner mit HF-Zugang): In
EMBEDDING_SPIKE_MODELL.md steht, wie EmbeddingGemma 2 (270M zuerst) per
MediaPipe/SentenceTransformers dieselbe Schnittstelle bedient und die
Hash-Vektoren ersetzt. Dann erst gilt die 15-Prozentpunkte-Schwelle.
"""
import hashlib
import json
import math
import time
from pathlib import Path
from metrics import recall_at_k, normalize

HERE = Path(__file__).resolve().parent
CORPUS = HERE.parent / "corpus"
DIM = 256


def embed(text: str, dim: int = DIM) -> list:
    text = normalize(text)
    vec = [0.0] * dim
    tri = [text[i:i + 3] for i in range(max(0, len(text) - 2))]
    for t in tri or ["   "]:
        h = int(hashlib.md5(t.encode()).hexdigest(), 16)
        vec[h % dim] += 1.0
    n = math.sqrt(sum(v * v for v in vec)) or 1.0
    return [v / n for v in vec]


def cosine(a: list, b: list) -> float:
    return sum(x * y for x, y in zip(a, b))


def doc_text(r: dict) -> str:
    # Roher Bontext wie nach der Texterkennung - derselbe Text, den die Baseline liest.
    # (Vorher: bereinigte Suchwoerter p["query"], die die App nie haben wird.)
    return f"title: {r['merchant']} | text: {r['text']}"


def main():
    receipts = json.loads((CORPUS / "receipts.json").read_text(encoding="utf-8"))
    queries = json.loads((CORPUS / "queries.json").read_text(encoding="utf-8"))
    t0 = time.perf_counter()
    docvecs = {r["id"]: embed(doc_text(r)) for r in receipts}
    t_index = time.perf_counter() - t0
    results, lat = {}, []
    for q in queries:
        q0 = time.perf_counter()
        qv = embed("task: search result | query: " + q["text"])
        ranked = sorted(docvecs, key=lambda rid: -cosine(qv, docvecs[rid]))
        lat.append((time.perf_counter() - q0) * 1000)
        results[q["id"]] = (q["target"], ranked)
    r3 = recall_at_k(results, 3)
    blob_bytes = len(receipts) * DIM * 4
    print(f"Hash-Embedding (Platzhalter, NICHT EmbeddingGemma): Recall@3 = {r3:.1%}")
    print(f"Indexzeit: {t_index * 1000:.1f} ms / {len(receipts)} Belege, "
          f"Anfrage-Latenz p50: {sorted(lat)[len(lat)//2]:.2f} ms")
    print(f"BLOB-Groesse {len(receipts)}x{DIM}x4B = {blob_bytes / 1024:.1f} KiB "
          f"(Hochrechnung 5000 Belege: {5000 * DIM * 4 / 1024 / 1024:.1f} MiB)")
    (HERE / "embedding_results.json").write_text(
        json.dumps({"recall_at_3": r3, "dim": DIM, "note": "hash-placeholder"}, indent=1))
    return r3


if __name__ == "__main__":
    main()
