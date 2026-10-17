package de.belegkasten.core.search

import de.belegkasten.core.database.*
import kotlin.test.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking

class SearchEngineTest {
    private class FakeReceipts(val rows: List<ReceiptEntity>) : ReceiptDao {
        override fun observeAll() = flowOf(rows)
        override suspend fun byId(id: String) = rows.find { it.id == id }
        override suspend fun referencesPath(path: String) = rows.any { it.imagePath == path || it.attachmentPath == path }
        override suspend fun all() = rows
        override suspend fun fullTextCandidates(query: String) = emptyList<ReceiptEntity>()
        override suspend fun filterCandidates(merchant: String?, month: String?, limit: Int) = rows.filter {
            (merchant == null || it.merchant.contains(merchant, true)) && (month == null || it.date.startsWith(month)) }.take(limit)
        override suspend fun insert(receipt: ReceiptEntity) = error("unused")
        override suspend fun index(entry: ReceiptFtsEntity) = error("unused")
        override suspend fun count() = rows.size
    }
    private fun receipt(id: String, merchant: String, date: String, category: String, text: String) =
        ReceiptEntity(id, merchant, date, 1000, category, null, "IMPORT", null, 0, text)
    private val base = listOf(
        receipt("R1", "H&M", "2026-03-14", "Kleidung", "WINTERJACKE PARKA"),
        receipt("R2", "ATU", "2024-06-19", "Auto", "MOTOROEL SCHEIBENWISCHER"),
        receipt("R3", "BAUHAUS", "2025-03-14", "Baumarkt", "BOHRMASCHINE"),
        receipt("R4", "dm", "2025-10-07", "Drogerie", "DUSCHGEL"),
    )
    private fun search(q: String, rows: List<ReceiptEntity> = base) = runBlocking { SearchEngine(FakeReceipts(rows)).search(q) }.map { it.receiptId }
    @Test fun itemWithoutMerchantFound() { assertEquals(listOf("R1"), search("Winterjacke 2026")) }
    @Test fun merchantAnywhereInQueryFound() { assertEquals(listOf("R2"), search("Motoroel bei ATU")) }
    @Test fun germanMonthAndCategory() { assertEquals(listOf("R3"), search("der vom Baumarkt im März")); assertEquals(listOf("R3"), search("Baumarkt maerz")) }
    @Test fun explicitYearRestricts() { assertEquals(listOf("R3"), search("März 2025")); assertTrue(search("März 2024").isEmpty()) }
    @Test fun noFalseMonthSubstring() { assertEquals(listOf("R4"), search("DUSCHGEL")); assertTrue(search("Mai").isEmpty()) }
    @Test fun oldestAmongManyFound() {
        val rows = (1..300).map { receipt("F$it", "Filler", "2026-09-01", "Sonstiges", "Kram") } + base
        assertEquals(listOf("R1"), search("Winterjacke", rows))
    }
    @Test fun emptyAndUnknownQueriesEmpty() { assertTrue(search("").isEmpty()); assertTrue(search("unbekannt").isEmpty()) }
    @Test fun shortMerchantWorks() { assertEquals(listOf("R4"), search("dm")) }
}
