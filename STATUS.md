# STATUS.md — Gesamtübersicht

Stand: 2026-10-16 · geprüft auf `main` (`710c812`, PR #1 gemergt)

| Phase | Inhalt | Status | Nachweis |
|---|---|---|---|
| 0 | Spike (Baseline vs. semantisch) | ⏳ Echter CPU-Lauf fertig; Echtbelege/Geräte offen | Baseline 72 %, Gemma 82 %: **+10 pp, unter 15-pp-Schwelle**; `spike/BERICHT.md` |
| 1 | Landingpage, Interviews, Concierge, Vorverkauf | ⏳ Artefakte fertig, Nutzermessung ausstehend | `phase1/` |
| 2 | Klick-Prototyp, Nutzertest-Anleitung | ⏳ Artefakte fertig, Test ausstehend | `phase2/` |
| 3 | Android-Beta | ⏳ Buildbares Gerüst, **keine vollständige Beta** | `assembleDebug` und 12 JVM-Tests grün; Kernflows teilweise Platzhalter |
| 4–7 | Komfort, Bindung, Härtung, Store | ⬜ nicht gestartet | `BAUPLAN.md` |

## Verifiziert am 2026-10-16

- `app/gradlew.bat testDebugUnitTest assembleDebug --console=plain` erfolgreich.
- 12 JVM-Tests: Prüfregeln (3), Regel-Parser (5), Suchlogik (4).
- Exakte Cent-Berechnung per BigDecimal und Such-Rückfall aus PR #1 enthalten.
- Spike-Baseline reproduziert: 36/50 Treffer (72 %); Hash: 11/50 (22 %).
- Neuer Runner für echtes EmbeddingGemma 2: korrekte Modell-ID, Text-only,
  float32, 256 Dimensionen; getrennte Ergebnisdatei, Korpus-Hashes und Top-3-IDs.
- Echter CPU-Lauf: 41/50 Treffer (82 %), +10 pp zur Baseline. Kein Produkt-GO
  aus synthetischen Belegen; Ergebnis unter der festgelegten +15-pp-Schwelle.
- Wiederholung mit festgehaltener Modellrevision aus lokalem Cache: alle
  50 Top-3-Ranglisten und Vergleichswerte identisch.
- 11 Python-Tests für Harness, Eingaben, Vektoren, Präfixe, Vergleichsschwelle,
  festgehaltene Modellrevision und Fehler ohne Hash-Rückfall.

## Korrektur der bisherigen Abnahmeangaben

Die Fassung vom 2026-10-11 setzte „Code komplett“ mit einem grünen Build gleich.
Das ist nicht durch den Quellcode gedeckt:

- `Capture.kt`: Kamera-/Importbuttons ohne Aktion; keine OCR-/Speicherkette.
- `MainActivity.kt`: vier Ziele deklariert, aber keine bedienbare Navigation.
- `Receipts.kt` / `Search.kt`: Listen ohne Detailziel; keine Bestätigungsansicht.
- `Settings.kt`: Sicherungsbuttons ohne Aktion. `validateAndImport()` liest nur
  das Manifest, stellt nichts wieder her und prüft keine ZIP-Datei-Hashes.
- `LicenseManager.kt`: Testphasenlogik vorhanden, im UI nicht wirksam verdrahtet;
  SKU vorhanden, echter Kauf-/Wiederherstellungsflow fehlt.
- `HashEmbedder` statt semantischem Modell; kein echtes PAD-Modul, nur Asset-README
  und Bibliotheksabhängigkeit. FTS4 und Diagnose-Export fehlen ebenfalls.
- Kein Gerät angeschlossen; Offline-, RAM- und Performance-Nachweise offen.

Weitere Abnahmelücken stehen in `ABNAHME-PHASE-3.md`. Keine echten Belege als
einzige Kopie in diesem Gerüst ablegen und keine Beta-/Store-Freigabe daraus ableiten.

## Nächster Entscheidungspunkt

1. Echte geschwärzte Belege und unabhängige Suchaufgaben für Phase 0 beschaffen;
   Gerätewerte und Offline-Nachweis messen. Die Go/No-Go-Regel aus `BAUPLAN.md` gilt.
2. Such-GO anhand dieses Testsets entscheiden oder die Produktannahme neu bewerten;
   die +15-pp-Schwelle nicht aufgrund des synthetischen Ergebnisses ändern.
3. Erst nach der Entscheidung die Phase-3-Kernflows vervollständigen: Eingang →
   OCR → Feldprüfung → atomar speichern → Suche → Detail; sichere Wiederherstellung.

## Befehle (aus dem Repository-Hauptordner)

```powershell
app\gradlew.bat -p app testDebugUnitTest assembleDebug --console=plain
python -m unittest discover -s spike/harness -p "test_*.py" -v
python tools/check_room_sql.py
adb install -r app\app\build\outputs\apk\debug\app-debug.apk
```

Echter Modelllauf: `spike/harness/EMBEDDING_SPIKE_MODELL.md`.

CI auf GitHub (`.github/workflows/ci.yml`): Gradle-Tests, `assembleDebug`, Prüfung der APK-Berechtigungen (kein `INTERNET`) Python-Tests des Spike-Harness und eine SQL-Prüfung der Room-Datenbank (`tools/check_room_sql.py`: Abfragen, Volltextsuche, Migration 1→2, ohne Android) bei jedem Push auf `main` und jedem Pull Request. Das ist die Gegenprobe zu „Build und Tests grün" auf einer frischen Maschine; ein erster Lauf steht aus.
