package de.belegkasten.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// version 1: receipts, items, embeddings, deadlines. Migrations must be added
// for every schema change (Phase 6.7) and keep backup files readable.
@Database(
    entities = [ReceiptEntity::class, ItemEntity::class, EmbeddingEntity::class, DeadlineEntity::class, ReceiptFtsEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class BelegkastenDb : RoomDatabase() {
    abstract fun receipts(): ReceiptDao
    abstract fun items(): ItemDao
    abstract fun embeddings(): EmbeddingDao
    abstract fun deadlines(): DeadlineDao
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE receipts ADD COLUMN ocrText TEXT NOT NULL DEFAULT ''")
        db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS receipt_fts USING FTS4(receiptId TEXT NOT NULL, merchant TEXT NOT NULL, category TEXT NOT NULL, ocrText TEXT NOT NULL, tokenize=unicode61)")
        db.execSQL("INSERT INTO receipt_fts (receiptId, merchant, category, ocrText) SELECT id, merchant, category, COALESCE((SELECT group_concat(label, ' ') FROM items WHERE receipt_id = receipts.id), '') FROM receipts")
    }
}
