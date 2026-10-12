package de.belegkasten.core.ocr

import javax.inject.Inject

// Fixed rules first: date, total, receipt number from stable lines.
// Gemma 4 (understand/) handles merchant/category/positions; this parser
// covers what regex can do reliably on German receipts, incl. faded print.
class RuleParser @Inject constructor() {

    data class Fields(
        val dateIso: String?,
        val totalCents: Long?,
        val receiptNumber: String?,
        // Lines the user must confirm (CheckBeforeSave problems surface here).
        val needsConfirmation: List<String>,
    )

    private val dateDe = Regex("""(\d{1,2})\.(\d{1,2})\.(\d{2,4})""")
    private val amount = Regex("""(\d{1,3}(?:\.\d{3})*,\d{2})""")
    private val sumLine = Regex("""(?i)(summe|gesamt|total|endbetrag|zu zahlen)""")
    private val bonNr = Regex("""(?i)(bon-?nr|beleg-?nr|re-?nr|kassenbon)\s*[:#]?\s*(\S+)""")

    fun parse(rawText: String): Fields {
        val needs = mutableListOf<String>()
        var dateIso: String? = null
        var totalCents: Long? = null
        var receiptNumber: String? = null

        dateDe.find(rawText)?.let {
            val (d, m, y) = it.destructured
            val year = if (y.length == 2) "20$y" else y
            dateIso = "$year-${m.padStart(2, '0')}-${d.padStart(2, '0')}"
        } ?: needs.add("date").also { }

        val sumLines = rawText.lines().filter { sumLine.containsMatchIn(it) }
        val amountText = sumLines.lastOrNull() ?: rawText.lines().lastOrNull { amount.containsMatchIn(it) }
        amountText?.let {
            totalCents = amount.find(it)?.value?.let(::toCents)
        }
        if (totalCents == null) needs.add("total_cents")

        bonNr.find(rawText)?.let { receiptNumber = it.groupValues[2] }

        return Fields(dateIso, totalCents, receiptNumber, needs)
    }

    // "1.234,56" -> 123456. German format only; anything else returns null.
    fun toCents(german: String): Long? = runCatching {
        val clean = german.replace(".", "").replace(",", ".")
        (clean.toDouble() * 100).toLong()
    }.getOrNull()
}
