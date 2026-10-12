package de.belegkasten.core.embed

import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

const val EMBED_DIM = 256

// Loads the bundled EmbeddingGemma variant via MediaPipe tasks-retrieval
// (UniversalEmbedder) from the install-time asset pack "embed_model".
// CRITICAL: float32/bfloat16 only — never float16 (NaN per model card).
// Task prefixes are mandatory: query "task: search result | query: ...",
// document "title: ... | text: ...".
interface Embedder {
    suspend fun embedQuery(text: String): FloatArray
    suspend fun embedDocument(title: String, text: String): FloatArray
}

// Fallback used until the model pack is wired (Spike 0.3 / Phase 3.5).
// Deterministic char-trigram hashing, L2-normalized. NOT a semantic search —
// replaced by the MediaPipe implementation before the acceptance test.
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
        return ByteBuffer.allocate(vector.size * 4).order(ByteOrder.LITTLE_ENDIAN).let { buf ->
            vector.forEach { buf.putFloat(it) }
            buf.array()
        }
    }

    fun fromBlob(blob: ByteArray): FloatArray {
        require(blob.size == EMBED_DIM * 4) { "corrupt embedding blob: ${blob.size} bytes" }
        return ByteBuffer.wrap(blob).order(ByteOrder.LITTLE_ENDIAN).let { buf ->
            FloatArray(EMBED_DIM) { buf.float }
        }
    }

    fun cosine(a: FloatArray, b: FloatArray): Float {
        require(a.size == b.size)
        var dot = 0.0
        for (i in a.indices) dot += a[i] * b[i]
        return dot.toFloat() // inputs are L2-normalized
    }

    fun rank(query: FloatArray, candidates: Map<String, FloatArray>, topK: Int = 3): List<String> =
        candidates.entries.sortedByDescending { cosine(query, it.value) }.take(topK).map { it.key }
}
