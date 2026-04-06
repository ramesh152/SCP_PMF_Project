package com.ramesh.scp_project

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.ramesh.scp_project.core.data.MediaEntity
import com.ramesh.scp_project.core.ranking.RankedMedia
import com.ramesh.scp_project.core.ui.SearchScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val sampleData = remember { sampleRankedMedia() }
                    val visibleResults = remember { mutableStateListOf<RankedMedia>().apply { addAll(sampleData) } }
                    var lastQuery by remember { mutableStateOf("") }

                    SearchScreen(
                        results = visibleResults,
                        onSearch = { query ->
                            lastQuery = query
                            val normalizedQuery = query.trim()
                            val filteredResults = if (normalizedQuery.isBlank()) {
                                sampleData
                            } else {
                                sampleData.filter { rankedMedia ->
                                    rankedMedia.media.extractedText.contains(normalizedQuery, ignoreCase = true) ||
                                        rankedMedia.media.appSource.contains(normalizedQuery, ignoreCase = true)
                                }
                            }

                            visibleResults.clear()
                            visibleResults.addAll(filteredResults)
                        },
                        onResultClick = { _ -> },
                        initialQuery = lastQuery
                    )
                }
            }
        }
    }
}

private fun sampleRankedMedia(): List<RankedMedia> {
    val now = System.currentTimeMillis()
    return listOf(
        RankedMedia(
            media = MediaEntity(
                id = "invoice_001",
                uri = "content://media/external/images/media/1",
                extractedText = "Amazon invoice total amount paid for electronics order",
                timestamp = now - 2_000_000L,
                appSource = "Amazon",
                embedding = floatArrayOf(0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.2f, 0.1f, 0.7f)
            ),
            score = 0.91f
        ),
        RankedMedia(
            media = MediaEntity(
                id = "receipt_002",
                uri = "content://media/external/images/media/2",
                extractedText = "Swiggy receipt payment total for food order",
                timestamp = now - 8_000_000L,
                appSource = "Swiggy",
                embedding = floatArrayOf(0.3f, 0.2f, 0.6f, 0.1f, 0.7f, 0.5f, 0.2f, 0.4f)
            ),
            score = 0.84f
        ),
        RankedMedia(
            media = MediaEntity(
                id = "receipt_003",
                uri = "content://media/external/images/media/3",
                extractedText = "Zomato receipt amount and payment confirmation",
                timestamp = now - 16_000_000L,
                appSource = "Zomato",
                embedding = floatArrayOf(0.2f, 0.1f, 0.4f, 0.6f, 0.5f, 0.3f, 0.4f, 0.2f)
            ),
            score = 0.79f
        )
    )
}
