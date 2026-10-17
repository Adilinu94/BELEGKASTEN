package de.belegkasten.feature.capture
import de.belegkasten.core.ocr.RuleParser
import kotlin.test.*
class DraftPositionsTest {
    @Test fun multipleItemsHaveExactAmountsAndStableIds() {
        val items = parsePositions("Bohrer;2;12,99\nSchrauben;1;1,15", "R1", RuleParser())
        assertEquals(listOf(1299L, 115L), items.map { it.priceCents })
        assertEquals(2, items[0].quantity)
        assertEquals(items, parsePositions("Bohrer;2;12,99\nSchrauben;1;1,15", "R1", RuleParser()))
    }
    @Test fun invalidRowsRejected() { listOf("Bon", "Bon;0;1,00", "Bon;1;-1,00", ";1;1,00", "Bon;1.5;1,00").forEach {
        assertFailsWith<IllegalArgumentException> { parsePositions(it, "R1", RuleParser()) }
    } }
    @Test fun blankPositionsAllowed() { assertTrue(parsePositions("\n ", "R1", RuleParser()).isEmpty()) }
}
