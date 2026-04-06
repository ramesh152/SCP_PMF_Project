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
    ): MediaEntity? {
        val extractedText = ocrExtractor.extractText(uri)?.trim().orEmpty()
        if (extractedText.isBlank()) return null
        if (!financialDocumentFilter.matches(extractedText)) return null

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
        return mediaEntity
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
            "amount"
        )
    }
}

interface EmbeddingGenerator {
    suspend fun generate(text: String): FloatArray
}

class MockEmbeddingGenerator : EmbeddingGenerator {
    override suspend fun generate(text: String): FloatArray {
        val seed = text.hashCode()
        return FloatArray(8) { index ->
            ((seed + index) % 1000) / 1000f
        }
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
