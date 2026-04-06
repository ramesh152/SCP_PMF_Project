package com.ramesh.scp_project

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import com.ramesh.scp_project.core.indexing.IndexingSummary
import com.ramesh.scp_project.core.indexing.IndexingWorker
import com.ramesh.scp_project.core.ranking.RankedMedia
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MainUiState(
    val query: String = "",
    val results: List<RankedMedia> = emptyList(),
    val isLoadingResults: Boolean = false,
    val isIndexing: Boolean = false,
    val hasMediaPermission: Boolean = false,
    val hasRequestedMediaPermission: Boolean = false,
    val shouldShowPermissionRationale: Boolean = false,
    val isPermissionPermanentlyDenied: Boolean = false,
    val statusMessage: String = "Grant photo access to index receipts and invoices.",
    val errorMessage: String? = null,
    val indexedDocumentCount: Int = 0,
    val lastIndexSummary: IndexingSummary? = null
)

class MainViewModel(
    application: Application,
    private val appContainer: AppContainer = (application as ScpApplication).appContainer
) : AndroidViewModel(application) {

    private val repository = appContainer.documentSearchRepository
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        observeIndexingWork()
    }

    /**
     * Marks that the user has actively requested permission at least once.
     *
     * Android does not expose a direct "permanently denied" flag. Tracking
     * whether a request was attempted is required to distinguish first launch
     * from a real denial that should route the user to Settings.
     */
    fun onPermissionRequestStarted() {
        _uiState.update { current ->
            current.copy(hasRequestedMediaPermission = true)
        }
    }

    /**
     * Applies the latest media-permission state and updates the UI copy so the
     * screen always reflects the next valid user action.
     *
     * When permission becomes available, the method refreshes cached counts and
     * search results. When permission is removed, it clears sensitive/stale
     * search data instead of leaving previously indexed results visible.
     */
    fun onPermissionStateChanged(
        isGranted: Boolean,
        shouldShowRationale: Boolean = false
    ) {
        _uiState.update { current ->
            // Android only exposes "show rationale" and "granted", so the
            // permanently denied state has to be inferred after at least one
            // request attempt has already been made.
            val permanentlyDenied = current.hasRequestedMediaPermission &&
                !isGranted &&
                !shouldShowRationale
            current.copy(
                hasMediaPermission = isGranted,
                shouldShowPermissionRationale = shouldShowRationale,
                isPermissionPermanentlyDenied = permanentlyDenied,
                statusMessage = when {
                    isGranted && current.indexedDocumentCount > 0 ->
                        "Indexed ${current.indexedDocumentCount} document(s). Search is ready."
                    isGranted ->
                        "Permission granted. Index the latest images to build the local search database."
                    permanentlyDenied ->
                        "Photo access is blocked. Open app settings and allow access to continue."
                    shouldShowRationale ->
                        "Photo access is needed to scan invoice and receipt images from the device."
                    else ->
                        "Photo access is required before indexing can start."
                }
            )
        }

        if (isGranted) {
            loadIndexedCount()
            refreshSearch()
        } else {
            _uiState.update {
                it.copy(
                    results = emptyList(),
                    isLoadingResults = false,
                    isIndexing = false
                )
            }
        }
    }

    /**
     * Keeps the query text in a single state holder so recomposition and search
     * execution always work with the same source of truth.
     */
    fun onQueryChanged(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    /**
     * Clears the transient error banner after the user has acknowledged it.
     */
    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * Schedules a single background indexing job.
     *
     * The UI guards against duplicate requests and missing permissions before
     * delegating work to WorkManager. The actual heavy work is intentionally not
     * executed directly from the ViewModel so it can survive configuration
     * changes and short-lived process transitions.
     */
    fun refreshIndex() {
        val snapshot = _uiState.value
        if (snapshot.isIndexing || !snapshot.hasMediaPermission) {
            if (!snapshot.hasMediaPermission) {
                _uiState.update {
                    it.copy(errorMessage = "Photo permission is required before indexing can start.")
                }
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isIndexing = true,
                    errorMessage = null,
                    statusMessage = "Scheduling background indexing for recent images..."
                )
            }

            IndexingWorker.enqueue(appContainer.workManager)
        }
    }

    /**
     * Triggers a fresh search using the current query snapshot.
     */
    fun submitSearch() {
        refreshSearch()
    }

    /**
     * Reads the current number of indexed documents from Room.
     *
     * This runs independently from search so the app can restore meaningful
     * status text even before the first query is submitted.
     */
    private fun loadIndexedCount() {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) { repository.indexedCount() }
            }.onSuccess { count ->
                _uiState.update { current ->
                    current.copy(
                        indexedDocumentCount = count,
                        statusMessage = if (count > 0 && current.hasMediaPermission) {
                            "Indexed $count document(s). Search is ready."
                        } else {
                            current.statusMessage
                        }
                    )
                }
            }
        }
    }

    /**
     * Executes the current search against the local Room-backed index.
     *
     * All database and ranking work stays on an IO dispatcher to keep Compose
     * rendering responsive. Cancellation is rethrown so structured concurrency
     * remains correct during rapid lifecycle changes.
     */
    private fun refreshSearch() {
        val snapshot = _uiState.value
        if (!snapshot.hasMediaPermission) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoadingResults = true,
                    errorMessage = null
                )
            }

            try {
                val query = _uiState.value.query.trim()
                // Search and count are both IO-bound. Keeping them off the main
                // thread prevents large local databases from stalling Compose.
                val results = withContext(Dispatchers.IO) {
                    repository.search(query)
                }
                val indexedCount = withContext(Dispatchers.IO) {
                    repository.indexedCount()
                }

                _uiState.update {
                    it.copy(
                        results = results,
                        indexedDocumentCount = indexedCount,
                        isLoadingResults = false,
                        statusMessage = when {
                            indexedCount == 0 ->
                                "No indexed documents yet. Run indexing to populate local search."
                            query.isBlank() ->
                                "Showing the most recent indexed documents."
                            results.isEmpty() ->
                                "No matching documents found for \"$query\"."
                            else ->
                                "Showing ${results.size} result(s) for \"$query\"."
                        }
                    )
                }
            } catch (cancellation: CancellationException) {
                _uiState.update {
                    it.copy(isLoadingResults = false)
                }
                throw cancellation
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingResults = false,
                        errorMessage = error.message ?: "Search failed unexpectedly.",
                        statusMessage = "Search failed. Fix the issue and retry."
                    )
                }
            }
        }
    }

    /**
     * Watches the unique indexing work and converts worker states into stable
     * UI state transitions.
     *
     * Unique work can briefly expose both finished and unfinished records. The
     * selection logic prefers an active record first and only falls back to the
     * most recent finished record when no active work exists.
     */
    private fun observeIndexingWork() {
        viewModelScope.launch {
            appContainer.workManager
                .getWorkInfosForUniqueWorkFlow(IndexingWorker.UNIQUE_WORK_NAME)
                .collectLatest { workInfos ->
                    val latestWork = workInfos.firstOrNull { !it.state.isFinished }
                        ?: workInfos.firstOrNull()
                    when {
                        latestWork == null -> {
                            _uiState.update { it.copy(isIndexing = false) }
                        }
                        latestWork.state == WorkInfo.State.ENQUEUED ||
                            latestWork.state == WorkInfo.State.RUNNING -> {
                            _uiState.update {
                                it.copy(
                                    isIndexing = true,
                                    errorMessage = null,
                                    statusMessage = "Scanning recent images and extracting financial documents..."
                                )
                            }
                        }
                        latestWork.state == WorkInfo.State.SUCCEEDED -> {
                            val summary = IndexingWorker.summaryFrom(latestWork.outputData)
                            val updatedCount = withContext(Dispatchers.IO) {
                                repository.indexedCount()
                            }
                            _uiState.update {
                                it.copy(
                                    isIndexing = false,
                                    indexedDocumentCount = updatedCount,
                                    lastIndexSummary = summary,
                                    statusMessage = buildIndexStatusMessage(summary, updatedCount)
                                )
                            }
                            refreshSearch()
                        }
                        latestWork.state == WorkInfo.State.FAILED -> {
                            val errorMessage = latestWork.outputData
                                .getString(IndexingWorker.KEY_ERROR)
                                ?: "Background indexing failed."
                            _uiState.update {
                                it.copy(
                                    isIndexing = false,
                                    errorMessage = errorMessage,
                                    statusMessage = "Indexing failed. Fix the issue and try again."
                                )
                            }
                        }
                        latestWork.state == WorkInfo.State.CANCELLED -> {
                            _uiState.update {
                                it.copy(
                                    isIndexing = false,
                                    statusMessage = "Indexing was cancelled."
                                )
                            }
                        }
                        else -> Unit
                    }
                }
        }
    }

    /**
     * Formats the post-indexing status line shown in the header card.
     */
    private fun buildIndexStatusMessage(summary: IndexingSummary, indexedCount: Int): String {
        return if (summary.indexedCount == 0 && indexedCount == 0) {
            "No financial documents were found in the scanned images."
        } else {
            "Indexed ${summary.indexedCount} new document(s). " +
                "Scanned ${summary.scannedCount}, skipped ${summary.skippedCount}, " +
                "failed ${summary.failureCount}, total stored $indexedCount."
        }
    }

    /**
     * Releases long-lived resources that are owned by the application container
     * but used by this ViewModel, such as the ML Kit recognizer.
     */
    override fun onCleared() {
        appContainer.close()
        super.onCleared()
    }

    companion object {
        fun factory(application: Application): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    return MainViewModel(application) as T
                }
            }
        }
    }
}
