# Belegkasten — Bauplan (v1, 10.10.2026)

**Grundlage:** `PRODUKT-BELEGKASTEN.md` v3. Dieser Bauplan beschreibt den Weg
von null bis zur vollwertigen Consumer-App im Play Store. Jede Phase hat Ziel,
Umfang, Abnahme und Stopp-Regel. Kein Code vor Phase 0.

**Technische Leitplanken (gelten für alle Phasen):**

- Stack: Kotlin, Jetpack Compose, Room (SQLite), WorkManager, MediaPipe
  tasks-retrieval, ML Kit (gebündelt) oder Offline-Alternative, minSdk 26,
  arm64-v8a zuerst.
- Regel: Das Modell liest und findet, der Code rechnet und prüft. Beträge in
  Cent, Summen aus SQL, nie aus dem Modell.
- Regel: Keine Internet-Berechtigung im Manifest ab Phase A. Modell per Play
  Asset Delivery (install-time).
- Regel: ADR-008 gilt ab der ersten zahlungspflichtigen Version — Lese-,
  Export- und Sicherungszugriff nie blockieren.
- Sicherung: AES-256-GCM, Schlüssel per PBKDF2-HMAC-SHA256 (600.000 Iterationen) aus Nutzerpasswort, siehe ADR-009.

---

## Phase 0 · Technik-Spike (1–2 Tage, kein Produktcode)

**Ziel:** Die drei tödlichsten Annahmen mit Zahlen belegen oder widerlegen,
bevor irgendetwas gebaut wird.

**Eingang:** Testset aus ca. 100 echten deutschen Belegen (eigene oder mit
Einwilligung, Namen/Adressen geschwärzt), davon verblasste; ca. 30 Paare
(Gegenstandsfoto, Beleg); 2–3 Mittelklasse-Geräte (z. B. Pixel 7a, Galaxy A54).

**Arbeitspakete:**

0.1. **OCR-Spike:** ML Kit Text Recognition (gebündelt) auf dem Testset.
Messen: Feldgenauigkeit Datum, Summe, Händler. Deutsch/Umlaute prüfen.
0.2. **Such-Baseline:** Volltext (FTS4) plus Feldﬁlter auf dem Testset.
Messen: Recall@3 für 50 echte Suchanfragen („Baumarkt März", „Bohrmaschine").
Das ist die Vergleichszahl.
0.3. **Embedding-Spike:** EmbeddingGemma 2 (Variante 270M zuerst, dann 440M
für multimodal) per MediaPipe auf denselben 50 Anfragen. Task-Präfixe setzen,
bfloat16/float32, 256 Dimensionen, normalisieren. Messen: Recall@3, Latenz,
RAM, Paketgröße. Differenz zur Baseline in Prozentpunkten.
0.4. **Foto-Spike:** 30 Gegenstandsfotos gegen Beleg-Embeddings. Messen:
Recall@3. Schwelle 50 %.
0.5. **Offline-Spike:** Test-App ohne Internet-Berechtigung, Modell per PAD.
Netzwerkmitschnitt bei Aufnahme und Suche. Scanner-Kandidaten vergleichen
(ML Kit Document Scanner gegen open-android-doc-scanner).
0.6. **Gemma-4-Spike (nur wenn 0.3 besteht):** Tool-Calling (suchen,
summieren) auf dem Gerät. Messen: Größe, Latenz, Qualität der Werkzeugwahl.

**Abnahme:** Alle Zahlen aus Abschnitt 15 der Produktbeschreibung liegen vor:
Recall-Differenz (15 Prozentpunkte?), Foto-Recall (50 %?), Latenz (2 s / 5 s),
RAM (400 MB), Mitschnitt sauber, Paketgröße gegen Store-Limits geprüft.

**Stopp:** Eine gerissene Schwelle stoppt oder beschneidet (Foto weg, Idee neu
bewerten, Offline-Versprechen streichen). Siehe Tötungskriterien.

**Ausgang:** Spike-Bericht (1–2 Seiten) mit Go/No-Go je Annahme. Kein
wiederverwendbarer Produktcode nötig, nur Messwerte.

---

## Phase 1 · Validierung am Markt (parallel zu Phase 0, 1–2 Wochen)

**Ziel:** Beweisen, dass Problem und Zahlbereitschaft existieren.

