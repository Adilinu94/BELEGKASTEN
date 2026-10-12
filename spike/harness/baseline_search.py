"""Baseline 0.2: Volltext (Token-Overlap) + Feldfilter (Haendler, Monat).

Rankt Belege nach: +3 Haendlerwort in Anfrage, +2 Monatsmatch,
+1 je Anfrage-Token im Belegtext. Dient als Vergleichszahl fuer 0.3.
"""
import json
from pathlib import Path
from metrics import tokenize, recall_at_k, MONTH_TO_NUM, normalize

HERE = Path(__file__).resolve().parent
CORPUS = HERE.parent / "corpus"


def search(query_text, receipts):
    qtokens = set(tokenize(query_text))
    qnorm = normalize(query_text)
    scored = []
    for r in receipts:
        score = 0
        rnorm = normalize(r["merchant"])
        if any(t in qnorm and len(t) > 2 and t in rnorm for t in qtokens):
            score += 3
        for month_name, num in MONTH_TO_NUM.items():
            if month_name in [t for t in qtokens]:
                if int(r["date"].split("-")[1]) == num:
                    score += 2
                break
        rtext = normalize(r["text"])
        score += sum(1 for t in qtokens if len(t) > 2 and t in rtext)
        scored.append((score, r["id"]))
    scored.sort(key=lambda x: (-x[0], x[1]))
    return [rid for _, rid in scored]


def main():
    receipts = json.loads((CORPUS / "receipts.json").read_text(encoding="utf-8"))
    queries = json.loads((CORPUS / "queries.json").read_text(encoding="utf-8"))
    results = {q["id"]: (q["target"], search(q["text"], receipts)) for q in queries}
    r3 = recall_at_k(results, 3)
    print(f"Baseline Volltext+Felder: Recall@3 = {r3:.1%} ({len(queries)} Anfragen)")
    misses = [qid for qid, (t, rk) in results.items() if t not in rk[:3]][:5]
    for qid in misses:
        t, rk = results[qid]
        q = next(q["text"] for q in queries if q["id"] == qid)
        print(f"  MISS {qid}: '{q}' -> Ziel {t}, Top3 {rk[:3]}")
    (HERE / "baseline_results.json").write_text(
        json.dumps({"recall_at_3": r3, "n": len(queries)}, indent=1))
    return r3


if __name__ == "__main__":
    main()
