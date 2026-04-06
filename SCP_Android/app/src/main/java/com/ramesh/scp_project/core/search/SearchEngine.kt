package com.ramesh.scp_project.core.search

import com.ramesh.scp_project.core.data.MediaDao
import com.ramesh.scp_project.core.data.MediaEntity
import com.ramesh.scp_project.core.indexing.EmbeddingGenerator
import com.ramesh.scp_project.core.ranking.RankedMedia
import com.ramesh.scp_project.core.ranking.RankingEngine
import kotlinx.coroutines.flow.first

class SearchEngine(
    private val mediaSearchRepository: MediaSearchRepository,
    private val embeddingGenerator: EmbeddingGenerator,
    private val rankingEngine: RankingEngine,
    private val queryParser: QueryParser = QueryParser()
) {

    suspend fun search(
        rawQuery: String,
        nowMillis: Long = System.currentTimeMillis()
    ): List<RankedMedia> {
        val parsedQuery = queryParser.parse(rawQuery)
        val baseText = parsedQuery.text.ifBlank { rawQuery.trim() }

        val candidates = mediaSearchRepository.getAllMedia()
            .asSequence()
            .filterByTime(parsedQuery.timeFilterDays, nowMillis)
            .filterByMerchant(parsedQuery.merchant)
            .toList()

        if (candidates.isEmpty()) return emptyList()

        if (baseText.isBlank()) {
            // Blank searches act as a "recent documents" view so the app stays
            // useful even before the user learns the query syntax.
            return candidates
                .sortedByDescending(MediaEntity::timestamp)
                .take(MAX_RESULTS)
                .map { media -> RankedMedia(media = media, score = 0f) }
        }

        val queryEmbedding = embeddingGenerator.generate(baseText)

        return rankingEngine.rank(
            items = candidates,
            queryEmbedding = queryEmbedding,
            queryText = baseText,
            preferredSource = parsedQuery.merchant,
            nowMillis = nowMillis
        ).take(MAX_RESULTS)
    }

    private fun Sequence<MediaEntity>.filterByTime(
        timeFilterDays: Int?,
        nowMillis: Long
    ): Sequence<MediaEntity> {
        if (timeFilterDays == null) return this

        val cutoffMillis = nowMillis - (timeFilterDays * MILLIS_PER_DAY)
        return filter { media -> media.timestamp >= cutoffMillis }
    }

    private fun Sequence<MediaEntity>.filterByMerchant(
        merchant: String?
    ): Sequence<MediaEntity> {
        if (merchant.isNullOrBlank()) return this

        return filter { media ->
            media.extractedText.contains(merchant, ignoreCase = true) ||
                media.appSource.contains(merchant, ignoreCase = true)
        }
    }

    companion object {
        private const val MAX_RESULTS = 20
        private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L
    }
}

interface MediaSearchRepository {
    suspend fun getAllMedia(): List<MediaEntity>
}

class MediaSearchRepositoryImpl(
    private val mediaDao: MediaDao
) : MediaSearchRepository {
    override suspend fun getAllMedia(): List<MediaEntity> {
        return mediaDao.getAll().first()
    }
}
