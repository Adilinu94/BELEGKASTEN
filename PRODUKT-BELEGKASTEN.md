# Belegkasten — Produktdokument vor dem Bau

**Stand: 10.10.2026 (v3) · Verfasser: Produktentwicklung**
Dieses Dokument wird geschrieben, **bevor** eine Zeile Code entsteht. Es denkt
die Idee zu Ende, benennt die Risiken und legt fest, woran wir messen. Die
Struktur passt auf jede Idee aus `GEMMA-IDEEN-10.md`.

*v3 ist überarbeitet nach externem Review vom 10.10.2026. v3 löst die offenen
Widersprüche auf: Offline-Auslieferung (Abschnitt 8), Foto-Suche als
Beta-Versprechen (Abschnitt 1 und 4), E-Rechnung als Fallback in Phase A
(Abschnitt 5 und 16), Messkonzept ohne Telemetrie (Abschnitt 13),
Schwellenwerte in Abschnitt 15, Testphase und Verschlüsselung (Abschnitt 10
und 8). Was sich je Abschnitt geändert hat, steht im Anhang am Ende.*

**Technische Grundlage:** EmbeddingGemma 2 (on-device, multimodal) plus Gemma 4
(für Antworten). Alles läuft auf dem Gerät.

---

## 1 · Pressemitteilung (zuerst geschrieben, nach der Regel „vom Kunden her denken")

*v3, Stand 10.10.2026. Der Kern ist jetzt das Wiederfinden, weil „lokal, kein Konto" und Garantie-Fristen schon kostenlos zu haben sind (Abschnitt 11). Was davon noch nicht bewiesen ist, steht in Abschnitt 4. Die Gegenstand-Foto-Suche steht bewusst konditional im Text: Sie ist ein Beta-Versprechen, kein Kaufgrund (Begründung Abschnitt 4).*

> **Belegkasten: Beschreib den Beleg, die App findet ihn**
>
> *Eine Beleg-App für Android, die sich merkt, was auf deinen Belegen steht, und sie nach Beschreibung wiederfindet. Alles bleibt auf dem Handy: kein Konto, keine Cloud, einmal bezahlt.*
>
> Wer nach einem Defekt die Rechnung braucht, weiß oft weder Händler noch Datum. Dann beginnt das Suchen in der Schublade, in der Fotogalerie, im Mail-Postfach. Garantie-Apps legen Belege ab und zählen Fristen, aber wer den Beleg nicht findet, hat nichts davon. Belegkasten liest jeden Beleg auf dem Handy und findet ihn, wenn man beschreibt, was man sucht: „der vom Baumarkt im März" oder „die Bohrmaschine". Belege nimmt man mit der Kamera auf oder teilt sie als PDF aus Mail und Browser direkt in die App. Wer will, bekommt vor Ablauf von Garantie und Rückgabefrist eine Erinnerung. Als experimentelle Zusatzfunktion testet die App außerdem: Wer den Händler vergessen hat, fotografiert den Gegenstand, und die App schlägt die passende Rechnung vor. Es gibt kein Konto und keine Cloud, und die App kommt ohne Internet-Berechtigung aus. Eine Sicherung legt man als Datei selbst ab und spielt sie auf einem neuen Handy wieder ein.
>
> „Ich habe jahrelang Zettel in Schuhkartons gesammelt und nie gefunden, was ich brauchte", sagt die Entwicklerin. „Belegkasten sortiert nicht, es erinnert sich einfach."
>
> Belegkasten ist ab sofort im Google Play Store erhältlich. Einmalig 14,99 Euro (Zielpreis, noch ungetestet), kein Abo. Das Archiv bleibt auch ohne Zahlung lesbar, nur neue Belege pausieren. Keine versteckten Kosten.

**FAQ zur Pressemitteilung (die Fragen, die ein Kunde stellen würde)**

- **Verlassen meine Belege das Handy?** Nein. Es gibt keinen Server. Die Erkennung und die Suche rechnen auf dem Gerät.
- **Brauche ich ein Konto?** Nein.
- **Brauche ich Internet?** Nein. Die App fordert keine Internet-Berechtigung. Erkennung und Suche laufen auf dem Gerät.
- **Was passiert, wenn ich nicht zahle?** Das Archiv bleibt lesbar. Nur das Aufnehmen neuer Belege pausiert.
- **Was, wenn mein Handy kaputtgeht oder ich wechsle?** Du sicherst dein Archiv als Datei an einem Ort deiner Wahl und spielst sie auf dem neuen Handy wieder ein. Ohne Konto gibt es keine automatische Wiederherstellung, die Sicherung liegt bei dir.
- **Es gibt kostenlose Garantie-Apps. Wozu Belegkasten?** Kostenlose Apps legen Belege ab und zählen Fristen, gesucht wird dort über Händler, Kategorie oder Volltext. Belegkasten findet auch, wenn man den Händler nicht mehr weiß. Wer nur Fristen braucht, kommt mit einer kostenlosen App aus.
- **Was kann die Foto-Suche (Gegenstand fotografieren)?** Das ist eine experimentelle Beta-Funktion, kein Kaufgrund. Sie schlägt eine passende Rechnung vor, verspricht aber keinen Treffer. Ob sie zuverlässig genug ist, entscheidet der Test in Abschnitt 4 und 15. Scheitert der Test, wird sie gestrichen.
- **Erkennt es Kassenzettel, die verblassen?** Ja, solange sie lesbar sind. Verblasster Druck ist eine echte Grenze, deshalb ersetzt Belegkasten den Zettel nicht, es merkt ihn sich.
- **Und Steuer?** Belegkasten ist kein Buchhaltungsprogramm. Es erinnert an Fristen und kann Belege für den Steuerberater exportieren.

---

## 2 · Problem und Warum jetzt

**Das Problem.** Belege gehen verloren. Wenn man sie braucht, sind sie weg.
Suchen kostet Zeit und endet oft ohne Ergebnis. Die vorhandenen Scanner-Apps
lösen das Speichern, aber nicht das **Wiederfinden**, und viele drängen zum
Abo. Auch kostenlose lokale Garantie-Apps legen Belege ab und zählen Fristen,
gesucht wird dort über Händler, Kategorie oder Volltext (Abschnitt 11).

**Warum jetzt.** Drei Dinge sind neu zusammen:

1. **EmbeddingGemma 2** macht multimodale Suche erstmals **auf dem Gerät**
   möglich. Vorher bedeutete KI-Suche: Cloud. Das Modell ist seit dem
   06.10.2026 veröffentlicht (Apache-2.0). Messungen auf Mittelklasse-Geräten
   fehlen noch.
2. **Der Gesetzgeber treibt es.** Seit 2025 müssen Unternehmen E-Rechnungen
   empfangen (XRechnung, ZUGFeRD). Das betrifft vor allem die zweite
   Zielgruppe. Ab 2028 soll die Pflicht zum Papierbon entfallen und durch die
   Pflicht ersetzt werden, einen digitalen Beleg bereitzustellen, zum Beispiel
   per QR-Code am Kundendisplay. Papier gibt es dann auf Verlangen. Das ist ein
   Gesetzentwurf, den das Kabinett im September 2026 beschlossen hat, kein
   verabschiedetes Gesetz, und Handels- und Handwerksverbände kritisieren ihn.
   Folgerung: Mehr Belege kommen digital an. Das stärkt Teilen-Knopf und
   Datei-Import und macht die Kamera als Eingang weniger wichtig.
