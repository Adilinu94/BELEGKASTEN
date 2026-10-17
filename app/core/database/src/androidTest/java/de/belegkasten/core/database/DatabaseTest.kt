package de.belegkasten.core.database

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import kotlin.test.*
import kotlinx.coroutines.runBlocking
import org.junit.Test

class DatabaseTest {
    @Test fun ftsAndTransactionRollback() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, BelegkastenDb::class.java).build()
        try {
            db.withTransaction {
                db.receipts().insert(ReceiptEntity("R1", "BAUHAUS", "2026-10-16", 1299, "Baumarkt", null, "IMPORT", null, 1, "Bohrmaschine"))
                db.receipts().index(ReceiptFtsEntity("R1", "BAUHAUS", "Baumarkt", "Bohrmaschine"))
            }
            assertEquals("R1", db.receipts().fullTextCandidates("Bohr*").single().id)
            try { db.withTransaction {
                db.receipts().insert(ReceiptEntity("R2", "dm", "2026-10-16", 0, "Drogerie", null, "IMPORT", null, 2))
                error("rollback")
            } } catch (_: IllegalStateException) { }
            assertEquals(1, db.receipts().count())
        } finally { db.close() }
    }
    @Test fun migrationPreservesVersionOneReceipt() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "belegkasten-migration-test.db"
        context.deleteDatabase(name)
        val file = context.getDatabasePath(name).also { it.parentFile!!.mkdirs() }
        SQLiteDatabase.openOrCreateDatabase(file, null).use { old ->
            old.execSQL("CREATE TABLE receipts (id TEXT NOT NULL PRIMARY KEY, merchant TEXT NOT NULL, date TEXT NOT NULL, total_cents INTEGER NOT NULL, category TEXT NOT NULL, imagePath TEXT, source TEXT NOT NULL, attachmentPath TEXT, createdAtEpochMs INTEGER NOT NULL)")
            old.execSQL("CREATE TABLE items (id TEXT NOT NULL PRIMARY KEY, receipt_id TEXT NOT NULL, label TEXT NOT NULL, quantity INTEGER NOT NULL, price_cents INTEGER NOT NULL, FOREIGN KEY(receipt_id) REFERENCES receipts(id) ON DELETE CASCADE)")
            old.execSQL("CREATE INDEX index_items_receipt_id ON items(receipt_id)")
            old.execSQL("CREATE TABLE embeddings (receipt_id TEXT NOT NULL PRIMARY KEY, dim INTEGER NOT NULL, vector BLOB NOT NULL, FOREIGN KEY(receipt_id) REFERENCES receipts(id) ON DELETE CASCADE)")
            old.execSQL("CREATE TABLE deadlines (id TEXT NOT NULL PRIMARY KEY, item_id TEXT NOT NULL, type TEXT NOT NULL, from_default INTEGER NOT NULL, due_date TEXT NOT NULL, notified INTEGER NOT NULL, FOREIGN KEY(item_id) REFERENCES items(id) ON DELETE CASCADE)")
            old.execSQL("CREATE INDEX index_deadlines_item_id ON deadlines(item_id)")
            old.execSQL("CREATE INDEX index_deadlines_due_date ON deadlines(due_date)")
            old.execSQL("INSERT INTO receipts VALUES ('OLD','BAUHAUS','2025-03-14',8999,'Baumarkt',NULL,'IMPORT',NULL,1)")
            old.execSQL("INSERT INTO items VALUES ('OLD-ITEM','OLD','Akkuschrauber',1,8999)")
            old.version = 1
        }
        val db = Room.databaseBuilder(context, BelegkastenDb::class.java, name).addMigrations(MIGRATION_1_2).build()
        try {
            assertEquals(8999L, db.receipts().byId("OLD")!!.totalCents)
            assertEquals("", db.receipts().byId("OLD")!!.ocrText)
            assertEquals("OLD", db.receipts().fullTextCandidates("BAUHAUS").single().id)
            assertEquals("OLD", db.receipts().fullTextCandidates("Akkuschrauber").single().id)
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
