package de.belegkasten.core.data

import de.belegkasten.core.database.ItemEntity
import de.belegkasten.core.database.ReceiptEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Pure JVM tests: no Android needed. Run with ./gradlew :core:data:testDebugUnitTest
// (or kotlinc directly if Gradle is offline).
class CheckBeforeSaveTest {
    private fun receipt(total: Long, date: String = "2025-03-14") = ReceiptEntity(
        id = "R1", merchant = "BAUHAUS", date = date, totalCents = total,
        category = "Baumarkt", imagePath = null, source = "CAMERA",
        createdAtEpochMs = 0,
    )

    @Test
    fun matchingSumPasses() {
        val items = listOf(ItemEntity("I1", "R1", "Bohrer", 1, 8999))
        assertTrue(CheckBeforeSave.checkReceipt(receipt(8999), items).isEmpty())
    }

    @Test
    fun mismatchedSumIsFlagged() {
        val items = listOf(ItemEntity("I1", "R1", "Bohrer", 1, 8999))
        val problems = CheckBeforeSave.checkReceipt(receipt(9000), items)
        assertEquals(1, problems.filterIsInstance<CheckBeforeSave.Problem.SumMismatch>().size)
    }

    @Test
    fun badDateIsFlagged() {
        val problems = CheckBeforeSave.checkReceipt(receipt(0, "14.03.2025"), emptyList())
        assertEquals(1, problems.filterIsInstance<CheckBeforeSave.Problem.BadDate>().size)
    }

    @Test fun invalidCalendarDatesRejected() {
        listOf("2025-02-30", "2025-02-29", "2025-13-01", "0000-00-00").forEach {
            assertTrue(CheckBeforeSave.checkReceipt(receipt(0, it), emptyList()).any { p -> p is CheckBeforeSave.Problem.BadDate })
        }
        assertTrue(CheckBeforeSave.validDate("2024-02-29"))
    }
    @Test fun overflowingAmountRejected() {
        val positions = listOf(ItemEntity("I1", "R1", "Artikel", 2, Long.MAX_VALUE))
        assertTrue(CheckBeforeSave.Problem.AmountOverflow in CheckBeforeSave.checkReceipt(receipt(0), positions))
    }
    @Test fun invalidItemsRejected() {
        listOf(ItemEntity("", "R1", "Artikel", 1, 1), ItemEntity("I1", "OTHER", "Artikel", 1, 1),
            ItemEntity("I1", "R1", "", 1, 1), ItemEntity("I1", "R1", "Artikel", 0, 1)).forEach {
            assertTrue(CheckBeforeSave.checkReceipt(receipt(1), listOf(it)).any { p -> p is CheckBeforeSave.Problem.InvalidItem })
        }
    }
}
