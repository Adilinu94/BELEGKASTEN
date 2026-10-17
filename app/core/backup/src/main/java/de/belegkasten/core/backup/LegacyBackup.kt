package de.belegkasten.core.backup

import android.database.sqlite.SQLiteDatabase
import com.google.gson.JsonParser
import de.belegkasten.core.data.ArchiveSnapshot
import de.belegkasten.core.data.validateSnapshot
import de.belegkasten.core.database.*
import de.belegkasten.core.embed.EmbeddingStore
import java.io.File
import java.util.zip.ZipInputStream

// Read-only adapter for the previous PBKDF2/raw-SQLite prototype.
// Its old exporter did not checkpoint WAL: missing data cannot be reconstructed.
object LegacyBackup {
    fun read(file: File, work: File, store: EmbeddingStore): ValidatedArchive {
        val files = linkedMapOf<String, File>()
        var size = 0L
        ZipInputStream(file.inputStream()).use { zip ->
            while (true) {
                val e = zip.nextEntry ?: break
                require(!e.isDirectory && (e.name in setOf("manifest.json", "db/belegkasten.db") || BackupArchive.safeName(e.name))) { "Unsicherer Alt-Sicherungspfad" }
                require(e.name !in files && files.size < 10000)
                val dest = File(work, "legacy-${files.size}")
                dest.outputStream().use { size += BackupArchive.copyLimited(zip, it, 32L * 1024 * 1024) }
                require(size <= BackupArchive.MAX_ARCHIVE_BYTES)
                files[e.name] = dest
            }
        }
        val manifest = JsonParser.parseString(requireNotNull(files.remove("manifest.json")).readText()).asJsonObject
        require(manifest.get("format").asInt == 1 && manifest.get("app").asString == "belegkasten")
        val hashes = manifest.getAsJsonObject("files")
        require(hashes.keySet() == files.keys)
        files.forEach { (name, data) -> require(BackupArchive.hash(data) == hashes.get(name).asString) { "Alt-Sicherung: Prüfsumme falsch" } }
        val database = requireNotNull(files.remove("db/belegkasten.db")) { "Alt-Sicherung ohne Datenbank" }
        val snapshot = SQLiteDatabase.openDatabase(database.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            require(db.version in 1..2) { "Nicht unterstützte Datenbankversion" }
            db.rawQuery("PRAGMA integrity_check", null).use { require(it.moveToFirst() && it.getString(0) == "ok") { "Datenbank beschädigt" } }
            val receipts = mutableListOf<ReceiptEntity>()
            db.rawQuery("SELECT * FROM receipts", null).use { c ->
                while (c.moveToNext()) {
                    fun str(name: String) = c.getString(c.getColumnIndexOrThrow(name))
                    fun path(name: String): String? {
                        val original = str(name) ?: return null
                        val prefix = if (name == "imagePath") "images" else "attachments"
                        val portable = "$prefix/${File(original).name}"
                        require(BackupArchive.safeName(portable) && portable in files) { "Alt-Sicherung: Anhang fehlt; Wiederherstellung abgebrochen" }
                        return portable
                    }
                    receipts += ReceiptEntity(str("id"), str("merchant"), str("date"), c.getLong(c.getColumnIndexOrThrow("total_cents")),
                        str("category"), path("imagePath"), str("source"), path("attachmentPath"), c.getLong(c.getColumnIndexOrThrow("createdAtEpochMs")),
                        if (db.version >= 2) str("ocrText") else "")
                }
            }
            val items = mutableListOf<ItemEntity>()
            db.rawQuery("SELECT * FROM items", null).use { c -> while (c.moveToNext()) {
                items += ItemEntity(c.getString(c.getColumnIndexOrThrow("id")), c.getString(c.getColumnIndexOrThrow("receipt_id")),
                    c.getString(c.getColumnIndexOrThrow("label")), c.getInt(c.getColumnIndexOrThrow("quantity")), c.getLong(c.getColumnIndexOrThrow("price_cents")))
            } }
            val embeddings = mutableListOf<EmbeddingEntity>()
            db.rawQuery("SELECT * FROM embeddings", null).use { c -> while (c.moveToNext()) {
                embeddings += EmbeddingEntity(c.getString(c.getColumnIndexOrThrow("receipt_id")), c.getInt(c.getColumnIndexOrThrow("dim")), c.getBlob(c.getColumnIndexOrThrow("vector")))
            } }
            val deadlines = mutableListOf<DeadlineEntity>()
            db.rawQuery("SELECT * FROM deadlines", null).use { c -> while (c.moveToNext()) {
                deadlines += DeadlineEntity(c.getString(c.getColumnIndexOrThrow("id")), c.getString(c.getColumnIndexOrThrow("item_id")),
                    c.getString(c.getColumnIndexOrThrow("type")), c.getInt(c.getColumnIndexOrThrow("from_default")) != 0,
                    c.getString(c.getColumnIndexOrThrow("due_date")), c.getInt(c.getColumnIndexOrThrow("notified")) != 0)
            } }
            ArchiveSnapshot(receipts, items, embeddings, deadlines)
        }
        validateSnapshot(snapshot, store)
        val referenced = snapshot.receipts.flatMap { listOfNotNull(it.imagePath, it.attachmentPath) }.toSet()
        require(referenced == files.keys) { "Nicht referenzierte/fehlende Alt-Anhänge" }
        return ValidatedArchive(snapshot, files)
    }
}
