"""Spike 0.3: echtes EmbeddingGemma 2, Text-only, 256 Dimensionen.

Vergleicht dieselben Belege und Ziel-IDs mit der bestehenden Baseline.
Rechnerwerte sind KEIN Android-/Echtbeleg-Abnahmenachweis.
Die optionalen Modell-Abhaengigkeiten werden erst in main() importiert.
"""
import argparse
import hashlib
import importlib.metadata
import json
import math
import platform
import statistics
import time
from datetime import date, datetime, timezone
from pathlib import Path

from baseline_search import search as baseline_search
from embedding_search import CORPUS, DIM, HERE, cosine, doc_text
from metrics import recall_at_k

MODEL_ID = "google/embeddinggemma-2"


def load_corpus(corpus: Path):
    receipts_path, queries_path = corpus / "receipts.json", corpus / "queries.json"
    receipts = json.loads(receipts_path.read_text(encoding="utf-8"))
    queries = json.loads(queries_path.read_text(encoding="utf-8"))
    for name, rows, fields in (
        ("receipts", receipts, ("id", "merchant", "date", "text")),
        ("queries", queries, ("id", "text", "target")),
    ):
        if not isinstance(rows, list) or not rows:
            raise ValueError(f"{name}: non-empty list required")
        ids = set()
        for row in rows:
            if not isinstance(row, dict) or any(
                not isinstance(row.get(f), str) or not row[f].strip() for f in fields
            ):
                raise ValueError(f"{name}: non-empty strings required for {fields}")
            if row["id"] in ids:
                raise ValueError(f"{name}: duplicate id {row['id']}")
            ids.add(row["id"])
    receipt_ids = {r["id"] for r in receipts}
    for r in receipts:
        if date.fromisoformat(r["date"]).isoformat() != r["date"]:
            raise ValueError(f"receipts: ISO date required for {r['id']}")
    for q in queries:
        if q["target"] not in receipt_ids:
            raise ValueError(f"queries: unknown target for {q['id']}")
    fingerprints = {
        p.name: hashlib.sha256(p.read_bytes()).hexdigest()
        for p in (receipts_path, queries_path)
    }
    return receipts, queries, fingerprints


def unit_vector(vector):
    values = [float(v) for v in vector]
    if len(values) != DIM or not all(math.isfinite(v) for v in values):
        raise ValueError(f"embedding must contain {DIM} finite values")
    norm = math.sqrt(sum(v * v for v in values))
    if not math.isfinite(norm) or norm == 0:
        raise ValueError("embedding must have a finite, non-zero norm")
    return [v / norm for v in values]


