package com.ramesh.scp_project.core.indexing

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters

import com.ramesh.scp_project.ScpApplication

class IndexingWorker(
    appContext: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(appContext, workerParameters) {

    /**
     * Executes one full indexing batch in the background.
     *
     * Any unexpected exception is converted into a failed work result rather
     * than crashing the process. This keeps the UI able to recover cleanly and
     * surface a readable error message.
     */
    override suspend fun doWork(): Result {
        val application = applicationContext as? ScpApplication
            ?: return Result.failure(errorData("Application container is unavailable."))

        return try {
            val summary = application.appContainer.mediaIndexer.indexLatestImages()
            Result.success(summary.toWorkData())
        } catch (error: Exception) {
            Result.failure(errorData(error.message ?: "Background indexing failed."))
        }
    }

    companion object {
        const val UNIQUE_WORK_NAME = "document-indexing"
        const val KEY_SCANNED = "scanned_count"
        const val KEY_INDEXED = "indexed_count"
        const val KEY_SKIPPED_BLANK = "skipped_blank_count"
        const val KEY_SKIPPED_NON_FINANCIAL = "skipped_non_financial_count"
        const val KEY_FAILURES = "failure_count"
        const val KEY_ERROR = "error_message"

        /**
         * Enqueues a unique one-shot indexing request.
         *
         * `KEEP` prevents duplicate OCR batches from running concurrently when
         * the user taps the index button multiple times.
         */
        fun enqueue(workManager: WorkManager) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .setRequiresStorageNotLow(true)
                .build()

            val request = OneTimeWorkRequestBuilder<IndexingWorker>()
                .setConstraints(constraints)
                .addTag(UNIQUE_WORK_NAME)
                .build()

            workManager.enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request
            )
        }

        /**
         * Reconstructs a typed indexing summary from WorkManager output data.
         */
        fun summaryFrom(data: Data): IndexingSummary {
            return IndexingSummary(
                scannedCount = data.getInt(KEY_SCANNED, 0),
                indexedCount = data.getInt(KEY_INDEXED, 0),
                skippedBlankTextCount = data.getInt(KEY_SKIPPED_BLANK, 0),
                skippedNonFinancialCount = data.getInt(KEY_SKIPPED_NON_FINANCIAL, 0),
                failureCount = data.getInt(KEY_FAILURES, 0)
            )
        }

        /**
         * Serializes the worker result into primitive values because WorkManager
         * only supports a restricted `Data` payload format.
         */
        private fun IndexingSummary.toWorkData(): Data {
            return Data.Builder()
                .putInt(KEY_SCANNED, scannedCount)
                .putInt(KEY_INDEXED, indexedCount)
                .putInt(KEY_SKIPPED_BLANK, skippedBlankTextCount)
                .putInt(KEY_SKIPPED_NON_FINANCIAL, skippedNonFinancialCount)
                .putInt(KEY_FAILURES, failureCount)
                .build()
        }

        /**
         * Packages a worker failure into output data so the ViewModel can show a
         * meaningful message without depending on logs.
         */
        private fun errorData(message: String): Data {
            return Data.Builder()
                .putString(KEY_ERROR, message)
                .build()
        }
    }
}
