"""Prüft die Room-Datenbank ohne Android gegen echtes SQLite (mit FTS4).

  python tools/check_room_sql.py [Pfad/zu/app/core/database]

  1. Jede @Query aus Daos.kt läuft per EXPLAIN gegen das von Room exportierte Schema
     (neueste Version unter schemas/). Fängt SQL-Syntax- und Spaltenfehler.
  2. Die Volltextabfrage liefert mit dem MATCH-Aufbau aus SearchEngine.kt die erwarteten Treffer.
  3. Migration 1->2: das v1-Schema aus DatabaseTest.kt plus die execSQL-Zeilen von MIGRATION_1_2
     ergeben das Schema aus 2.json; Altdaten bleiben erhalten und sind per Volltext auffindbar.

Nicht geprüft: Room-Codegenerierung, Hilt, die SQLite-Version des Zielgeräts (hier: die von Python).
Setzt voraus, dass schemas/N.json zu Entities.kt passt; Room schreibt die Datei bei jedem Build neu.
"""
import json
import re
import sqlite3
import sys
from pathlib import Path

DEFAULT_ROOT = Path(__file__).resolve().parents[1] / "app" / "core" / "database"
MIGRATION_TARGET = 2  # Version, die MIGRATION_1_2 erzeugen muss; bei einer weiteren Migration erweitern
PKG = "src/main/java/de/belegkasten/core/database"


def fail(msg):
    sys.exit(f"FEHLER: {msg}")


def balanced(text, start, opening, closing):
    """Inhalt zwischen der Klammer bei `start` und ihrer Gegenklammer; Kotlin-Strings werden übersprungen."""
    depth, i, in_str = 0, start, False
    while i < len(text):
        c = text[i]
        if in_str:
            if c == "\\":
                i += 1
            elif c == '"':
                in_str = False
        elif c == '"':
            in_str = True
        elif c == opening:
            depth += 1
        elif c == closing:
            depth -= 1
            if depth == 0:
                return text[start + 1:i]
        i += 1
    fail("Klammer nicht geschlossen, Quelltext nicht lesbar")


def exec_sql(text):
    return [s.replace('\\"', '"') for s in re.findall(r'execSQL\("((?:[^"\\]|\\.)*)"\)', text)]


def load_schemas(root):
    files = {int(p.stem): p for p in root.glob("schemas/*/*.json") if p.stem.isdigit()}
    if not files:
        fail(f"kein exportiertes Room-Schema unter {root}/schemas gefunden")
    return {v: json.loads(p.read_text(encoding="utf-8"))["database"] for v, p in files.items()}


def create(con, schema):
    for e in schema["entities"]:
        con.execute(e["createSql"].replace("${TABLE_NAME}", e["tableName"]))
        for ix in e.get("indices", []):
            con.execute(ix["createSql"].replace("${TABLE_NAME}", e["tableName"]))


def read_queries(root):
    src = (root / PKG / "Daos.kt").read_text(encoding="utf-8")
    queries = []
    for m in re.finditer(r"@Query\(", src):
        body = balanced(src, m.end() - 1, "(", ")")
        queries.append("".join(re.findall(r'"((?:[^"\\]|\\.)*)"', body)).replace('\\"', '"'))
    if not queries:
        fail("keine @Query in Daos.kt gefunden")
    return queries


def check_queries(con, queries):
    problems = []
    for q in queries:
        params = {n: (5 if n == "limit" else "x") for n in re.findall(r":(\w+)", q)}
        try:
            con.execute("EXPLAIN " + q, params).fetchall()
        except sqlite3.Error as ex:
            flat = re.sub(r"\s+", " ", q)[:100]
            problems.append(f"@Query ungültig: {flat} -> {ex}")
    print(f"{len(queries)} @Query geprüft")
    return problems


def check_fulltext(con, queries):
    """Die MATCH-Abfrage aus Daos.kt mit dem Aufbau aus SearchEngine.kt: Wörter normalisiert, mit * und OR verbunden."""
    fts = [q for q in queries if "MATCH" in q]
    if len(fts) != 1:
        fail(f"genau eine @Query mit MATCH erwartet, gefunden: {len(fts)}")
    sql = fts[0]
    info = list(con.execute("PRAGMA table_info(receipts)"))  # cid, name, type, notnull, default, pk

    def add(rid, merchant, text):
        given = {"id": rid, "merchant": merchant, "date": "2026-03-10", "category": "Werkzeug", "ocrText": text, "source": "CAMERA"}
        row = {}
        for _, name, typ, notnull, dflt, _pk in info:
            row[name] = given.get(name, (1 if "INT" in typ.upper() else "x") if notnull and dflt is None else None)
        con.execute(f"INSERT INTO receipts ({','.join(row)}) VALUES ({','.join('?' * len(row))})", list(row.values()))
        con.execute("INSERT INTO receipt_fts (receiptId, merchant, category, ocrText) VALUES (?,?,?,?)", (rid, merchant, "Werkzeug", text))

    add("R1", "BAUHAUS", "Bohrmaschine Akku 18V")
    add("R2", "dm", "Zahnpasta Duschgel")
    add("R3", "Bäckerei Müller", "Brötchen Käsekuchen")
    problems = []
    for match, want in [("bohr*", {"R1"}), ("bohr* OR zahnpasta*", {"R1", "R2"}), ("backerei*", {"R3"}), ("unbekannt*", set())]:
        try:
            got = {r[0] for r in con.execute(sql, {"query": match})}
        except sqlite3.Error as ex:
            problems.append(f"Volltext MATCH '{match}' schlägt fehl: {ex}")
            continue
        if got != want:
            problems.append(f"Volltext MATCH '{match}' liefert {sorted(got)}, erwartet {sorted(want)}")
    print("Volltextabfrage geprüft")
    return problems


