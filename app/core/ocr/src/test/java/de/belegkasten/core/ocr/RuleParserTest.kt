package de.belegkasten.core.ocr

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RuleParserTest {
    @Test fun ungroupedLargeTotalsAreNotTruncated() { assertEquals(123456L, RuleParser().parse("SUMME 1234,56").totalCents) }
    @Test fun itemAndNegativeAmountsNotGuessedAsTotal() {
        assertEquals(null, RuleParser().parse("Artikel 12,99\nMWST 2,07").totalCents)
        assertEquals(null, RuleParser().parse("SUMME -12,99").totalCents)
    }
    @Test fun threeDigitYearNotAccepted() { assertEquals(null, RuleParser().parse("12.03.202").dateIso) }
    private val parser = RuleParser()

    @Test
    fun parsesGermanReceipt() {
        val raw = "BAUHAUS Filiale 12\n14.03.2025\n1x BOHRMASCH. PSB 750 89,99\nSUMME 89,99\nBON-NR 100042"
        val f = parser.parse(raw)
        assertEquals("2025-03-14", f.dateIso)
        assertEquals(8999, f.totalCents)
        assertEquals("100042", f.receiptNumber)
        assertTrue(f.needsConfirmation.isEmpty())
    }

    @Test
    fun missingTotalNeedsConfirmation() {
        val raw = "BAUHAUS\n14.03.2025\nBON-NR 7"
        val f = parser.parse(raw)
        assertTrue("total_cents" in f.needsConfirmation)
    }

    @Test
    fun toCentsHandlesThousands() {
        assertEquals(123456, parser.toCents("1.234,56"))
    }

    // Double rechnet 1,15 * 100 zu 114,99... -> 114 Cent. Diese Werte sind die Falle.
    @Test
    fun toCentsIsExactForValuesDoubleGetsWrong() {
        assertEquals(29L, parser.toCents("0,29"))
        assertEquals(115L, parser.toCents("1,15"))
        assertEquals(870L, parser.toCents("8,70"))
        assertEquals(1999L, parser.toCents("19,99"))
    }

    @Test
    fun parseReadsTotalExactly() {
        assertEquals(1999L, parser.parse("EDEKA\n03.04.2025\nSUMME 19,99\nBON-NR 7").totalCents)
    }
    @Test fun impossibleDateNeedsConfirmation() {
        val fields = parser.parse("BAUHAUS\n30.02.2025\nSUMME 12,99")
        assertEquals(null, fields.dateIso)
        assertTrue("date" in fields.needsConfirmation)
    }
    @Test fun malformedAmountsRejected() {
        listOf("1.15", "1,234", "NaN", "-1,00", "1e5", "9.22.1,00").forEach { assertEquals(null, parser.toCents(it)) }
    }
}
