"""Synthetisches Testkorpus fuer den Belegkasten-Spike (Phase 0).

Erzeugt deterministisch (Seed 42):
- corpus/receipts.json      ~100 deutsche Belege (Haendler, Datum, Positionen, Summen)
- corpus/queries.json       50 Suchanfragen mit Ziel-Beleg (fuer Recall@3)
- corpus/photo_pairs.json   30 Paare (Gegenstandsfoto-Beschreibung -> Beleg)

KEINE echten Personendaten. Ersetzt NICHT das Abnahme-Testset aus echten
Belegen (PRODUKT-BELEGKASTEN.md Abschnitt 14), sondern prueft nur, dass das
Mess-Harness laeuft (Plumbing).
"""
import json
import random
from datetime import date
from pathlib import Path

RNG = random.Random(42)
OUT = Path(__file__).resolve().parent.parent / "corpus"

# (Haendler, Kategorie, [(Bon-Label, Suchwort, Foto-Beschreibung, Preisspanne in Cent)])
MERCHANTS = [
    ("BAUHAUS", "Baumarkt", [
        ("BOHRMASCH. PSB 750 RCE", "Bohrmaschine", "gruene Bosch Bohrmaschine", (5999, 12999)),
        ("AKKUSCHR. GSR 18V-55", "Akkuschrauber", "blauer Bosch Akkuschrauber", (8999, 15999)),
        ("FARBROLLER-SET 3TLG", "Farbroller", "Farbroller-Set mit Wanne", (999, 2499)),
        ("HOLZSCHRAUBEN 4X40 200ST", "Holzschrauben", "Schachtel Holzschrauben", (499, 1299)),
    ]),
    ("OBI", "Baumarkt", [
        ("STICHSAEGE PST 800", "Stichsaege", "grüne Bosch Stichsaege", (6999, 11999)),
        ("SILIKON SANITAER GRAU", "Silikon", "Kartusche Sanitärsilikon", (599, 1299)),
        ("ARBEITSHANDSCHUHE L", "Arbeitshandschuhe", "graue Arbeitshandschuhe", (799, 1499)),
        ("DUEBEL-SET UX 100ST", "Duebel", "rotes Fischer Duebel-Set", (899, 1999)),
    ]),
    ("MediaMarkt", "Elektronik", [
        ("KOPFH. SONY WH-1000XM5", "Kopfhoerer", "schwarze Sony Kopfhoerer", (24999, 34999)),
        ("SSD SAMSUNG 870EVO 1TB", "SSD", "Samsung SSD Festplatte", (7999, 12999)),
        ("HDMI-KABEL 2M ULTRA", "HDMI-Kabel", "HDMI-Kabel verpackt", (1299, 2999)),
        ("WASSERKOCHER 1,7L EDELST", "Wasserkocher", "Edelstahl-Wasserkocher", (2999, 5999)),
    ]),
    ("Saturn", "Elektronik", [
        ("BT-LAUTSPR. JBL FLIP6", "Lautsprecher", "schwarzer JBL Lautsprecher", (8999, 12999)),
        ("USB-C LADENETZT. 65W", "Ladegeraet", "weisses USB-C Ladegeraet", (2499, 4499)),
        ("DRUCKERPATR. HP 305XL", "Druckerpatronen", "HP Druckerpatronen Doppelpack", (3499, 5499)),
        ("LED-DECKE BATH 24W", "Deckenlampe", "runde LED Deckenlampe", (3999, 7999)),
    ]),
    ("dm", "Drogerie", [
        ("DUSCHGEL MILCH&HONIG 250", "Duschgel", "dm Duschgel Flasche", (95, 195)),
        ("ZAHNPASTA KRÄUTER 75ML", "Zahnpasta", "Zahnpasta Tube", (145, 295)),
        ("WASCHMITTEL COLOR 20WL", "Waschmittel", "dm Waschmittel Flasche", (395, 795)),
        ("WINDELN PREMIUM GR.4", "Windeln", "Windelpackung", (899, 1499)),
    ]),
    ("IKEA", "Moebel", [
        ("BILLY REGAL WEISS 80", "Regal", "weisses Billy Regal", (4999, 7999)),
        ("KISSEN-SET FJAEDRAR 2ST", "Kissen", "zwei graue Sofakissen", (1499, 2999)),
        ("KERZEN GLIMMA 20ST", "Kerzen", "Packung Teelichter", (299, 799)),
        ("BILDERRAHMEN RIBBA 50", "Bilderrahmen", "schwarzer Bilderrahmen", (999, 1999)),
    ]),
    ("EDEKA", "Lebensmittel", [
        ("KAFFEEBOHNEN CREMA 1KG", "Kaffee", "Kaffeepackung Bohnen", (999, 1599)),
        ("OLIVENOEL NATIV 750ML", "Olivenoel", "Olivenoelflasche", (699, 1299)),
        ("BERGKAESE 400G STK", "Kaese", "Stueck Bergkaese", (499, 999)),
        ("SCHOKOLADE VOLLMILCH 100", "Schokolade", "Schokoladentafel", (99, 199)),
    ]),
    ("Deichmann", "Schuhe", [
        ("SNEAKER COURT VISION 43", "Sneaker", "weisse Sneaker", (3999, 6999)),
        ("WANDERSCHUHE MID GTX 44", "Wanderschuhe", "braune Wanderschuhe", (7999, 12999)),
        ("EINLEGESOHLEN COMFORT", "Einlegesohlen", "Einlegesohlen Paar", (799, 1499)),
    ]),
    ("ATU", "Auto", [
        ("SCHEIBENWISCHER AERO 650", "Scheibenwischer", "Scheibenwischer Paar", (1999, 3499)),
        ("MOTOROEL 5W-30 5L", "Motoroel", "Motoroel Kanister", (3499, 5999)),
        ("BATTERIELADEGER. 4A", "Batterieladegeraet", "Autobatterie-Ladegeraet", (4999, 8999)),
    ]),
    ("Apotheke am Markt", "Gesundheit", [
        ("NASENSPRAY 0,1% 10ML", "Nasenspray", "Nasenspray Flasche", (495, 895)),
        ("VITAMIN D3 1000 90ST", "Vitamin D", "Vitamin D Tablettendose", (895, 1495)),
        ("PFLASTER-SET WASSERFEST", "Pflaster", "Pflasterbox", (395, 795)),
    ]),
    ("H&M", "Kleidung", [
        ("JEANS SLIM FIT 32/32", "Jeans", "blaue Jeans", (1999, 3999)),
        ("T-SHIRT 3ER-PACK WEISS", "T-Shirts", "drei weisse T-Shirts", (1299, 2499)),
        ("WINTERJACKE PARKA M", "Winterjacke", "schwarze Winterjacke", (5999, 9999)),
    ]),
    ("Thalia", "Buecher", [
        ("ROMAN TASCHENBUCH BESTS", "Roman", "Taschenbuch Roman", (1299, 1699)),
        ("KOCHBUCH ITALIENISCH", "Kochbuch", "Kochbuch mit Pasta-Cover", (2499, 3499)),
        ("NOTIZBUCH A5 LINIERT", "Notizbuch", "schwarzes Notizbuch", (899, 1499)),
    ]),
]

