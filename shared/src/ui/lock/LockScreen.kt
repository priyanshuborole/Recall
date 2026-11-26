package com.zerotwodev.recall.ui.lock

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.zerotwodev.recall.ui.RecallIcons
import kotlinx.coroutines.launch
import kotlin.time.Clock

/** Coming back after this long locks the app again. Short trips, like the photo picker, don't. */
private const val LOCK_AFTER_MS = 30_000L

@Stable
class AppLockState(locked: Boolean) {
    var locked by mutableStateOf(locked)
}

/** Locked when the app starts, and again after it has been in the background for 30 seconds. */
@Composable
fun rememberAppLockState(enabled: Boolean, clock: Clock): AppLockState {
    var lockedSaved by rememberSaveable { mutableStateOf(true) }
    var backgroundAt by rememberSaveable { mutableLongStateOf(0L) }
    val state = remember { AppLockState(lockedSaved) }
    lockedSaved = state.locked

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { backgroundAt = clock.now().toEpochMilliseconds() }
    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        val away = clock.now().toEpochMilliseconds() - backgroundAt
        if (enabled && backgroundAt > 0 && away >= LOCK_AFTER_MS) state.locked = true
        backgroundAt = 0L
    }
    // Turning the lock on from Settings shouldn't lock the app straight away.
    LaunchedEffect(enabled) { if (!enabled) state.locked = false }
    return state
}

/** Board 1. Opens the system prompt straight away; the buttons bring it back if dismissed. */
@Composable
fun LockScreen(unlock: suspend () -> Unit) {
    val scope = rememberCoroutineScope()
    val ask: () -> Unit = { scope.launch { unlock() } }
    LaunchedEffect(Unit) { unlock() }
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(start = 32.dp, end = 32.dp, top = 88.dp, bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(72.dp).clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "R",
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 40.sp, fontStyle = FontStyle.Italic),
            )
        }
        Text("Recall", modifier = Modifier.padding(top = 14.dp), style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp))

        Row(
            Modifier.padding(top = 64.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(RecallIcons.Lock, contentDescription = null, tint = muted, modifier = Modifier.size(16.dp))
            Text("Locked", style = MaterialTheme.typography.bodySmall, color = muted)
        }
        Text(
            "Your memories are locked",
            modifier = Modifier.padding(top = 10.dp),
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp, lineHeight = 34.sp),
            textAlign = TextAlign.Center,
        )
        Text(
            "Use your fingerprint or your phone’s PIN to open your journal.",
            modifier = Modifier.padding(top = 10.dp),
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, lineHeight = 22.sp),
            color = muted,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.weight(1f))
        Surface(
            onClick = ask,
            modifier = Modifier.size(96.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            contentColor = MaterialTheme.colorScheme.primary,
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(RecallIcons.Fingerprint, contentDescription = "Unlock with fingerprint", modifier = Modifier.size(44.dp))
            }
        }
        TextButton(onClick = ask, modifier = Modifier.padding(top = 12.dp)) { Text("Use PIN", fontSize = 15.sp) }
        Row(
            Modifier.padding(top = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(RecallIcons.Shield, contentDescription = null, tint = muted, modifier = Modifier.size(13.dp))
            Text("Stored only on your phone", style = MaterialTheme.typography.labelMedium, color = muted)
        }
    }
}
