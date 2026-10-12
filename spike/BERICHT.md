# Spike-Bericht Belegkasten (Template — vor Phase 2/3 ausfuellen)

Stand: __.__.____ · Tester: ______ · Geraete: ______

## 0.1 OCR (ML Kit gebuendelt, Testset echte Belege)
| Feld | Genauigkeit |
|---|---|
| Datum | ___ % |
| Summe | ___ % |
| Haendler | ___ % |
| Umlaute/Deutsch ok? | ja / nein |

## 0.2/0.3 Suche (Recall@3, 50 Anfragen)
| Verfahren | Recall@3 |
|---|---|
| Volltext + Felder (Baseline) | ___ % |
| EmbeddingGemma 2 (Variante ___) | ___ % |
| **Differenz** | ___ Prozentpunkte (Schwelle: >= 15) |

## 0.4 Foto-Suche (30 Paare, Recall@3)
Ergebnis: ___ % (Schwelle: >= 50 %) · GO / STREICHEN

## 0.5 Offline + Geraet
| Kennzahl | Wert | Schwelle |
|---|---|---|
| Textsuche-Latenz | ___ ms | < 2000 ms |
| Foto-Latenz | ___ ms | < 5000 ms |
| RAM-Dauerlast | ___ MB | < 400 MB |
| Mitschnitt sauber? | ja / nein | ja |
| Paketgroesse / Store-Limit | ___ MB | < 500 MB Warnmarke |
| Scanner-Entscheidung | ML Kit Doc Scanner / open-android-doc-scanner | per Mitschnitt |

## 0.6 Gemma 4 Tool-Calling (falls 0.3 GO)
Groesse ___ MB · Werkzeugwahl korrekt ___ % · Latenz ___ s

## Go / No-Go
- [ ] Suche-GO (>= 15 pp) oder Idee neu bewerten
- [ ] Foto-GO (>= 50 %) oder als Beta streichen
- [ ] Offline-GO (Mitschnitt sauber) oder Versprechen streichen
- [ ] Geraet-GO (Latenz/RAM) oder Mindestanforderung festlegen

Unterschrift: ______  Datum: ______
