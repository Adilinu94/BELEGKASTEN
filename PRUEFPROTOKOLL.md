# Prüfprotokoll — technische Weiterarbeit 2026-10-16

## Runde 1: Vollständigkeit, Funktionen, Kategorien (Blickwinkel 1–3)

Neue Befunde: 6
- Hoch: `Capture.kt`, `MainActivity.kt`, `Settings.kt` hatten leere Aktionen; Eingangs-, Archiv- und Sicherungsflow fehlten. Umsetzung gestartet.
- Kritisch: `BackupManager.validateAndImport` stellte nichts wieder her; SHA-256 wurde nicht geprüft. Neuer logischer Snapshot und transaktionales Merge implementiert, Tests laufen.
- Kritisch: `DataModule` erlaubte destruktive Downgrades. Entfernt; Migration 1→2 hinzugefügt.
- Hoch: `CheckBeforeSave` akzeptierte unmögliche Daten und überlaufende Summen. Kalender-/Overflow-/Artikelprüfung und Regressionen ergänzt.
- Hoch: `SearchEngine` präsentierte Hash-Ranking als Kernsuche und verlor Filter. FTS4/Text-Feld-Baseline implementiert; Modellfreigabe bleibt offen.
- Hoch: Backup-KDF entsprach nicht dem Bauplan. Neue BKBF2-Dateien verwenden Argon2id; alter BKBF1-Import wird gesondert validiert.

## Runde 2: Ressourcen, Verträge, Plattform (Blickwinkel 4–6)

Neue Befunde: 5
- Hoch: roher DB-Dateiexport verlor WAL-Daten. Logischer Snapshot aus einer Room-Transaktion ersetzt ihn.
- Hoch: alte Sicherungen enthalten absolute Android-Pfade. BKBF1-Leseadapter setzt geprüfte portable Pfade; fehlende Anhänge werden nicht stillschweigend verworfen.
- Mittel: OCR-Client wurde nie geschlossen. Ein Client je Verarbeitung, `finally`-Cleanup.
- Mittel: FileProvider durfte kompletten Cache teilen. Auf Kamera-Unterverzeichnis beschränkt.
- Mittel: LifecycleOwner-Import passte nicht zur vorhandenen Compose-Version. Korrigiert; erneuter Build läuft.

## Runde 3: Fehlerfälle und Zustandsübergänge (Blickwinkel 7–8)

Neue Befunde: 3
- Hoch: Lizenzzähler konnte nach Prozessabbruch hinter DB zurückbleiben. Capture-Prüfung nimmt max(DB-Zahl, lokaler Lebenszeitzähler).
- Hoch: Fehler beim optionalen Zähler nach Commit ließ erfolgreiches Speichern wie Fehlschlag aussehen. DB-Zahl ist maßgeblich, Aggregate blockieren das Commit-Ergebnis nicht.
- Hoch: bei Kopierfehler blieb ein partieller Anhang liegen. Neue Datei vor dem Kopieren in Cleanup-Liste aufgenommen; bestehende Dateien werden niemals überschrieben.

Weitere Runden: UX, Konfiguration, Sicherheit, Datenzugriff, tatsächliche Testqualität und Dokumentkonsistenz folgen nach Build-/Android-Nachweisen.

## Runde 4: UX, Konfiguration, Sicherheit, Datenzugriff (Blickwinkel 9–12)

Neue Befunde: 8
- Kritisch: `BackupManager.validateAndImport` konnte bei Coroutine-Abbruch nach DB-Commit Originale im Cleanup löschen. Dateiinstallation/Commit jetzt bounded `NonCancellable`; jede neue Datei wird vor Commit atomar umbenannt.
- Hoch: `CaptureViewModel.discard` löschte Dateien auch nach bereits abgeschlossenem Commit ohne UI-Bestätigung. Repository prüft Archivreferenzen unter derselben Schreibsperre.
- Hoch: `app/proguard-rules.pro` hielt portable JSON-Datensätze nicht stabil. Explizite Keep-Regeln für Snapshot und Entities hinzugefügt; Release-Nachweis folgt.
- Hoch: `RuleParser.amount` schnitt ungruppierte vierstellige Eurobeträge ab und konnte negative Rückerstattungen als positiven Gesamtbetrag ausgeben. Ganze Beträge und Token-Grenzen plus Regressionen ergänzt; nicht unterstützte Negativbelege erfordern manuelle Bearbeitung.
- Mittel: fehlendes OCR-Datum wurde still durch heutiges Datum ersetzt. Unbekanntes Datum jetzt leer; Original bleibt auch bei OCR-Fehler erhalten.
- Mittel: `ReceiptFiles.resolve` akzeptierte keine `.jpeg`-Anhänge, obwohl Backup sie erlaubte. Vertrag vereinheitlicht.
- Mittel: Fristenworker markierte blockierte Benachrichtigungen als zugestellt und überschreiben konnte zwischenzeitliche Datumskorrekturen. Berechtigungs-/Channel-Prüfung und bedingtes Update ergänzt.
- Mittel: Positionskorrektur war auf einen Gesamtposten beschränkt. Mehrzeilige manuelle Positionen mit stabilen IDs, Mengen und Centprüfung ergänzt.

