# ADR-009 · Sicherung: Schlüsselableitung per PBKDF2 statt Argon2id

**Status:** vorgeschlagen (gleicht die Dokumente an den Code an; gilt, bis der Projektinhaber anders entscheidet)
**Datum:** 17.10.2026

## Kontext

Produktdokument v3 (Abschnitt 8 und 17) und `BAUPLAN.md` nennen für die
verschlüsselte Sicherungsdatei Argon2id. `BackupManager.kt` leitet den Schlüssel
dagegen per PBKDF2-HMAC-SHA256 mit 600.000 Iterationen ab, und `STATUS.md` und
`ABNAHME-PHASE-3.md` beschreiben das so. Dokument und Code widersprachen sich.

Android bringt für Argon2 keine eigene Implementierung mit. Argon2id bräuchte
eine Fremdbibliothek: nativen Code (z. B. argon2kt, libsodium-Bindings) oder eine
reine Java-Fassung (z. B. Bouncy Castle), die auf Mobilgeräten deutlich langsamer ist.

## Entscheidung

Die Sicherung nutzt **AES-256-GCM** mit einem Schlüssel aus **PBKDF2-HMAC-SHA256,
600.000 Iterationen**, Salt 16 Byte und IV 12 Byte im Dateikopf (`BKBF1`).

## Alternativen

1. **Argon2id.** Verworfen für jetzt: neue Abhängigkeit (teils mit nativem Code)
   in einer App, deren Kernversprechen „kein Netz, kleine Angriffsfläche" ist, und
   noch nicht umgesetzt.
2. **PBKDF2-HMAC-SHA256, 600.000 Iterationen.** **Gewählt.** Sie steckt in der
   Android-Standardbibliothek, und der Code ist schon geschrieben. 600.000 ist
   der Wert, den OWASP für PBKDF2-HMAC-SHA256 nennt. OWASP empfiehlt aber als
   erste Wahl Argon2id (mindestens 19 MiB Speicher, 2 Durchläufe, 1 Thread) und
   sieht PBKDF2 als Alternative, etwa wenn FIPS-validierte Verfahren gefordert
   sind. PBKDF2 ist hier also die zweitbeste Wahl, bewusst gewählt gegen den
   Verzicht auf eine Fremdbibliothek.
   Quelle: [OWASP Password Storage Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html), abgerufen 17.10.2026.

## Begründung

- Keine zusätzliche Bibliothek, kein nativer Code in der Lieferkette.
- Schon umgesetzt und mit der Android-Standardbibliothek lauffähig.
- Das Format trägt eine Versionskennung im Kopf (`BKBF1`). Ein späterer Wechsel
  auf Argon2id ist als `BKBF2` möglich.

## Konsequenzen

- PBKDF2 ist nicht speicherhart. Ein Angreifer mit Grafikkarten kann schwache
  Passwörter günstiger durchprobieren als bei Argon2id. Der Code verlangt derzeit
  nur 8 Zeichen; im Export-Dialog sollte eine Passphrase empfohlen werden.
- **Vor der ersten veröffentlichten Version entscheiden, ob Argon2id doch
  gewollt ist.** Danach muss die Wiederherstellung beide Formate lesen, damit
  ältere Sicherungen lesbar bleiben (ADR-008: die Sicherung ist Notfallschutz).
- `BAUPLAN.md` und `PRODUKT-BELEGKASTEN.md` nennen jetzt PBKDF2 und verweisen auf
  diese Entscheidung.
- Die Wiederherstellung ist im Code noch nicht umgesetzt (siehe `STATUS.md`).
