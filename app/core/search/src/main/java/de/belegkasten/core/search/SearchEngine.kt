package de.belegkasten.core.search

import de.belegkasten.core.database.ReceiptDao
import de.belegkasten.core.model.Cents
import de.belegkasten.core.model.IsoDate
import de.belegkasten.core.model.SearchHit
import java.text.Normalizer
import java.util.Locale
import javax.inject.Inject

// Honest baseline until a real on-device model passes the spike.
// FTS4 provides candidates; explicit merchant/month/year and lexical score rank.
class SearchEngine @Inject constructor(private val receipts: ReceiptDao) {
    private val months = mapOf("januar" to "01", "februar" to "02", "marz" to "03", "maerz" to "03",
        "april" to "04", "mai" to "05", "juni" to "06", "juli" to "07", "august" to "08",
        "september" to "09", "oktober" to "10", "november" to "11", "dezember" to "12")
    private val fillers = setOf("der", "die", "das", "vom", "von", "im", "bei", "den", "ein", "eine", "und", "fur")

    suspend fun search(rawQuery: String, topK: Int = 10): List<SearchHit> {
        require(topK > 0)
        val tokens = normalize(rawQuery).split(Regex("[^\\p{L}\\p{N}]+"))
            .filter { it.isNotBlank() && it !in fillers }.distinct()
        if (tokens.isEmpty()) return emptyList()
        val month = tokens.firstNotNullOfOrNull { months[it] }
        val year = tokens.firstOrNull { Regex("(?:19|20)\\d{2}").matches(it) }
        val words = tokens.filter { it !in months && it != year }
        val all = receipts.all()
        // Recognize merchant anywhere in the query; never assume its first word.
        val merchantTokens = words.filter { word -> all.any { word in normalize(it.merchant).split(Regex("[^\\p{L}\\p{N}]+")) } }
        val fts = if (words.isEmpty()) emptySet() else receipts.fullTextCandidates(
            words.joinToString(" OR ") { "$it*" }).map { it.id }.toSet()
        return all.asSequence().filter { r ->
            (month == null || r.date.drop(5).take(2) == month) &&
                (year == null || r.date.startsWith(year)) &&
                (merchantTokens.isEmpty() || merchantTokens.any { it in normalize(r.merchant) })
        }.map { r ->
            val text = normalize("${r.merchant} ${r.category} ${r.ocrText}")
            val lexical = words.count { it in text }
            val score = lexical.toFloat() + (if (r.id in fts) 1f else 0f) + merchantTokens.size * 3f
            SearchHit(r.id, r.merchant, IsoDate(r.date), Cents(r.totalCents), score)
        }.filter { it.score > 0 || words.isEmpty() }
            .sortedWith(compareByDescending<SearchHit> { it.score }.thenByDescending { it.date.value }.thenBy { it.receiptId })
            .take(topK).toList()
    }

    private fun normalize(s: String) = Normalizer.normalize(s.lowercase(Locale.GERMANY), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "").replace("ß", "ss")
}