## Runde 5: Testqualität (Blickwinkel 13)

Neue Befunde: 1
- Hoch: Fake-DAO-Tests prüften keine SQLite-FTS-Syntax. Android-Test fand: `"Bohr"*` liefert keine Präfix-Treffer auf Android FTS4. Auf sichere, aus alphanumerischen Tokens gebildete `Bohr*`-Abfrage korrigiert. Migration 1→2 bestand bereits auf API 36; FTS/Backup werden erneut ausgeführt.

**Noch keine Abschluss-/Store-Freigabe.** Emulator ist kein Mittelklasse-Gerätetest;
synthetische Belege sind kein Recall-/OCR-Akzeptanzset. Keine Kaufhandlung ausgeführt.

## Runde 6: Konfiguration, Sicherheit, Konsistenz (Blickwinkel 10–14)

Neue Befunde: 5
- Hoch: Billing 7 veraltet für reguläre Einreichungen seit 31.08.2026. Auf 8.0.0 mit neuem Produktdetails-Callback/Offer-Token aktualisiert. Quelle: https://developer.android.com/google/play/billing/deprecation-faq und /migrate-gpblv8.
- Store-Blocker: targetSdk 35 reicht im Oktober 2026 nicht für neue Einreichungen (API 36 erforderlich). API-36-/AGP-Migration bleibt ausdrücklich offene Release-Arbeit; kein falscher Store-Bereitschaftsnachweis. Quelle: https://support.google.com/googleplay/android-developer/answer/11926878.
- Hoch: `BackupCrypto.decrypt` vertraute dem plattformabhängigen CipherInputStream-Abschluss. Explizites `Cipher.doFinal` erzwingt GCM-Tag-Prüfung vor Archivimport.
- Mittel: Backup-Passwort ging bei Rotation während der Dateiauswahl verloren. Flüchtiger ViewModel-Zwischenspeicher, keine Persistenz; nach Prozessverlust klarer Neustarthinweis.
- Mittel: Fehler optionaler Diagnose nach erfolgreichem Export/Merge wurde als fehlgeschlagene Kernaktion angezeigt. Aggregate können Erfolg nicht mehr rückwirkend ändern; Dateifehler geben keine externen Rohmeldungen aus.

## Runde 7: Gegenprüfung der Korrekturen (Blickwinkel 4, 7, 9, 13)

Neue Befunde: 2
- Mittel: `SearchViewModel` ließ Diagnosefehler erfolgreiche Suchtreffer ersetzen. Optionales Aggregat jetzt isoliert wie beim Speichern/Export.
- Niedrig: `CsvExport` lud unnötig sämtliche Vektoren/Fristen in den Speicher. Export liest nur Belegzeilen.

Ergänzender Zustandsbefund (hoch): ein erfolgreicher manueller Play-Abgleich ohne SKU ließ einen früheren Kauf ewig aktiv (Erstattung/Kontowechsel). Nur ein erfolgreicher expliziter Abgleich ohne SKU darf jetzt den Kaufcache entfernen; Verbindungs-/Signaturfehler tun das nicht. Lesen, Suchen und Exporte bleiben frei. Store-Test dieser Übergänge weiterhin extern offen.

Testumfang erweitert: Wiederherstellung enthält nun 100 Positionen, 100 Vektoren und 200 Fristen; außerdem geprüft werden aktionsfreie Wiederholung nach Trial-Ende, Schutz referenzierter Originale, Datei-/Vektorkonflikte, Rollback nach vorheriger Einfügung, manipuliertes GCM-Tag und CSV ohne Kauf. UI-Test verwendet eine lokal gezeichnete synthetische Bonabbildung, keine privaten Belege. Neue Tests noch nicht als bestanden zählen, bis der Lauf abgeschlossen ist.
