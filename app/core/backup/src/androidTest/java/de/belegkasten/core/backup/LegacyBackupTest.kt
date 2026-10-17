package de.belegkasten.core.backup

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import de.belegkasten.core.embed.EmbeddingStore
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.*
import org.junit.Test

class LegacyBackupTest {
    @Test fun readsVersionOnePortableOriginalAndRejectsBadHash() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dir = File.createTempFile("legacy-backup-test", "", context.cacheDir).also { it.delete(); it.mkdir() }
        try {
            val database = File(dir, "old.db")
            SQLiteDatabase.openOrCreateDatabase(database, null).use { db ->
                db.execSQL("CREATE TABLE receipts (id TEXT, merchant TEXT, date TEXT, total_cents INTEGER, category TEXT, imagePath TEXT, source TEXT, attachmentPath TEXT, createdAtEpochMs INTEGER)")
                db.execSQL("CREATE TABLE items (id TEXT, receipt_id TEXT, label TEXT, quantity INTEGER, price_cents INTEGER)")
                db.execSQL("CREATE TABLE embeddings (receipt_id TEXT, dim INTEGER, vector BLOB)")
                db.execSQL("CREATE TABLE deadlines (id TEXT, item_id TEXT, type TEXT, from_default INTEGER, due_date TEXT, notified INTEGER)")
                db.execSQL("INSERT INTO receipts VALUES ('OLD','Test','2026-10-16',100,'Test','/data/user/0/old.package/files/images/a.jpeg','CAMERA',NULL,1)")
                db.execSQL("INSERT INTO items VALUES ('OLD-I','OLD','Bohrer',1,100)")
                db.version = 1
            }
            val image = File(dir, "a.jpeg").also { it.writeText("original") }
            fun archive(badHash: Boolean): File {
                val zip = File(dir, "legacy.zip")
                ZipOutputStream(zip.outputStream()).use { out ->
                    mapOf("db/belegkasten.db" to database, "images/a.jpeg" to image).forEach { (name, file) ->
                        out.putNextEntry(ZipEntry(name)); file.inputStream().use { it.copyTo(out) }; out.closeEntry()
                    }
                    out.putNextEntry(ZipEntry("manifest.json"))
                    out.write("""{"format":1,"app":"belegkasten","files":{"db/belegkasten.db":"${BackupArchive.hash(database)}","images/a.jpeg":"${if (badHash) "bad" else BackupArchive.hash(image)}"}}""".toByteArray())
                    out.closeEntry()
                }
                return zip
            }
            val work = File(dir, "work").also { it.mkdir() }
            val result = LegacyBackup.read(archive(false), work, EmbeddingStore())
            assertEquals("images/a.jpeg", result.snapshot.receipts.single().imagePath)
            assertEquals("original", result.media.getValue("images/a.jpeg").readText())
            assertEquals("Bohrer", result.snapshot.items.single().label)
            assertFails { LegacyBackup.read(archive(true), work, EmbeddingStore()) }
        } finally { dir.deleteRecursively() }
    }
}
