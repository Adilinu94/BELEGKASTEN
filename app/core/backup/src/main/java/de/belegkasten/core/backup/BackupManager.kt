package de.belegkasten.core.backup

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import de.belegkasten.core.data.ArchiveGate
import de.belegkasten.core.data.ReceiptRepository
import de.belegkasten.core.embed.EmbeddingStore
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.NonCancellable

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repo: ReceiptRepository,
    private val gate: ArchiveGate,
    private val store: EmbeddingStore,
) {
    suspend fun export(password: String, target: Uri): String = withContext(Dispatchers.IO) {
        gate.exclusive {
            val work = File.createTempFile("backup-", "", context.cacheDir).also { it.delete(); it.mkdir() }
            val secret = password.toCharArray()
            try {
                val snapshot = repo.snapshot()
                val media = snapshot.receipts.flatMap { listOfNotNull(it.imagePath, it.attachmentPath) }
                    .distinct().associateWith { name -> managedFile(name).also { require(it.isFile) { "Anhang fehlt" } } }
                val zip = File(work, "backup.zip")
                BackupArchive.write(zip, snapshot, media, work)
                val encrypted = File(work, "backup.bkbf")
                zip.inputStream().use { input -> encrypted.outputStream().use { BackupCrypto.encrypt(input, it, secret) } }
                require(encrypted.length() <= BackupArchive.MAX_ARCHIVE_BYTES) { "Sicherung zu groß" }
                requireNotNull(context.contentResolver.openOutputStream(target, "wt")) { "Datei kann nicht geschrieben werden" }
                    .use { out -> encrypted.inputStream().use { it.copyTo(out) } }
                BackupArchive.hash(encrypted)
            } finally { secret.fill('\u0000'); work.deleteRecursively() }
        }
    }

    // Merge, never replace: a validation/ID/file conflict rolls back the DB and
    // removes only newly copied files. Existing archive files are never overwritten.
    suspend fun validateAndImport(password: String, source: Uri): Int = withContext(Dispatchers.IO) {
        gate.exclusive {
            val work = File.createTempFile("restore-", "", context.cacheDir).also { it.delete(); it.mkdir() }
            val copied = mutableListOf<File>()
            val secret = password.toCharArray()
            try {
                val raw = File(work, "encrypted.bkbf")
                requireNotNull(context.contentResolver.openInputStream(source)).use { input ->
                    raw.outputStream().use { BackupArchive.copyLimited(input, it, BackupArchive.MAX_ARCHIVE_BYTES) }
                }
                val zip = File(work, "decrypted.zip")
                raw.inputStream().use { input -> zip.outputStream().use { BackupCrypto.decrypt(input, it, secret) } }
                val version = raw.inputStream().use { input -> ByteArray(5).also { input.read(it) }.toString(Charsets.US_ASCII) }
                val archive = if (version == "BKBF1") LegacyBackup.read(zip, work, store) else BackupArchive.read(zip, work, store)
                // Don't delete committed originals if cancellation arrives on the
                // Room→caller boundary. Only this bounded install/commit is protected.
                withContext(NonCancellable) {
                    archive.media.forEach { (name, staged) ->
                        val destination = managedFile(name)
                        if (destination.exists()) require(BackupArchive.hash(destination) == BackupArchive.hash(staged)) { "Anhangskonflikt; nichts verändert" }
                        else {
                            destination.parentFile!!.mkdirs()
                            val temporary = File.createTempFile("restore-", ".part", destination.parentFile)
                            try {
                                staged.inputStream().use { input -> temporary.outputStream().use { out -> input.copyTo(out); out.fd.sync() } }
                                check(temporary.renameTo(destination)) { "Anhang konnte nicht installiert werden" }
                                copied += destination
                            } finally { temporary.delete() }
                        }
                    }
                    val count = repo.merge(archive.snapshot)
                    copied.clear()
                    count
                }
            } finally {
                copied.forEach { it.delete() }
                secret.fill('\u0000'); work.deleteRecursively()
            }
        }
    }

    private fun managedFile(name: String): File {
        require(BackupArchive.safeName(name) && name != "archive.json") { "Ungültiger Anhangspfad" }
        return File(context.filesDir, name)
    }
}
