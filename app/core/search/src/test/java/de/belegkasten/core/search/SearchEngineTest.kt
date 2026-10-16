package de.belegkasten.core.search

import de.belegkasten.core.database.EmbeddingDao
import de.belegkasten.core.database.EmbeddingEntity
import de.belegkasten.core.database.ReceiptDao
import de.belegkasten.core.database.ReceiptEntity
import de.belegkasten.core.embed.EmbeddingStore
import de.belegkasten.core.embed.HashEmbedder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking

// Bildet die SQL aus Daos.kt nach: merchant LIKE '%x%' (ohne Groß-/Kleinschreibung),
// ORDER BY date DESC, LIMIT. Nutzt HashEmbedder, prüft also die Suchlogik, nicht das Modell.
class SearchEngineTest {

    private class FakeReceipts(private val all: List<ReceiptEntity>) : ReceiptDao {
        override fun observeAll(): Flow<List<ReceiptEntity>> = flowOf(all)
        override suspend fun byId(id: String) = all.firstOrNull { it.id == id }
        override suspend fun filterCandidates(merchant: String?, month: String?, limit: Int) =
            all.filter {
                (merchant == null || it.merchant.contains(merchant, ignoreCase = true)) &&
                    (month == null || it.date.startsWith(month))
            }.sortedByDescending { it.date }.take(limit)
        override suspend fun insert(receipt: ReceiptEntity) = throw UnsupportedOperationException()
        override suspend fun count() = all.size
    }

    private class FakeEmbeddings(private val byId: Map<String, EmbeddingEntity>) : EmbeddingDao {
        override suspend fun forReceipt(receiptId: String) = byId[receiptId]
        override suspend fun all() = byId.values.toList()
        override suspend fun upsert(embedding: EmbeddingEntity) = throw UnsupportedOperationException()
    }

    private fun receipt(id: String, merchant: String, date: String, category: String) =
        ReceiptEntity(id, merchant, date, 1000, category, null, "CAMERA", null, 0)

    private fun engineFor(data: List<Pair<ReceiptEntity, String>>): SearchEngine {
        val store = EmbeddingStore()
        val embedder = HashEmbedder()
        val vectors = runBlocking {
            data.associate { (r, text) ->
                r.id to EmbeddingEntity(
                    r.id, 256, store.toBlob(embedder.embedDocument(r.merchant, "$text ${r.category}")),
                )
            }
        }
        return SearchEngine(FakeReceipts(data.map { it.first }), FakeEmbeddings(vectors), embedder, store)
    }

    private val base = listOf(
        receipt("R1", "H&M", "2026-03-14", "Kleidung") to "JEANS SLIM FIT WINTERJACKE PARKA M",
        receipt("R2", "ATU", "2024-06-19", "Auto") to "SCHEIBENWISCHER AERO MOTOROEL 5W-30 5L",
        receipt("R3", "BAUHAUS", "2025-03-14", "Baumarkt") to "BOHRMASCH. PSB 750 RCE",
        receipt("R4", "dm", "2025-10-07", "Drogerie") to "DUSCHGEL MILCH&HONIG ZAHNPASTA",
    )

    private fun top(engine: SearchEngine, query: String, k: Int = 3) =
        runBlocking { engine.search(query, k) }.map { it.receiptId }

    @Test
    fun wordThatIsNoMerchantStillFindsReceipts() {
        val engine = engineFor(base)
        assertEquals("R1", top(engine, "Winterjacke 2026").first())
        assertEquals("R2", top(engine, "Motoroel bei ATU").first())
    }

    @Test
    fun categoryWordWithMonthFindsReceipt() {
        // "Baumarkt" steht in keinem Händlernamen; der Monatsfilter bleibt aktiv.
        assertTrue("R3" in top(engineFor(base), "der vom Baumarkt im maerz"))
    }

    @Test
    fun merchantFilterStillNarrowsCandidates() {
        val engine = engineFor(base)
        assertEquals(listOf("R2"), top(engine, "ATU", k = 10))
        assertEquals("R3", top(engine, "der vom BAUHAUS im maerz").first())
    }

    @Test
    fun oldestReceiptIsFoundAmongMoreThan200() {
        val filler = (1..300).map {
            receipt("F$it", "Filler", "2026-%02d-%02d".format(1 + it % 9, 1 + it % 28), "Sonstiges") to "ZEUG KRAM"
        }
        val old = receipt("OLD", "H&M", "2020-01-02", "Kleidung") to "WINTERJACKE PARKA M"
        assertTrue("OLD" in top(engineFor(filler + old), "Winterjacke"))
    }
}
