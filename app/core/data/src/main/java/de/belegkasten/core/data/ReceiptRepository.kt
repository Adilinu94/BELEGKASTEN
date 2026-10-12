package de.belegkasten.core.data

import de.belegkasten.core.database.DeadlineDao
import de.belegkasten.core.database.EmbeddingDao
import de.belegkasten.core.database.ItemDao
import de.belegkasten.core.database.ReceiptDao
import de.belegkasten.core.model.Cents
import de.belegkasten.core.model.IsoDate
import de.belegkasten.core.model.Receipt
import de.belegkasten.core.model.ReceiptSource
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import de.belegkasten.core.database.ReceiptEntity as DbReceipt

// Single source of truth for receipts. Money math happens here in SQL/Kotlin,
// never inside a model (Bauplan Leitplanke).
class ReceiptRepository @Inject constructor(
    private val receipts: ReceiptDao,
    private val items: ItemDao,
    private val embeddings: EmbeddingDao,
    private val deadlines: DeadlineDao,
) {
    fun observeAll(): Flow<List<Receipt>> = receipts.observeAll().map { list ->
        list.map { it.toModel() }
    }

    suspend fun sumByCategory(category: String, from: IsoDate, to: IsoDate): Cents {
        // Implemented via DAO filter + Kotlin sum to keep SQL portable (no FTS dependency).
        val all = receipts.filterCandidates(null, null, Int.MAX_VALUE)
        return Cents(
            all.filter { it.category == category && it.date in from.value..to.value }
                .sumOf { it.totalCents },
        )
    }

    private fun DbReceipt.toModel() = Receipt(
        id = id,
        merchant = merchant,
        date = IsoDate(date),
        total = Cents(totalCents),
        category = category,
        imagePath = imagePath,
        source = runCatching { ReceiptSource.valueOf(source) }.getOrDefault(ReceiptSource.CAMERA),
        attachmentPath = attachmentPath,
        createdAtEpochMs = createdAtEpochMs,
    )
}