def evaluate(receipts, queries, model, batch_size=4):
    # Prefixes exactly once: an empty prompt disables any model default prompt.
    options = dict(
        prompt="", truncate_dim=DIM, normalize_embeddings=True,
        convert_to_numpy=True, batch_size=batch_size, show_progress_bar=False,
    )
    start = time.perf_counter()
    encoded = model.encode([doc_text(r) for r in receipts], **options)
    if len(encoded) != len(receipts):
        raise ValueError("model returned the wrong number of document embeddings")
    vectors = {r["id"]: unit_vector(v) for r, v in zip(receipts, encoded)}
    index_ms = (time.perf_counter() - start) * 1000
    baseline, embedding, timings, details = {}, {}, [], []
    for q in queries:
        baseline_ranked = baseline_search(q["text"], receipts)
        start = time.perf_counter()
        query = unit_vector(model.encode("task: search result | query: " + q["text"], **options))
        ranked = sorted(vectors, key=lambda rid: (-cosine(query, vectors[rid]), rid))
        elapsed_ms = (time.perf_counter() - start) * 1000
        timings.append(elapsed_ms)
        baseline[q["id"]] = (q["target"], baseline_ranked)
        embedding[q["id"]] = (q["target"], ranked)
        details.append({
            "query_id": q["id"], "target": q["target"],
            "baseline_top3": baseline_ranked[:3], "embedding_top3": ranked[:3],
            "latency_ms": elapsed_ms,
        })
    baseline_r3, embedding_r3 = recall_at_k(baseline, 3), recall_at_k(embedding, 3)
    baseline_hits = sum(q["target"] in q["baseline_top3"] for q in details)
    embedding_hits = sum(q["target"] in q["embedding_top3"] for q in details)
    return {
        "n_receipts": len(receipts), "n_queries": len(queries), "dim": DIM,
        "baseline_recall_at_3": baseline_r3, "recall_at_3": embedding_r3,
        "baseline_hits": baseline_hits, "embedding_hits": embedding_hits,
        "delta_pp": (embedding_hits - baseline_hits) * 100 / len(queries),
        "delta_threshold_pp": 15,
        "delta_threshold_met": (embedding_hits - baseline_hits) * 100 >= 15 * len(queries),
        "index_ms": index_ms,
        "query_latency_ms": {
            "p50": statistics.median(timings),
            "p95": sorted(timings)[math.ceil(len(timings) * .95) - 1],
            "max": max(timings),
        },
        "queries": details,
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--corpus", type=Path, default=CORPUS)
    parser.add_argument("--output", type=Path, default=HERE / "gemma_results.json")
    parser.add_argument("--revision", default="main", help="HF commit or revision to load")
    parser.add_argument("--local-files-only", action="store_true", help="No model download")
    parser.add_argument("--batch-size", type=int, default=4)
    args = parser.parse_args()
    if args.batch_size < 1:
        parser.error("--batch-size must be positive")
    try:
        receipts, queries, fingerprints = load_corpus(args.corpus)
    except (OSError, ValueError) as e:
        parser.exit(1, f"Invalid corpus: {e}\n")
    try:
        import torch
        from huggingface_hub import snapshot_download
        from sentence_transformers import SentenceTransformer
    except ImportError as e:
        parser.exit(1, f"Missing model dependency: {e}. See EMBEDDING_SPIKE_MODELL.md.\n")

    print(f"Loading {MODEL_ID}, text-only, float32, CPU ...", flush=True)
    start = time.perf_counter()
    try:
        # Resolve the revision BEFORE loading. Transformers v5 may leave
        # config._commit_hash empty; the snapshot directory pins the actual files.
        snapshot = snapshot_download(
            MODEL_ID, revision=args.revision, local_files_only=args.local_files_only,
            allow_patterns=["*.json", "*.safetensors", "*.model", "*.jinja", "*/*.json"],
        )
        model = SentenceTransformer(
            snapshot, device="cpu", local_files_only=True,
            config_kwargs={"vision_config": None, "audio_config": None},
            model_kwargs={"torch_dtype": torch.float32},
        )
        load_ms = (time.perf_counter() - start) * 1000
        print(f"Encoding {len(receipts)} receipts / {len(queries)} queries ...", flush=True)
        result = evaluate(receipts, queries, model, args.batch_size)
    except (ImportError, OSError, RuntimeError, ValueError) as e:
        parser.exit(1, f"Model run failed (no hash fallback): {e}\n")

    result.update({
        "note": "embeddinggemma-2-text-only",
        "measured_at": datetime.now(timezone.utc).isoformat(),
        "model": MODEL_ID, "revision_requested": args.revision,
        "revision_resolved": Path(snapshot).name,
        "active_parameters": sum(p.numel() for p in model.parameters()),
        "device": str(model.device), "dtype": str(model.dtype),
        "model_load_ms_including_download": load_ms,
        "batch_size": args.batch_size,
        "local_files_only": args.local_files_only,
        "corpus_sha256": fingerprints,
        "corpus_kind": "synthetic" if args.corpus.resolve() == CORPUS.resolve() else "unverified",
        "acceptance_eligible": False,
        "limitations": [
            "Desktop CPU, not Android: no device latency/RAM/offline proof.",
            "Bundled corpus is synthetic; other corpora need independent validation.",
            "Existing single-target labels unchanged, even for ambiguous queries.",
            "Baseline is token/field scoring, not SQLite FTS4.",
            "No object photos tested; descriptions are not images.",
        ],
        "runtime": {
            "python": platform.python_version(), "platform": platform.platform(),
            "cpu": platform.processor(), "torch_threads": torch.get_num_threads(),
            **{name: importlib.metadata.version(name) for name in (
                "torch", "sentence-transformers", "transformers", "numpy", "huggingface-hub",
                "torchvision", "pillow",
            )},
        },
    })
    args.output.write_text(json.dumps(result, indent=2, allow_nan=False) + "\n", encoding="utf-8")
    print(f"Baseline: {result['baseline_recall_at_3']:.1%}; Gemma: {result['recall_at_3']:.1%}; "
          f"delta: {result['delta_pp']:+.1f} pp")
    print(f"CPU query latency (embedding + ranking), p50: {result['query_latency_ms']['p50']:.1f} ms; "
          f"p95: {result['query_latency_ms']['p95']:.1f} ms")
    print(f"Saved: {args.output}. NO product GO: real receipts and Android tests still required.")


if __name__ == "__main__":
    main()
