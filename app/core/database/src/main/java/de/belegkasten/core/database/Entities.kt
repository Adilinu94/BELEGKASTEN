package de.belegkasten.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// Amounts in cents, dates ISO-8601. Checked by CheckBeforeSave (core:data).
@Entity(tableName = "receipts")
data class ReceiptEntity(
    @PrimaryKey val id: String,
    val merchant: String,
    // yyyy-MM-dd
    val date: String,
    @ColumnInfo(name = "total_cents") val totalCents: Long,
    val category: String,
    val imagePath: String?,
    val source: String,
    val attachmentPath: String? = null,
    val createdAtEpochMs: Long,
)

@Entity(
    tableName = "items",
    foreignKeys = [ForeignKey(
        entity = ReceiptEntity::class,
        parentColumns = ["id"],
        childColumns = ["receipt_id"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("receipt_id")],
)
data class ItemEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "receipt_id") val receiptId: String,
    val label: String,
    val quantity: Int,
    @ColumnInfo(name = "price_cents") val priceCents: Long,
)

// 256-dim float32 vector as BLOB (1024 bytes per receipt).
// Cosine similarity is computed in code (no native vector extension).
@Entity(
    tableName = "embeddings",
    foreignKeys = [ForeignKey(
        entity = ReceiptEntity::class,
        parentColumns = ["id"],
        childColumns = ["receipt_id"],
        onDelete = ForeignKey.CASCADE,
    )],
)
data class EmbeddingEntity(
    @PrimaryKey @ColumnInfo(name = "receipt_id") val receiptId: String,
    val dim: Int,
    val vector: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EmbeddingEntity) return false
        return receiptId == other.receiptId && dim == other.dim && vector.contentEquals(other.vector)
    }

    override fun hashCode(): Int {
        var r = receiptId.hashCode()
        r = 31 * r + dim
        r = 31 * r + vector.contentHashCode()
        return r
    }
}

@Entity(
    tableName = "deadlines",
    foreignKeys = [ForeignKey(
        entity = ItemEntity::class,
        parentColumns = ["id"],
        childColumns = ["item_id"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("item_id"), Index("due_date")],
)
data class DeadlineEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "item_id") val itemId: String,
    // RETURN | WARRANTY
    val type: String,
    @ColumnInfo(name = "from_default") val fromDefault: Boolean,
    @ColumnInfo(name = "due_date") val dueDate: String,
    val notified: Boolean = false,
)