3. **Vertrauen ist knapp.** Finanzdaten in der Cloud sind vielen unangenehm.
   Eine lokale Lösung ist die Grundlage des Vertrauens. Als alleiniger
   Verkaufsgrund reicht sie nicht mehr, weil es kostenlose lokale Apps gibt
   (Abschnitt 11).

**Was gemessen ist und was nicht.**

| Aussage | Status |
|---|---|
| Viele Anbieter zwingen zur Cloud oder zum Abo | belegt (Marktrecherche) |
| E-Rechnungspflicht seit 2025 | belegt (BMF, Gesetz) |
| Digitaler Kassenbon ab 2028 | Gesetzentwurf des Kabinetts, nicht beschlossen (Bundesregierung, Presse) |
| Menschen zahlen für ein Belegfinden-Tool | **nicht belegt**, muss getestet werden |
| Wie groß der Markt ist | **nicht gemessen** |
| Kostenlose lokale Garantie-Apps existieren | belegt (READMEs, GitHub-Recherche 10.10.2026) |
| Ein Projekt bietet semantische Belegsuche fertig auf dem Gerät | in der Recherche **nicht gefunden** (nicht vollständig) |
| Foto des Gegenstands findet den Beleg | **nicht belegt**, Test nötig |
| Belege durchblättern, App nimmt sie auf | **nicht belegt**, kein Projekt gefunden |
| Modell läuft flüssig auf Mittelklasse-Handys | **nicht gemessen** |

---

## 3 · Zielgruppe

**Erste Zielgruppe (eng):** Privatpersonen, die viel kaufen und Belege verlieren.
Der Aufhänger ist die **Garantie**, nicht die Steuer. Der Moment, in dem die App
gebraucht wird, ist der Defekt: „Das Gerät ist kaputt, wo ist die Rechnung?"

Wer nur Fristen zählen will, ist mit kostenlosen Apps bedient. Gemeint ist, wer den Beleg im Ernstfall finden muss, auch wenn Händler und Datum vergessen sind.

**Persona:** „Mara, 38, zwei Kinder, kauft oft online und im Laden. In der
Schublade liegen Belege, sortiert nach Zufall. Wenn die Waschmaschine streikt,
sucht sie eine halbe Stunde."

**Wie erreichen wir sie?** Noch offen (Abschnitt 17). Gesucht ist ein Kanal, über den Privatpersonen mit genau diesem Problem auffindbar sind.

**Zweite Tür (später):** Selbstständige und kleine Betriebe. Dort liegt das
Geld, dort liegt aber auch die Pflicht (Aufbewahrung, Export). Diese Tür wird
geöffnet, wenn die erste steht.

**Bewusst nicht erste Zielgruppe:** große Firmen. Zu langsam, zu viel
Integration, zu viel Haftung.

---

## 4 · Positionierung

*v2, Stand 10.10.2026. Der Unterschied ist nicht mehr „lokal, kein Konto, kein Abo". Das gibt es kostenlos (Abschnitt 11). Der Unterschied ist das Wiederfinden.*

