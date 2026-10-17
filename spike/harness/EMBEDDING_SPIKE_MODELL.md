# Spike 0.3: EmbeddingGemma 2 reproduzierbar messen

Stand: 2026-10-16. `embedding_search.py` ist weiterhin nur der Hash-Platzhalter.
Das echte Modell laeuft separat in `gemma_search.py`; bei einem Fehler gibt es
keinen stillen Rueckfall auf Hash-Vektoren.

## Korrekte Modellkonfiguration

- Modell-ID: **`google/embeddinggemma-2`**, nicht `google/embeddinggemma-2-270m`.
- Text-only (270M aktive Parameter) ueber
  `config_kwargs={"vision_config": None, "audio_config": None}`. Das ist eine
  Encoder-Konfiguration, kein eigenes Modell-Repository. Der HF-Checkpoint
  enthaelt trotzdem die multimodalen Gewichte (1.419,9 MiB / ca. 1,39 GiB); das ist weder die
  Android-Paketgroesse noch ein Nachweis fuer Play Asset Delivery.
- CPU-Inferenz in **float32**, nie float16.
- Beleg: `title: {merchant} | text: {text}` aus rohem Bontext. Keine
  bereinigten `positions[*].query`-Suchwoerter oder Foto-Beschreibungen.
- Anfrage: `task: search result | query: {query}`. `prompt=""` verhindert einen
  zweiten automatisch hinzugefuegten Praefix.
- **256 Dimensionen**, nach dem Kuerzen L2-normalisieren. Nicht-endliche Werte,
  Nullvektoren und falsche Dimensionen brechen die Messung ab.

## Weg A: Rechner (aus dem Repository-Hauptordner)

Eine separate Python-Umgebung verwenden; die Android-App bekommt keine neue
Abhaengigkeit und keine Internet-Berechtigung.

```powershell
python -m venv .venv-spike
.\.venv-spike\Scripts\python.exe -m pip install torch==2.14.1 torchvision==0.29.1 --index-url https://download.pytorch.org/whl/cpu
.\.venv-spike\Scripts\python.exe -m pip install "sentence-transformers[image]==6.1.0" transformers==5.20.0
.\.venv-spike\Scripts\python.exe spike/harness/gemma_search.py
```

Auch Text-only braucht in dieser Bibliotheksversion den gemeinsamen
Multimodal-Processor: deshalb das `[image]`-Extra (Pillow + torchvision).
Es aktiviert keine Bildsuche und laedt keinen Vision-Encoder ins Modell.

Beim ersten Lauf werden Pakete und Modell auf den **Entwicklungsrechner**
geladen. Die oeffentliche Modellkarte weist Apache-2.0 aus; beim geprueften
HF-Repository war kein Zugangstoken erforderlich. Falls sich die Freigabe
aendert, Fehler beheben, nicht mit einem Ersatzmodell weiterrechnen.

Der Lauf schreibt `spike/harness/gemma_results.json` mit:

- Recall@3 und Trefferzahlen fuer beide Verfahren auf denselben Ziel-IDs,
- Differenz in Prozentpunkten und erfuellter/nicht erfuellter 15-pp-Schwelle,
- Top-3-IDs je Anfrage (keine Bontexte oder Suchtexte im Ergebnisexport),
- Indexzeit und Anfrage-Latenz (Embedding + Ranking, CPU),
- Korpus-SHA-256, Modellrevision, Praezision und Paketversionen,
- `acceptance_eligible: false` und expliziten Messgrenzen.

Ergebnis vom 2026-10-16: **72 % Baseline, 82 % Gemma, +10 pp**. Damit ist die
15-pp-Schwelle auf dem synthetischen Korpus nicht erfuellt. Ein Wiederholungslauf
mit der festgehaltenen Revision und lokalem Cache ergab dieselben Top-3-IDs
fuer alle 50 Anfragen. Einordnung und offene Abnahmen: `spike/BERICHT.md`.

Fuer einen Wiederholungslauf die `revision_resolved` aus der Ergebnisdatei
einsetzen; `main` kann sich spaeter aendern:

```powershell
.\.venv-spike\Scripts\python.exe spike/harness/gemma_search.py --revision 914f7f89142e33e77833254d9c9b90c3cef7303b --local-files-only
```

`--local-files-only` verhindert weitere Modell-Downloads. Es ersetzt keinen
Netzwerkmitschnitt. `--corpus <ordner>` liest eigene `receipts.json` und
`queries.json`; nur geschwaerzte Belege mit Einwilligung lokal verwenden,
niemals echte Beleginhalte oder Zugangstoken committen.

Regressionstests ohne Modell und ohne Zusatzpakete:

```powershell
python -m unittest discover -s spike/harness -p "test_*.py" -v
```

## Messgrenzen: kein Produkt-GO aus dem synthetischen Lauf

Das vorhandene Korpus enthaelt 100 **synthetische** Belege und 50 Anfragen.
Die einzelne Ziel-ID pro Anfrage bleibt unveraendert, auch wenn mehrere
Belege plausibel passen. Labels nicht nachtraeglich auf gute Treffer zuschneiden.
Die Baseline ist das vorhandene Token-/Feld-Scoring, **nicht SQLite FTS4**.
Das Ergebnis belegt nur den Vergleich dieser beiden Harness-Verfahren.

`photo_pairs.json` enthaelt Beschreibungen, keine Bilder. Daraus laesst sich
kein Foto-Recall berechnen. RAM, APK-/PAD-Groesse, Android-Latenz, OCR-Qualitaet
und Offline-Verhalten bleiben in diesem Lauf ungemessen.

## Weg B: Android (weiter offen)

1. Echte, geschwaerzte Belege und unabhaengig beschriftete Suchanfragen sammeln.
2. Den tatsaechlich verfuegbaren Android-Runtime- und Modell-Artefaktweg pruefen;
   die Python-Schnittstelle ist kein Nachweis fuer MediaPipe- oder PAD-Integration.
3. Volltext-/Feld-Baseline und Modell auf demselben Testset vergleichen.
4. Auf 2-3 Zielgeraeten messen: Textsuche < 2 s, Foto < 5 s (nur mit Bildern),
   RAM-Dauerlast < 400 MB; Modell- und Store-Paketgroesse dokumentieren.
5. Start, Erfassung und Suche im Flugmodus plus Netzwerkmitschnitt pruefen.

Erst echte Belege, Android-Messwerte und die vorher festgelegten Schwellen
tragen das Go/No-Go in `spike/BERICHT.md`. Bis dahin bleibt `HashEmbedder` in der
App ein kenntlich gemachter Platzhalter und Foto-Suche gesperrt.

## Primarquellen (geprueft am 2026-10-16)

- [Google-Modellkarte](https://huggingface.co/google/embeddinggemma-2):
  Modell-ID, Encoder-Konfiguration, Task-Praefixe, MRL und float32/bfloat16.
- [Google-Inferenzanleitung](https://ai.google.dev/gemma/docs/embeddinggemma/inference-embeddinggemma-with-sentence-transformers):
  SentenceTransformers und Text-only-Konfiguration.
