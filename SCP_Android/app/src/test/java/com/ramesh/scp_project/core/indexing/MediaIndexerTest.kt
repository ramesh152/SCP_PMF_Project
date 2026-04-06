package com.ramesh.scp_project.core.indexing

import android.net.Uri
import com.ramesh.scp_project.core.data.MediaDao
import com.ramesh.scp_project.core.data.MediaEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MediaIndexerTest {

    @Test
    fun `indexing summary counts indexed skipped and failed documents`() = runBlocking {
        val scanner = FakeMediaScanner(
            listOf(
                Uri.parse("content://test/indexed"),
                Uri.parse("content://test/blank"),
                Uri.parse("content://test/non-financial"),
                Uri.parse("content://test/failure")
            )
        )
        val pipeline = FakeIndexingPipeline(
            outcomes = mapOf(
                "content://test/indexed" to IndexingOutcome.Indexed(mediaEntity("indexed")),
                "content://test/blank" to IndexingOutcome.SkippedBlankText,
                "content://test/non-financial" to IndexingOutcome.SkippedNonFinancial
            ),
            failingUris = setOf("content://test/failure")
        )
        val indexer = MediaIndexer(
            mediaScanner = scanner,
            indexingPipeline = pipeline,
            mediaDao = FakeMediaDao()
        )

        val summary = indexer.indexLatestImages()

        assertEquals(4, summary.scannedCount)
        assertEquals(1, summary.indexedCount)
        assertEquals(1, summary.skippedBlankTextCount)
        assertEquals(1, summary.skippedNonFinancialCount)
        assertEquals(1, summary.failureCount)
    }

    private fun mediaEntity(id: String): MediaEntity {
        return MediaEntity(
            id = id,
            uri = "content://test/$id",
            extractedText = "receipt payment",
            timestamp = 1L,
            appSource = "MediaStore",
            embedding = floatArrayOf(1f)
        )
    }

    private class FakeMediaScanner(
        private val uris: List<Uri>
    ) : MediaScanSource {
        override fun getLatestImageUris(limit: Int): List<Uri> = uris.take(limit)
    }

    private class FakeIndexingPipeline(
        private val outcomes: Map<String, IndexingOutcome>,
        private val failingUris: Set<String>
    ) : DocumentIndexingPipeline {
        override suspend fun index(
            uri: Uri,
            appSource: String,
            indexedAtMillis: Long
        ): IndexingOutcome {
            val uriString = uri.toString()
            if (uriString in failingUris) {
                throw IllegalStateException("Synthetic failure")
            }
            return outcomes.getValue(uriString)
        }
    }

    private class FakeMediaDao : MediaDao {
        override suspend fun insert(media: MediaEntity) = Unit
        override fun getAll(): Flow<List<MediaEntity>> = flowOf(emptyList())
        override suspend fun count(): Int = 0
        override fun getByTimeRange(start: Long, end: Long): Flow<List<MediaEntity>> = flowOf(emptyList())
        override suspend fun deleteAll() = Unit
    }
}