**Für** Menschen, die Belege aufheben, aber im Ernstfall nicht wiederfinden,
**die** schnell eine Rechnung brauchen (Garantie, Umtausch, Steuer),
**ist Belegkasten** ein Beleg-Gedächtnis für das Handy,
**das** Belege nach Beschreibung findet („der vom Baumarkt im März"),
**anders als** Garantie-Apps (Ablage, Fristen, Suche über Händler und Kategorie) und Scanner-Apps (Aufnahme, Ablage),
**weil** der Index auf dem Gerät liegt: ohne Konto, ohne Cloud, ohne Internet-Berechtigung.

**Der eine Satz:** Belegkasten sortiert nicht, es erinnert sich.

**Was wir nicht mehr als Unterschied behaupten:** „lokal, kein Konto" und die Garantie-Erinnerung. Beides bleibt Pflicht und Grundlage des Vertrauens, trägt den Kaufgrund aber nicht allein. „Kein Abo" unterscheidet nur noch von Bezahl-Anbietern.

**Was wir behaupten und wie es bewiesen wird** (Beweise sind Vorschläge, die Zahlen sind noch zu bestätigen)

| Behauptung | Status | Beweis |
|---|---|---|
| Findet Belege nach Beschreibung | unbelegt | Technik-Spike: richtiger Beleg unter den ersten 3 Treffern, gemessen an etwa 100 echten deutschen Belegen, im Vergleich zu Volltext plus Feldern (Baseline) |
| Findet den Beleg per Foto des Gegenstands (Beta) | unbelegt, riskant: Kassenzettel nennen oft Abkürzungen oder Modellnummern statt Beschreibungen | Test an etwa 30 echten Paaren (Gegenstand, Beleg). Schwelle: 50 % Recall@3 (Abschnitt 15). **Wird bei Scheitern gestrichen, steht nicht als Kaufgrund in der Kommunikation.** |
| Läuft ohne Netz | Ziel | Phase-A-Kriterium: Berechtigungsliste und Netzwerkmitschnitt (Abschnitt 8 und 16) |
| Belege vor der Kamera durchblättern, App nimmt sie auf | nicht in der Pressemitteilung | In keinem gefundenen Projekt umgesetzt (Abschnitt 8). Erst nach dem Prototyp wieder aufnehmen. |
| Einmalig 14,99 Euro (Zielpreis) | Preis nicht getestet | Concierge- und Vorverkaufstest (Abschnitt 14 und 17) |

---

## 5 · Kernfunktionen und wo Gemma wirkt

Priorisiert nach MoSCoW. Die Einstufung beschreibt das fertige Produkt, die Reihenfolge der Phasen steht in Abschnitt 16. „Muss" heißt: ohne diese Funktion ist das Produkt unverkäuflich. Alles mit Phasenvermerk in Klammern kommt später, zählt aber zum Muss-Umfang des fertigen Produkts.

| Priorität | Funktion | Wie Gemma wirkt |
|---|---|---|
| **Muss** | Aufnahme per Kamera mit Auto-Capture je Beleg | nicht Gemma: Scanner offen (ML Kit Document Scanner oder eine Offline-Alternative, Abschnitt 8) |
| **Muss** | Teilen-Knopf aus jeder App, Import aus Galerie und Dateien | nicht Gemma: Aufnahme ohne Kamera, holt vorhandene Belege rein (Kaltstart) |
| **Muss** | Erkennen: Betrag, Datum, Empfänger, Kategorie | feste Felder per Regel, Gemma 4 für Händler, Kategorie und Positionen. Code prüft, Nutzer bestätigt markierte Felder |
| **Muss** | Suchen mit Worten: „der Beleg vom Baumarkt im März" | Filter über Felder plus **EmbeddingGemma 2**: Bedeutung statt Stichwort. Vorher gegen Volltext messen. |
| **Muss** | Alles auf dem Gerät, kein Konto | Kern der Datenschutz-Zusage, ohne Internet-Berechtigung |
| **Muss** | Sicherung und Wiederherstellung als Datei | kein Gemma: ohne Konto sonst kein Schutz vor Handyverlust |
| **Muss (ab Phase C)** | Garantie-Erinnerung | Fristen mit editierbaren Standardwerten (Belege nennen selten eine), Gemma 4 liest eine Frist, wenn sie dasteht, die App meldet sich |
| **Muss (ab Phase D)** | E-Rechnung einlesen (XRechnung, ZUGFeRD) | strukturierte Daten, kein OCR nötig. In Phase A nur als Fallback über Teilen-Knopf: PDF wird als Anhang gespeichert und Felder von Hand bestätigt, das XML-Parsing kommt in Phase D |
| **Soll** | Zeigen statt tippen: Gegenstand fotografieren, Beleg finden (Beta-Funktion) | **Multimodales Embedding** (Bild plus Text im selben Raum). Unbelegt, Test nötig (Abschnitt 4). Als Beta gekennzeichnet, kein Kaufgrund |
| **Soll** | Fragen: „Wie viel habe ich für Werkzeug ausgegeben?" | Gemma 4 wählt ein Werkzeug (suchen, summieren), die Zahl kommt aus SQL, Antwort mit Quellen |
| **Soll** | Doppelte Belege erkennen | Embedding-Vergleich |
| **Soll** | E-Rechnung und PDF einlesen (vollständig, Phase D) | strukturierte Daten, kein OCR nötig (Phase D, vor allem zweite Zielgruppe) |
| **Soll** | Dauer-Aufnahme: Belege vor der Kamera durchblättern, App nimmt selbst auf | nicht Gemma; in keinem gefundenen Projekt umgesetzt, erst nach dem Prototyp |
| **Kann** | E-Mail-Postfach einlesen | der größte Reibungs-Killer |
| **Kann** | Abgleich mit Bankumsätzen, Lücken melden | Fuzzy-Match über Embeddings |
| **Kann** | Export für den Steuerberater (CSV, DATEV) | für die Business-Tür |
| **Kann** | Stapel-Foto mit automatischer Trennung | Bildanalyse |

**Der Kern in einem Satz:** Ohne Gemma gäbe es keine „Suche nach Bedeutung", kein
„Zeigen statt tippen", kein „Fragen". Ohne Gemma wäre es eine weitere lokale Beleg- und Garantie-App unter vielen kostenlosen (Abschnitt 11).

---

## 6 · Nutzerfluss (die drei Momente, die zählen)

**Moment 1 · Erfassen (muss mühelos sein)**
Beleg vor die Kamera halten: Die App erkennt je Beleg automatisch den richtigen
Moment und nimmt auf (Auto-Capture je Beleg), kurze Vibration. Oder aus Mail und
Browser teilen, oder aus der Galerie holen. Kein Sortieren, nur markierte Felder
werden bestätigt. Das Durchblättern eines Stapels in einem Zug (Dauer-Aufnahme)
ist eine spätere Soll-Funktion und kein Teil von Moment 1.

**Moment 2 · Wiederfinden (muss sofort sein)**
„Beleg vom Baumarkt im März“ tippen. Treffer in Sekunden. Die Foto-Variante
(Gegenstand fotografieren) ist eine Beta-Funktion und gilt nur, wenn der Test
besteht (Abschnitt 4).

**Moment 3 · Erinnern (muss überraschen)**
Wochen später eine Mitteilung: „Garantie auf die Waschmaschine läuft in 3 Wochen
ab." Das ist der Grund, warum man bleibt. Die Frist stammt meist aus einem Standardwert, den der Nutzer ändern kann.

---

## 7 · Was bewusst NICHT gebaut wird (Non-Goals)

- **Keine Buchhaltung.** Kein doppelter Buchungssatz, keine
  Umsatzsteuer-Voranmeldung, kein Bankabgleich als Kern. Das machen Lexoffice
  und sevdesk.
- **Keine Cloud, kein Konto.** Das ist keine Funktion, das ist die Grundlage.
- **Keine Cloud-Sicherung.** Die Sicherung ist eine Datei, die der Nutzer selbst ablegt.
- **Kein Abo für Privatnutzer.** Das Archiv bleibt immer lesbar. Für Business gibt es später eine Jahreslizenz (Abschnitt 10).
- **Keine Mehrbenutzer im ersten Schritt.** Kein Teilen, keine Teams.
- **Kein Web-Dashboard im ersten Schritt.**
- **Keine Werbung.**

---

## 8 · Technik und Architektur (alles auf dem Gerät)

*v2, Stand 10.10.2026. „Belegt" heißt: steht in der Dokumentation oder im README des genannten Projekts, selbst nicht ausprobiert. „Zu messen" heißt: gehört in den Technik-Spike vor dem Bau.*

**Grundsatz:** Das Modell liest und findet, der Code rechnet und prüft. Beträge liegen in Cent in SQLite, Summen kommen aus SQL, nie aus dem Modell. Dasselbe Muster nutzen [Cesto](https://github.com/mbahmani90/Cesto) („Numbers come from SQL, not from the LLM's arithmetic") und [invoice-rag](https://github.com/lruizap/invoice-rag).

**Aufnahme**
- Teilen-Knopf über Android-Intent-Filter (PDF und Bild). Optional: Ordner beobachten (Storage Access Framework) für Scanner-PDFs.
- Scanner, **Entscheidung offen**, zwei Kandidaten:
  - [ML Kit Document Scanner](https://developers.google.com/ml-kit/vision/doc-scanner): Auto-Capture, Seitenlimit, Galerie-Import abschaltbar. Oberfläche und Modelle kommen über die Google Play-Dienste (belegt). Ob der Erstdownload Netz braucht, nennt die Seite nicht (**zu messen**). Ohne Play-Dienste (etwa auf Geräten ohne Google-Dienste) ist er vermutlich nicht nutzbar (Folgerung, **zu prüfen**).
  - [open-android-doc-scanner](https://github.com/phillip9933/open-android-doc-scanner) (Apache-2.0): offline, gebündeltes Modell, keine Netzwerkberechtigung, automatische Mehrseiten-Aufnahme (belegt). Aber Version 0.1.0-rc11, die Abnahme mit echter Kamera steht laut README aus.
- **Dauer-Aufnahme** (Belege vor der Kamera durchblättern): In keinem gefundenen Projekt umgesetzt. Auto-Capture je Dokument gibt es, einen fortlaufenden Stapel nicht. Das ist ein Prototyp-Risiko, kein Zukauf-Baustein, und gehört in den Spike.

**Verstehen (Erkennung)**
- Texterkennung: [ML Kit Text Recognition](https://developers.google.com/ml-kit/vision/text-recognition/v2/android), **gebündelte** Variante (etwa 4 MB je Schrift und Architektur, sofort nutzbar). Die ungebündelte Variante lädt das Modell über die Play-Dienste nach und braucht dafür eine Verbindung. Deutsch und Umlaute nennt die Produktseite nicht ausdrücklich (**zu messen**, Testset).
- Feste Felder zuerst per Regel: Datum, Gesamtsumme und Belegnummer aus festen Zeilen. Gemma 4 für Händler, Kategorie, Positionen und Fristen.
- **Prüfen vor dem Speichern:** Summe der Positionen gegen Zwischensumme, Beträge in Cent, Pflichtfelder, Datumsformat. Schlägt eine Prüfung fehl, wird das Feld markiert und der Nutzer bestätigt, bevor gespeichert wird.
- E-Rechnung: XML direkt lesen, ohne Texterkennung. Entweder [Mustang](https://github.com/ZUGFeRD/mustangproject) (Apache-2.0, Java, Java 21 empfohlen, Android-Tauglichkeit auf der Seite nicht genannt) oder ein eigener minimaler Parser für die wenigen Felder. Testrechnungen: [ZUGFeRD/corpus](https://github.com/ZUGFeRD/corpus) (Apache-2.0). Phase D.

**Einbetten**
- EmbeddingGemma 2 über MediaPipe `tasks-retrieval` (UniversalEmbedder, SemanticRetriever) oder LiteRT-LM. Referenzen: die Android-Beispiele in [mediapipe-samples PR #695](https://github.com/google-ai-edge/mediapipe-samples/pull/695) (gemergt am 06.10.2026) und [eg2-media-search](https://github.com/xueweijian/eg2-media-search) (früh, ohne Messwerte).
- Varianten laut [Modellkarte](https://huggingface.co/google/embeddinggemma-2): 270M nur Text, 440M Text und Bild, 570M Text und Audio, 740M alles. Das Google-Beispiel nutzt ein Modell mit etwa 470 MB, Gewichte waren zum Stand des PR noch nicht öffentlich. Welche Variante wir laden und wie groß der Download wird, ist **zu messen**.
- 256 Dimensionen (Matrjoschka). Nach dem Kürzen normalisieren. Abfrage und Beleg müssen dieselbe Dimension haben. 128 Dimensionen nicht für die Bildsuche (laut Modellkarte deutlich schlechter).
- Texte mit Task-Präfix einbetten, sonst sinkt die Qualität: Abfrage `task: search result | query: …`, Dokument `title: … | text: …`.
- **Nicht in float16 rechnen** (laut Modellkarte NaN oder stillschweigend schlechtere Embeddings), sondern bfloat16 oder float32. Ob ein GPU-Beschleuniger auf den Zielgeräten damit zurechtkommt, ist **zu messen**.
- Das Modell nennt über 100 Sprachen, Deutsch wird nicht gesondert genannt (**zu messen**).

**Speichern und Suchen**
- SQLite für Felder (Betrag in Cent, Datum, Händler, Kategorie, Fristen) und für die Volltextsuche.
- **Vektorindex ohne Erweiterung:** Vektoren als BLOB in SQLite, Ähnlichkeit per Kosinus im Code (so macht es Cesto) oder über den `SqliteVectorStore` aus MediaPipe. Größe: 5.000 Belege × 256 Dimensionen × 4 Byte ≈ 5 MB.
- **Nicht verwenden:**
  - [sqlite-vector](https://github.com/sqliteai/sqlite-vector): Elastic License 2.0, kostenlos nur in Open-Source-Projekten, für den produktiven Einsatz ist eine Kauflizenz nötig.
  - [sqlite-vec](https://github.com/asg017/sqlite-vec): laut README vor Version 1, Android wird nicht genannt.
  - [ObjectBox](https://github.com/objectbox/objectbox-java): die Apache-Lizenz gilt laut README nur für den Repo-Code, die Lizenz der nativen Bibliothek ist ungeklärt.
- **Zweistufige Suche:** erst Filter über die Felder („Baumarkt, März"), dann Bedeutungssuche für Freitext und Foto. Vor dem Bau eine Baseline messen: Reichen Volltext plus Felder für den Alltag? Die Embeddings müssen beweisen, dass sie darüber hinaus etwas finden.

**Fragen und Antworten**
- Die Frage geht an Gemma 4, das ein Werkzeug wählt: Belege suchen, Summe nach Zeitraum oder Kategorie, Liste. Die App führt das Werkzeug lokal aus, die Zahlen kommen aus SQL, Gemma 4 formuliert die Antwort mit Quellenangabe. Cesto und invoice-rag arbeiten so, aber mit Cloud-Modell (Gemini) beziehungsweise Ollama. Auf dem Gerät ist es **zu messen** (Größe, Latenz von Gemma 4).

**Erinnern**
- WorkManager prüft lokal: Garantie, Rückgabe, Aufbewahrungsfrist. Lokale Mitteilung.
- Zwei Fristen je Artikel (Rückgabe, Garantie) mit editierbaren Standardwerten. Kassenzettel nennen selten eine Frist, die Werte kommen also meist aus Standardwerten und nicht aus dem Beleg.
- **GoBD-Hinweis (v3):** Die Aufbewahrungs-Erinnerung ist eine Gedächtnisstütze, keine GoBD-konforme Ablage. Das steht so in der App und in der Kommunikation. GoBD-Konformität (unveränderbare Ablage, Verfahrensdokumentation) kommt erst mit Phase D für die Business-Tür.

**Backup und Gerätewechsel**
- Ohne Konto gibt es keine Wiederherstellung, wenn das Gerät verloren geht. Deshalb Export als ZIP mit Datenbank, Anhängen und Manifest samt SHA-256-Prüfung, plus Wiederherstellung (Vorbild: [Warrant Book](https://github.com/rwrife/warrant-book), MIT).
- **Entscheidung Verschlüsselung (v3):** Die Sicherungsdatei wird **standardmäßig verschlüsselt** (AES-256-GCM, Schlüssel aus einem vom Nutzer gewählten Passwort per PBKDF2-HMAC-SHA256 mit 600.000 Iterationen abgeleitet; v3 nannte Argon2id, der Code nutzt PBKDF2, siehe ADR-009). Begründung: Die Datei enthält alle Belege mit Beträgen, Händlern und Daten. Unverschlüsselt auf einem USB-Stick oder in einer Cloud abgelegt wäre sie das größte Datenleck des ganzen Produkts. Der Nutzer wählt den Speicherort selbst, das Passwort vergibt er beim Export. Wer das Passwort verliert, verliert die Sicherung — das steht so im Export-Dialog.
- Der Nutzer wählt den Speicherort selbst. Export und Sicherung zählen zum lesenden Zugriff und bleiben ohne Lizenz nutzbar (ADR-008).

**Offline-Nachweis und Modell-Auslieferung (entschieden in v3)**
- Die App fordert keine Internet-Berechtigung. Das Android-Projekt [Petty Tracker](https://github.com/petty-foss-dev/Petty-Tracker) macht es so.
- **Entscheidung Modell-Download:** Das Modell kommt **nicht** per Download zur Laufzeit, sondern **gebündelt im Store-Paket** (Play Asset Delivery, install-time). Begründung: Ohne Internet-Berechtigung kann die App zur Laufzeit nichts nachladen. Die Kehrseite ist die Paketgröße (Google-Beispiel: etwa 470 MB), die früh gegen die Store-Limits zu prüfen ist (Abschnitt 17, Frage 9).
- **Entscheidung Scanner:** Fällt die Wahl auf ML Kit Document Scanner (Play-Dienste), gilt: Erstnutzung nur mit vorhandener Play-Dienste-Version. Der Offline-Nachweis in Phase A deckt genau diesen Weg ab, nicht ein Labor ohne Play-Dienste.
- Nachweis in Phase A: Berechtigungsliste der App plus Netzwerkmitschnitt beim Aufnehmen und Suchen. Ein Hinweis aus dem README von [ReceiptMind](https://github.com/naumanbutt2002/ReceiptMind-Private-Offline-AI): ML Kit sendet normalerweise anonyme Nutzungsmetriken an Google, ohne Netzwerkberechtigung ist das nicht möglich.

**Geräteanforderung (ehrlich):** Das Modell braucht Platz und Speicher. Google nennt für ein quantisiertes Modell auf einem Pixel 11 Pro etwa 191 MB RAM (nur Text) und etwa 567 MB (multimodal). Messungen auf anderen Geräten fehlen. `eg2-media-search` setzt Android 8 (minSdk 26) und arm64-v8a voraus. Ein gutes Mittelklasse-Handy genügt vermutlich, ein sehr schwaches nicht. Das wird vor dem Bau auf echten Geräten gemessen.

---

## 9 · Datenschutz und Recht

- **DSGVO:** Kein Server heißt: keine Übermittlung, keine Auftragsverarbeitung.
  Die Datenschutzerklärung wird kurz und ehrlich. Zu prüfen bleibt, ob
  Google-Komponenten (ML Kit, Play-Dienste) Daten senden: Ein anderes Projekt
  schreibt in seinem README, ML Kit sende normalerweise anonyme Nutzungsmetriken.
  Erst wenn Berechtigungsliste und Netzwerkmitschnitt sauber sind, steht
  „kein Netzwerkverkehr“ in der Kommunikation.
- **Sicherungsdatei:** Sie enthält alle Belege. Ob sie verschlüsselt wird, ist
  zu entscheiden (Abschnitt 8). Die Datenschutzerklärung nennt die Datei.
- **Kein Konto:** keine personenbezogenen Zugangsdaten.
- **Aufbewahrungsfristen:** seit 2025 gelten 8 Jahre für Buchungsbelege, 10
  Jahre für grundlegende Unterlagen. Die App rechnet mit und erinnert.
- **GoBD (für die Business-Tür):** unveränderbare Ablage,
  Verfahrensdokumentation, ersetzendes Scannen. Das ist Arbeit und Haftung.
  Deshalb erst in Phase D.
- **E-Rechnung:** Empfang im Format XRechnung und ZUGFeRD ab 2.0.1 (BMF).
- **Thermopapier:** Kassenzettel verblassen. Die App ersetzt den Zettel nicht,
  sie merkt ihn sich. Das steht so in der Kommunikation.
- **Berechtigungen:** Kamera und Mitteilungen, keine Internet-Berechtigung
  (Abschnitt 8). Bankimport über eine Datei, die der Nutzer selbst wählt, spart
  heikle Rechte.

---

## 10 · Monetarisierung

- **Privat:** Einmalzahlung, Zielpreis 14,99 Euro. Der Preis ist nicht getestet, und es gibt kostenlose Konkurrenz (Abschnitt 11). Vor dem Kauf muss man Aufnehmen und Suchen ausprobieren können, weil der Nutzen erst mit einem Archiv sichtbar wird.
- **Testphase (entschieden in v3):** 30 Tage oder 50 Belege, was zuerst erreicht ist. Danach pausiert nur die Erfassung (Aufnahme, Import, neue Analyse). Lesen, Suchen, Export und Sicherung laufen weiter (ADR-008). Begründung: 50 Belege genügen, um ein eigenes Archiv zu spüren und die Suche zu erleben; 30 Tage geben auch Wenig-Käufern die Chance. Wer darunter bleibt, hat keinen Verlust.
- **Business (später):** Jahreslizenz je Arbeitsplatz. Auch hier bleibt das Archiv ohne Lizenz lesbar (ADR-008).
- **Die eine Regel:** Das Archiv bleibt auch ohne Zahlung lesbar. Damit ist die
  App das Gegenteil der Abo-Falle, die in Bewertungen am häufigsten kritisiert
  wird. Nur Aufnehmen und neue Analyse pausieren (ADR-008). Folgerung: Export
  und Sicherung zählen zum Lesen und bleiben ohne Lizenz nutzbar.
- **Warum Einmalzahl möglich ist:** kein Server, keine laufenden Serverkosten. Wartung (Android-Updates, Modell-Updates, Support, Store-Gebühren) bleibt und gehört in die Preisrechnung.

---

## 11 · Wettbewerb und Differenzierung

*v2, Stand 10.10.2026. Ergänzt um die GitHub-Recherche. Alle Angaben stammen aus den READMEs der Projekte, keines wurde ausprobiert, die Recherche ist nicht vollständig.*

**Kommerzielle und große Anbieter (unverändert)**

| Gegner | Stärke | Unsere Antwort |
|---|---|---|
| Scanner-Apps (CamScanner, Adobe, Genius) | gute Aufnahme | wir finden, sie speichern |
| Belegscanner-Apps (anybill, Klippa, Fiscara, meinebuchhaltung) | GoBD, Export | wir sind lokal und ohne Abo |
| Buchhaltung (Lexoffice, sevdesk, WISO) | vollständig | wir ersetzen sie nicht, wir füttern sie |
| Google Fotos / Drive | bequem, aber Cloud | wir bleiben auf dem Gerät |

**Neu: lokale Open-Source-Apps für Handy und Android**

| Projekt | Was es kann | Lizenz | Stand |
|---|---|---|---|
| [Warden](https://github.com/Surge77/warden) | Android, ML-Kit-Texterkennung (Händler, Datum, Betrag), zwei Fristen je Artikel, lokale Erinnerungen, JSON-Export, Pro-Version mit eigenen Erinnerungszeiten | MIT | 17 Commits, 0 Sterne |
| [Warrant Book](https://github.com/rwrife/warrant-book) | Flutter, Garantie- und Rückgabefristen, keine Texterkennung (im MVP ausgeschlossen), ZIP-Backup mit Wiederherstellung | MIT | 8 Commits, 0 Sterne |
| [Petty Tracker](https://github.com/petty-foss-dev/Petty-Tracker) | Garantien, Abos, Dokumente, Android in Kotlin. Beleg-Scan nur auf iOS, auf Android nur Anhängen. Keine Internet-Berechtigung. | AGPL-3.0 | keine Releases, 1 Stern |
| [ReceiptMind](https://github.com/naumanbutt2002/ReceiptMind-Private-Offline-AI) | Flutter, ML Kit, Regel-Parser, lokales Sprachmodell erst geplant | Apache-2.0 | früh, keine Releases |
| [Receipt Wrangler Android](https://github.com/quatrejuin/receipt_wrangler_android) | Texterkennung auf dem Gerät, Dubletten, Teilen-Intent, Export | nicht genannt | archiviert seit 27.09.2026 |

**Neu: Self-Hosted (Server nötig)**
[Warracker](https://github.com/sassanix/Warracker) (1,5k Sterne, AGPL, Texterkennung nicht erwähnt), [receiptly](https://github.com/Thoomaastb/receiptly) (deutsch, Tesseract, Volltextsuche, vor Version 1.0, AGPL), [Receipt Wrangler](https://github.com/Receipt-Wrangler) (272 Sterne, der mobile Client ist archiviert), [paperless-gpt](https://github.com/icereed/paperless-gpt) (2,7k Sterne, MIT), [PaperCortex](https://github.com/renefichtmueller/PaperCortex) (semantische Suche und DATEV-Export über Ollama, früh, MIT).

**Am nächsten an Belegkasten**
- [eg2-media-search](https://github.com/xueweijian/eg2-media-search): lokale multimodale Suche auf Android mit EmbeddingGemma 2. Noch am Anfang, die Integration der Suche ist offen.
- [Cesto](https://github.com/mbahmani90/Cesto): Fragen an Belege mit SQL für Summen und Embeddings für die Suche. Die Embeddings laufen aber in der Google-Cloud, EmbeddingGemma ist nur geplant.

**Was das für uns heißt**
- **Nicht mehr als Alleinstellung tragfähig:** „lokal, kein Konto" und die Garantie-Erinnerung. Beides gibt es schon kostenlos. „Kein Abo" unterscheidet nur noch von Bezahl-Anbietern.
- **Noch unbesetzt (nach dieser Recherche):** Wiederfinden nach Bedeutung und per Foto, fertig und auf dem Gerät. Das Durchblättern vieler Belege vor der Kamera. Eine ausgereifte Android-App für deutsche Belege (die meisten Projekte sind früh oder nur aus dem Quellcode zu bauen).
- **Zeitfenster:** Die Konkurrenten sind frisch (0 bis 1 Sterne, 8 bis 25 Commits). Cesto und `eg2-media-search` zeigen, dass andere denselben Baustein einsetzen wollen. Das stützt offene Frage 4, ohne sie zu beantworten.
- **Zahlungsbereitschaft:** Wer gegen kostenlose Open-Source-Apps antritt, muss beweisen, dass die fertige App mit Suche etwas wert ist. Offene Frage 1 wird damit wichtiger.
- **Nachnutzung:** Von MIT- und Apache-Projekten (Warden, Warrant Book, ReceiptMind, Cesto, PaperCortex, open-android-doc-scanner) kann man Ideen und Code lernen. AGPL- und GPL-Projekte (Petty Tracker, Warracker, receiptly, FairScan) sind für eine geschlossene Bezahl-App nicht kopierbar. Rechtlich gegenprüfen, bevor Code übernommen wird.

**Unsere echte Differenz:** der lokale, multimodale Index plus Frage-Antwort. Aufnahme und OCR sind austauschbar. Das Gedächtnis ist es nicht. Heute ist diese Lücke offen, aber nicht geschützt.

---

## 12 · Risiken und Gegenmaßnahmen

| Risiko | Wahrscheinlichkeit | Wirkung | Gegenmaßnahme |
|---|---|---|---|
| Google oder Apple bauen es ein | mittel | hoch | schnell sein, auf Vertrauen setzen, nicht auf Technik allein |
| Markt ist überfüllt | hoch | mittel | enger Auftrag: finden und erinnern, nicht verwalten. Kostenlose lokale Apps gibt es schon (Abschnitt 11) |
| Zahlt niemand (Bewertungen zeigen fast kein Kaufsignal) | mittel | hoch | außerhalb der Stores testen, Warteliste, Vorverkaufstest und bezahlte Beta. Kostenlose Konkurrenz einpreisen |
| Rechenlast auf schwachen Geräten | mittel | mittel | vorher messen, Mindestanforderung klar nennen |
| OCR versagt bei verblasstem Druck | mittel | mittel | Suche ist fehlertolerant, doppelte Quellen nutzen |
| GoBD und Export sind zu schwer | mittel | mittel | erst Phase D, nicht am Anfang |
| Nutzen zeigt sich zu spät | mittel | hoch | Import vorhandener Belege und erste Suche in der ersten Sitzung, Testphase vor dem Kauf, Garantie-Mitteilung als zweiter früher Nutzen |
| Andere bauen dieselbe Suche nach (offenes Modell, Beispielcode von Google) | mittel | hoch | Tempo, Vertrauen, ausgereifte deutsche Belegerkennung, Zeitfenster beobachten |
| Foto-Suche funktioniert nicht zuverlässig | unbekannt | mittel | Test an echten Paaren, sonst fällt die Funktion weg (Abschnitt 4) |
| Dauer-Aufnahme ist nicht machbar | unbekannt | mittel | Auto-Capture je Beleg, Teilen und Import genügen für Phase A |
| „Kein Netzwerkverkehr“ lässt sich nicht halten (Play-Dienste, ML Kit) | mittel | hoch | gebündelte Modelle, keine Internet-Berechtigung, Netzwerkmitschnitt vor dem Versprechen |
| Modell ist zu groß für Download oder Store (Google-Beispiel: etwa 470 MB) | mittel | mittel | Variante wählen, Auslieferung und Größenlimits früh prüfen |
| Datenverlust bei Handyverlust (kein Konto) | mittel | hoch | Sicherungsdatei und Wiederherstellung in Phase A |
| Lizenzfalle bei Bausteinen (zum Beispiel sqlite-vector, AGPL- und GPL-Code) | niedrig, wenn beachtet | hoch | Abschnitt 8 und 11, Lizenz prüfen vor jeder Code-Übernahme |

*Neue Zeilen und geänderte Gegenmaßnahmen: Einschätzung von v2, nicht gemessen. „Unbekannt“ heißt: erst der Spike liefert eine Zahl.*

---

## 13 · Erfolgsmessung

**Nordstern:** Anzahl der Belege, die ein Nutzer **per Suche wiederfindet**, pro
Monat. Nicht die Zahl der gescannten Belege. Finden ist der Zweck.

**Messung ohne Telemetrie (entschieden in v3):** Die App sendet nichts, auch keine
anonymen Statistiken. Stattdessen drei Bausteine:

1. **Lokales Tagebuch für den Nutzer:** Zähler für Suchanfragen, geöffnete Treffer
   und Garantie-Erinnerungen, nur auf dem Gerät sichtbar („Deine 30 Tage").
   Das ist Nutzen für den Nutzer, kein Tracking für uns.
2. **Freiwilliger Diagnose-Export in der Beta:** Ein Button „Beta-Feedback senden"
   erzeugt eine Datei mit aggregierten Zahlen (keine Beleginhalte, keine Händler,
   keine Beträge), die der Nutzer per Mail selbst schickt. Nur so kommen wir in
   der Beta an Zahlen.
3. **Interviews und Vorverkauf als Wahrheit:** Was zählt, sind bezahlte Beta-Plätze
   und beobachtete Suchzeiten im Nutzertest, nicht Klickzahlen. Die Entscheidung
   zum Weiterbau fällt nach Abschnitt 15, nicht nach einem Dashboard.

**Aktivierung:** In der ersten Sitzung wird ein **älterer, importierter** Beleg
per Suche wiedergefunden. Ein eben aufgenommener Beleg zählt nicht, weil das
zu leicht gelingt.

**Bindung:** Anteil der Nutzer, die im vierten Wochenfenster wiederkommen. Der
Garantie-Wecker ist der Hebel.

**Ehrlichkeit:** Anteil der Nutzer, die verstehen, dass nichts in die Cloud geht
(aus Umfrage).

**Geld:** Anteil der Warteliste, die zur bezahlten Beta wird. Für die Entscheidung
ist das die wichtigste Zahl. Eine Warteliste misst Interesse, nicht Zahlung,
deshalb zusätzlich ein Vorverkauf, zum Beispiel über einen rückerstattbaren
Zahlungslink.

---

## 14 · Validierung vor dem Bau (die Reihenfolge ist entscheidend)

1. **Technik-Spike (1 bis 2 Tage), parallel zu 2.** Testset aus etwa 100 echten
   deutschen Belegen, auch verblasste. Messen: Genauigkeit der Felder, richtiger
   Beleg unter den ersten 3 Treffern (Volltext plus Felder gegen Embeddings),
   Foto-Test an etwa 30 Paaren (Gegenstand, Beleg), Modell auf 2 bis 3 echten
   Mittelklasse-Geräten (RAM, Latenz, Akku, Größe), Offline-Nachweis des
   Aufnahmewegs. Testbelege enthalten persönliche Daten: eigene Belege oder
   Belege von Freiwilligen mit Einwilligung, Namen und Adressen schwärzen.
2. **Landingpage mit Warteliste.** Aussage: findet jeden Beleg, lokal, kein
   Konto. Messen: wie viele Besucher melden sich an, aus einer vorher
   festgelegten Traffic-Quelle. Schwellen: Abschnitt 15. Eine Warteliste misst
   Interesse, nicht Zahlung, deshalb zusätzlich ein Vorverkaufstest.
3. **8 bis 10 Problemgespräche.** „Wann hast du zuletzt einen Beleg gebraucht,
   und was ist passiert?“ Auch nach der Steuererklärung fragen, weil sie ein
   jährlicher Anlass ist. Ein Garantiefall kommt selten vor.
4. **Prototyp der Kernfunktion.** Aufnahme, finden. Fragen nur, wenn der Spike
   zeigt, dass Gemma 4 auf dem Gerät trägt. Kein Garantie-Modul, kein Export.
5. **Test mit 10 echten Nutzern.** Aufgabe: eine Kiste Belege erfassen und einen
   konkreten Beleg finden. Messen: Zeit und ob es Spaß oder Frust war.
6. **Concierge-Test.** Wir machen den Dienst fünf Leuten von Hand, bevor die App
   fertig ist. Zahlt jemand dafür?
7. **Erst dann** bauen.

**Diese Reihenfolge ist die Lehre aus dem Projekt:** 28 Ideen sind gescheitert,
weil von Beschwerden direkt auf ein Produkt geschlossen wurde. Hier wird vor dem
Bau gemessen. Die Reihenfolge folgt dem Risiko: zuerst die Tests, die die Idee am
billigsten widerlegen können (Technik, Problem, Zahlungsbereitschaft), danach die
aufwendigen (Prototyp, Nutzertest).

---

## 15 · Tötungskriterien (wann wir aufhören)

*Alle Schwellen in v3 festgelegt. Sie sind Annahmen aus Branchenwerten und der
eigenen Risikoabwägung, keine gemessenen Zahlen. Gilt eine Schwelle nach dem
Spike als falsch, wird sie mit Begründung geändert, bevor weitergebaut wird.*

- **Landingpage:** 500 Besucher aus der vorher festgelegten Traffic-Quelle und unter 5 Anmeldungen (unter 1 % Anmeldequote). Begründung: Unter 1 % trägt kein bezahlter Kanal die Akquisekosten eines Einmalpreises von 14,99 Euro. Bei 1 bis 3 % wird die Botschaft überarbeitet und einmal wiederholt, erst darunter wird gestoppt.
- **Prototyp:** Nutzer finden den Beleg nicht in unter 30 Sekunden (Median über 10 Nutzer, je 3 Suchaufgaben).
- **Concierge:** Von 5 Getesteten zahlt keiner. Bei 1 von 5 wird die Stichprobe auf 10 erweitert, erst bei 1 von 10 oder schlechter wird gestoppt.
- **Technik:** Das Modell läuft auf dem Zielgerät nicht flüssig. „Flüssig“ heißt: Texteingabe-Suche unter 2 Sekunden vom Tippen bis zur Trefferliste, Foto-Suche unter 5 Sekunden, RAM-Dauerlast unter 400 MB auf einem Mittelklasse-Gerät von 2023 (zum Beispiel Pixel 7a oder Galaxy A54). Werden zwei von drei Werten auf zwei von drei Testgeräten verfehlt, ist das Kriterium gerissen.
- **Suche:** Die Bedeutungssuche liegt im Spike nicht klar über Volltext plus Felder. „Klar darüber“ heißt: mindestens **15 Prozentpunkte** mehr Treffer unter den ersten 3 (Recall@3) auf dem Testset aus Abschnitt 14. Beispiel: Volltext schafft 60 %, Embeddings müssen 75 % schaffen. Dann ist der Unterschied zur Konkurrenz weg, und die Idee wird neu bewertet.
- **Foto-Suche:** Der Test an etwa 30 Paaren scheitert. „Scheitert“ heißt: unter **50 %** der Gegenstände finden ihren Beleg unter den ersten 3 Vorschlägen. Dann fällt nur die Foto-Funktion weg, nicht das Projekt.
- **Offline:** Der Netzwerkmitschnitt zeigt Verkehr beim Aufnehmen oder Suchen, den wir nicht abstellen können. Dann gilt das Versprechen „kein Netzwerkverkehr“ nicht.

Wer diese Kriterien nicht vorher aufschreibt, baut weiter, auch wenn die Idee
längst tot ist.

---

## 16 · MVP und Phasen (nach Ergebnis, nicht nach Zeit)

**Phase A · Beweis der Kernfunktion**
Aufnahme (Kamera, Teilen, Import), Erkennen mit Prüfung, Suchen mit Worten,
Sicherung und Wiederherstellung, E-Rechnungs-PDF als Anhang mit Handbestätigung
(Fallback, kein XML-Parsing), alles lokal. Ziel: es funktioniert.

**Phase B · Reibung wegnehmen**
Import in Masse (Ordner beobachten), E-Mail-Eingang, Dauer-Aufnahme, wenn der
Prototyp sie trägt. Ziel: Erfassen wird mühelos.

**Phase C · Bindung**
Garantie- und Rückgabe-Erinnerung mit editierbaren Standardwerten,
Aufbewahrungsfrist. Ziel: Nutzer kommen zurück, ohne dass wir sie rufen.

**Phase D · Geld und Business**
E-Rechnung, Doppel-Erkennung, Bankabgleich, Export, Jahreslizenz. Ziel:
Selbstständige zahlen.

**Abnahmekriterien Phase A (messbar):**
- Ein Beleg wird in unter 3 Sekunden aufgenommen.
- Eine Suche mit Worten liefert den richtigen Beleg unter den ersten 3 Treffern (gemessen am Testset aus echten deutschen Belegen, Abschnitt 14).
- Kein Netzwerkverkehr beim Aufnehmen und Suchen. Nachweis: Berechtigungsliste ohne Internet und Netzwerkmitschnitt.
- Die App startet und arbeitet ohne Internet.
- Eine Sicherung lässt sich auf einem zweiten Gerät wiederherstellen (verschlüsselt, mit Passwort).
- Ein geteiltes E-Rechnungs-PDF landet als Anhang am Beleg, auch ohne XML-Parsing.

---

## 17 · Offene Fragen

1. **Zahlbereitschaft.** In Bewertungen fast null gemessen. Nur außerhalb der
   Stores zu klären. Dazu kommt kostenlose Konkurrenz (Abschnitt 11).
2. **Marktgröße.** Bewertungen und Downloads zählen keine Nutzer.
3. **Rechenlast** auf echten Mittelklasse-Geräten. Google nennt Werte nur für ein
   Pixel 11 Pro (quantisiert etwa 191 MB RAM nur Text, etwa 567 MB multimodal).
   Mittelklasse nicht gemessen.
4. **Wie lange das Zeitfenster offen ist,** bevor Google oder Apple es selbst
   bringen. Auch andere Entwickler setzen denselben Baustein ein (Abschnitt 11).
5. **Ob die Business-Tür** den Aufwand für GoBD und Haftung wirklich lohnt.
6. **Foto-Suche.** Findet das Foto eines Gegenstands den Beleg zuverlässig?
7. **Dauer-Aufnahme.** Lässt sich ein Stapel Belege in einem Zug aufnehmen?
8. **Scanner und Offline-Anspruch (entschieden in v3).** ML Kit Document Scanner
   (Play-Dienste) als erste Wahl, Offline-Alternative als Rückfall, wenn der
   Netzwerkmitschnitt Verkehr zeigt. Der Spike entscheidet anhand des Mitschnitts.
9. **Modellgröße und Auslieferung (teilentschieden in v3).** Auslieferung per
   Play Asset Delivery (install-time) steht fest. Offen bleibt: welche Variante
   (270M/440M/740M) und ob die Paketgröße die Store-Limits einhält. Das misst
   der Spike.
10. ~~**Sicherungsdatei.** Verschlüsseln oder nicht?~~ **Entschieden in v3:**
    standardmäßig verschlüsselt (AES-256-GCM, Passwort per PBKDF2, siehe ADR-009 und Abschnitt 8).
11. **Vertrieb.** Wie erreichen wir Privatpersonen mit diesem Problem?
12. ~~**Testphase.** Wie viel kann man vor dem Kauf nutzen?~~ **Entschieden in v3:**
    30 Tage oder 50 Belege, was zuerst eintritt (Abschnitt 10).

---

## 18 · Quellen

- [EmbeddingGemma 2: The Developer Guide](https://developers.googleblog.com/embeddinggemma-2-the-developer-guide/)
- [ML Kit Document Scanner](https://developers.google.com/ml-kit/vision/doc-scanner)
- [Bundesfinanzministerium: FAQ E-Rechnung](https://www.bundesfinanzministerium.de/Content/DE/FAQ/e-rechnung.html)
- [Aufbewahrungsfrist Rechnungen 8 oder 10 Jahre](https://e-rechn.de/de/blog/aufbewahrungsfrist-rechnungen-8-10)
- [Digitale Belege ab 2028 (Bundesregierung)](https://www.bundesregierung.de/breg-de/aktuelles/abschaffung-bonpflicht-2453568)
- Projekt: `GEMMA-IDEEN-10.md` (Namensgeber der Struktur; `GEMMA-IDEEN.md` und
  `out/reports/BEFUNDE-ZAHLUNGSBELGE.md` lagen im Arbeitsordner vom 10.10.2026
  nicht vor und sind hier nur als Herkunft genannt, nicht als geprüfte Quelle).
- Recherche 10.10.2026 (Websuche und einzelne GitHub-Seiten, nicht vollständig): die Quellen zu Technik und Wettbewerb stehen direkt in Abschnitt 8 und 11.
- [EmbeddingGemma 2 (Google Blog)](https://blog.google/innovation-and-ai/technology/developers-tools/embeddinggemma-2/)
- [EmbeddingGemma 2 Modellkarte (Hugging Face)](https://huggingface.co/google/embeddinggemma-2)
- Digitaler Kassenbon (Gesetzentwurf): [Handwerksblatt](https://www.handwerksblatt.de/handwerkspolitik/bonpflicht-wird-nicht-abgeschafft), [Deutsche Handwerks Zeitung](https://www.deutsche-handwerks-zeitung.de/?p=384034), [mobiFlip](https://www.mobiflip.de/bonpflicht-bleibt-und-wird-digital/)
- Die Links zum Developer Guide und zur Bundesregierung oben wurden am 10.10.2026 geöffnet und sind erreichbar.

---

## Anhang · Änderungen in v3 (10.10.2026, externes Review)

- **Kopf:** Stand v3 mit Aufzählung aller gelösten Widersprüche.
- **1 Pressemitteilung:** Gegenstand-Foto-Suche als experimentelle Beta-Funktion
  umformuliert (kein Kaufversprechen), neues FAQ zur Beta, Preis als Zielpreis
  gekennzeichnet.
- **4 Positionierung:** Foto-Suche aus dem Positionierungssatz gestrichen (nur
  noch Beschreibungssuche als Kern), Beta-Zeile in der Beweistabelle mit
  50-%-Schwelle.
- **5 Kernfunktionen:** MoSCoW-Klärung im Vorspann, E-Rechnung in Muss (Phase D)
  plus Fallback in Phase A, Foto-Suche als Beta gekennzeichnet.
- **6 Nutzerfluss:** Moment 1 auf Auto-Capture je Beleg festgelegt, Moment 2 mit
  Beta-Hinweis.
- **8 Technik:** Modell-Auslieferung per Play Asset Delivery entschieden,
  Verschlüsselung der Sicherung entschieden (AES-256-GCM, Argon2id; später per ADR-009 auf PBKDF2 angeglichen),
  GoBD-Hinweis bei Erinnern, ADR-008-Verweis für Export/Sicherung.
- **10 Monetarisierung:** Testphase entschieden (30 Tage oder 50 Belege),
  Preis einheitlich als Zielpreis 14,99 Euro.
- **13 Erfolgsmessung:** Messkonzept ohne Telemetrie entschieden (Tagebuch,
  Diagnose-Export, Interviews/Vorverkauf).
- **15 Tötungskriterien:** alle Schwellen festgelegt (1 % Landingpage mit
  Wiederholungsregel, 0/5 Concierge mit Erweiterung, Latenz/RAM-Zahlen,
  15 Prozentpunkte Suche, 50 % Foto).
- **16 Phasen:** E-Rechnungs-Fallback in Phase A und als Abnahmekriterium.
- **17 Offene Fragen:** Fragen 8, 9, 10, 12 als (teil-)entschieden markiert.
- **18 Quellen:** tote Referenzen als ungeprüft gekennzeichnet.

**Noch zu entscheiden (in dieser Fassung offen):** Scanner-Wahl (Spike per
Mitschnitt), Modellvariante und Paketgröße (Spike), Vertriebskanal,
Zahlbereitschaft, Marktgröße, Business-Tür (GoBD-Aufwand), Foto- und
Dauer-Aufnahme (Spike/Prototyp).

## Anhang · Änderungen in v2 (10.10.2026)

*Änderungsliste der Fassung v2, unverändert übernommen. Das „Noch zu entscheiden" am Ende dieses Blocks ist durch v3 überholt.*

- **Kopf:** Stand und Hinweis auf v2.
- **1 Pressemitteilung:** Kern ist das Wiederfinden. Dauer-Aufnahme heraus, FAQ um Internet, Handywechsel und kostenlose Apps erweitert.
- **2 Problem und Warum jetzt:** kostenlose Konkurrenz, Kassenbon als Gesetzentwurf, E-Rechnung vor allem für die zweite Zielgruppe, fünf neue Zeilen in der Belegtabelle.
- **3 Zielgruppe:** Abgrenzung zu „nur Fristen zählen“, Vertriebskanal als offene Frage.
- **4 Positionierung:** neuer Kern und Tabelle „Behauptung, Status, Beweis“.
- **5 Kernfunktionen:** Aufnahme in Kamera und Teilen/Import geteilt (beide Muss), Dauer-Aufnahme auf Soll, Sicherung neu (Muss), Garantie mit Standardwerten und Phase C, Erkennen mit Prüfung.
- **6 Nutzerfluss:** Moment 1 ohne Stapel-Durchblättern, Hinweise bei Foto-Suche und Fristen.
- **7 Non-Goals:** Kein Abo nur für Privatnutzer, keine Cloud-Sicherung.
- **8 Technik:** neu geschrieben (Bausteine, Lizenzen, Vektorindex, Offline-Nachweis, Sicherung).
- **9 Datenschutz:** keine Internet-Berechtigung, Google-Komponenten prüfen, Sicherungsdatei.
- **10 Monetarisierung:** Preis ungetestet, Testphase, Export bleibt ohne Lizenz nutzbar, Wartungskosten.
- **11 Wettbewerb:** neu geschrieben (lokale Open-Source-Apps, Self-Hosted, nächste Projekte).
- **12 Risiken:** sieben neue Zeilen, drei Gegenmaßnahmen angepasst.
- **13 Erfolgsmessung:** Messung ohne Telemetrie, strengere Aktivierung, Vorverkauf.
- **14 Validierung:** Reihenfolge nach Risiko, Technik-Spike und Problemgespräche zuerst, Umgang mit Testbelegen.
- **15 Tötungskriterien:** Traffic-Quelle, vier neue Kriterien mit Platzhaltern für Schwellen. Der Widerspruch „200 und 0“ (Abschnitt 14) gegen „500 und unter 5“ ist aufgelöst, es gilt Abschnitt 15.
- **16 Phasen:** Teilen und Import in Phase A, Sicherung in Phase A, Abnahmekriterien ergänzt.
- **17 Offene Fragen:** Frage 1, 3 und 4 ergänzt, Frage 6 bis 12 neu.
- **18 Quellen:** ergänzt.

**Noch zu entscheiden (Stand v2, in v3 überholt):** Schwellenwerte in Abschnitt 15, Umfang der Testphase, Verschlüsselung der Sicherung, Scanner-Wahl, Vertriebskanal, Messung ohne Telemetrie. In v3 entschieden: Schwellenwerte, Testphase, Verschlüsselung, Messung ohne Telemetrie. Offen bleibt die Liste im v3-Block oben.
