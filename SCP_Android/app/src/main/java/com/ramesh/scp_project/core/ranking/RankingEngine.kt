package com.ramesh.scp_project.core.ranking

import com.ramesh.scp_project.core.data.MediaEntity
import kotlin.math.max

data class RankedMedia(
    val media: MediaEntity,
    val score: Float
)

class RankingEngine {

    fun rank(
        items: List<MediaEntity>,
        queryEmbedding: FloatArray,
        queryText: String,
        preferredSource: String? = null,
        nowMillis: Long = System.currentTimeMillis()
    ): List<RankedMedia> {
        return items
            .map { media ->
                RankedMedia(
                    media = media,
                    score = calculateScore(
                        media = media,
                        queryEmbedding = queryEmbedding,
                        queryText = queryText,
                        preferredSource = preferredSource,
                        nowMillis = nowMillis
                    )
                )
            }
            .sortedByDescending { it.score }
    }

    fun calculateScore(
        media: MediaEntity,
        queryEmbedding: FloatArray,
        queryText: String,
        preferredSource: String? = null,
        nowMillis: Long = System.currentTimeMillis()
    ): Float {
        val embeddingScore = embeddingScore(queryEmbedding, media.embedding)
        val keywordScore = keywordScore(queryText, media.extractedText)
        val recencyScore = recencyScore(media.timestamp, nowMillis)
        val sourceBoost = sourceBoost(media.appSource, preferredSource)

        return (0.5f * embeddingScore) +
                (0.3f * keywordScore) +
                (0.1f * recencyScore) +
                (0.1f * sourceBoost)
    }

    private fun embeddingScore(queryEmbedding: FloatArray, mediaEmbedding: FloatArray): Float {
        if (queryEmbedding.size != mediaEmbedding.size) return 0f

        var dotProduct = 0f
        var normQuery = 0f
        var normMedia = 0f

        for (index in queryEmbedding.indices) {
            val queryValue = queryEmbedding[index]
            val mediaValue = mediaEmbedding[index]

            dotProduct += queryValue * mediaValue
            normQuery += queryValue * queryValue
            normMedia += mediaValue * mediaValue
        }

        if (normQuery == 0f || normMedia == 0f) return 0f

        val denominator = kotlin.math.sqrt(normQuery.toDouble()) *
                kotlin.math.sqrt(normMedia.toDouble())

        return (dotProduct / denominator).toFloat().coerceIn(0f, 1f)
    }

    private fun keywordScore(queryText: String, extractedText: String): Float {
        val queryTokens = queryText
            .lowercase()
            .split(Regex("\\W+"))
            .filter { it.isNotBlank() }
            .toSet()

        if (queryTokens.isEmpty()) return 0f

        val documentTokens = extractedText
            .lowercase()
            .split(Regex("\\W+"))
            .filter { it.isNotBlank() }
            .toSet()

        val matches = queryTokens.count { it in documentTokens }
        return matches.toFloat() / queryTokens.size.toFloat()
    }

    private fun recencyScore(timestamp: Long, nowMillis: Long): Float {
        val ageMillis = max(0L, nowMillis - timestamp)
        val ageDays = ageMillis / MILLIS_PER_DAY
        return (1f - (ageDays.toFloat() / MAX_RECENCY_DAYS.toFloat())).coerceIn(0f, 1f)
    }

    private fun sourceBoost(appSource: String, preferredSource: String?): Float {
        if (preferredSource.isNullOrBlank()) return DEFAULT_SOURCE_SCORE
        return if (appSource.equals(preferredSource, ignoreCase = true)) 1f else 0f
    }

    companion object {
        private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L
        private const val MAX_RECENCY_DAYS = 30L
        private const val DEFAULT_SOURCE_SCORE = 0.5f
    }
}