# STATUS.md — Gesamtübersicht

Stand: 2026-10-16 (nach Abgleich mit dem Code; Zeilen mit „laut Autor" sind nicht unabhängig geprüft)

| Phase | Inhalt | Status | Nachweis |
|---|---|---|---|
| 0 | Spike (100 Belege, Baseline vs. semantisch) | ⏳ offen | `spike/BERICHT.md` noch ausfüllen; Harness läuft nur auf **synthetischen** Daten (Baseline Recall@3 72,0 %), echte Belege und echtes Modell fehlen; Vergleich per `spike/harness/compare.py` |
| 1 | Landingpage, Interviews, Concierge, Vorverkauf | ⏳ Artefakte fertig, Nutzerzmessung ausstehend | `phase1/` |
| 2 | Klick-Prototyp, Nutzertest-Anleitung | ⏳ Artefakte fertig, Test ausstehend | `phase2/` |
| 3 | Beta-App (Android) | 🟡 Kernlogik steht, Hauptschleife nicht verdrahtet; Gerätetest offen | Build und Tests grün laut Autor; Details unter „Noch nicht verdrahtet" |
| 4–7 | Polieren, Modell-Pack, Store | ⬜ nicht gestartet | `BAUPLAN.md` |

## Was heute fertig wurde (Phase 3)
- Multi-Module-Android-Projekt unter `app/`, Build grün laut Autor
- Kernlogik als Pure-Kotlin testbar: `CheckBeforeSave`, `RuleParser`, `SearchEngine` (Tests siehe Zweig `fix/geld-suche-spike`)
- ADR-008 als Logik in `LicenseManager`: Lizenzende stoppt nur die Erfassung, nie den Zugriff (noch nicht an die Erfassung angebunden)
- Sicherung (nur Export-Logik): AES-256-GCM + PBKDF2 600k, ZIP/SHA-256, Header `BKBF1`
- Vier Bildschirme angelegt (Erfassen, Belege, Suchen, Einstellungen), noch ohne Wege dazwischen

## Noch nicht verdrahtet (Stand Code, 2026-10-16)
- **Erfassen:** Buttons leer, CameraX nicht eingebunden. `OcrEngine`, `RuleParser` und `CheckBeforeSave` haben keinen Aufrufer; nichts schreibt einen Beleg in die Datenbank, nichts erzeugt ein Embedding. Die Suche findet deshalb nichts.
- **Navigation:** `NavHost` ohne `navigate`/Leiste; erreichbar ist nur „Erfassen".
- **Kauf:** nur die Konstante `SKU_LIFETIME`; kein `BillingClient`, `markPurchased()` wird nie aufgerufen.
- **Sicherung:** Export-Button leer; „Wiederherstellung" prüft nur Passwort und Manifest und stellt nichts wieder her.
- **Modell:** `HashEmbedder` ist Platzhalter; unter `assets/embed_model/` liegt nur ein README, kein Asset-Pack-Modul.
- **Fristen:** `DeadlineWorker` ist leer (Phase 5).
- **Gemma 4:** das `understand/`-Modul aus dem Bauplan gibt es noch nicht.

## Die drei Haken bis echte Beta
1. Spike-Bericht ausfüllen → Zahl für Modell-Pack
2. Gerät aussuchen fur RAM/Zeit-Werte (Ruhe < 400 MB; Text < 2 s; Foto < 5 s)
3. `HashEmbedder` → MediaPipe EmbeddingGemma 2 tauschen
4. Hauptschleife verdrahten (Erfassen → OCR → Prüfen → Speichern → Embedding). Laut Bauplan („Kein Code vor dem Spike-Bericht") erst nach Haken 1.

## Sofort-Befehle
```powershell
cd "G:\AI Agentur\BELEGKASTEN\app"
.\gradlew.bat assembleDebug          # APK bauen
.\gradlew.bat testDebugUnitTest      # Tests
adb install -r app\build\outputs\apk\debug\app-debug.apk
```
```bash
cd app                                # Linux/macOS
./gradlew assembleDebug && ./gradlew testDebugUnitTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
cd ../spike/harness && python compare.py    # Spike-Vergleich (Platzhalter-Modell)
```
