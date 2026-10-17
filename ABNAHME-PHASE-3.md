# Abnahme-Checkliste — Phase 3 (Android-Beta)

Stand: 2026-10-16 · **nicht abgenommen**. Buildbares Gerüst; Kernflows und
Hardware-Nachweise fehlen. Die frühere „code-seitige Abnahme“ ist korrigiert.

## 3.1 Projekt & Build

- [x] Gradle-Wrapper 8.9; AGP 8.7.3, Kotlin 1.9.24, KSP, Hilt, Room 2.6.1.
- [x] Java/JVM-Target 17 in allen Modulen.
- [x] `testDebugUnitTest assembleDebug` am 2026-10-16 erfolgreich.
- [x] 12 JVM-Tests grün (Prüfregeln 3, Parser 5, Suchlogik 4).
- [x] Module `core/{model,database,data,ocr,embed,search,backup,license,ui}` und
  `feature/{capture,receipts,search,settings}` vorhanden.

## 3.2 Offline & Modell-Auslieferung

- [x] Quellmanifest ohne `android.permission.INTERNET`.
- [x] ML Kit Text Recognition als gebündelte Variante eingebunden.
- [ ] Zusammengeführtes Manifest und Netzwerkmitschnitt prüfen.
- [ ] Start, Erfassen und Suchen im Flugmodus auf Gerät verifizieren.
- [ ] Echter install-time-PAD-Pack: bislang nur Bibliothek + Asset-README,
  kein Asset-Pack-Modul und kein Modell.

## 3.3 Eingang → OCR → Prüfung → Speichern

- [ ] Kameraaufnahme / Auto-Capture und Galerie-/Datei-Import implementieren.
- [ ] Teilen-Intent tatsächlich verarbeiten (Filter existieren, Handler fehlt).
- [ ] OCR-/Parser-Aufruf aus der Erfassung verdrahten.
- [ ] Bestätigungs- und Korrekturansicht für Felder implementieren.
- [ ] Beleg, Positionen und Embedding atomar speichern.
- [x] Prüfregeln markieren abweichende Positionssumme, leere Pflichtfelder und
  falsch geformte Datumsstrings.
- [x] Beträge als Cent `Long`; Parser nutzt BigDecimal statt Double.
- [ ] Kalenderdatum prüfen: der aktuelle Regex akzeptiert z. B. `2025-02-30`.
- [ ] E-Rechnungs-PDF als dauerhaften Anhang speichern und öffnen.

## 3.4 Navigation & Suche

- [ ] Bedienbare Navigation zu Erfassen / Belege / Suchen / Einstellungen.
- [ ] Belegdetail und Öffnen aus Treffer-/Belegliste.
- [x] Suchlogik hat Rückfall ohne Händlertreffer, mit Regressionstests.
- [ ] Echte semantische Suche: `HashEmbedder` ist weiterhin Platzhalter.
- [ ] FTS4-/Feld-Baseline und Modellvergleich auf echten Belegen.
- [ ] Foto-Suche nur nach bestandenem Bild-Spike (Recall@3 ≥ 50 %).
- [ ] UI-Durchgang und Such-/Erfassungszeiten auf Gerät messen.

## 3.5 Lizenz (ADR-008)

- [x] Testphase (30 Tage / 50 Belege) als Manager-Logik vorhanden.
- [x] `archiveUsable()` ist immer wahr; SKU `belegkasten_lifetime` definiert.
- [ ] Startzeit/Zähler und Erfassungssperre tatsächlich im Kernflow verdrahten.
- [ ] Play-Billing-Kauf, Bestätigung und Kaufwiederherstellung implementieren/testen.
- [ ] Lesen, Suchen, Export und Sicherung bei abgelaufener Testphase im UI testen.

## 3.6 Sicherung & Datenintegrität

- [x] Exportcode für `BKBF1`, AES-256-GCM und PBKDF2-HMAC-SHA256 (600.000).
- [x] ZIP-Export enthält Manifest und SHA-256-Werte.
- [ ] KDF-Widerspruch auflösen: Produktdokument/Bauplan verlangen Argon2id,
  aktueller Code verwendet PBKDF2 (keine dokumentierte Entscheidungsänderung).
- [ ] Konsistenter Datenbanksnapshot einschließlich ausstehender WAL-Schreibvorgänge.
- [ ] Bilder **und PDF-Anhänge** vollständig sichern; Pfade auf Zweitgerät abgleichen.
- [ ] ZIP-Inhalte, Datei-Hashes und Schema-Version tatsächlich validieren.
- [ ] Wiederherstellen implementieren: `validateAndImport()` liest derzeit nur Manifest.
- [ ] Passwort-/Dateidialoge und Fehlerbehandlung im UI verdrahten.
- [ ] Sicherung mit 100 Belegen auf Zweitgerät wiederherstellen und vergleichen.
- [ ] Destruktiven Downgrade-Pfad prüfen: `DataModule` aktiviert derzeit
  `fallbackToDestructiveMigrationOnDowngrade()` trotz gegenteiligem Kommentar.

## 3.7 Weitere offene Nachweise

- [ ] Lokales Tagebuch und freiwilliger Diagnose-Export (nur Aggregate).
- [ ] Geräte-Matrix, RAM, Paketgröße und Erkennungsqualität.
- [ ] Spike-Go/No-Go anhand echter Belege, nicht synthetischer Harness-Ergebnisse.

Der echte CPU-Runner und seine Grenzen stehen in
`spike/harness/EMBEDDING_SPIKE_MODELL.md`. Ein grüner Build allein erfüllt
keines der noch offenen End-to-End-Abnahmekriterien.