def check_migration(root, expected):
    test = root / "src/androidTest/java/de/belegkasten/core/database/DatabaseTest.kt"
    mig = root / PKG / "BelegkastenDb.kt"
    if not test.is_file() or not mig.is_file():
        fail("DatabaseTest.kt oder BelegkastenDb.kt nicht gefunden")
    v1 = exec_sql(test.read_text(encoding="utf-8"))
    src = mig.read_text(encoding="utf-8")
    at = src.find("MIGRATION_1_2")
    if at < 0:
        fail("MIGRATION_1_2 nicht gefunden")
    steps = exec_sql(balanced(src, src.index("{", at), "{", "}"))
    if not v1 or not steps:
        fail(f"v1-Schema ({len(v1)} Anweisungen) oder Migrationsschritte ({len(steps)}) nicht lesbar")

    con = sqlite3.connect(":memory:")
    con.execute("PRAGMA foreign_keys=ON")
    for s in v1 + steps:
        con.execute(s)

    problems = []
    for e in expected["entities"]:
        t = e["tableName"]
        have = {r[1]: r for r in con.execute(f"PRAGMA table_info({t})")}  # cid, name, type, notnull, default, pk
        want_cols = [f["columnName"] for f in e["fields"]]
        if "ftsVersion" in e:
            if list(have) != want_cols:
                problems.append(f"Migration: {t} hat Spalten {list(have)}, erwartet {want_cols}")
            sql = (con.execute("SELECT sql FROM sqlite_master WHERE name=?", (t,)).fetchone() or [""])[0]
            if "unicode61" not in sql:
                problems.append(f"Migration: {t} ohne Tokenizer unicode61")
            continue
        for f in e["fields"]:
            r = have.get(f["columnName"])
            if r is None:
                problems.append(f"Migration: {t}.{f['columnName']} fehlt")
            elif r[2].upper() != f["affinity"].upper() or bool(r[3]) != bool(f["notNull"]) or r[4] != f.get("defaultValue"):
                problems.append(f"Migration: {t}.{f['columnName']} ist ({r[2]}, notnull={r[3]}, default={r[4]!r}), "
                                f"erwartet ({f['affinity']}, notnull={f['notNull']}, default={f.get('defaultValue')!r})")
        extra = set(have) - set(want_cols)
        if extra:
            problems.append(f"Migration: {t} hat Spalten, die Room nicht kennt: {sorted(extra)}")
        for ix in e.get("indices", []):
            if not con.execute("SELECT 1 FROM sqlite_master WHERE type='index' AND name=?", (ix["name"],)).fetchone():
                problems.append(f"Migration: Index {ix['name']} fehlt")
    old = con.execute("SELECT total_cents FROM receipts WHERE id='OLD'").fetchone()
    hit = con.execute("SELECT receiptId FROM receipt_fts WHERE receipt_fts MATCH 'akkuschrauber*'").fetchall()
    if old != (8999,):
        problems.append("Migration: Altbeleg 'OLD' fehlt oder wurde verändert")
    if hit != [("OLD",)]:
        problems.append("Migration: Altbeleg nach Migration nicht per Volltext auffindbar")
    if con.execute("PRAGMA foreign_key_check").fetchall():
        problems.append("Migration: Fremdschlüssel verletzt")
    print("Migration 1->2 geprüft")
    return problems


def main():
    root = Path(sys.argv[1]) if len(sys.argv) > 1 else DEFAULT_ROOT
    schemas = load_schemas(root)
    latest = max(schemas)
    if MIGRATION_TARGET not in schemas:
        fail(f"schemas/{MIGRATION_TARGET}.json fehlt")
    con = sqlite3.connect(":memory:")
    create(con, schemas[latest])
    print(f"Schema {latest}.json aufgebaut: {[e['tableName'] for e in schemas[latest]['entities']]}")
    queries = read_queries(root)
    problems = check_queries(con, queries) + check_fulltext(con, queries) + check_migration(root, schemas[MIGRATION_TARGET])
    if problems:
        print("\nPROBLEME:", *problems, sep="\n  - ")
        sys.exit(1)
    print("Alles in Ordnung.")


if __name__ == "__main__":
    main()
