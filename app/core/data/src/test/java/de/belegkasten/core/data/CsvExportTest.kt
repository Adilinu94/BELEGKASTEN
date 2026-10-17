package de.belegkasten.core.data
import kotlin.test.*
class CsvExportTest {
    @Test fun quotesAndSeparatorsEscaped() { assertEquals("\"A;\"\"B\"\"\"", csvCell("A;\"B\"")) }
    @Test fun formulasNeutralized() { listOf("=cmd()", "+1", "-1", "@SUM(1)", " =cmd()").forEach { assertTrue(csvCell(it).startsWith("\"'")) } }
}
