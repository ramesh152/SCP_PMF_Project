package com.ramesh.scp_project

import android.app.Application
import androidx.work.WorkManager
import com.ramesh.scp_project.core.data.AppDatabase
import com.ramesh.scp_project.core.indexing.IndexingPipeline
import com.ramesh.scp_project.core.indexing.KeywordFinancialDocumentFilter
import com.ramesh.scp_project.core.indexing.MediaIndexer
import com.ramesh.scp_project.core.indexing.MediaRepositoryImpl
import com.ramesh.scp_project.core.indexing.MediaScanner
import com.ramesh.scp_project.core.indexing.MockEmbeddingGenerator
import com.ramesh.scp_project.core.indexing.OcrEngine
import com.ramesh.scp_project.core.ranking.RankingEngine
import com.ramesh.scp_project.core.search.MediaSearchRepositoryImpl
import com.ramesh.scp_project.core.search.SearchEngine

/**
 * A small manual service locator keeps the sample app easy to understand
 * without introducing a dependency injection framework before the project
 * has multiple screens or environment flavors.
 */
class AppContainer(application: Application) {

    private val appContext = application.applicationContext
    val workManager by lazy { WorkManager.getInstance(appContext) }
    private val database by lazy { AppDatabase.getInstance(appContext) }
    private val mediaDao by lazy { database.mediaDao() }
    private val embeddingGenerator by lazy { MockEmbeddingGenerator() }
    private val ocrEngine by lazy { OcrEngine(appContext) }
    private val indexingPipeline by lazy {
        IndexingPipeline(
            ocrExtractor = ocrEngine,
            financialDocumentFilter = KeywordFinancialDocumentFilter(),
            embeddingGenerator = embeddingGenerator,
            mediaRepository = MediaRepositoryImpl(mediaDao)
        )
    }
    private val searchEngine by lazy {
        SearchEngine(
            mediaSearchRepository = MediaSearchRepositoryImpl(mediaDao),
            embeddingGenerator = embeddingGenerator,
            rankingEngine = RankingEngine()
        )
    }

    val mediaIndexer by lazy {
        MediaIndexer(
            mediaScanner = MediaScanner(appContext.contentResolver),
            indexingPipeline = indexingPipeline,
            mediaDao = mediaDao
        )
    }

    val documentSearchRepository by lazy {
        DocumentSearchRepository(
            mediaDao = mediaDao,
            searchEngine = searchEngine
        )
    }

    fun close() {
        ocrEngine.close()
    }
}
