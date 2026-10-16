package de.belegkasten.core.search

import de.belegkasten.core.database.EmbeddingDao
import de.belegkasten.core.database.ReceiptDao
import de.belegkasten.core.embed.Embedder
import de.belegkasten.core.embed.EmbeddingStore
import de.belegkasten.core.model.Cents
import de.belegkasten.core.model.IsoDate
import de.belegkasten.core.model.SearchHit
import java.text.Normalizer
import javax.inject.Inject

// Two-stage search: (1) field filters (merchant word, yyyy-MM) narrow to
// candidates, (2) embedding cosine ranks them. Mirrors spike/harness logic.
class SearchEngine @Inject constructor(
    private val receipts: ReceiptDao,
    private val embeddings: EmbeddingDao,
    private val embedder: Embedder,
    private val store: EmbeddingStore,
) {
    private val months = mapOf(
        "januar" to "01", "februar" to "02", "maerz" to "03", "april" to "04",
        "mai" to "05", "juni" to "06", "juli" to "07", "august" to "08",
        "september" to "09", "oktober" to "10", "november" to "11", "dezember" to "12",
    )

    suspend fun search(rawQuery: String, topK: Int = 10): List<SearchHit> {
        val q = normalize(rawQuery)
        val monthNum = months.entries.firstOrNull { q.contains(it.key) }?.value
        val merchantWord = q.split(" ").firstOrNull { w ->
            w.length > 2 && w !in months.keys && w !in setOf("der", "die", "das", "vom", "von", "im", "bei", "den")
        }
        // The first non-filler word is only a guess at the merchant ("Winterjacke 2026" has
        // none). If no merchant matches, rank all receipts instead of returning nothing.
        val byMerchant = if (merchantWord == null) emptyList() else receipts.filterCandidates(
            merchant = merchantWord,
            // Month filter needs a year; without one we skip it and let ranking decide.
            month = null,
            limit = 200,
        )
        val cands = byMerchant.ifEmpty {
            receipts.filterCandidates(merchant = null, month = null, limit = Int.MAX_VALUE)
        }.let { list ->
            if (monthNum != null) list.filter { it.date.substring(5, 7) == monthNum }.ifEmpty { list }
            else list
        }
        if (cands.isEmpty()) return emptyList()
        val qv = embedder.embedQuery(rawQuery)
        val scored = cands.map { r ->
            val emb = embeddings.forReceipt(r.id)?.let { store.fromBlob(it.vector) }
            val sim = if (emb != null) store.cosine(qv, emb) else 0f
            SearchHit(r.id, r.merchant, IsoDate(r.date), Cents(r.totalCents), sim)
        }
        return scored.sortedByDescending { it.score }.take(topK)
    }

    private fun normalize(s: String): String =
        Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
}
