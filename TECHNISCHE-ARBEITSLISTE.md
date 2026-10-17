# Technische Arbeitsliste — 2026-10-16

Auftrag: offene Punkte gründlich bearbeiten. Vorhandene lokale Spike-Arbeit bleibt erhalten.
Kein Push, keine Store-Veröffentlichung, kein Kauf ohne menschliche Freigabe.

## Umsetzung und Nachweise

- [ ] Datenintegrität: Kalenderdaten, Cent-Overflow, Artikelbezüge, Vektoren; keine destruktiven Migrationen.
- [ ] Versioniertes Room-Schema, FTS4, transaktionales Speichern und Archiv-Snapshot.
- [ ] Kamera-/Bild-/PDF-Eingang, Teilen-Intent, Feldkorrektur, Speichern mit Lizenzprüfung.
- [ ] Bedienbare Navigation, Belegdetail, Öffnen/Teilen der Originale, Fehler- und Leerzustände.
- [ ] Such-Baseline auf gespeichertem OCR-Text; keine Hash-Suche als semantisches Modell ausgeben.
- [ ] Verschlüsselte Sicherung: Argon2id, Datei-Hashes, Größen-/Pfadprüfung, konfliktfreie Wiederherstellung.
- [ ] Passwort-/Dateidialoge, CSV-Export und freiwillige inhaltsfreie Diagnose.
- [ ] Testphasen-Zustandsübergänge, Play-Billing-Kauf und Wiederherstellung; Archiv immer frei.
- [ ] Fristenlogik und Ressourcen-/Lifecycle-Prüfung vorhandener Worker/OCR.
- [ ] Regressionen, Datenbank-/Wiederherstellungsintegration, Debug-/Release-Build und Manifestprüfung.
- [ ] Abschließende Prüfung aus 14 Blickwinkeln; Status/Abnahme/Beta-Anleitung aktualisieren.

## Nicht als erledigt ausgeben

- Echte geschwärzte Belege und unabhängig beschriftete Suchaufgaben fehlen.
- Keine echten Foto-Paare und kein Android-Zielgerät: OCR, Auto-Capture, Geschwindigkeit,
  RAM, Flugmodus, Netzwerkmitschnitt und Wiederherstellung auf Zweitgerät bleiben Geräteabnahmen.
- Modell-GO offen: synthetischer CPU-Lauf +10 pp statt geforderter +15 pp. Android-Modell,
  PAD und Gemma-4-Extraktion dürfen nicht mit Platzhalter-Dateien vorgetäuscht werden.
- Play-Console-Produkt, Lizenztester, Signierung, Store-Texte und menschliche Release-Freigabe fehlen.
- Interviews, Markt-/Prototypentests und spätere Phasen 4–7 sind keine bereits umgesetzten Funktionen.
- DATEV benötigt fachliche Konten-/Steuerkonfiguration; generischer CSV-Export ist kein DATEV-Nachweis.

## Prüfprotokoll

Nach jeder zusammenhängenden Änderung Tests/Build; am Ende Anforderungen gegen Quellcode
und Ergebnisse abgleichen. Externe Abnahmen bleiben offen, auch bei grünen JVM-Tests.
