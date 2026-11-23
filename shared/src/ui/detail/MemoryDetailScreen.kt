package com.zerotwodev.recall.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zerotwodev.recall.domain.Memory
import com.zerotwodev.recall.presentation.MemoryDetailUiState
import com.zerotwodev.recall.presentation.MemoryDetailViewModel
import com.zerotwodev.recall.ui.RecallIcons
import com.zerotwodev.recall.ui.components.MoodChip
import com.zerotwodev.recall.util.toDisplayDate

@Composable
fun MemoryDetailScreen(
    viewModel: MemoryDetailViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = {},
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(RecallIcons.Back, contentDescription = "Back") }
            },
            actions = {
                val loaded = state as? MemoryDetailUiState.Loaded
                if (loaded != null) {
                    IconButton(onClick = { menuOpen = true }) { Icon(RecallIcons.More, contentDescription = "More options") }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(if (loaded.memory.isHidden) "Show in recaps" else "Hide from recaps") },
                            onClick = {
                                viewModel.setHidden(!loaded.memory.isHidden)
                                menuOpen = false
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = {
                                menuOpen = false
                                confirmDelete = true
                            },
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            // The app Scaffold already pads for system bars.
            windowInsets = WindowInsets(0, 0, 0, 0),
        )

        when (val s = state) {
            MemoryDetailUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            MemoryDetailUiState.NotFound -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("This memory is no longer here.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            is MemoryDetailUiState.Loaded -> MemoryContent(s.memory)
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this memory?") },
            text = { Text("It will be removed from your timeline.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.delete(onDeleted = onBack)
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun MemoryContent(memory: Memory) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            memory.occurredAt.toDisplayDate(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(memory.title, style = MaterialTheme.typography.headlineMedium)
        memory.mood?.let { MoodChip(it) }
        if (memory.body.isNotBlank()) Text(memory.body, style = MaterialTheme.typography.bodyLarge)
        memory.transcript?.takeIf { it.isNotBlank() && it != memory.body }?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
