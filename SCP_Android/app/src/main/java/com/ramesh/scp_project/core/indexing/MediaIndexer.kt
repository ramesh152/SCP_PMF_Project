package com.ramesh.scp_project.core.indexing

import com.ramesh.scp_project.core.data.MediaDao

data class IndexingSummary(
    val scannedCount: Int,
    val indexedCount: Int,
    val skippedBlankTextCount: Int,
    val skippedNonFinancialCount: Int,
    val failureCount: Int
) {
    val skippedCount: Int
        get() = skippedBlankTextCount + skippedNonFinancialCount
}

class MediaIndexer(
    private val mediaScanner: MediaScanSource,
    private val indexingPipeline: DocumentIndexingPipeline,
    private val mediaDao: MediaDao
) {

    /**
     * Scans the latest images and attempts to index each one independently.
     *
     * The function is deliberately tolerant: one corrupted or unreadable image
     * should not abort the whole batch. Every outcome is counted so the caller
     * can explain exactly what happened to the user.
     */
    suspend fun indexLatestImages(limit: Int = DEFAULT_SCAN_LIMIT): IndexingSummary {
        val imageUris = mediaScanner.getLatestImageUris(limit)
        if (imageUris.isEmpty()) {
            return IndexingSummary(
                scannedCount = 0,
                indexedCount = 0,
                skippedBlankTextCount = 0,
                skippedNonFinancialCount = 0,
                failureCount = 0
            )
        }

        var indexedCount = 0
        var skippedBlankTextCount = 0
        var skippedNonFinancialCount = 0
        var failureCount = 0

        for (uri in imageUris) {
            try {
                when (indexingPipeline.index(uri)) {
                    is IndexingOutcome.Indexed -> indexedCount += 1
                    IndexingOutcome.SkippedBlankText -> skippedBlankTextCount += 1
                    IndexingOutcome.SkippedNonFinancial -> skippedNonFinancialCount += 1
                }
            } catch (_: Exception) {
                // Single-document failures should not abort the entire batch
                // because media stores often contain partially readable items.
                failureCount += 1
            }
        }

        return IndexingSummary(
            scannedCount = imageUris.size,
            indexedCount = indexedCount,
            skippedBlankTextCount = skippedBlankTextCount,
            skippedNonFinancialCount = skippedNonFinancialCount,
            failureCount = failureCount
        )
    }

    /**
     * Returns the current number of stored indexed documents.
     */
    suspend fun indexedCount(): Int = mediaDao.count()

    companion object {
        private const val DEFAULT_SCAN_LIMIT = 200
    }
}
