package com.ramesh.scp_project.core.indexing

import android.net.Uri
import com.ramesh.scp_project.core.data.MediaDao
import com.ramesh.scp_project.core.data.MediaEntity

class IndexingPipeline(
    private val ocrExtractor: OcrExtractor,
    private val financialDocumentFilter: FinancialDocumentFilter,
    private val embeddingGenerator: EmbeddingGenerator,
    private val mediaRepository: MediaRepository
) {

    suspend fun index(
        uri: Uri,
        appSource: String = DEFAULT_APP_SOURCE,
        indexedAtMillis: Long = System.currentTimeMillis()
    ): IndexingOutcome {
        // OCR can fail on corrupted or protected media. Those cases are treated
        // as skippable outcomes so the batch job can continue.
        val extractedText = ocrExtractor.extractText(uri)?.trim().orEmpty()
        if (extractedText.isBlank()) {
            return IndexingOutcome.SkippedBlankText
        }
        if (!financialDocumentFilter.matches(extractedText)) {
            return IndexingOutcome.SkippedNonFinancial
        }

        val embedding = embeddingGenerator.generate(extractedText)
        val mediaEntity = MediaEntity(
            id = uri.toString(),
            uri = uri.toString(),
            extractedText = extractedText,
            timestamp = indexedAtMillis,
            appSource = appSource,
            embedding = embedding
        )

        mediaRepository.insert(mediaEntity)
        return IndexingOutcome.Indexed(mediaEntity)
    }

    companion object {
        private const val DEFAULT_APP_SOURCE = "MediaStore"
    }
}

interface OcrExtractor {
    suspend fun extractText(uri: Uri): String?
}

interface FinancialDocumentFilter {
    fun matches(text: String): Boolean
}

class KeywordFinancialDocumentFilter(
    private val keywords: Set<String> = DEFAULT_FINANCIAL_KEYWORDS
) : FinancialDocumentFilter {

    override fun matches(text: String): Boolean {
        val normalizedText = text.lowercase()
        return keywords.any(normalizedText::contains)
    }

    companion object {
        private val DEFAULT_FINANCIAL_KEYWORDS = setOf(
            "invoice",
            "receipt",
            "payment",
            "total",
            "amount",
            "gst",
            "upi",
            "tax",
            "order id",
            "paid"
        )
    }
}

interface EmbeddingGenerator {
    suspend fun generate(text: String): FloatArray
}

class MockEmbeddingGenerator : EmbeddingGenerator {
    override suspend fun generate(text: String): FloatArray {
        val normalized = text
            .trim()
            .lowercase()
            .split(Regex("\\W+"))
            .filter(String::isNotBlank)

        if (normalized.isEmpty()) {
            return FloatArray(EMBEDDING_SIZE)
        }

        // This is still a placeholder embedding, but it is deterministic and
        // stable across runs, which matters for predictable local search tests.
        return FloatArray(EMBEDDING_SIZE) { index ->
            normalized.sumOf { token ->
                val tokenHash = token.hashCode().toLong()
                ((tokenHash shr (index % 16)) and 0xFF).toInt()
            }.let { bucket ->
                (bucket % 1000) / 1000f
            }
        }
    }

    companion object {
        private const val EMBEDDING_SIZE = 16
    }
}

interface MediaRepository {
    suspend fun insert(mediaEntity: MediaEntity)
}

class MediaRepositoryImpl(
    private val mediaDao: MediaDao
) : MediaRepository {
    override suspend fun insert(mediaEntity: MediaEntity) {
        mediaDao.insert(mediaEntity)
    }
}

sealed interface IndexingOutcome {
    data class Indexed(val mediaEntity: MediaEntity) : IndexingOutcome
    data object SkippedBlankText : IndexingOutcome
    data object SkippedNonFinancial : IndexingOutcome
}
