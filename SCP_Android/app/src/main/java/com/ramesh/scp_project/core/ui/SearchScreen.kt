package com.ramesh.scp_project.core.ui

import android.net.Uri
import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.ramesh.scp_project.core.ranking.RankedMedia
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SearchScreen(
    results: List<RankedMedia>,
    onSearch: (String) -> Unit,
    onResultClick: (RankedMedia) -> Unit,
    modifier: Modifier = Modifier,
    initialQuery: String = ""
) {
    var query by rememberSaveable { mutableStateOf(initialQuery) }
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.weight(1f),
                label = { Text(text = "Search") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        focusManager.clearFocus()
                        onSearch(query.trim())
                    }
                )
            )

            Button(
                onClick = {
                    focusManager.clearFocus()
                    onSearch(query.trim())
                }
            ) {
                Text(text = "Go")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (results.isEmpty()) {
            EmptyResultsState(
                hasQuery = query.isNotBlank(),
                modifier = Modifier.fillMaxSize()
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = results,
                    key = { rankedMedia -> rankedMedia.media.id }
                ) { rankedMedia ->
                    SearchResultCard(
                        result = rankedMedia,
                        query = query,
                        onClick = { onResultClick(rankedMedia) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyResultsState(
    hasQuery: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (hasQuery) "No results found." else "Enter a query to search.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SearchResultCard(
    result: RankedMedia,
    query: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
            imageView.setImageURI(Uri.parse(uri))
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
    return DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(timestamp))
}