MONTHS = ["januar", "februar", "maerz", "april", "mai", "juni", "juli",
          "august", "september", "oktober", "november", "dezember"]

QUERY_TEMPLATES = [
    "der beleg von {merchant} im {month}",
    "der vom {merchant_short} im {month}",
    "{item_query} bei {merchant}",
    "die {item_query} vom {month}",
    "{item_query} {year}",
]


def make_receipts(n=100):
    receipts = []
    for i in range(n):
        merchant, category, items = RNG.choice(MERCHANTS)
        year = RNG.choice([2024, 2025, 2025, 2026])
        month = RNG.randint(1, 12 if year < 2026 else 9)
        day = RNG.randint(1, 28)
        picked = RNG.sample(items, k=RNG.randint(1, min(4, len(items))))
        lines, total = [], 0
        positions = []
        for label, query, photo, (lo, hi) in picked:
            qty = RNG.randint(1, 3)
            price = RNG.randrange(lo, hi + 1)
            lines.append(f"{qty}x {label} {price / 100:.2f}".replace(".", ","))
            total += qty * price
            positions.append({"label": label, "query": query, "photo": photo,
                              "qty": qty, "price_cents": price})
        rid = f"R{i + 1:04d}"
        text = (f"{merchant} Filiale 12\n{day:02d}.{month:02d}.{year}\n"
                + "\n".join(lines)
                + f"\nSUMME {total / 100:.2f}".replace(".", ",")
                + f"\nBON-NR {100000 + i}\nKategorie: {category}")
        receipts.append({
            "id": rid, "merchant": merchant, "category": category,
            "date": f"{year}-{month:02d}-{day:02d}",
            "total_cents": total, "positions": positions, "text": text,
        })
    return receipts


def make_queries(receipts, n=50):
    queries = []
    for qi in range(n):
        r = RNG.choice(receipts)
        y, m, _ = r["date"].split("-")
        pos = RNG.choice(r["positions"])
        tpl = RNG.choice(QUERY_TEMPLATES)
        q = tpl.format(merchant=r["merchant"], merchant_short=r["merchant"].split()[0],
                       month=MONTHS[int(m) - 1], item_query=pos["query"], year=y)
        queries.append({"id": f"Q{qi + 1:03d}", "text": q, "target": r["id"]})
    return queries


def make_photo_pairs(receipts, n=30):
    pairs = []
    used = set()
    cands = [(r["id"], p["photo"]) for r in receipts for p in r["positions"]]
    RNG.shuffle(cands)
    for rid, photo in cands:
        if photo in used:
            continue
        used.add(photo)
        pairs.append({"id": f"P{len(pairs) + 1:03d}", "object": photo,
                      "target": rid})
        if len(pairs) >= n:
            break
    return pairs


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    receipts = make_receipts()
    queries = make_queries(receipts)
    pairs = make_photo_pairs(receipts)
    (OUT / "receipts.json").write_text(json.dumps(receipts, ensure_ascii=False, indent=1), encoding="utf-8")
    (OUT / "queries.json").write_text(json.dumps(queries, ensure_ascii=False, indent=1), encoding="utf-8")
    (OUT / "photo_pairs.json").write_text(json.dumps(pairs, ensure_ascii=False, indent=1), encoding="utf-8")
    print(f"OK: {len(receipts)} Belege, {len(queries)} Anfragen, {len(pairs)} Foto-Paare -> {OUT}")


if __name__ == "__main__":
    main()
