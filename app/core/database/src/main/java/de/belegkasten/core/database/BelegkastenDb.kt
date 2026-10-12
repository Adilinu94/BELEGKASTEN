package de.belegkasten.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

// version 1: receipts, items, embeddings, deadlines. Migrations must be added
// for every schema change (Phase 6.7) and keep backup files readable.
@Database(
    entities = [ReceiptEntity::class, ItemEntity::class, EmbeddingEntity::class, DeadlineEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class BelegkastenDb : RoomDatabase() {
    abstract fun receipts(): ReceiptDao
    abstract fun items(): ItemDao
    abstract fun embeddings(): EmbeddingDao
    abstract fun deadlines(): DeadlineDao
}
