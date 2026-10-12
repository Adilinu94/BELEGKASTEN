# STATUS.md — Gesamtübersicht

Stand: 2026-10-11

| Phase | Inhalt | Status | Nachweis |
|---|---|---|---|
| 0 | Spike (100 Belege, Baseline vs. semantisch) | ⏳ offen | `spike/BERICHT.md` noch ausfüllen; Harness läuft (Baseline Recall@3 72,0 %) |
| 1 | Landingpage, Interviews, Concierge, Vorverkauf | ⏳ Artefakte fertig, Nutzerzmessung ausstehend | `phase1/` |
| 2 | Klick-Prototyp, Nutzertest-Anleitung | ⏳ Artefakte fertig, Test ausstehend | `phase2/` |
| 3 | Beta-App (Android) | ✅ Code komplett; Gerätetest offen | `assembleDebug` SUCCESS, 6/6 Tests grün |
| 4–7 | Polieren, Modell-Pack, Store | ⬜ nicht gestartet | `BAUPLAN.md` |

## Was heute fertig wurde (Phase 3)
- Vollständiges Multi-Module-Android-Projekt unter `app/`, Build grün
- Kernlogik als Pure-Kotlin testbar: `CheckBeforeSave` (3 Tests), `RuleParser` (3 Tests)
- ADR-008 in Code: Lizenzende stoppt nur die Erfassung, nie den Zugriff
- Sicherung: AES-256-GCM + PBKDF2 600k, ZIP/SHA-256, Header `BKBF1`
- Navigation: Erfassen → Prüfen → Suchen → Einstellungen

## Die drei Haken bis echte Beta
1. Spike-Bericht ausfüllen → Zahl für Modell-Pack
2. Gerät aussuchen fur RAM/Zeit-Werte (Ruhe < 400 MB; Text < 2 s; Foto < 5 s)
3. `HashEmbedder` → MediaPipe EmbeddingGemma 2 tauschen

## Sofort-Befehle
```powershell
cd "G:\AI Agentur\BELEGKASTEN\app"
.\gradlew.bat assembleDebug          # APK bauen
.\gradlew.bat testDebugUnitTest      # Tests
adb install -r app\build\outputs\apk\debug\app-debug.apk
```
