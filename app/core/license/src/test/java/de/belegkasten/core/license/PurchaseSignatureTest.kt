package de.belegkasten.core.license
import java.security.KeyPairGenerator
import java.security.Signature
import java.util.Base64
import kotlin.test.*
class PurchaseSignatureTest {
    @Test fun validSignatureOnly() {
        val pair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        val data = "{\"productId\":\"belegkasten_lifetime\"}"
        val signature = Signature.getInstance("SHA1withRSA").run { initSign(pair.private); update(data.toByteArray()); sign() }
        val key = Base64.getEncoder().encodeToString(pair.public.encoded)
        val encoded = Base64.getEncoder().encodeToString(signature)
        assertTrue(verifyPurchaseSignature(key, data, encoded))
        assertFalse(verifyPurchaseSignature(key, data + " ", encoded))
        assertFalse(verifyPurchaseSignature("", data, encoded))
        assertFalse(verifyPurchaseSignature("not-a-key", data, encoded))
    }
}
