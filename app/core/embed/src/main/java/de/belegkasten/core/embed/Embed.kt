package de.belegkasten.core.embed

import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

const val EMBED_DIM = 256

// Future real-model contract. No model loader or PAD pack is integrated yet.
// CRITICAL: float32/bfloat16 only — never float16 (NaN per model card).
// Task prefixes are mandatory: query "task: search result | query: ...",
// document "title: ... | text: ...".
interface Embedder {
    suspend fun embedQuery(text: String): FloatArray
    suspend fun embedDocument(title: String, text: String): FloatArray
}

// Development-only vectors persisted until the model decision is resolved.
// Deterministic char-trigram hashing, L2-normalized. NOT a semantic search —
// Not used by the current text/field search or eligible for semantic acceptance.
class HashEmbedder @Inject constructor() : Embedder {
    override suspend fun embedQuery(text: String): FloatArray =
        embed("task: search result | query: $text")

    override suspend fun embedDocument(title: String, text: String): FloatArray =
        embed("title: $title | text: $text")

    private fun embed(text: String): FloatArray {
        val vec = FloatArray(EMBED_DIM)
        val norm = text.lowercase()
        val tris = if (norm.length < 3) listOf(norm) else norm.windowed(3)
        for (t in tris) {
            vec[(t.hashCode() and Int.MAX_VALUE) % EMBED_DIM] += 1f
        }
        val n = sqrt(vec.sumOf { (it * it).toDouble() }).toFloat().takeIf { it > 0 } ?: 1f
        return vec.map { it / n }.toFloatArray()
    }
}

@Singleton
class EmbeddingStore @Inject constructor() {
    fun toBlob(vector: FloatArray): ByteArray {
        require(vector.size == EMBED_DIM) { "expected $EMBED_DIM dims, got ${vector.size}" }
        validate(vector)
        return ByteBuffer.allocate(vector.size * 4).order(ByteOrder.LITTLE_ENDIAN).let { buf ->
            vector.forEach { buf.putFloat(it) }
            buf.array()
        }
    }

    fun fromBlob(blob: ByteArray): FloatArray {
        require(blob.size == EMBED_DIM * 4) { "corrupt embedding blob: ${blob.size} bytes" }
        return ByteBuffer.wrap(blob).order(ByteOrder.LITTLE_ENDIAN).let { buf ->
            FloatArray(EMBED_DIM) { buf.float }.also(::validate)
        }
    }

    fun cosine(a: FloatArray, b: FloatArray): Float {
        require(a.size == EMBED_DIM && b.size == EMBED_DIM)
        validate(a)
        validate(b)
        var dot = 0.0
        for (i in a.indices) dot += a[i] * b[i]
        return dot.toFloat() // inputs are L2-normalized
    }

    fun rank(query: FloatArray, candidates: Map<String, FloatArray>, topK: Int = 3): List<String> =
        candidates.entries.sortedByDescending { cosine(query, it.value) }.take(topK).map { it.key }

    private fun validate(vector: FloatArray) {
        require(vector.all { it.isFinite() }) { "non-finite embedding" }
        val norm = vector.sumOf { it.toDouble() * it.toDouble() }
        require(kotlin.math.abs(norm - 1.0) < 0.01) { "embedding must be L2-normalized" }
    }
}
