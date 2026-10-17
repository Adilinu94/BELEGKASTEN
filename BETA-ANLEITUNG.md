# Beta-Anleitung — 50 bis 200 Tester

Stand: 2026-10-16

> **Noch nicht für die Beta-Testgruppe freigegeben.** Die App ist aktuell ein
> buildbares Gerüst: Aufnahme, Import, Detailnavigation und Wiederherstellung
> sind nicht vollständig implementiert. Die folgenden Aufgaben sind das
> geplante Testprotokoll, kein Nachweis vorhandener Funktionalität.
> Aktuelle Lücken: `ABNAHME-PHASE-3.md`. Nur synthetische Testdaten verwenden.

## 1. Voraussetzungen
- Android ab 8.0 (API 26)
- Installierte APK (Debug-Build reicht für Beta) oder geschlossener Test
- Kein Google-Konto-Zwang nötig; Belege bleiben lokal.

## 2. Installation
```powershell
adb install -r app\app\build\outputs\apk\debug\app-debug.apk
```
Hinweis: Aktuell ist kein Gerät in der Entwicklungsumgebung verbunden —
der Testlauf muss auf eigenem Gerät erfolgen.

## 3. Was getestet werden soll (feste Reihenfolge)
1. Beleg fotografieren → Vorschläge prüfen (Kaufhaus, Baumarkt, Apotheke je 3 Belege)
2. In der Prüfansicht eine falsche Summe/ein falsches Datum korrigieren
3. Wortsuche: „der vom Baumarkt im März"
4. Sicherung exportieren mit Passwort, App neu installieren, Sicherung einlesen
5. In den Einstellungen Testzeitraum-Limit prüfen; nach Ablauf müssen Lesen,
   Suchen, Export und Sicherung weiterhin funktionieren.

## 4. Messung ohne Telemetrie
- Telemetrie ist absichtlich deaktiviert.
- Erwünscht: kurzes Tagebuch pro Tag — wurde der Beleg in < 10 s abgelegt? Wurde er wiedergefunden?
- Geplant, noch nicht vorhanden: Diagnose-Export in den Einstellungen mit
  aggregierten Nutzungszahlen, ohne Beleginhalte. Bis dahin Fehlerablauf,
  Android-Version und Fehlermeldung notieren; keine echten Belegbilder mitsenden.

## 5. Abbruchkriterien für Beta
- Ein Beleg nicht auffindbar in < 2 s
- Erfassung > 60 s für 15 Aktionen
- Sicherung-Wiederherstellung verliert Daten
→ Eine Rückmeldung davon genügt, um die Phase 3 nicht zu veröffentlichen.

## 6. Wichtig für Tester
- Nur Testbelege. Echte Belege erst nach Sicherung.
- Testphase endet nach 30 Tagen/50 Belegen; bestehende Belege bleiben voll nutzbar (Lesen, Suchen, Export, Sicherung).
