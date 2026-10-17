package de.belegkasten.core.license

import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

// Public verification key from Play Console, never a service-account credential.
fun verifyPurchaseSignature(publicKey: String, json: String, signature: String): Boolean = runCatching {
    if (publicKey.isBlank() || json.isBlank() || signature.isBlank()) return false
    val key = KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(Base64.getDecoder().decode(publicKey)))
    Signature.getInstance("SHA1withRSA").run {
        initVerify(key)
        update(json.toByteArray(Charsets.UTF_8))
        verify(Base64.getDecoder().decode(signature))
    }
}.getOrDefault(false)
