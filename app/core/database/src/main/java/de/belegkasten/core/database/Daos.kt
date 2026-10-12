package de.belegkasten.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReceiptDao {
    @Query("SELECT * FROM receipts ORDER BY date DESC, createdAtEpochMs DESC")
    fun observeAll(): Flow<List<ReceiptEntity>>

    @Query("SELECT * FROM receipts WHERE id = :id")
    suspend fun byId(id: String): ReceiptEntity?

    // First stage of two-stage search: field filters (merchant, month) narrow candidates.
    @Query(
        "SELECT * FROM receipts WHERE " +
            "(:merchant IS NULL OR merchant LIKE '%' || :merchant || '%') AND " +
            "(:month IS NULL OR substr(date, 1, 7) = :month) " +
            "ORDER BY date DESC LIMIT :limit",
    )
    suspend fun filterCandidates(merchant: String?, month: String?, limit: Int = 200): List<ReceiptEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(receipt: ReceiptEntity)

    @Query("SELECT COUNT(*) FROM receipts")
    suspend fun count(): Int
}

@Dao
interface ItemDao {
    @Query("SELECT * FROM items WHERE receipt_id = :receiptId")
    suspend fun forReceipt(receiptId: String): List<ItemEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(items: List<ItemEntity>)
}

@Dao
interface EmbeddingDao {
    @Query("SELECT * FROM embeddings WHERE receipt_id = :receiptId")
    suspend fun forReceipt(receiptId: String): EmbeddingEntity?

    @Query("SELECT * FROM embeddings")
    suspend fun all(): List<EmbeddingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(embedding: EmbeddingEntity)
}

@Dao
interface DeadlineDao {
    @Query("SELECT * FROM deadlines WHERE notified = 0 AND due_date <= :until ORDER BY due_date")
    suspend fun dueUntil(until: String): List<DeadlineEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(deadlines: List<DeadlineEntity>)

    @Update
    suspend fun update(deadline: DeadlineEntity)
}
