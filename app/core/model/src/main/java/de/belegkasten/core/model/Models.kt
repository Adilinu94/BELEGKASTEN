package de.belegkasten.core.model

// Money is always stored as cents (Long). Never as Float/Double.
@JvmInline
value class Cents(val value: Long)

// Dates as ISO-8601 strings (yyyy-MM-dd), validated before insert.
@JvmInline
value class IsoDate(val value: String)

enum class ReceiptSource { CAMERA, SHARE, IMPORT, E_INVOICE_PDF }

data class Receipt(
    val id: String,
    val merchant: String,
    val date: IsoDate,
    val total: Cents,
    val category: String,
    val imagePath: String?,
    val source: ReceiptSource,
    // Phase-A fallback: shared E-Rechnung PDF is kept as attachment, parsed in Phase D.
    val attachmentPath: String? = null,
    val createdAtEpochMs: Long,
)

data class ReceiptItem(
    val id: String,
    val receiptId: String,
    val label: String,
    val quantity: Int,
    val price: Cents,
)

enum class DeadlineType { RETURN, WARRANTY }

data class Deadline(
    val id: String,
    val itemId: String,
    val type: DeadlineType,
    // From receipt (rare) or from editable default per category (usual case).
    val fromDefault: Boolean,
    val dueDate: IsoDate,
    val notified: Boolean = false,
)

data class SearchHit(
    val receiptId: String,
    val merchant: String,
    val date: IsoDate,
    val total: Cents,
    val score: Float,
)
