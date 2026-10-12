# Beta-Anleitung — 50 bis 200 Tester

Stand: 2026-10-11

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
5. In den Einstellungen Testzeitraum-Limit sichtb

## 4. Messung ohne Telemetrie
- Telemetrie ist absichtlich deaktiviert.
- Erwünscht: kurzes Tagebuch pro Tag — wurde der Beleg in < 10 s abgelegt? Wurde er wiedergefunden?
- Bei Absturz: Menü → „Diagnose-Export" in einstelllungen, Datei als ZIP-Anhang melden. Enthält nur lokale Logs, keine Belegdaten.

## 5. Abbruchkriterien für Beta
- Ein Beleg nicht auffindbar in < 2 s
- Erfassung > 60 s für 15 Aktionen
- Sicherung-Wiederherstellung verliert Daten
→ Eine Rückmeldung davon genügt, um die Phase 3 nicht zu veröffentlichen.

## 6. Wichtig für Tester
- Nur Testbelege. Echte Belege erst nach Sicherung.
- Testphase endet nach 30 Tagen/50 Belegen; bestehende Belege bleiben voll nutzbar (Lesen, Suchen, Export, Sicherung).
