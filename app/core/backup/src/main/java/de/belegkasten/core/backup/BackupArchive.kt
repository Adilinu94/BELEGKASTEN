package de.belegkasten.core.backup

import com.google.gson.Gson
import de.belegkasten.core.data.ArchiveSnapshot
import de.belegkasten.core.data.validateSnapshot
import de.belegkasten.core.embed.EmbeddingStore
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class BackupManifest(val format: Int, val schema: Int, val files: Map<String, String>)
data class ValidatedArchive(val snapshot: ArchiveSnapshot, val media: Map<String, File>)

object BackupArchive {
    const val MAX_ARCHIVE_BYTES = 128L * 1024 * 1024
    private const val MAX_FILE_BYTES = 32L * 1024 * 1024
    private val gson = Gson()

    fun safeName(name: String): Boolean = name == "archive.json" ||
        Regex("(images|attachments)/[A-Za-z0-9_-]{1,80}\\.(jpg|jpeg|png|webp|pdf)").matches(name)

    fun hash(file: File): String = file.inputStream().use { input ->
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)
        while (true) { val n = input.read(buffer); if (n < 0) break; digest.update(buffer, 0, n) }
        digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun copyLimited(input: InputStream, output: OutputStream, limit: Long): Long {
        val buffer = ByteArray(8192)
        var size = 0L
        while (true) {
            val n = input.read(buffer)
            if (n < 0) return size
            size += n
            require(size <= limit) { "Datei/Sicherung zu groß" }
            output.write(buffer, 0, n)
        }
    }

    fun write(output: File, snapshot: ArchiveSnapshot, media: Map<String, File>, work: File) {
        validateSnapshot(snapshot, EmbeddingStore())
        val references = snapshot.receipts.flatMap { listOfNotNull(it.imagePath, it.attachmentPath) }.toSet()
        require(references == media.keys && "archive.json" !in media) { "Sicherung unvollständig" }
        val data = File(work, "archive.json").also { it.writeText(gson.toJson(snapshot)) }
        val entries = mapOf("archive.json" to data) + media
        require(entries.keys.all(::safeName)) { "Ungültiger Dateipfad" }
        require(entries.size < 10000 && entries.values.all { it.length() <= MAX_FILE_BYTES }) { "Zu viele/große Anhänge" }
        require(entries.values.sumOf { it.length() } <= MAX_ARCHIVE_BYTES)
        val manifest = BackupManifest(2, 2, entries.mapValues { hash(it.value) })
        val manifestBytes = gson.toJson(manifest).toByteArray(Charsets.UTF_8)
        require(manifestBytes.size <= 2 * 1024 * 1024 && entries.values.sumOf { it.length() } + manifestBytes.size <= MAX_ARCHIVE_BYTES)
        ZipOutputStream(output.outputStream()).use { zip ->
            entries.forEach { (name, file) ->
                zip.putNextEntry(ZipEntry(name))
                file.inputStream().use { copyLimited(it, zip, MAX_FILE_BYTES) }
                zip.closeEntry()
            }
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write(manifestBytes)
            zip.closeEntry()
        }
    }

    fun read(file: File, work: File, store: EmbeddingStore): ValidatedArchive {
        val entries = linkedMapOf<String, File>()
        var expanded = 0L
        ZipInputStream(file.inputStream()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                require(!entry.isDirectory && (entry.name == "manifest.json" || safeName(entry.name))) { "Unsicherer ZIP-Pfad" }
                require(entry.name !in entries && entries.size < 10000) { "Doppelter ZIP-Eintrag/zu viele Dateien" }
                val target = File(work, "entry-${entries.size}")
                target.outputStream().use { expanded += copyLimited(zip, it,
                    if (entry.name == "manifest.json") 2L * 1024 * 1024 else MAX_FILE_BYTES) }
                require(expanded <= MAX_ARCHIVE_BYTES) { "Entpackte Sicherung zu groß" }
                entries[entry.name] = target
                zip.closeEntry()
            }
        }
        val manifest = gson.fromJson(requireNotNull(entries.remove("manifest.json")) { "Manifest fehlt" }.readText(), BackupManifest::class.java)
        require(manifest.format == 2 && manifest.schema == 2) { "Sicherung benötigt andere App-Version" }
        require(manifest.files.keys == entries.keys) { "Dateien fehlen oder sind nicht im Manifest" }
        entries.forEach { (name, data) -> require(hash(data) == manifest.files[name]) { "Dateiprüfsumme falsch" } }
        val snapshot = gson.fromJson(requireNotNull(entries.remove("archive.json")) { "Archivdaten fehlen" }.readText(), ArchiveSnapshot::class.java)
        validateSnapshot(snapshot, store)
        val referenced = snapshot.receipts.flatMap { listOfNotNull(it.imagePath, it.attachmentPath) }.toSet()
        require(referenced == entries.keys && referenced.all(::safeName)) { "Anhänge fehlen oder Pfade ungültig" }
        return ValidatedArchive(snapshot, entries)
    }
}
