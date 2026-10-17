"""Harness-Regressionen ohne Modell-Download oder PyTorch."""
import json
import io
import math
import tempfile
import unittest
from contextlib import redirect_stdout, redirect_stderr
from pathlib import Path
from types import SimpleNamespace
from unittest.mock import Mock, patch

import gemma_search
from embedding_search import DIM
from gemma_search import evaluate, load_corpus, unit_vector


class FakeModel:
    def __init__(self):
        self.calls = []

    def encode(self, texts, **options):
        self.calls.append((texts, options))
        if isinstance(texts, list):
            return [[1.] + [0.] * (DIM - 1), [0., 1.] + [0.] * (DIM - 2)]
        return [0., 1.] + [0.] * (DIM - 2)


class GemmaSearchTest(unittest.TestCase):
    def setUp(self):
        self.receipts = [
            {"id": "R1", "merchant": "A", "date": "2025-01-01", "text": "ROHER BONTEXT A"},
            {"id": "R2", "merchant": "B", "date": "2025-02-01", "text": "ROHER BONTEXT B",
             "positions": [{"query": "GEHEIMES SUCHWORT", "photo": "FOTO-BESCHREIBUNG"}]},
        ]
        self.queries = [{"id": "Q1", "text": "Bohrmaschine", "target": "R2"}]

    def test_raw_text_and_prefixes_exactly_once(self):
        model = FakeModel()
        result = evaluate(self.receipts, self.queries, model)
        self.assertEqual(model.calls[0][0], [
            "title: A | text: ROHER BONTEXT A", "title: B | text: ROHER BONTEXT B",
        ])
        self.assertEqual(model.calls[1][0], "task: search result | query: Bohrmaschine")
        for _, options in model.calls:
            self.assertEqual(options["prompt"], "")
            self.assertEqual(options["truncate_dim"], 256)
            self.assertTrue(options["normalize_embeddings"])
        self.assertEqual(result["queries"][0]["embedding_top3"][0], "R2")
        self.assertEqual(result["recall_at_3"], 1.)
        self.assertEqual(result["delta_pp"], 0.)
        self.assertFalse(result["delta_threshold_met"])
        # Export contains IDs and metrics, not receipt/query contents.
        self.assertNotIn("Bohrmaschine", json.dumps(result))
        self.assertNotIn("BONTEXT", json.dumps(result))

    def test_vectors_normalized_after_truncation(self):
        v = unit_vector([3., 4.] + [0.] * (DIM - 2))
        self.assertAlmostEqual(v[0], .6)
        self.assertAlmostEqual(sum(x * x for x in v), 1.)

    def test_bad_vectors_rejected(self):
        for v in (
            [1.] * 768, [0.] * DIM,
            [math.nan] + [0.] * (DIM - 1),
            [math.inf] + [0.] * (DIM - 1),
            [1e308] * DIM,
        ):
            with self.subTest(first=v[0]), self.assertRaises(ValueError):
                unit_vector(v)

    def test_document_count_mismatch_rejected(self):
        with self.assertRaises(ValueError):
            evaluate(self.receipts[:1], self.queries, FakeModel())

    def test_ties_sorted_by_receipt_id(self):
        class TiedModel(FakeModel):
            def encode(self, texts, **options):
                v = [1.] + [0.] * (DIM - 1)
                return [v, v] if isinstance(texts, list) else v
        result = evaluate(list(reversed(self.receipts)), self.queries, TiedModel())
        self.assertEqual(result["queries"][0]["embedding_top3"], ["R1", "R2"])

    def test_delta_compares_same_targets_to_baseline(self):
        class BetterModel(FakeModel):
            def encode(self, texts, **options):
                if isinstance(texts, list):
                    return [[1., 0.] + [0.] * (DIM - 2)] * 3 + [[0., 1.] + [0.] * (DIM - 2)]
                return [0., 1.] + [0.] * (DIM - 2)
        receipts = [{**self.receipts[0], "id": f"R{i}"} for i in range(1, 5)]
        queries = [{**self.queries[0], "target": "R4"}]
        result = evaluate(receipts, queries, BetterModel())
        self.assertEqual(result["baseline_hits"], 0)
        self.assertEqual(result["embedding_hits"], 1)
        self.assertEqual(result["delta_pp"], 100.)
        self.assertTrue(result["delta_threshold_met"])

    def test_fifteen_point_threshold_is_inclusive(self):
        class BoundaryModel(FakeModel):
            def __init__(self):
                super().__init__()
                self.query_count = 0

            def encode(self, texts, **options):
                if isinstance(texts, list):
                    return [[1., 0.] + [0.] * (DIM - 2)] * 3 + [[0., 1.] + [0.] * (DIM - 2)]
                self.query_count += 1
                return ([0., 1.] if self.query_count <= 3 else [1., 0.]) + [0.] * (DIM - 2)
        receipts = [{**self.receipts[0], "id": f"R{i}"} for i in range(1, 5)]
        queries = [{"id": f"Q{i}", "text": "unbekannt", "target": "R4"} for i in range(20)]
        result = evaluate(receipts, queries, BoundaryModel())
        self.assertEqual(result["delta_pp"], 15.)
        self.assertTrue(result["delta_threshold_met"])

    def write_corpus(self, folder, receipts=None, queries=None):
        (folder / "receipts.json").write_text(json.dumps(
            self.receipts if receipts is None else receipts), encoding="utf-8")
        (folder / "queries.json").write_text(json.dumps(
            self.queries if queries is None else queries), encoding="utf-8")

    def test_corpus_and_fingerprints(self):
        with tempfile.TemporaryDirectory() as tmp:
            folder = Path(tmp)
            self.write_corpus(folder)
            r, q, hashes = load_corpus(folder)
            self.assertEqual(r, self.receipts)
            self.assertEqual(q, self.queries)
            self.assertEqual(set(hashes), {"receipts.json", "queries.json"})
            self.assertTrue(all(len(h) == 64 for h in hashes.values()))

    def test_invalid_corpus_rejected(self):
        cases = [
            ([], self.queries), (self.receipts, []),
            (self.receipts + self.receipts, self.queries),
            (self.receipts, self.queries + self.queries),
            ([{**self.receipts[0], "date": "2025-02-30"}], self.queries),
            ([{**self.receipts[0], "text": " "}], self.queries),
            (self.receipts, [{**self.queries[0], "target": "UNKNOWN"}]),
            (self.receipts, [{**self.queries[0], "text": 42}]),
        ]
        with tempfile.TemporaryDirectory() as tmp:
            folder = Path(tmp)
            for receipts, queries in cases:
                with self.subTest(receipts=receipts, queries=queries), self.assertRaises(ValueError):
                    self.write_corpus(folder, receipts, queries)
                    load_corpus(folder)

    def test_main_loads_pinned_text_only_snapshot(self):
        snapshot_download = Mock(return_value="cache/snapshots/" + "a" * 40)
        model = FakeModel()
        model.parameters = lambda: [SimpleNamespace(numel=lambda: 271002624)]
        model.device, model.dtype = "cpu", "torch.float32"
        constructor = Mock(return_value=model)
        modules = {
            "torch": SimpleNamespace(float32="torch.float32", get_num_threads=lambda: 4),
            "huggingface_hub": SimpleNamespace(snapshot_download=snapshot_download),
            "sentence_transformers": SimpleNamespace(SentenceTransformer=constructor),
        }
        with tempfile.TemporaryDirectory() as tmp:
            folder = Path(tmp)
            self.write_corpus(folder)
            output = folder / "result.json"
            args = ["gemma_search.py", "--corpus", str(folder), "--output", str(output),
                    "--revision", "a" * 40, "--local-files-only"]
            with patch.dict("sys.modules", modules), patch("sys.argv", args), \
                    patch("gemma_search.importlib.metadata.version", return_value="test"), \
                    redirect_stdout(io.StringIO()):
                gemma_search.main()
            snapshot_download.assert_called_once_with(
                gemma_search.MODEL_ID, revision="a" * 40, local_files_only=True,
                allow_patterns=["*.json", "*.safetensors", "*.model", "*.jinja", "*/*.json"],
            )
            constructor.assert_called_once_with(
                snapshot_download.return_value, device="cpu", local_files_only=True,
                config_kwargs={"vision_config": None, "audio_config": None},
                model_kwargs={"torch_dtype": "torch.float32"},
            )
            result = json.loads(output.read_text(encoding="utf-8"))
            self.assertEqual(result["revision_resolved"], "a" * 40)
            self.assertEqual(result["active_parameters"], 271002624)
            self.assertTrue(result["local_files_only"])
            self.assertFalse(result["acceptance_eligible"])
            self.assertEqual(result["corpus_kind"], "unverified")

    def test_model_load_failure_does_not_write_or_fall_back(self):
        constructor = Mock(side_effect=ImportError("missing processor"))
        modules = {
            "torch": SimpleNamespace(float32="torch.float32"),
            "huggingface_hub": SimpleNamespace(snapshot_download=Mock(return_value="cache/snapshot")),
            "sentence_transformers": SimpleNamespace(SentenceTransformer=constructor),
        }
        with tempfile.TemporaryDirectory() as tmp:
            folder = Path(tmp)
            self.write_corpus(folder)
            output = folder / "result.json"
            output.write_text("previous evidence", encoding="utf-8")
            args = ["gemma_search.py", "--corpus", str(folder), "--output", str(output)]
            stderr = io.StringIO()
            with patch.dict("sys.modules", modules), patch("sys.argv", args), \
                    patch("gemma_search.evaluate") as evaluate_mock, \
                    redirect_stdout(io.StringIO()), redirect_stderr(stderr), \
                    self.assertRaises(SystemExit) as error:
                gemma_search.main()
            self.assertEqual(error.exception.code, 1)
            evaluate_mock.assert_not_called()
            self.assertIn("no hash fallback", stderr.getvalue())
            self.assertEqual(output.read_text(encoding="utf-8"), "previous evidence")


if __name__ == "__main__":
    unittest.main()
