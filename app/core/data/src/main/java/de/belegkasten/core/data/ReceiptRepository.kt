package de.belegkasten.core.data

import de.belegkasten.core.database.DeadlineDao
import de.belegkasten.core.database.EmbeddingDao
import de.belegkasten.core.database.ItemDao
import de.belegkasten.core.database.ReceiptDao
import de.belegkasten.core.database.BelegkastenDb
import de.belegkasten.core.database.ItemEntity
import de.belegkasten.core.database.ReceiptFtsEntity
import de.belegkasten.core.database.EmbeddingEntity
import de.belegkasten.core.database.DeadlineEntity
import de.belegkasten.core.embed.Embedder
import de.belegkasten.core.embed.EmbeddingStore
import de.belegkasten.core.embed.EMBED_DIM
import de.belegkasten.core.license.LicenseManager
import androidx.room.withTransaction
import de.belegkasten.core.model.Cents
import de.belegkasten.core.model.IsoDate
import de.belegkasten.core.model.Receipt
import de.belegkasten.core.model.ReceiptSource
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import de.belegkasten.core.database.ReceiptEntity as DbReceipt

// Single source of truth for receipts. Money math happens here in SQL/Kotlin,
// never inside a model (Bauplan Leitplanke).
@Singleton
class ReceiptRepository @Inject constructor(
    private val receipts: ReceiptDao,
    private val items: ItemDao,
    private val embeddings: EmbeddingDao,
    private val deadlines: DeadlineDao,
    private val db: BelegkastenDb,
    private val gate: ArchiveGate,
    private val embedder: Embedder,
    private val store: EmbeddingStore,
    private val license: LicenseManager,
    private val diagnostics: Diagnostics,
    private val files: ReceiptFiles,
) {
    fun observeAll(): Flow<List<Receipt>> = receipts.observeAll().map { list ->
        list.map { it.toModel() }
    }

    suspend fun detail(id: String): ReceiptDetail? = db.withTransaction {
        receipts.byId(id)?.let { ReceiptDetail(it.toModel(), items.forReceipt(id), deadlines.forReceipt(id)) }
    }

    suspend fun save(receipt: DbReceipt, positions: List<ItemEntity>) = gate.exclusive {
        require(CheckBeforeSave.checkReceipt(receipt, positions).isEmpty()) { "Belegfelder oder Positionssumme ungültig" }
        val existing = receipts.byId(receipt.id)
        if (existing != null) {
            require(existing == receipt && items.forReceipt(receipt.id).sortedBy { it.id } == positions.sortedBy { it.id }) { "Beleg-ID bereits vorhanden" }
            return@exclusive
        }
        license.ensureFirstStart()
        check(license.captureEnabled()) { "Testphase beendet. Dein Archiv bleibt nutzbar." }
        listOfNotNull(receipt.imagePath, receipt.attachmentPath).forEach {
            require(files.resolve(it).isFile) { "Originaldatei fehlt. Bitte erneut importieren." }
        }
        ReceiptSource.valueOf(receipt.source)
        val vector = store.toBlob(embedder.embedDocument(receipt.merchant, receipt.ocrText))
        db.withTransaction {
            receipts.insert(receipt)
            items.insertAll(positions)
            embeddings.upsert(EmbeddingEntity(receipt.id, EMBED_DIM, vector))
            receipts.index(ReceiptFtsEntity(receipt.id, receipt.merchant, receipt.category,
                receipt.ocrText + "\n" + positions.joinToString("\n") { it.label }))
        }
        // The authoritative count is the committed DB; optional aggregate writes
        // must not make a committed receipt look like a failed save/retry.
        runCatching { license.recordReceiptAdded() }
        runCatching { diagnostics.record("capture") }
    }

    // Logical snapshot under one Room transaction: includes committed WAL data.
    // No raw SQLite file copying and no writes can race managed file export/import.
    suspend fun snapshot(): ArchiveSnapshot = db.withTransaction {
        ArchiveSnapshot(receipts.all(), items.all(), embeddings.all(), deadlines.all())
    }

    suspend fun receiptRows(): List<DbReceipt> = receipts.all()

    suspend fun discardUnreferenced(path: String) = gate.exclusive {
        if (!receipts.referencesPath(path)) files.discard(path)
    }

    suspend fun merge(snapshot: ArchiveSnapshot): Int = db.withTransaction {
        validateSnapshot(snapshot, store)
        val positionsByReceipt = snapshot.items.groupBy { it.receiptId }
        val vectorsByReceipt = snapshot.embeddings.associateBy { it.receiptId }
        var added = 0
        for (receipt in snapshot.receipts) {
            val existing = receipts.byId(receipt.id)
            if (existing != null) {
                require(existing == receipt && items.forReceipt(receipt.id).sortedBy { it.id } ==
                    positionsByReceipt[receipt.id].orEmpty().sortedBy { it.id }) {
                    "Konflikt mit vorhandenem Beleg. Das Archiv wurde nicht verändert."
                }
                val vector = vectorsByReceipt[receipt.id]
                require(vector == null || embeddings.forReceipt(receipt.id) == vector) { "Vektorkonflikt; nichts wiederhergestellt" }
                continue
            }
            receipts.insert(receipt)
            val positions = positionsByReceipt[receipt.id].orEmpty()
            items.insertAll(positions)
            vectorsByReceipt[receipt.id]?.let { embeddings.upsert(it) }
            receipts.index(ReceiptFtsEntity(receipt.id, receipt.merchant, receipt.category,
                receipt.ocrText + "\n" + positions.joinToString("\n") { it.label }))
            added++
        }
        val existingDeadlines = deadlines.all().associateBy { it.id }
        snapshot.deadlines.forEach {
            val existing = existingDeadlines[it.id]
            require(existing == null || existing == it) { "Fristenkonflikt; nichts wiederhergestellt" }
            require(existingDeadlines.values.none { other -> other.id != it.id && other.itemId == it.itemId && other.type == it.type }) { "Mehrere widersprüchliche Fristen für denselben Artikel" }
            if (existing == null) deadlines.insertAll(listOf(it))
        }
        added
    }

    suspend fun sumByCategory(category: String, from: IsoDate, to: IsoDate): Cents {
        // Implemented via DAO filter + Kotlin sum to keep SQL portable (no FTS dependency).
        val all = receipts.filterCandidates(null, null, Int.MAX_VALUE)
        return Cents(
            all.filter { it.category == category && it.date in from.value..to.value }
                .fold(0L) { sum, r -> Math.addExact(sum, r.totalCents) },
        )
    }

    suspend fun setDeadline(itemId: String, type: String, dueDate: String) = gate.exclusive {
        require(CheckBeforeSave.validDate(dueDate)) { "Ungültiges Fristdatum" }
        de.belegkasten.core.model.DeadlineType.valueOf(type)
        db.withTransaction {
            require(items.all().any { it.id == itemId }) { "Artikel fehlt" }
            val existing = deadlines.all().firstOrNull { it.itemId == itemId && it.type == type }
            if (existing == null) deadlines.insertAll(listOf(DeadlineEntity(java.util.UUID.randomUUID().toString(), itemId, type, false, dueDate)))
            else deadlines.update(existing.copy(dueDate = dueDate, fromDefault = false, notified = if (existing.dueDate == dueDate) existing.notified else false))
        }
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
        ocrText = ocrText,
    )
}

