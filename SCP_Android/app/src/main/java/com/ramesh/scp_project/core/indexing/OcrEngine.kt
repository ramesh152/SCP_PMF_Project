package com.ramesh.scp_project.core.indexing

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

class OcrEngine(
    context: Context,
    private val recognizer: TextRecognizer = TextRecognition.getClient(
        TextRecognizerOptions.DEFAULT_OPTIONS
    )
) : OcrExtractor {

    private val appContext = context.applicationContext
    private val recognizerClosed = AtomicBoolean(false)

    override suspend fun extractText(uri: Uri): String? {
        if (recognizerClosed.get()) return null

        val image = try {
            InputImage.fromFilePath(appContext, uri)
        } catch (_: Exception) {
            return null
        }

        return suspendCancellableCoroutine { continuation ->
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    if (!continuation.isActive) return@addOnSuccessListener
                    val text = visionText.text.trim()
                    continuation.resume(text.ifBlank { null })
                }
                .addOnFailureListener {
                    if (!continuation.isActive) return@addOnFailureListener
                    continuation.resume(null)
                }
                .addOnCanceledListener {
                    if (!continuation.isActive) return@addOnCanceledListener
                    continuation.resume(null)
                }
        }
    }

    fun close() {
        if (recognizerClosed.compareAndSet(false, true)) {
            recognizer.close()
        }
    }
}
