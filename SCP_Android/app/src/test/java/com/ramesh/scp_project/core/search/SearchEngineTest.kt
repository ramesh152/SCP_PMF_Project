package com.ramesh.scp_project.core.search

import com.ramesh.scp_project.core.data.MediaEntity
import com.ramesh.scp_project.core.indexing.EmbeddingGenerator
import com.ramesh.scp_project.core.ranking.RankingEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchEngineTest {

    @Test
    fun `blank query returns latest indexed documents first`() = runBlocking {
        val repository = FakeMediaSearchRepository(
            listOf(
                media(id = "old", text = "first receipt", timestamp = 1_000L, source = "Amazon"),
                media(id = "new", text = "second receipt", timestamp = 5_000L, source = "Swiggy")
            )
        )
        val searchEngine = SearchEngine(
            mediaSearchRepository = repository,
            embeddingGenerator = FakeEmbeddingGenerator(),
            rankingEngine = RankingEngine()
        )

        val results = searchEngine.search("")

        assertEquals(listOf("new", "old"), results.map { it.media.id })
        assertTrue(results.all { it.score == 0f })
    }

    @Test
    fun `merchant filter narrows candidates before ranking`() = runBlocking {
        val repository = FakeMediaSearchRepository(
            listOf(
                media(id = "amazon", text = "invoice payment total", timestamp = 4_000L, source = "Amazon"),
                media(id = "swiggy", text = "receipt payment total", timestamp = 5_000L, source = "Swiggy")
            )
        )
        val searchEngine = SearchEngine(
            mediaSearchRepository = repository,
            embeddingGenerator = FakeEmbeddingGenerator(),
            rankingEngine = RankingEngine()
        )

        val results = searchEngine.search("amazon payment")

        assertEquals(listOf("amazon"), results.map { it.media.id })
    }

    private fun media(
        id: String,
        text: String,
        timestamp: Long,
        source: String
    ): MediaEntity {
        return MediaEntity(
            id = id,
            uri = "content://test/$id",
            extractedText = text,
            timestamp = timestamp,
            appSource = source,
            embedding = floatArrayOf(1f, 0.5f, 0.25f, 0.125f)
        )
    }

    private class FakeMediaSearchRepository(
        private val media: List<MediaEntity>
    ) : MediaSearchRepository {
        override suspend fun getAllMedia(): List<MediaEntity> = media
    }

    private class FakeEmbeddingGenerator : EmbeddingGenerator {
        override suspend fun generate(text: String): FloatArray {
            return floatArrayOf(1f, 0.5f, 0.25f, 0.125f)
        }
    }
}