data class ReceiptDetail(val receipt: Receipt, val items: List<ItemEntity>, val deadlines: List<DeadlineEntity>)

data class ArchiveSnapshot(
    val receipts: List<DbReceipt>, val items: List<ItemEntity>,
    val embeddings: List<EmbeddingEntity>, val deadlines: List<DeadlineEntity>,
)

fun validateSnapshot(snapshot: ArchiveSnapshot, store: EmbeddingStore) {
    fun unique(ids: List<String>) = ids.all { it.isNotBlank() } && ids.toSet().size == ids.size
    require(unique(snapshot.receipts.map { it.id }) && unique(snapshot.items.map { it.id }) &&
        unique(snapshot.embeddings.map { it.receiptId }) && unique(snapshot.deadlines.map { it.id })) { "Doppelte/fehlende IDs" }
    val receiptIds = snapshot.receipts.map { it.id }.toSet()
    val itemIds = snapshot.items.map { it.id }.toSet()
    require(snapshot.items.all { it.receiptId in receiptIds } &&
        snapshot.embeddings.all { it.receiptId in receiptIds } &&
        snapshot.deadlines.all { it.itemId in itemIds }) { "Verwaiste Datensätze" }
    val positionsByReceipt = snapshot.items.groupBy { it.receiptId }
    require(snapshot.receipts.size <= 10000 && snapshot.items.size <= 100000 && snapshot.deadlines.size <= 200000) { "Archiv enthält zu viele Datensätze" }
    snapshot.receipts.forEach {
        require(CheckBeforeSave.checkReceipt(it, positionsByReceipt[it.id].orEmpty()).isEmpty()) { "Ungültiger Beleg" }
        de.belegkasten.core.model.ReceiptSource.valueOf(it.source)
        require(it.ocrText.length <= 60000 && it.merchant.length <= 500 && it.category.length <= 500) { "Belegtext zu groß" }
    }
    snapshot.embeddings.forEach { require(it.dim == EMBED_DIM); store.fromBlob(it.vector) }
    snapshot.deadlines.forEach {
        require(CheckBeforeSave.checkDeadline(it).isEmpty())
        de.belegkasten.core.model.DeadlineType.valueOf(it.type)
    }
    require(snapshot.deadlines.map { it.itemId to it.type }.distinct().size == snapshot.deadlines.size) { "Doppelte Artikelfrist" }
}
