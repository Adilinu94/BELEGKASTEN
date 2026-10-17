# Spike-Bericht Belegkasten — Zwischenstand, kein Produkt-GO

Stand: 2026-10-16 · Ausführung: Entwicklungsrechner (Windows 11, CPU)

## Testumfang und Grenzen

Gemessen wurden 100 **synthetische** Belege und 50 unveränderte Suchanfragen
aus `spike/corpus/`. Das Testset aus echten, geschwärzten Belegen und die
2–3 Android-Zielgeräte aus dem Bauplan liegen noch nicht vor.

Die Baseline ist das vorhandene Token-/Feld-Scoring in `baseline_search.py`,
**nicht SQLite FTS4**. EmbeddingGemma wird im Harness ohne vorgeschalteten
Feldfilter über alle Belege gerankt, nicht über die Android-Suchpipeline.
Beide Verfahren erhalten denselben Bontext und dieselben einzelnen Ziel-IDs;
bereinigte Positions-Suchwörter werden nicht in Dokumente übernommen.
Mehrdeutige Anfragen wurden nicht nachträglich umbeschriftet.

## 0.2/0.3 Suche — echter Modelllauf

| Verfahren | Treffer in Top 3 | Recall@3 |
|---|---:|---:|
| Token-/Feld-Baseline | 36 / 50 | 72,0 % |
| Hash-Platzhalter (kein semantisches Modell) | 11 / 50 | 22,0 % |
| **EmbeddingGemma 2, Text-only** | **41 / 50** | **82,0 %** |
| **Differenz Modell − Baseline** | **+5 Treffer** | **+10,0 Prozentpunkte** |

**Die vorher festgelegte Schwelle von mindestens +15 Prozentpunkten wird in
diesem Lauf nicht erreicht.** Bei 50 Anfragen und 36 Baseline-Treffern wären
mindestens 44 Modell-Treffer nötig (88 %). Die Schwelle bleibt unverändert.
Weder ein Such-GO noch ein abschließendes Produkt-No-Go ist aus dem
synthetischen Korpus ableitbar; der erforderliche Echtbeleg-Spike bleibt offen.

Modell: `google/embeddinggemma-2`, nur Text-Encoder (271.002.624 aktive Parameter),
CPU float32, 256 Dimensionen, L2-Normalisierung nach dem Kürzen. Dokument- und
Anfragepräfixe werden exakt einmal gesetzt. Der erste Lauf scheiterte am
fehlenden gemeinsamen Bild-Processor; Pillow/torchvision wurden in einer
separaten Entwicklungsumgebung ergänzt, nicht in der Android-App.

Nachweis: `spike/harness/gemma_results.json` enthält Top-3-IDs je Anfrage,
Trefferzahlen, Modellrevision, Paketversionen und SHA-256-Fingerprints des Korpus.
Reproduktion: `spike/harness/EMBEDDING_SPIKE_MODELL.md`.

Die festgehaltene Modellrevision
`914f7f89142e33e77833254d9c9b90c3cef7303b` wurde mit `--local-files-only`
erneut geladen. Der unabhängige Wiederholungslauf aus dem Cache ergab für
alle 50 Anfragen **identische Top-3-IDs** und dieselben Trefferzahlen.

## 0.1, 0.4–0.6 — noch nicht gemessen

| Messpunkt | Stand |
|---|---|
| OCR: Datum, Summe, Händler, Umlaute | Kein Echtbeleg-/Bildtest |
| Gegenstand-Foto-Recall@3 | Keine Fotos; `photo_pairs.json` enthält nur Beschreibungen |
| Android-Text-/Foto-Latenz | Kein angeschlossenes Zielgerät |
| RAM-Dauerlast < 400 MB | Ungemessen |
| Start und Kernflows ohne Netz, Mitschnitt | Ungemessen; Quellmanifest allein reicht nicht |
| Android-Modell-/PAD-Paketgröße, Store-Limits | Kein reales Modell-Pack |
| Scanner-Entscheidung | Offen |
| Gemma-4-Tool-Calling | Nicht begonnen; abhängig vom Such-Spike |

Die in der Ergebnisdatei angegebenen CPU-Zeiten sind reine
Embedding-/Ranking-Zeiten, kein UI-, OCR- oder Android-Abnahmenachweis.

## Go / No-Go und nächste Arbeit

- [ ] Suche-GO: Echtbelege + unabhängige Suchaufgaben und ≥15 pp Verbesserung.
- [ ] Foto-GO: echte Gegenstand-/Beleg-Bildpaare und ≥50 % Recall@3.
- [ ] Offline-GO: funktionierende Aufnahme/Suche + sauberer Netzwerkmitschnitt.
- [ ] Geräte-GO: Android-Latenz, RAM und Paketgröße im Zielbereich.

**Aktuell keine Freigabe für weiteren Phasenbau oder Store-Veröffentlichung.**
Vor Modell-Integration und Phase-3-Ausbau den Echtbeleg-Spike nachholen oder
die Produktentscheidung ausdrücklich neu bewerten. Für bessere Zahlen weder
Ziel-IDs zurechtschneiden noch Schwellen nachträglich absenken.

Das Android-Projekt ist ein buildbares Gerüst, keine bereits abgenommene Beta.
Die korrigierten Kernflow-Lücken stehen in `ABNAHME-PHASE-3.md`.
