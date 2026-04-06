package com.ramesh.scp_project.core.indexing

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

class OcrEngine(
    context: Context,
    private val recognizer: TextRecognizer = TextRecognition.getClient(
        TextRecognizerOptions.DEFAULT_OPTIONS
    )
) : OcrExtractor {

    private val appContext = context.applicationContext

    override suspend fun extractText(uri: Uri): String? {
        val image = try {
            InputImage.fromFilePath(appContext, uri)
        } catch (_: Exception) {
            return null
        }

        return suspendCancellableCoroutine { continuation ->
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val text = visionText.text.trim()
                    continuation.resume(text.ifBlank { null })
                }
                .addOnFailureListener {
                    continuation.resume(null)
                }
                .addOnCanceledListener {
                    continuation.resume(null)
                }
        }
    }
}
