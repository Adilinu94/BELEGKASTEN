package de.belegkasten.core.data

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ReceiptFiles @Inject constructor(@ApplicationContext private val context: Context) {
    suspend fun import(uri: Uri): String = withContext(Dispatchers.IO) {
        require(uri.scheme == "content") { "Bitte eine Datei über die Android-Dateiauswahl teilen" }
        val temp = File.createTempFile("receipt-", ".part", context.cacheDir)
        try {
            requireNotNull(context.contentResolver.openInputStream(uri)).use { input ->
                temp.outputStream().use { out ->
                    val buffer = ByteArray(8192)
                    var total = 0L
                    while (true) {
                        val n = input.read(buffer); if (n < 0) break
                        total += n
                        require(total <= 20L * 1024 * 1024) { "Datei größer als 20 MB" }
                        out.write(buffer, 0, n)
                    }
                }
            }
            val header = ByteArray(12)
            val size = temp.inputStream().use { it.read(header) }
            val ext = when {
                size >= 5 && String(header, 0, 5, Charsets.US_ASCII) == "%PDF-" -> "pdf"
                size >= 3 && header[0] == 0xff.toByte() && header[1] == 0xd8.toByte() && header[2] == 0xff.toByte() -> "jpg"
                size >= 8 && header.copyOfRange(0, 8).contentEquals(byteArrayOf(-119, 80, 78, 71, 13, 10, 26, 10)) -> "png"
                size >= 12 && String(header, 0, 4, Charsets.US_ASCII) == "RIFF" && String(header, 8, 4, Charsets.US_ASCII) == "WEBP" -> "webp"
                else -> error("Nur JPEG, PNG, WebP oder PDF werden unterstützt")
            }
            val path = "${if (ext == "pdf") "attachments" else "images"}/${UUID.randomUUID()}.$ext"
            val file = resolve(path)
            file.parentFile!!.mkdirs()
            check(temp.renameTo(file)) { "Datei konnte nicht gespeichert werden" }
            path
        } finally { temp.delete() }
    }

    fun resolve(path: String): File {
        require(Regex("(images|attachments)/[A-Za-z0-9_-]{1,80}\\.(jpg|jpeg|png|webp|pdf)").matches(path)) { "Ungültiger Belegpfad" }
        return File(context.filesDir, path)
    }

    fun discard(path: String) {
        val file = resolve(path)
        check(!file.exists() || file.delete()) { "Datei konnte nicht gelöscht werden" }
    }
}
