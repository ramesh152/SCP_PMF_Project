package com.ramesh.scp_project

import com.ramesh.scp_project.core.data.MediaDao
import com.ramesh.scp_project.core.indexing.IndexingSummary
import com.ramesh.scp_project.core.indexing.MediaIndexer
import com.ramesh.scp_project.core.ranking.RankedMedia
import com.ramesh.scp_project.core.search.SearchEngine

class DocumentSearchRepository(
    private val mediaDao: MediaDao,
    private val searchEngine: SearchEngine
) {

    suspend fun search(query: String): List<RankedMedia> {
        return searchEngine.search(query)
    }

    suspend fun indexedCount(): Int {
        return mediaDao.count()
    }

    companion object {
        const val DEFAULT_INDEX_LIMIT = 200
    }
}
