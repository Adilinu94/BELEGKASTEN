package de.belegkasten.core.data

import de.belegkasten.core.database.DeadlineEntity
import de.belegkasten.core.database.ItemEntity
import de.belegkasten.core.database.ReceiptEntity
import java.time.LocalDate

// "Pruefen vor dem Speichern": fixed rules run before every insert.
// Sums of positions must match subtotal, amounts in cents, ISO dates.
object CheckBeforeSave {
    private val DATE = Regex("""\d{4}-\d{2}-\d{2}""")

    sealed interface Problem {
        data class SumMismatch(val expectedCents: Long, val actualCents: Long) : Problem
        data class BadDate(val value: String) : Problem
        data class MissingField(val field: String) : Problem
        data class InvalidItem(val id: String) : Problem
        data object AmountOverflow : Problem
    }

    fun checkReceipt(receipt: ReceiptEntity, items: List<ItemEntity>): List<Problem> {
        val problems = mutableListOf<Problem>()
        if (receipt.merchant.isBlank()) problems += Problem.MissingField("merchant")
        if (receipt.category.isBlank()) problems += Problem.MissingField("category")
        if (!validDate(receipt.date)) problems += Problem.BadDate(receipt.date)
        if (receipt.id.isBlank()) problems += Problem.MissingField("id")
        if (receipt.totalCents < 0) problems += Problem.MissingField("total_cents")
        val ids = mutableSetOf<String>()
        items.forEach {
            if (it.id.isBlank() || !ids.add(it.id) || it.receiptId != receipt.id ||
                it.label.isBlank() || it.quantity <= 0 || it.priceCents < 0
            ) problems += Problem.InvalidItem(it.id)
        }
        if (items.isNotEmpty()) {
            val sum = try {
                items.fold(0L) { acc, item ->
                    Math.addExact(acc, Math.multiplyExact(item.priceCents, item.quantity.toLong()))
                }
            } catch (_: ArithmeticException) {
                problems += Problem.AmountOverflow
                null
            }
            if (sum != null && sum != receipt.totalCents) {
                problems += Problem.SumMismatch(expectedCents = sum, actualCents = receipt.totalCents)
            }
        }
        return problems
    }

    fun checkDeadline(deadline: DeadlineEntity): List<Problem> {
        return buildList {
            if (!validDate(deadline.dueDate)) add(Problem.BadDate(deadline.dueDate))
            if (deadline.id.isBlank() || deadline.itemId.isBlank()) add(Problem.MissingField("deadline_id/item_id"))
            if (deadline.type !in setOf("RETURN", "WARRANTY")) add(Problem.MissingField("deadline_type"))
        }
    }

    fun validDate(value: String): Boolean = DATE.matches(value) &&
        runCatching { LocalDate.parse(value).toString() == value }.getOrDefault(false)
}
