package com.ramesh.scp_project.core.ui

import android.net.Uri
import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.ramesh.scp_project.MainUiState
import com.ramesh.scp_project.R
import com.ramesh.scp_project.core.ranking.RankedMedia
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SearchScreen(
    state: MainUiState,
    onQueryChanged: (String) -> Unit,
    onSearch: () -> Unit,
    onIndexRequest: () -> Unit,
    onGrantPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    MaterialTheme {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatusCard(
                state = state,
                onIndexRequest = onIndexRequest,
                onGrantPermission = onGrantPermission,
                onOpenSettings = onOpenSettings
            )

            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = stringResource(R.string.search_hint)) },
                singleLine = true,
                enabled = state.hasMediaPermission,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        focusManager.clearFocus()
                        onSearch()
                    }
                )
            )

            if (state.errorMessage != null) {
                ErrorCard(
                    message = state.errorMessage,
                    onDismiss = onDismissError
                )
            }

            if (state.isLoadingResults) {
                LoadingState()
            } else if (state.results.isEmpty()) {
                EmptyResultsState(
                    hasQuery = state.query.isNotBlank(),
                    hasPermission = state.hasMediaPermission,
                    hasIndexedDocuments = state.indexedDocumentCount > 0,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = state.results,
                        key = { rankedMedia -> rankedMedia.media.id }
                    ) { rankedMedia ->
                        SearchResultCard(
                            result = rankedMedia,
                            query = state.query
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusCard(
    state: MainUiState,
    onIndexRequest: () -> Unit,
    onGrantPermission: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Receipt Search",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = state.statusMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Indexed documents: ${state.indexedDocumentCount}",
                style = MaterialTheme.typography.labelLarge
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when {
                    state.hasMediaPermission -> {
                        Button(
                            onClick = onIndexRequest,
                            enabled = !state.isIndexing
                        ) {
                            if (state.isIndexing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(text = stringResource(R.string.index_now))
                            }
                        }
                    }
                    state.isPermissionPermanentlyDenied -> {
                        Button(onClick = onOpenSettings) {
                            Text(text = stringResource(R.string.open_settings))
                        }
                    }
                    state.shouldShowPermissionRationale || !state.hasRequestedMediaPermission -> {
                        Button(onClick = onGrantPermission) {
                            Text(text = stringResource(R.string.grant_access))
                        }
                    }
                    else -> {
                        Button(onClick = onGrantPermission) {
                            Text(text = stringResource(R.string.grant_access))
                        }
                    }
                }
            }

            state.lastIndexSummary?.let { summary ->
                Text(
                    text = "Last scan: ${summary.scannedCount} scanned, ${summary.indexedCount} indexed, " +
                        "${summary.skippedCount} skipped, ${summary.failureCount} failed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ErrorCard(
    message: String,
    onDismiss: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.size(12.dp))
            Button(onClick = onDismiss) {
                Text(text = "Dismiss")
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyResultsState(
    hasQuery: Boolean,
    hasPermission: Boolean,
    hasIndexedDocuments: Boolean,
    modifier: Modifier = Modifier
) {
    val message = when {
        !hasPermission -> "Grant media access to start scanning receipts and invoices."
        !hasIndexedDocuments -> "No indexed documents yet. Run indexing to populate the local database."
        hasQuery -> "No results found for the current query."
        else -> "Run a search or browse your latest indexed documents."
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SearchResultCard(
    result: RankedMedia,
    query: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ResultThumbnail(uri = result.media.uri)

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = highlightQueryMatches(
                        text = result.media.extractedText.ifBlank { result.media.id },
                        query = query
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = formatDate(result.media.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = result.media.appSource,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun ResultThumbnail(uri: String) {
    AndroidView(
        factory = { context ->
            ImageView(context).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                clipToOutline = true
            }
        },
        modifier = Modifier
            .size(88.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp)
            ),
        update = { imageView ->
            runCatching {
                imageView.setImageURI(Uri.parse(uri))
            }.onFailure {
                imageView.setImageDrawable(null)
            }
        }
    )
}

private fun highlightQueryMatches(
    text: String,
    query: String
): AnnotatedString {
    if (text.isBlank() || query.isBlank()) {
        return AnnotatedString(text)
    }

    val terms = query
        .trim()
        .split(Regex("\\W+"))
        .filter { it.isNotBlank() }
        .distinct()

    if (terms.isEmpty()) {
        return AnnotatedString(text)
    }

    val ranges = mutableListOf<IntRange>()
    val lowerText = text.lowercase(Locale.getDefault())

    for (term in terms) {
        val lowerTerm = term.lowercase(Locale.getDefault())
        var startIndex = lowerText.indexOf(lowerTerm)

        while (startIndex >= 0) {
            ranges += startIndex until (startIndex + lowerTerm.length)
            startIndex = lowerText.indexOf(lowerTerm, startIndex + lowerTerm.length)
        }
    }

    if (ranges.isEmpty()) {
        return AnnotatedString(text)
    }

    return buildAnnotatedString {
        append(text)
        ranges.forEach { range ->
            addStyle(
                style = SpanStyle(
                    background = Color(0xFFFFF176),
                    fontWeight = FontWeight.SemiBold
                ),
                start = range.first,
                end = range.last + 1
            )
        }
    }
}

private fun formatDate(timestamp: Long): String {
    return DateFormat.getDateTimeInstance(
        DateFormat.MEDIUM,
        DateFormat.SHORT
    ).format(Date(timestamp))
}