1.1. **Landingpage mit Warteliste:** Aussage „findet jeden Beleg, lokal, kein
Konto". Feste Traffic-Quelle vorab definieren (z. B. ein Forum, eine
Anzeigenschaltung). Schwelle: 500 Besucher, unter 5 Anmeldungen = Stopp.
1.2. **Vorverkaufstest:** Rückerstattbarer Zahlungslink für die Beta (Zielpreis
14,99 Euro). Warteliste misst Interesse, nur Zahlung misst Kauf.
1.3. **8–10 Problemgespräche:** „Wann hast du zuletzt einen Beleg gebraucht?"
Steuererklärung als jährlichen Anlass mit abfragen.
1.4. **Concierge-Test:** 5 Personen den Dienst von Hand machen (Belege
einsammeln, bei Anfrage heraussuchen). Zahlt einer? Bei 1 von 5 auf 10
erweitern.

**Abnahme:** Mind. 1 % Anmeldequote oder Lernschleife, mind. 1 von 5 (bzw.
2 von 10) zahlt im Concierge.

**Stopp:** Tötungskriterien Landingpage und Concierge.

---

## Phase 2 · Klick-Prototyp (3–5 Tage, nach bestandenem Spike)

**Ziel:** Die Kerninteraktion testen, bevor Architektur gebaut wird.

**Umfang:** Attrappe mit echten Belegbildern: Aufnehmen (simuliert), Felder
bestätigen, Suche mit Worten, Trefferliste. Keine echte Erkennung, keine echte
Suche, keine Sicherung. Maximal ein Tag pro Bildschirm.

**Test:** 10 echte Nutzer, je 3 Suchaufgaben an einer Kiste Belege. Messen:
Median-Zeit bis zum richtigen Beleg, Frust-Momente (laut denken lassen).

**Abnahme:** Median unter 30 Sekunden.

**Stopp:** Prototyp-Kriterium. Zweiter Anlauf mit überarbeitetem Suchbildschirm
erlaubt, danach Stopp.

---

## Phase 3 · Phase-A-App / geschlossene Beta (6–10 Wochen)

**Ziel:** Die Kernfunktion als installierbare App: aufnehmen, erkennen, suchen,
sichern — alles lokal. Das ist das MVP.

**Architektur (neu, sauber):**

```
app/
├── capture/        # Kamera, Auto-Capture je Beleg, Teilen-Intent, Galerie-/Datei-Import
├── ocr/            # ML Kit (gebündelt) oder Alternative, Regel-Parser Datum/Summe/Nummer
├── understand/     # Gemma-4-Felderextraktion (Händler, Kategorie, Positionen), Prüfregeln
├── embed/          # EmbeddingGemma 2, Task-Präfixe, BLOB-Speicherung, Kosinus-Suche
├── store/          # Room: receipts, items, embeddings, warranties; FTS4; DAO-Schicht
├── search/         # Zweistufig: Feldﬁlter → Embedding-Ranking → Trefferliste
├── backup/         # ZIP-Export (DB, Anhänge, Manifest, SHA-256), AES-256-GCM, Import
├── license/        # Einmalkauf (Play Billing), zwei Zustände nach ADR-008, Testphase 30 Tage/50 Belege
└── ui/             # Compose: Erfassen, Bestätigen, Suchen, Detail, Einstellungen
```

**Datenmodell (Kern):**

- `receipts`: id, merchant, date, total_cents, category, image_path, source
  (camera/share/import), created_at.
- `items`: id, receipt_id, label, price_cents, warranty_months, return_days.
- `embeddings`: receipt_id, dim, vector_blob (256 × float32).
- `warranties`: item_id, type (return/warranty), due_date, notified.
- Beträge immer Cent (Long), Datum ISO-8601, Pflichtfeldprüfung vor Insert.

**Arbeitspakete:**

