package de.belegkasten.core.data

import de.belegkasten.core.database.DeadlineEntity
import de.belegkasten.core.database.ItemEntity
import de.belegkasten.core.database.ReceiptEntity
import java.time.LocalDate

// "Pruefen vor dem Speichern": fixed rules run before every insert.
// Sums of positions must match subtotal, amounts in cents, ISO dates.
object CheckBeforeSave {
    private val DATE = Regex("""\d{4}-\d{2}-\d{2}""")

    // The pattern alone lets 2025-13-45 through (an OCR misread); strict ISO parsing rejects it.
    private fun isValidDate(value: String): Boolean =
        DATE.matches(value) && runCatching { LocalDate.parse(value) }.isSuccess

    sealed interface Problem {
        data class SumMismatch(val expectedCents: Long, val actualCents: Long) : Problem
        data class BadDate(val value: String) : Problem
        data class MissingField(val field: String) : Problem
    }

    fun checkReceipt(receipt: ReceiptEntity, items: List<ItemEntity>): List<Problem> {
        val problems = mutableListOf<Problem>()
        if (receipt.merchant.isBlank()) problems += Problem.MissingField("merchant")
        if (receipt.category.isBlank()) problems += Problem.MissingField("category")
        if (!isValidDate(receipt.date)) problems += Problem.BadDate(receipt.date)
        if (receipt.totalCents < 0) problems += Problem.MissingField("total_cents")
        if (items.isNotEmpty()) {
            val sum = items.sumOf { it.priceCents * it.quantity }
            if (sum != receipt.totalCents) {
                problems += Problem.SumMismatch(expectedCents = sum, actualCents = receipt.totalCents)
            }
        }
        return problems
    }

    fun checkDeadline(deadline: DeadlineEntity): List<Problem> {
        if (!isValidDate(deadline.dueDate)) return listOf(Problem.BadDate(deadline.dueDate))
        return emptyList()
    }
}
