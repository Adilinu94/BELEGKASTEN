package de.belegkasten.core.ocr

import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject

// Fixed rules first: date, total, receipt number from stable lines.
// Gemma 4 merchant/category/position extraction is not yet integrated; this parser
// covers what regex can do reliably on German receipts, incl. faded print.
class RuleParser @Inject constructor() {

    data class Fields(
        val dateIso: String?,
        val totalCents: Long?,
        val receiptNumber: String?,
        // Lines the user must confirm (CheckBeforeSave problems surface here).
        val needsConfirmation: List<String>,
    )

    private val dateDe = Regex("""(?<!\d)(\d{1,2})\.(\d{1,2})\.(\d{4}|\d{2})(?!\d)""")
    private val amount = Regex("""(?<![\d.,+\-])(?:\d{1,3}(?:\.\d{3})+|\d+),\d{2}(?![\d.,])""")
    private val sumLine = Regex("""(?i)\b(summe|gesamt(?:betrag)?|total|endbetrag|zu zahlen)\b""")
    private val bonNr = Regex("""(?i)(bon-?nr|beleg-?nr|re-?nr|kassenbon)\s*[:#]?\s*(\S+)""")

    fun parse(rawText: String): Fields {
        val needs = mutableListOf<String>()
        var dateIso: String? = null
        var totalCents: Long? = null
        var receiptNumber: String? = null

        dateDe.find(rawText)?.let {
            val (d, m, y) = it.destructured
            val year = if (y.length == 2) "20$y" else y
            dateIso = runCatching { LocalDate.of(year.toInt(), m.toInt(), d.toInt()).toString() }.getOrNull()
        }
        if (dateIso == null) needs.add("date")

        val sumLines = rawText.lines().filter { sumLine.containsMatchIn(it) }
        // Never silently label a VAT, item or cash/change amount as the total.
        val amountText = sumLines.lastOrNull()
        amountText?.let {
            totalCents = amount.find(it)?.value?.let(::toCents)
        }
        if (totalCents == null) needs.add("total_cents")

        bonNr.find(rawText)?.let { receiptNumber = it.groupValues[2] }

        return Fields(dateIso, totalCents, receiptNumber, needs)
    }

    // "1.234,56" -> 123456. German format only; anything else returns null.
    // BigDecimal, not Double: 1.15 * 100 is 114.99999999999999 as a Double, i.e. 114 cents.
    fun toCents(german: String): Long? = runCatching {
        require(Regex("(?:\\d+|\\d{1,3}(?:\\.\\d{3})+)(?:,\\d{1,2})?").matches(german))
        BigDecimal(german.replace(".", "").replace(",", ".")).movePointRight(2).longValueExact()
    }.getOrNull()
}
