package com.zerotwodev.recall.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zerotwodev.recall.domain.Memory
import com.zerotwodev.recall.domain.MemoryType
import com.zerotwodev.recall.domain.Mood
import com.zerotwodev.recall.ui.RecallIcons
import com.zerotwodev.recall.ui.theme.colors
import com.zerotwodev.recall.util.toDisplayDate

/** Small pill with a dot and a word. Selectable when [onClick] is set (capture sheet). */
@Composable
fun MoodChip(
    mood: Mood,
    modifier: Modifier = Modifier,
    selected: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val colors = mood.colors()
    val container = if (selected) colors.container else MaterialTheme.colorScheme.surfaceContainerLowest
    val border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    val content: @Composable () -> Unit = {
        Row(
            modifier = Modifier.height(if (onClick != null) 36.dp else 24.dp).padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(Modifier.size(6.dp).background(colors.content, CircleShape))
            Text(mood.label, style = MaterialTheme.typography.labelMedium, color = colors.content)
        }
    }
    if (onClick != null) {
        Surface(onClick = onClick, modifier = modifier, shape = CircleShape, color = container, border = border) { content() }
    } else {
        Surface(modifier = modifier, shape = CircleShape, color = container) { content() }
    }
}

@Composable
fun MemoryCard(
    memory: Memory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(memory.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    memory.occurredAt.toDisplayDate(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (memory.type == MemoryType.VOICE) {
                Icon(RecallIcons.Mic, contentDescription = "Has a voice note", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            memory.mood?.let { MoodChip(it) }
        }
    }
}

/** The capture row from the Home design: write · big mic · photo. */
@Composable
fun CaptureBar(
    onWrite: () -> Unit,
    onMicPress: () -> Unit,
    onMicSlide: (willCancel: Boolean) -> Unit,
    onMicRelease: () -> Unit,
    onPhoto: () -> Unit,
    isRecording: Boolean,
    willCancel: Boolean,
    modifier: Modifier = Modifier,
) {
    val press by rememberUpdatedState(onMicPress)
    val slide by rememberUpdatedState(onMicSlide)
    val release by rememberUpdatedState(onMicRelease)
    var dragX by remember { mutableFloatStateOf(0f) }
    val cancelDistance = with(LocalDensity.current) { 96.dp.toPx() }
    val sideAlpha = if (isRecording) 0f else 1f

    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SmallCaptureButton(RecallIcons.Pen, "Write a memory", onWrite, !isRecording, Modifier.alpha(sideAlpha))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // The gesture sits on a box that doesn't move, so sliding the mic can't shift its own coordinates.
            Box(
                Modifier
                    .size(80.dp)
                    .semantics {
                        role = Role.Button
                        contentDescription = "Hold to record a memory"
                    }
                    .pointerInput(cancelDistance) {
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            down.consume()
                            dragX = 0f
                            press()
                            try {
                                while (true) {
                                    val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                                    if (!change.pressed) {
                                        change.consume()
                                        break
                                    }
                                    val dx = (change.position.x - down.position.x).coerceAtMost(0f)
                                    dragX = dx
                                    slide(dx <= -cancelDistance)
                                    change.consume()
                                }
                            } finally {
                                dragX = 0f
                                release()
                            }
                        }
                    },
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize().graphicsLayer {
                        translationX = if (isRecording) dragX else 0f
                        val scale = if (isRecording) 1.08f else 1f
                        scaleX = scale
                        scaleY = scale
                    },
                    shape = CircleShape,
                    color = if (willCancel) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    contentColor = if (willCancel) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary,
                    shadowElevation = 6.dp,
                ) {
                    Icon(RecallIcons.Mic, contentDescription = null, modifier = Modifier.padding(23.dp))
                }
            }
            Text(
                when {
                    willCancel -> "Release to cancel"
                    isRecording -> "Slide left to cancel"
                    else -> "Hold to speak"
                },
                modifier = Modifier.padding(top = 6.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SmallCaptureButton(RecallIcons.Camera, "Add a photo memory", onPhoto, !isRecording, Modifier.alpha(sideAlpha))
    }
}

@Composable
private fun SmallCaptureButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.size(52.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Icon(icon, contentDescription = description, modifier = Modifier.padding(15.dp))
    }
}
