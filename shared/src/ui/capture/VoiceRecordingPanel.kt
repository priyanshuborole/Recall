package com.zerotwodev.recall.ui.capture

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zerotwodev.recall.presentation.MAX_RECORDING_MS
import com.zerotwodev.recall.presentation.VoiceCaptureUiState

/** Fills the Home content while the mic is held. The mic itself stays in the capture bar. */
@Composable
fun VoiceRecordingPanel(state: VoiceCaptureUiState, modifier: Modifier = Modifier) {
    val barColor = if (state.willCancel) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Text(
                    if (state.willCancel) "Release to cancel" else "Listening on your phone…",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (state.willCancel) barColor else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(formatRecordingTime(state.elapsedMs), style = MaterialTheme.typography.headlineMedium)
                Canvas(Modifier.fillMaxWidth().height(72.dp)) {
                    val slot = size.width / state.levels.size
                    val barWidth = slot * 0.55f
                    state.levels.forEachIndexed { index, level ->
                        val barHeight = (size.height * level).coerceAtLeast(barWidth)
                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(index * slot + (slot - barWidth) / 2, (size.height - barHeight) / 2),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(barWidth / 2),
                        )
                    }
                }
                if (state.transcript.isNotBlank()) {
                    Text(
                        state.transcript,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        maxLines = 6,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    "Up to ${formatRecordingTime(MAX_RECORDING_MS)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun formatRecordingTime(ms: Long): String {
    val seconds = ms / 1000
    return "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
}
