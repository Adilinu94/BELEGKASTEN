package de.belegkasten.core.backup

import de.belegkasten.core.data.ArchiveSnapshot
import de.belegkasten.core.database.ReceiptEntity
import de.belegkasten.core.embed.EmbeddingStore
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.*

class BackupTest {
    @Test fun cryptoRoundTripAndPasswordFailure() {
        val original = "Belegkasten test".toByteArray()
        val encrypted = ByteArrayOutputStream().also { BackupCrypto.encrypt(original.inputStream(), it, "testpassword".toCharArray()) }.toByteArray()
        val restored = ByteArrayOutputStream()
        BackupCrypto.decrypt(encrypted.inputStream(), restored, "testpassword".toCharArray())
        assertContentEquals(original, restored.toByteArray())
        assertFails { BackupCrypto.decrypt(encrypted.inputStream(), ByteArrayOutputStream(), "wrongpassword".toCharArray()) }
        encrypted[encrypted.lastIndex] = (encrypted.last().toInt() xor 1).toByte()
        assertFails { BackupCrypto.decrypt(encrypted.inputStream(), ByteArrayOutputStream(), "testpassword".toCharArray()) }
    }
    @Test fun unsafePathsAndLimitsRejected() {
        listOf("../file", "/etc/passwd", "images/../file.jpg", "images/a\\b.jpg", "db/belegkasten.db").forEach { assertFalse(BackupArchive.safeName(it)) }
        assertTrue(BackupArchive.safeName("images/abc-123.jpg"))
        assertFails { BackupArchive.copyLimited(ByteArray(10).inputStream(), ByteArrayOutputStream(), 9) }
    }
    @Test fun archiveRoundTripWithAttachment() {
        val dir = Files.createTempDirectory("backup-test").toFile()
        try {
            val media = dir.resolve("input.pdf").also { it.writeText("%PDF-test") }
            val snapshot = ArchiveSnapshot(listOf(ReceiptEntity("R1", "BAUHAUS", "2026-10-16", 1299,
                "Baumarkt", null, "E_INVOICE_PDF", "attachments/abc.pdf", 1, "Bohrer")), emptyList(), emptyList(), emptyList())
            val zip = dir.resolve("backup.zip")
            BackupArchive.write(zip, snapshot, mapOf("attachments/abc.pdf" to media), dir)
            val work = dir.resolve("read").also { it.mkdir() }
            val result = BackupArchive.read(zip, work, EmbeddingStore())
            assertEquals(snapshot, result.snapshot)
            assertEquals("%PDF-test", result.media.getValue("attachments/abc.pdf").readText())
        } finally { dir.deleteRecursively() }
    }
    @Test fun maliciousZipRejectedBeforeExtraction() {
        val dir = Files.createTempDirectory("backup-test").toFile()
        try {
            val zip = dir.resolve("bad.zip")
            ZipOutputStream(zip.outputStream()).use { it.putNextEntry(ZipEntry("../escape")); it.write(byteArrayOf(1)); it.closeEntry() }
            assertFails { BackupArchive.read(zip, dir, EmbeddingStore()) }
            assertFalse(requireNotNull(dir.parentFile).resolve("escape").exists())
        } finally { dir.deleteRecursively() }
    }
    @Test fun missingExtraAndTamperedFilesRejected() {
        val dir = Files.createTempDirectory("backup-validation").toFile()
        try {
            val media = dir.resolve("input.pdf").also { it.writeText("%PDF-test") }
            val snapshot = ArchiveSnapshot(listOf(ReceiptEntity("R1", "Test", "2026-10-16", 100,
                "Test", null, "IMPORT", "attachments/a.pdf", 1)), emptyList(), emptyList(), emptyList())
            assertFails { BackupArchive.write(dir.resolve("missing.zip"), snapshot, emptyMap(), dir) }
            val zip = dir.resolve("valid.zip")
            BackupArchive.write(zip, snapshot, mapOf("attachments/a.pdf" to media), dir)
            val entries = java.util.zip.ZipFile(zip).use { file -> file.entries().asSequence().associate { it.name to file.getInputStream(it).readBytes() } }
            for (variant in listOf(entries - "attachments/a.pdf", entries + ("attachments/a.pdf" to "tampered".toByteArray()), entries + ("images/extra.jpg" to byteArrayOf(1)))) {
                val bad = dir.resolve("bad.zip")
                ZipOutputStream(bad.outputStream()).use { out -> variant.forEach { (name, bytes) -> out.putNextEntry(ZipEntry(name)); out.write(bytes); out.closeEntry() } }
                assertFails { BackupArchive.read(bad, dir, EmbeddingStore()) }
            }
        } finally { dir.deleteRecursively() }
    }
}
