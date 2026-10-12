# Abnahme-Checke — Phase 3 (Beta-App)

Stand: 2026-10-11 · Status: Code-seitig abgenommen, Hardware-Test offen

## 3.1 Projekt & Build
- [x] Gradle-Wrapper 8.9 vorhanden (`./gradlew`)
- [x] AGP 8.7.3, Kotlin 1.9.24, KSP, Hilt, Room 2.6.1
- [x] Java 17 / JVM-Target 17 in allen Modulen konsistent
- [x] `./gradlew assembleDebug` → BUILD SUCCESSFUL
- [x] `./gradlew testDebugUnitTest` → 6/6 Tests grün
- [x] Module: core/{model,database,data,ocr,embed,search,backup,license,ui}, feature/{capture,receipts,search,settings}

## 3.2 Offline-Garantie (Kernversprechen)
- [x] Kein `android.permission.INTERNET` im Manifest
- [x] kein Laufzeit-Download des Modells (Play Asset Delivery install-time vorbereitet, `assets/embed_model/`)
- [x] ML Kit gebundelt-Variante (`text-recognition`), keine Play-Services-Version
- [ ] Verifikation im Flugmodus auf Gerät (10.1, ausstehend)

## 3.3 Prüfregeln (CheckBeforeSave)
- [x] Summe stimmt → keine Meldung
- [x] Summe falsch → `SumMismatch` + Korrekturdialog
- [x] Datum nicht ISO-8601 → `BadDate` + Korrekturdialog
- [x] Beträge in Cent `Long`, kein Float-Geld

## 3.4 Lizensierung (ADR-008)
- [x] Testphase: 30 Tage oder 50 Belege, danach NEUE Erfassung pausiert
- [x] Lesen / Suchen / Export / Sicherung auch abgelaufen voll nutzbar
- [x] SKU `belegkasten_lifetime`, Kaufpreis 14,99 €
- [ ] Kauf-Flow mit ernsten App-Konten testen (10.2, ausstehend)

## 3.5 Sicherung
- [x] Format-Header `BKBF1`
- [x] PBKDF2-HMAC-SHA256, 600.000 Iterationen, Salt+IV im Header
- [x] ZIP mit JSON-Manifest + SHA-256-Integrität
- [ ] Wiederherstellung mit echter 100-Beleg-Sicherung (10.3, ausstehend)

## 3.6 Navigation (Flow)
- [x] Erfassen → Prüfen → Suchen → Detail → Einstellungen verdrahtet
- [x] Alle Ziele ohne Lizenz erreichbar (ADR-008)
- [ ] UI-Durchgang 15 Aktionen < 60 s (10.4, ausstehend — Gerät)

## 3.7 Bekannte Lücken (vor Store-Release)
- [ ] `HashEmbedder` ist Platzhalter → MediaPipe EmbeddingGemma 2 tauschen (Spike 0.3 offen)
- [ ] Foto-Suche hinter Beta-Flag; erst freigeben wenn Recall@3 ≥ 50 %
- [ ] E-Rechnung nur PDF-Anhang-Fallback (XML ab Phase D)
- [ ] Kein Gerät angeschlossen → Performance-/RAM-Tests ausstehend

## Offene Nachweise (ausgelagert)
- Spike-Score ≥ 15 pp über Baseline in BERICHT.md eintragen (Voraussetzung für Kaufentscheidung 3.7)
