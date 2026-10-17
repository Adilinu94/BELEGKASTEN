package de.belegkasten.core.backup

import android.content.*
import androidx.core.content.FileProvider
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import de.belegkasten.core.data.*
import de.belegkasten.core.database.*
import de.belegkasten.core.embed.*
import de.belegkasten.core.license.LicenseManager
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.*

class RestoreIntegrationTest {
    private class FileContext(base: Context, val root: File) : ContextWrapper(base) {
        override fun getFilesDir() = File(root, "files").also { it.mkdirs() }
        override fun getCacheDir() = File(root, "cache").also { it.mkdirs() }
    }

    @Test fun hundredReceiptsRestoreIdempotentlyAndConflictsRollback() = runBlocking {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val testContext = InstrumentationRegistry.getInstrumentation().context
        val testRoot = File(testContext.cacheDir, "backup-tests").also { it.mkdirs() }
        val root = File.createTempFile("restore-test", "", testRoot).also { it.delete(); it.mkdir() }
        val context = FileContext(base, root)
        val db = Room.inMemoryDatabaseBuilder(base, BelegkastenDb::class.java).build()
        val gate = ArchiveGate()
        val store = EmbeddingStore()
        val repo = ReceiptRepository(db.receipts(), db.items(), db.embeddings(), db.deadlines(), db, gate,
            HashEmbedder(), store, LicenseManager(context, db.receipts()), Diagnostics(context), ReceiptFiles(context))
        val manager = BackupManager(context, repo, gate, store)
        try {
            val receipts = (1..100).map { i -> ReceiptEntity("R$i", "Händler $i", "2026-10-16", i * 100L,
                "Test", null, "E_INVOICE_PDF", "attachments/R$i.pdf", i.toLong(), "Bohrmaschine $i") }
            val items = receipts.map { ItemEntity("${it.id}-item", it.id, "Bohrer ${it.id}", 1, it.totalCents) }
            val vectors = receipts.map { EmbeddingEntity(it.id, EMBED_DIM, store.toBlob(HashEmbedder().embedDocument(it.merchant, it.ocrText))) }
            val deadlines = items.flatMap { item -> listOf(
                DeadlineEntity("${item.id}-return", item.id, "RETURN", false, "2026-11-16"),
                DeadlineEntity("${item.id}-warranty", item.id, "WARRANTY", false, "2028-10-16")) }
            val snapshot = ArchiveSnapshot(receipts, items, vectors, deadlines)
            receipts.forEach { File(context.filesDir, it.attachmentPath!!).also { f -> f.parentFile!!.mkdirs(); f.writeText("%PDF-${it.id}") } }
            assertEquals(100, repo.merge(snapshot))
            // Restore/read/export stay available past the 50-receipt trial limit.
            assertFalse(LicenseManager(context, db.receipts()).captureEnabled())
            repo.save(receipts.first(), listOf(items.first())) // committed-save retry
            repo.discardUnreferenced(receipts.first().attachmentPath!!)
            assertTrue(File(context.filesDir, receipts.first().attachmentPath!!).isFile)
            val draft = File(context.filesDir, "images/unreferenced.jpg").also { it.parentFile!!.mkdirs(); it.writeText("draft") }
            repo.discardUnreferenced("images/unreferenced.jpg")
            assertFalse(draft.exists())
            val uri = FileProvider.getUriForFile(testContext, testContext.packageName + ".backup", File(root, "transfer.bkbf"))
            manager.export("testpassword", uri)
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { db.clearAllTables() }
            context.filesDir.resolve("attachments").deleteRecursively()
            assertEquals(100, manager.validateAndImport("testpassword", uri))
            assertEquals(receipts.sortedBy { it.id }, repo.snapshot().receipts)
            assertEquals(items.sortedBy { it.id }, repo.snapshot().items)
            assertEquals(vectors.sortedBy { it.receiptId }, repo.snapshot().embeddings)
            assertEquals(deadlines.sortedBy { it.id }, repo.snapshot().deadlines)
            assertEquals(0, manager.validateAndImport("testpassword", uri))
            assertEquals(100, db.receipts().count())
            receipts.forEach { assertEquals("%PDF-${it.id}", File(context.filesDir, it.attachmentPath!!).readText()) }
            assertFails { manager.validateAndImport("wrongpassword", uri) }
            assertEquals(100, db.receipts().count())
            val originalFile = File(context.filesDir, receipts.first().attachmentPath!!)
            originalFile.writeText("changed original")
            assertFails { manager.validateAndImport("testpassword", uri) }
            assertEquals("changed original", originalFile.readText())
            originalFile.writeText("%PDF-R1")
            val conflict = receipts[0].copy(totalCents = 1)
            val newReceipt = receipts[0].copy(id = "NEW", attachmentPath = null)
            assertFails { repo.merge(ArchiveSnapshot(listOf(newReceipt, conflict), listOf(items.first().copy(priceCents = 1)), emptyList(), emptyList())) }
            assertNull(db.receipts().byId("NEW")) // insertion before conflict rolled back
            assertEquals(100L, db.receipts().byId("R1")!!.totalCents)
            val conflictingVector = vectors.first().copy(vector = vectors[1].vector)
            assertFails { repo.merge(ArchiveSnapshot(listOf(receipts.first()), listOf(items.first()), listOf(conflictingVector), emptyList())) }
            assertTrue(db.receipts().fullTextCandidates("Bohrmaschine").isNotEmpty())
            val exportUri = FileProvider.getUriForFile(testContext, testContext.packageName + ".backup", File(root, "export.csv"))
            assertEquals(100, CsvExport(context, repo).export(exportUri))
            assertTrue(File(root, "export.csv").readText().contains("Händler 100"))
            val transfer = File(root, "transfer.bkbf")
            val bytes = transfer.readBytes()
            bytes[bytes.lastIndex] = (bytes.last().toInt() xor 1).toByte()
            transfer.writeBytes(bytes)
            assertFails { manager.validateAndImport("testpassword", uri) }
            assertEquals(100, db.receipts().count())
        } finally { db.close(); root.deleteRecursively() }
    }
}