3.1. **Gerüst:** Projekt, Room-Schema, Navigation, Design-Token, Manifest ohne
Internet-Berechtigung, PAD-Modul für das Modell.
3.2. **Capture:** Kamera mit Auto-Capture je Beleg, Teilen-Intent (Bild/PDF),
Galerie- und Datei-Import. E-Rechnungs-PDF als Anhang speichern (Fallback).
3.3. **OCR + Regeln:** Texterkennung, Regel-Parser für Datum/Summe/Nummer,
Bestätigungs-UI für markierte Felder.
3.4. **Verstehen:** Gemma-4-Extraktion für Händler/Kategorie/Positionen,
Prüfregeln (Positionssumme = Zwischensumme, Cent-Rundung, Datumsformat).
3.5. **Embed + Suche:** Belegtexte einbetten, zweistufige Suche, Trefferliste
mit den ersten 3 prominent. Foto-Suche nur, wenn Spike bestanden (Beta-Flag).
3.6. **Backup:** Verschlüsselter ZIP-Export mit Passwort-Dialog, Import mit
Prüfung (SHA-256, Schema-Version), Wiederherstellung auf Zweitgerät testen.
3.7. **Lizenz + Testphase:** Play-Billing Einmalkauf, Zähler (Tage/Belege),
Sperrung nur der Erfassung, Lesemodus nach ADR-008.
3.8. **Offline-Nachweis:** Berechtigungsliste, Mitschnitt, Start ohne Netz.
3.9. **Lokales Tagebuch:** „Deine 30 Tage" (Suchanfragen, Treffer, Erinnerungen),
Beta-Feedback-Export (nur Aggregate, keine Inhalte).

**Abnahme (alle messbar):**

- Belegaufnahme unter 3 Sekunden.
- Suche: richtiger Beleg unter den ersten 3 (Testset).
- Kein Netzverkehr (Manifest + Mitschnitt).
- Start und Arbeit ohne Internet.
- Sicherung auf Zweitgerät wiederherstellbar.
- E-Rechnungs-PDF als Anhang speicherbar.

**Beta:** Geschlossene Testgruppe (50–200 Personen aus Warteliste), freiwilliger
Diagnose-Export, Interviews. Keine öffentliche Bewertung möglich.

**Stopp:** Abnahmekriterien nicht erfüllbar → zurück in Spike oder Stopp.

---

## Phase 4 · Phase B — Reibung wegnehmen (4–6 Wochen)

**Ziel:** Erfassen wird mühelos. Nichts an der Kernarchitektur ändern, nur
Eingangskanäle verbreitern.

4.1. **Massen-Import:** Ordner beobachten (Storage Access Framework),
Stapelverarbeitung mit Fortschritt, Duplikat-Erkennung per Embedding-Vergleich.
4.2. **E-Mail-Eingang:** Anleitung + Teilen-Workflow für Mail-Anhänge
(kein Postfach-Zugriff, kein Konto — Nutzer teilt Anhang in die App).
4.3. **Dauer-Aufnahme:** Nur wenn Prototyp trägt: Stapel durchblättern,
automatische Trennung je Beleg. Eigenes Tötungskriterium: Trennungsgenauigkeit
unter 90 % → streichen.
4.4. **Stapel-Foto-Trennung:** Mehrere Belege auf einem Foto erkennen und
trennen (Bildanalyse).
4.5. **Fragen-Modus (wenn Gemma-4-Spike bestanden):** „Wie viel für Werkzeug?"
→ Werkzeugwahl → SQL → Antwort mit Quellen.

**Abnahme:** 50 Belege in unter 10 Minuten importiert und suchbar;
Duplikat-Erkennung über 95 % Präzision auf Testset.

---

## Phase 5 · Phase C — Bindung (3–4 Wochen)

**Ziel:** Nutzer kommen zurück, ohne gerufen zu werden.

