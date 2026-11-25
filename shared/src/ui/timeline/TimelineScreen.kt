package com.zerotwodev.recall.ui.timeline

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zerotwodev.recall.presentation.TimelineViewModel
import com.zerotwodev.recall.ui.ImageStore
import com.zerotwodev.recall.ui.RecallIcons
import com.zerotwodev.recall.ui.components.MemoryCard

/** Boards 6a and 6b: month groups with counts, photo thumbnails, and a designed empty state. */
@Composable
fun TimelineScreen(
    viewModel: TimelineViewModel,
    imageStore: ImageStore,
    onOpenMemory: (String) -> Unit,
    onGoHome: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val noMemoriesYet = state.isEmpty && state.query.isBlank()

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 28.dp)) {
            Text("Timeline", style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp, lineHeight = 34.sp))
            SearchField(
                query = state.query,
                enabled = !noMemoriesYet,
                onQueryChange = viewModel::onQueryChange,
                modifier = Modifier.padding(top = 14.dp),
            )
        }

        when {
            noMemoriesYet -> EmptyTimeline(onGoHome)
            state.isEmpty -> Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.TopCenter) {
                Text(
                    "Nothing matches “${state.query}”.",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.sections.forEach { section ->
                    stickyHeader(key = "header-${section.header}") {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background)
                                .padding(top = 14.dp, bottom = 2.dp),
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            Text(section.header, style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp), modifier = Modifier.weight(1f))
                            val count = section.memories.size
                            Text(
                                if (count == 1) "1 memory" else "$count memories",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    items(section.memories, key = { it.id }) { memory ->
                        MemoryCard(memory, onClick = { onOpenMemory(memory.id) }, imageStore = imageStore)
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchField(query: String, enabled: Boolean, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        modifier = modifier.fillMaxWidth().height(48.dp),
        shape = CircleShape,
        color = if (enabled) MaterialTheme.colorScheme.surfaceContainerLowest else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(RecallIcons.Search, contentDescription = null, tint = muted, modifier = Modifier.size(18.dp))
            Box(Modifier.weight(1f)) {
                if (query.isEmpty()) Text("Search memories, people, places", style = MaterialTheme.typography.bodyLarge, color = muted)
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    enabled = enabled,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Search memories, people, places" },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                )
            }
        }
    }
}

@Composable
private fun EmptyTimeline(onGoHome: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 36.dp).padding(top = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            RecallIcons.Book,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(width = 148.dp, height = 104.dp),
        )
        Text(
            "Your first memory is one tap away",
            modifier = Modifier.padding(top = 28.dp),
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 24.sp, lineHeight = 30.sp),
            textAlign = TextAlign.Center,
        )
        Text(
            "Hold the mic on Home and talk about something from today. It takes less than 15 seconds.",
            modifier = Modifier.padding(top = 10.dp),
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, lineHeight = 23.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onGoHome, modifier = Modifier.padding(top = 28.dp).height(52.dp), shape = CircleShape) {
            Icon(RecallIcons.Mic, contentDescription = null, modifier = Modifier.size(20.dp))
            Text("Go to Home", modifier = Modifier.padding(start = 8.dp), fontSize = 16.sp)
        }
    }
}
