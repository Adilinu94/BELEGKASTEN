# Spike 0.3 Stufe 2: EmbeddingGemma 2 echt messen (Anleitung, ~30 Min)

Der Hash-Platzhalter in `embedding_search.py` prueft nur Plumbing.
Fuer die Go/No-Go-Zahl (15 Prozentpunkte ueber Baseline) muss das echte
Modell laufen. Zwei Wege, je nach Geraet:

## Weg A: Schnell am Rechner (PyTorch + SentenceTransformers)

```bash
pip install sentence-transformers torch --index-url https://download.pytorch.org/whl/cpu
python - <<'EOF'
from sentence_transformers import SentenceTransformer
import json
from pathlib import Path
# Hugging-Face-Zugang + Lizenzakzeptanz (Apache-2.0) noetig:
m = SentenceTransformer("google/embeddinggemma-2-270m")
C = Path("spike/corpus"); R = json.loads((C/"receipts.json").read_text("utf-8"))
Q = json.loads((C/"queries.json").read_text("utf-8"))
docs = [f"title: {r['merchant']} | text: {r['text']}" for r in R]  # roher Bontext, wie die Baseline
dv = m.encode(docs, normalize_embeddings=True)
hits = 0
for q in Q:
    qv = m.encode("task: search result | query: " + q["text"], normalize_embeddings=True)
    import numpy as np
    sims = dv @ qv
    top3 = [R[i]["id"] for i in np.argsort(-sims)[:3]]
    hits += q["target"] in top3
print(f"Echter Recall@3 EmbeddingGemma-270M: {hits/len(Q):.1%}")
EOF
```

## Weg B: Auf Android (entscheidend fuer Latenz/RAM)

1. MediaPipe tasks-retrieval (UniversalEmbedder) ins Spike-Projekt, Modell
   `embeddinggemma-2-270m` per Play Asset Delivery (siehe Bauplan).
2. Dieselben 50 Anfragen, Zeit pro Anfrage + RAM (Android Studio Profiler).
3. Schwellen: Textsuche < 2 s, Foto < 5 s, RAM-Dauerlast < 400 MB
   (Pixel 7a / Galaxy A54 o. ae.).

## Schnellster Weg: `compare.py`

```bash
cd spike/harness
python compare.py --model st:google/embeddinggemma-2-270m
python compare.py --model st:google/embeddinggemma-2-270m --corpus ../corpus_real   # eigene Belege
```

Misst Baseline, Embedding allein und Feldfilter + Embedding (wie die App) in einem Lauf
und prueft die 15-Prozentpunkte-Schwelle. Schema fuer eigene Belege: Kopf von `compare.py`.

## Danach

Baseline aus `baseline_search.py` gegen echten Wert halten:
Differenz >= 15 Prozentpunkte -> GO, sonst Idee neu bewerten.
Ergebnis in `spike/BERICHT.md` eintragen.
