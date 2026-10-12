package de.belegkasten.core.ocr

import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import javax.inject.Inject
import kotlinx.coroutines.tasks.await

// Bundled ML Kit variant: works without network after install.
// Unbundled (Play-services download) is NOT used, to keep the offline promise.
// Callers build InputImage from a local file (fromFilePath with a content Uri
// or fromBitmap) and pass it here; this class never touches the network.
class OcrEngine @Inject constructor() {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun recognize(image: InputImage): String {
        val result = recognizer.process(image).await()
        return result.text
    }
}
