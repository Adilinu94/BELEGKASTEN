# Abnahme-Checke — Phase 3 (Beta-App)

Stand: 2026-10-11 · Status: Kernlogik vorhanden, Hauptschleife nicht verdrahtet, Hardware-Test offen
Korrigiert 2026-10-16 nach Abgleich mit dem Code (siehe STATUS.md „Noch nicht verdrahtet"): Haken, die der Code nicht stützt, sind zurückgenommen.

## 3.1 Projekt & Build
- [x] Gradle-Wrapper 8.9 vorhanden (`./gradlew`)
- [x] AGP 8.7.3, Kotlin 1.9.24, KSP, Hilt, Room 2.6.1
- [x] Java 17 / JVM-Target 17 in allen Modulen konsistent
- [x] `./gradlew assembleDebug` → BUILD SUCCESSFUL
- [x] `./gradlew testDebugUnitTest` → 6/6 Tests grün
- [x] Module: core/{model,database,data,ocr,embed,search,backup,license,ui}, feature/{capture,receipts,search,settings}

## 3.2 Offline-Garantie (Kernversprechen)
- [x] Kein `android.permission.INTERNET` im Quell-Manifest
- [ ] Berechtigungen der gebauten APK geprüft (`aapt dump permissions`): Billing, Asset Delivery und ML Kit können Einträge mitbringen
- [ ] Modell-Auslieferung: unter `assets/embed_model/` liegt nur ein README, es gibt kein Asset-Pack-Modul; Play Asset Delivery (install-time) ist noch einzurichten
- [x] ML Kit gebundelt-Variante (`text-recognition`), keine Play-Services-Version (`document-scanner` steht nur im Versionskatalog, ungenutzt)
- [ ] Verifikation im Flugmodus auf Gerät (10.1, ausstehend)

## 3.3 Prüfregeln (CheckBeforeSave)
- [x] Summe stimmt → keine Meldung
- [x] Summe falsch → `SumMismatch` (Logik + Test)
- [x] Datum nicht ISO-8601 → `BadDate` (Logik + Test)
- [x] Beträge in Cent `Long`, kein Float-Geld (`RuleParser.toCents` rechnete bis 2026-10-16 über `Double`, siehe Zweig `fix/geld-suche-spike`)
- [ ] `CheckBeforeSave` wird vor dem Speichern aufgerufen und zeigt einen Korrekturdialog (kein Aufrufer, keine Dialog-UI)

## 3.4 Lizensierung (ADR-008)
- [x] Testphase-Logik: 30 Tage oder 50 Belege (`LicenseManager.captureEnabled`)
- [ ] Testphase an die Erfassung angebunden: `recordReceiptAdded()` hat keinen Aufrufer, die Erfassen-Buttons sind nicht gesperrt
- [x] Lesen / Suchen / Export / Sicherung nie lizenzabhängig (`archiveUsable()` ist immer wahr)
- [ ] Kauf: nur die Konstante `SKU_LIFETIME`; kein `BillingClient`, `markPurchased()` hat keinen Aufrufer
- [ ] Kauf-Flow mit ernsten App-Konten testen (10.2, ausstehend)

## 3.5 Sicherung
- [x] Format-Header `BKBF1`
- [x] PBKDF2-HMAC-SHA256, 600.000 Iterationen, Salt+IV im Header
- [x] ZIP mit JSON-Manifest + SHA-256 je Datei (beim Einlesen nicht verglichen; der GCM-Tag schützt das Ganze)
- [ ] Wiederherstellung umgesetzt: `validateAndImport` prüft nur Passwort und Manifest; Export- und Wiederherstellen-Button sind leer
- [ ] Konsistente Datenbankkopie: `belegkasten.db` wird ohne WAL-Abgleich gelesen (Room-Journal-Modus prüfen)
- [ ] Speicherbedarf: ZIP und Verschlüsselung laufen komplett im RAM, bei großen Bildarchiven Absturzgefahr
- [ ] Wiederherstellung mit echter 100-Beleg-Sicherung (10.3, ausstehend)

## 3.6 Navigation (Flow)
- [ ] Erfassen → Prüfen → Suchen → Detail → Einstellungen verdrahtet: vier Ziele im `NavHost`, aber kein `navigate` und keine Leiste; Prüf- und Detailansicht gibt es nicht
- [ ] Alle Ziele ohne Lizenz erreichbar (ADR-008): keine Lizenzsperre im Code, die Erreichbarkeit hängt an der fehlenden Navigation
- [ ] Hauptschleife Erfassen → Texterkennung → Prüfen → Speichern → Embedding (keine Aufrufer, siehe STATUS.md)
- [ ] UI-Durchgang 15 Aktionen < 60 s (10.4, ausstehend — Gerät)

## 3.7 Bekannte Lücken (vor Store-Release)
- [ ] `HashEmbedder` ist Platzhalter → MediaPipe EmbeddingGemma 2 tauschen (Spike 0.3 offen)
- [ ] Foto-Suche hinter Beta-Flag; erst freigeben wenn Recall@3 ≥ 50 %
- [ ] E-Rechnung nur PDF-Anhang-Fallback (XML ab Phase D)
- [ ] Kein Gerät angeschlossen → Performance-/RAM-Tests ausstehend

## Offene Nachweise (ausgelagert)
- Spike-Score ≥ 15 pp über Baseline in BERICHT.md eintragen (Voraussetzung für Kaufentscheidung 3.7), gemessen mit echten Belegen und echtem Modell, nicht mit dem synthetischen Korpus und nicht mit dem Hash-Platzhalter
