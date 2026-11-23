package com.zerotwodev.recall.ui.home

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zerotwodev.recall.domain.Memory
import com.zerotwodev.recall.presentation.HomeViewModel
import com.zerotwodev.recall.presentation.VoiceCaptureViewModel
import com.zerotwodev.recall.ui.RecallIcons
import com.zerotwodev.recall.ui.capture.TextCaptureSheet
import com.zerotwodev.recall.ui.capture.VoiceRecordingPanel
import com.zerotwodev.recall.ui.components.CaptureBar
import com.zerotwodev.recall.ui.components.MemoryCard
import com.zerotwodev.recall.ui.components.MoodChip
import com.zerotwodev.recall.util.greetingFor
import com.zerotwodev.recall.util.toDisplayDate
import com.zerotwodev.recall.util.toLongDay
import com.zerotwodev.recall.util.yearsAgoText
import kotlin.time.Clock
import kotlin.time.Instant

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    voiceViewModel: VoiceCaptureViewModel,
    onOpenMemory: (String) -> Unit,
    onSeeAll: () -> Unit,
    onReview: (String) -> Unit,
    onMessage: (String) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val voice by voiceViewModel.state.collectAsStateWithLifecycle()
    var showWriteSheet by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(voiceViewModel) { voiceViewModel.messages.collect { onMessage(it) } }
    LaunchedEffect(viewModel) { viewModel.messages.collect { onMessage(it) } }
    LaunchedEffect(viewModel) { viewModel.savedMemories.collect { onReview(it) } }
    LaunchedEffect(voiceViewModel) { voiceViewModel.savedMemories.collect { onReview(it) } }
    // Leaving the app ends the recording and keeps what was said.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { voiceViewModel.onRelease() }

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 24.dp),
            ) {
                Greeting()
                Spacer(Modifier.height(18.dp))
                PromptCard()

                state.onThisDay?.let { memory ->
                    Spacer(Modifier.height(12.dp))
                    OnThisDayCard(memory, now = state.now, onClick = { onOpenMemory(memory.id) })
                }

                Spacer(Modifier.height(20.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Recent", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    if (state.recent.isNotEmpty()) TextButton(onClick = onSeeAll) { Text("See all") }
                }
                Spacer(Modifier.height(6.dp))
                if (!state.isLoading && state.recent.isEmpty()) {
                    Text(
                        "Your first memory is one tap away.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.recent.forEach { memory -> MemoryCard(memory, onClick = { onOpenMemory(memory.id) }) }
                }
            }
            if (voice.isRecording) VoiceRecordingPanel(voice)
        }

        CaptureBar(
            onWrite = { showWriteSheet = true },
            onMicPress = voiceViewModel::onPress,
            onMicSlide = voiceViewModel::onSlide,
            onMicRelease = voiceViewModel::onRelease,
            onPhoto = viewModel::addPhoto,
            isRecording = voice.isRecording,
            willCancel = voice.willCancel,
        )
    }

    if (showWriteSheet) {
        TextCaptureSheet(
            onDismiss = { showWriteSheet = false },
            onSave = { text, mood ->
                viewModel.saveTextMemory(text, mood)
                showWriteSheet = false
            },
        )
    }
}

@Composable
private fun Greeting() {
    val now = remember { Clock.System.now() }
    Row(verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                now.toLongDay(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(greetingFor(now), style = MaterialTheme.typography.headlineMedium)
        }
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceVariant) {
            Row(
                Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(RecallIcons.Lock, contentDescription = null, modifier = Modifier.size(12.dp))
                Text("On device", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun PromptCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("TODAY’S QUESTION", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            Text("What’s one thing from today worth remembering?", style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun OnThisDayCard(memory: Memory, now: Instant?, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            val ago = now?.let { memory.occurredAt.yearsAgoText(it).uppercase() } ?: ""
            Text("ON THIS DAY · $ago", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(memory.title, style = MaterialTheme.typography.titleSmall)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                memory.mood?.let { MoodChip(it) }
                Text(
                    memory.occurredAt.toDisplayDate(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
