package de.belegkasten.core.backup

import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherOutputStream
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters

// BKBF2: fixed, authenticated header; Argon2id v1.3, 64 MiB, 3 passes, 1 lane.
// No attacker-controlled KDF cost. BKBF1 is the previous PBKDF2 prototype.
object BackupCrypto {
    private val magic = "BKBF2".toByteArray(Charsets.US_ASCII)

    private fun key(password: CharArray, salt: ByteArray): SecretKeySpec {
        require(password.size >= 8) { "Passwort muss mindestens 8 Zeichen haben" }
        val generator = Argon2BytesGenerator()
        generator.init(Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withVersion(Argon2Parameters.ARGON2_VERSION_13).withSalt(salt)
            .withMemoryAsKB(65536).withIterations(3).withParallelism(1).build())
        val bytes = ByteArray(32)
        generator.generateBytes(password, bytes)
        return try { SecretKeySpec(bytes, "AES") } finally { bytes.fill(0) }
    }

    fun encrypt(input: InputStream, output: OutputStream, password: CharArray) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val header = magic + salt + iv
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key(password, salt), GCMParameterSpec(128, iv))
        cipher.updateAAD(header)
        output.write(header)
        CipherOutputStream(output, cipher).use { input.copyTo(it) }
    }

    fun decrypt(input: InputStream, output: OutputStream, password: CharArray) {
        val header = ByteArray(33)
        var read = 0
        while (read < header.size) {
            val n = input.read(header, read, header.size - read)
            require(n > 0) { "Sicherung unvollständig" }
            read += n
        }
        val version = header.copyOfRange(0, 5).toString(Charsets.US_ASCII)
        require(version == "BKBF2" || version == "BKBF1") { "Nicht unterstütztes Sicherungsformat" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val salt = header.copyOfRange(5, 21)
        val derived = if (version == "BKBF2") key(password, salt) else {
            val spec = PBEKeySpec(password, salt, 600000, 256)
            try {
                val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
                try { SecretKeySpec(bytes, "AES") } finally { bytes.fill(0) }
            } finally { spec.clearPassword() }
        }
        cipher.init(Cipher.DECRYPT_MODE, derived,
            GCMParameterSpec(128, header.copyOfRange(21, 33)))
        if (version == "BKBF2") cipher.updateAAD(header)
        try {
            // CipherInputStream may suppress authentication errors on some Android
            // providers. Always call doFinal explicitly before trusting the staged ZIP.
            val buffer = ByteArray(8192)
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                cipher.update(buffer, 0, n)?.let(output::write)
            }
            output.write(cipher.doFinal())
        } catch (e: Exception) {
            throw IllegalArgumentException("Falsches Passwort oder beschädigte Sicherung", e)
        }
    }
}
