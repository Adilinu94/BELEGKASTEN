# ADR-008 · Archiv bleibt ohne Zahlung lesbar

**Status:** angenommen
**Datum:** 06.10.2026
**Letzte Aktualisierung:** 10.10.2026 (v2-Abgleich mit PRODUKT-BELEGKASTEN.md)

## Kontext

Das Projekt hat gemessen, dass Abo- und Testfall-Fallen der häufigste Defekt in
den Bewertungen sind. Zugleich gibt es praktisch kein Kaufsignal in
Kritiktexten.

## Entscheidung

Das Archiv (Belege, erkannte Felder, Suche, Export, Sicherungsdatei) bleibt **ohne Zahlung
lesbar und nutzbar**. Nur neue Aufnahmen oder neue Analyse pausieren ohne Lizenz.

## Alternativen

1. **Harter Abo-Stopp.** Verworfen: erzeugt die Falle, die wir vermeiden wollen,
   und sperrt Nutzerdaten aus.
2. **Archiv bleibt frei.** **Gewählt.**

## Begründung

- Vertrauen ist der einzige Unterschied, den ein kleiner Anbieter gegen Google
  und die Großen hat.
- Wer nach Jahren nicht zahlt, darf seine eigenen Daten nicht verlieren. Alles
  andere ist Erpressung mit Kundendaten.
- Das Projekt hat belegt: „ohne Abo" bringt keinen Sterne-Effekt, aber ein
  Abo-Stopp erzeugt Abwanderungs- und Vertrauensschaden.

## Konsequenzen

- Die Lizenzprüfung darf **nie** den Lesezugriff blockieren, nur das Schreiben
  neuer Daten.
- Wir brauchen zwei getrennte Zustände: „Archiv nutzbar" und „Erfassung
  freigeschaltet".
- **Zum Archiv nutzbar gehören ausdrücklich:** Lesen, Suchen, Öffnen einzelner
  Belege, Export für den Steuerberater (CSV, DATEV) und Sicherung/Wiederherstellung
  als Datei. Die Sicherung ist Notfallschutz, kein Bezahlhebel.
- Die Lizenzprüfung darf **nie** den Lese-, Export- oder Sicherungszugriff
  blockieren, nur das Schreiben neuer Daten (Aufnahme, Import, neue Analyse).

## Belege

- `PRODUKT-BELEGKASTEN.md`, `out/reports/BEFUNDE-ZAHLUNGSBELGE.md`
