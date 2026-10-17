package de.belegkasten.core.data

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.math.BigDecimal
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun csvCell(value: String): String {
    val safe = if (value.trimStart().startsWith("=") || value.trimStart().startsWith("+") ||
        value.trimStart().startsWith("-") || value.trimStart().startsWith("@") || value.startsWith("\t") || value.startsWith("\r")) "'$value" else value
    return "\"${safe.replace("\"", "\"\"")}\""
}

class CsvExport @Inject constructor(@ApplicationContext private val context: Context, private val repo: ReceiptRepository) {
    suspend fun export(target: Uri): Int = withContext(Dispatchers.IO) {
        // CSV doesn't need attachments, embeddings, items or deadline BLOBs.
        val receipts = repo.receiptRows()
        requireNotNull(context.contentResolver.openOutputStream(target, "wt")).bufferedWriter(Charsets.UTF_8).use { out ->
            out.write("\uFEFFDatum;Händler;Kategorie;Betrag_EUR;Quelle\r\n")
            receipts.forEach {
                out.write(listOf(it.date, it.merchant, it.category, BigDecimal.valueOf(it.totalCents, 2).toPlainString().replace('.', ','), it.source).joinToString(";", transform = ::csvCell) + "\r\n")
            }
        }
        receipts.size
    }
}
