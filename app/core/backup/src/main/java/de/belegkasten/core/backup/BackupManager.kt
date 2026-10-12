package de.belegkasten.core.backup

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

// Encrypted backup (v3 decision): AES-256-GCM, key from user password via
// PBKDF2-HMAC-SHA256 (600k iterations). ZIP holds db, images, manifest.json
// with SHA-256 per file. Lose the password = lose the backup (shown in dialog).
@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    data class Result(val uri: Uri, val sha256: String)

    suspend fun export(password: String, target: Uri): Result = withContext(Dispatchers.IO) {
        require(password.length >= 8) { "password must have at least 8 characters" }
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val key = deriveKey(password, salt)
        val zipBytes = buildZip()
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        val encrypted = cipher.doFinal(zipBytes)
        // file layout: magic(4) + salt(16) + iv(12) + ciphertext
        val out = "BKBF1".toByteArray() + salt + iv + encrypted
        context.contentResolver.openOutputStream(target)!!.use { it.write(out) }
        Result(target, sha256(out))
    }

    suspend fun validateAndImport(password: String, source: Uri): JSONObject = withContext(Dispatchers.IO) {
        val raw = context.contentResolver.openInputStream(source)!!.use { it.readBytes() }
        require(raw.size > 32 && raw.copyOfRange(0, 5).toString(Charsets.US_ASCII) == "BKBF1") {
            "not a Belegkasten backup"
        }
        val salt = raw.copyOfRange(5, 21)
        val iv = raw.copyOfRange(21, 33)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, deriveKey(password, salt), GCMParameterSpec(128, iv))
        val zipBytes = try {
            cipher.doFinal(raw.copyOfRange(33, raw.size))
        } catch (e: Exception) {
            throw IllegalArgumentException("wrong password or damaged file", e)
        }
        readManifest(zipBytes)
    }

    private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password.toCharArray(), salt, 600_000, 256)
        val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        return SecretKeySpec(bytes, "AES")
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    // Copies the Room db file + images dir into an in-memory ZIP with manifest.
    private fun buildZip(): ByteArray {
        val manifest = JSONObject().put("format", 1).put("app", "belegkasten")
        val files = JSONObject()
        val buf = java.io.ByteArrayOutputStream()
        ZipOutputStream(buf).use { zip ->
            val dbFile = context.getDatabasePath("belegkasten.db")
            if (dbFile.exists()) {
                val bytes = dbFile.readBytes()
                zip.putNextEntry(ZipEntry("db/belegkasten.db")); zip.write(bytes); zip.closeEntry()
                files.put("db/belegkasten.db", sha256(bytes))
            }
            val imgDir = File(context.filesDir, "receipt_images")
            imgDir.listFiles()?.forEach { f ->
                val bytes = f.readBytes()
                zip.putNextEntry(ZipEntry("images/${f.name}")); zip.write(bytes); zip.closeEntry()
                files.put("images/${f.name}", sha256(bytes))
            }
            manifest.put("files", files)
            val m = manifest.toString().toByteArray()
            zip.putNextEntry(ZipEntry("manifest.json")); zip.write(m); zip.closeEntry()
        }
        return buf.toByteArray()
    }

    private fun readManifest(zipBytes: ByteArray): JSONObject {
        ZipInputStream(zipBytes.inputStream()).use { zip ->
            var e = zip.nextEntry
            while (e != null) {
                if (e.name == "manifest.json") return JSONObject(zip.readBytes().toString(Charsets.UTF_8))
                e = zip.nextEntry
            }
        }
        throw IllegalArgumentException("manifest.json missing in backup")
    }
}