5.1. **Fristen-Engine:** Zwei Fristen je Artikel (Rückgabe, Garantie),
editierbare Standardwerte pro Kategorie (z. B. Elektronik 24 Monate),
WorkManager-Prüfung täglich, lokale Mitteilung („Garantie Waschmaschine in
3 Wochen").
5.2. **Aufbewahrungs-Erinnerung:** Fristen 8/10 Jahre als Gedächtnisstütze mit
GoBD-Hinweis in der App (keine GoBD-Ablage behaupten).
5.3. **Tagebuch-Ausbau:** Garantie-Übersicht („läuft ab", „abgelaufen"),
leise Auffindbarkeit ohne Push-Spam (max. 1 Mitteilung je Frist).

**Abnahme:** Erinnerung erscheint zur richtigen Zeit (±1 Tag) auf Testgeräten;
Standardwerte änderbar; GoBD-Hinweis sichtbar.

---

## Phase 6 · Härtung für den Store (3–4 Wochen)

**Ziel:** Von der Beta zur store-fähigen Consumer-App. Kein neues Feature,
nur Härte.

6.1. **Geräte-Matrix:** 10 Geräte (schwach bis stark, Android 8–16),
Leistungstests (Start, Suche, Speicher), Mindestanforderung in die
Store-Beschreibung.
6.2. **Verblasste Belege:** Eigenes Testset, Fehlertoleranz der Suche,
ehrliche Kommunikation („ersetzt den Zettel nicht").
6.3. **Leere-Zustände und Onboarding:** Erste Sitzung führt zu importiertem
Beleg + erster Suche (Aktivierungsmoment aus Abschnitt 13).
6.4. **Rechtstexte:** Kurze Datenschutzerklärung (kein Server, ML-Kit-Hinweis,
Sicherungsdatei), Impressum, Widerrufsbelehrung für digitalen Kauf.
6.5. **Store-Assets:** Beschreibungstext (Beta-Hinweis Foto-Suche, Offline,
ADR-008-Regel), Screenshots, Feature-Grafik, Altersfreigabe, Kategorie.
6.6. **Absturz- und Fehlerbehandlung:** Offline-Crash-Dialog (kein Versand),
beschädigte Sicherung abfangen, Vollspeicher-Fall.
6.7. **Update-Pfad:** Schema-Migrationen (Room), Modell-Update per
App-Update (PAD), Rückwärtskompatibilität der Sicherung (Versionsfeld).

**Abnahme:** Keine Abstürze auf der Geräte-Matrix bei Kernflows; Store-Review
ohne Nachfragen zu Berechtigungen; Sicherung v1 auf v2 lesbar.

---

## Phase 7 · Öffentlicher Start (2–3 Wochen + laufend)

**Ziel:** Vollwertige Consumer-App im Play Store.

7.1. **Offener Test (1 Woche):** Öffentlicher Beta-Track, Bewertungen noch
ausgeschlossen, letzte Fehler sammeln.
7.2. **Release 1.0:** Phased Rollout (10 % → 50 % → 100 %), Beobachtung
(Store-Bewertungen, Erstattungsquote, Absturzrate per Vitals — geräteseitig,
kein eigenes Tracking).
7.3. **Support-Betrieb:** FAQ (Offline, Sicherung, Foto-Beta, Testphase),
Antwortzeit-Ziel 48 h, Erstattungsregel kulant in den ersten 14 Tagen.
7.4. **Preis-Test:** Nach 500 Verkäufen prüfen, ob 14,99 Euro trägt
(Erstattung, Bewertungslage, Concierge-Erkenntnisse). Erst dann anfassen.

**Abnahme:** App öffentlich installierbar, Kauf funktioniert, Lesemodus ohne
Kauf verifizierbar, Sicherung über Gerätewechsel getestet.

**Nicht in 1.0:** Phase D (E-Rechnungs-Parsing, Bankabgleich, DATEV-Export,
Jahreslizenz), Mehrbenutzer, Web-Dashboard, iOS. Das ist die Business-Roadmap
nach bewiesener Consumer-Traktion.

---

## Reihenfolge und Abhängigkeiten

```
Phase 0 (Spike) ──┬── Phase 1 (Markt, parallel)
                  │
                  ▼
            Phase 2 (Prototyp) ── Stopp möglich
                  │
                  ▼
            Phase 3 (Beta-App) ── Stopp möglich
                  │
                  ▼
            Phase 4 (Reibung) ── parallel planbar, erst nach stabiler Phase 3
                  │
                  ▼
            Phase 5 (Bindung)
                  │
                  ▼
            Phase 6 (Härtung) ── keine Features mehr
                  │
                  ▼
            Phase 7 (Store)
```

**Eiserne Regeln:**

1. Kein Code vor dem Spike-Bericht.
2. Kein Store vor bestandenen Abnahmekriterien Phase A.
3. Kein Feature in Phase 6 — nur Härte.
4. Jede Phase endet mit Go/No-Go nach Tötungskriterien.
5. ADR-008 ab der ersten Version mit Kauf: testen, dass Lesen/Export/Sicherung
   ohne Lizenz funktionieren.
