package de.belegkasten.feature.capture

import de.belegkasten.core.database.ItemEntity
import de.belegkasten.core.ocr.RuleParser

// Manual rows only; stable IDs make retry after a committed save idempotent.
fun parsePositions(rows: String, receiptId: String, parser: RuleParser): List<ItemEntity> =
    rows.lineSequence().filter { it.isNotBlank() }.mapIndexed { index, row ->
        val fields = row.split(';').map(String::trim)
        require(fields.size == 3 && fields[0].isNotBlank()) {
            "Position ${index + 1}: Bezeichnung;Menge;Einzelpreis (z. B. Bohrer;2;12,99)"
        }
        val quantity = fields[1].toIntOrNull()
        val price = parser.toCents(fields[2])
        require(quantity != null && quantity > 0 && price != null) { "Position ${index + 1}: Menge oder Betrag ungültig" }
        ItemEntity("$receiptId-item-$index", receiptId, fields[0], quantity, price)
    }.toList()
