package com.zerotwodev.recall.ui.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerotwodev.recall.domain.Mood
import com.zerotwodev.recall.ui.RecallIcons
import com.zerotwodev.recall.ui.capture.PersonInitial
import com.zerotwodev.recall.ui.components.MoodChip

/** Boards 2a to 2c. Shown once; Skip and Continue both finish it. */
@Composable
fun OnboardingScreen(
    initialReminderMinute: Int,
    onFinish: (reminderMinuteOfDay: Int?) -> Unit,
) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var picking by rememberSaveable { mutableStateOf(false) }
    val time = rememberTimePickerState(
        initialHour = initialReminderMinute / 60,
        initialMinute = initialReminderMinute % 60,
        is24Hour = false,
    )

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 24.dp),
    ) {
        Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.semantics { contentDescription = "Step ${step + 1} of 3" },
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                repeat(3) { index ->
                    Box(
                        Modifier
                            .width(28.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(if (index <= step) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            if (step < 2) TextButton(onClick = { onFinish(null) }) { Text("Skip") }
        }

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            when (step) {
                0 -> WhyStep()
                1 -> ReminderStep(time.hour * 60 + time.minute, onPickTime = { picking = true })
                else -> PrivacyStep()
            }
        }

        Button(
            onClick = { if (step < 2) step++ else onFinish(time.hour * 60 + time.minute) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = CircleShape,
        ) { Text(if (step < 2) "Next" else "Continue", fontSize = 16.sp) }
    }

    if (picking) {
        // A dialog, so the keyboard doesn't open over the screen as an inline time field would.
        AlertDialog(
            onDismissRequest = { picking = false },
            confirmButton = { TextButton(onClick = { picking = false }) { Text("Done") } },
            text = { TimePicker(state = time) },
        )
    }
}

@Composable
private fun StepHeading(step: Int, title: String, body: String) {
    Text(
        "Step $step of 3",
        modifier = Modifier.padding(top = 20.dp),
        style = MaterialTheme.typography.bodySmall.copy(letterSpacing = 0.4.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(title, modifier = Modifier.padding(top = 8.dp), style = MaterialTheme.typography.headlineMedium.copy(fontSize = 30.sp, lineHeight = 36.sp))
    Text(
        body,
        modifier = Modifier.padding(top = 14.dp),
        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, lineHeight = 23.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun WhyStep() {
    StepHeading(
        1,
        "Remember the moments that matter, in your own words.",
        "Speak for a few seconds. Recall keeps the words, the people, the place and how it felt.",
    )
    // A sample memory, tilted like a card on a table.
    Surface(
        modifier = Modifier.padding(top = 32.dp).fillMaxWidth().rotate(-1.2f),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Canvas(Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(12.dp))) {
                val w = size.width
                val h = size.height
                drawRect(Color(0xFFE8C9A2))
                drawRect(Color(0xFFB98058), topLeft = Offset(0f, h * 0.65f), size = Size(w, h * 0.35f))
                drawCircle(Color(0xFFF4E4C4), radius = h * 0.15f, center = Offset(w * 0.33f, h * 0.6f))
                drawCircle(Color(0xFFF4E4C4), radius = h * 0.125f, center = Offset(w * 0.63f, h * 0.59f))
                drawRect(Color(0xFFF7EFD9), topLeft = Offset(w * 0.475f, h * 0.33f), size = Size(w * 0.025f, h * 0.22f))
                drawCircle(Color(0xFFF2B451), radius = h * 0.033f, center = Offset(w * 0.4875f, h * 0.29f))
            }
            Column(Modifier.padding(start = 4.dp, end = 4.dp, bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Amma’s birthday dinner at home", style = MaterialTheme.typography.titleLarge)
                Text("Sat, 14 Mar 2026 · Nashik", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MoodChip(Mood.HAPPY)
                    SampleChip("Amma")
                    SampleChip("Papa")
                    Spacer(Modifier.weight(1f))
                    Icon(RecallIcons.Mic, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

@Composable
private fun SampleChip(name: String) {
    Surface(shape = CircleShape, color = Color.Transparent, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
        Row(
            Modifier.height(26.dp).padding(start = 4.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            PersonInitial(name)
            Text(name, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun ReminderStep(minuteOfDay: Int, onPickTime: () -> Unit) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    StepHeading(2, "A gentle question, once a night.", "Pick a quiet time. Skip it whenever you like. Nothing is counted.")
    Surface(
        onClick = onPickTime,
        modifier = Modifier.padding(top = 28.dp).fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("REMIND ME AT", style = MaterialTheme.typography.labelSmall, color = muted)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(twelveHour(minuteOfDay), style = MaterialTheme.typography.headlineMedium.copy(fontSize = 40.sp, lineHeight = 46.sp))
                Spacer(Modifier.weight(1f))
                Text("Change", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
    Text("YOU’LL SEE", modifier = Modifier.padding(top = 24.dp), style = MaterialTheme.typography.labelSmall, color = muted)
    Surface(
        modifier = Modifier.padding(top = 10.dp).fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = 2.dp,
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "R",
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.titleMedium.copy(fontStyle = FontStyle.Italic),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Recall · ${twelveHour(minuteOfDay)}", style = MaterialTheme.typography.bodySmall, color = muted)
                Text("What’s one thing from today worth remembering?", style = MaterialTheme.typography.titleSmall)
                Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Hold to speak", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text("Not tonight", style = MaterialTheme.typography.labelLarge, color = muted)
                }
            }
        }
    }
}

@Composable
private fun PrivacyStep() {
    Box(
        Modifier.padding(top = 24.dp).size(88.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(RecallIcons.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
    }
    StepHeading(3, "Your memories stay on your phone.", "Recall has no account and no servers of its own.")
    Surface(
        modifier = Modifier.padding(top = 28.dp).fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            // Only what is true today. Add Drive backup, app lock and export here once they exist.
            PrivacyRow(RecallIcons.Lock, "Stored on this phone, not on a server")
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            PrivacyRow(RecallIcons.Home, "Your phone’s own backup can include your words")
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            PrivacyRow(RecallIcons.Camera, "Photos stay on this phone")
        }
    }
}

@Composable
private fun PrivacyRow(icon: ImageVector, text: String) {
    Row(Modifier.padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp))
    }
}

private fun twelveHour(minuteOfDay: Int): String {
    val hour = minuteOfDay / 60
    val h12 = (hour % 12).let { if (it == 0) 12 else it }
    return "$h12:${(minuteOfDay % 60).toString().padStart(2, '0')} ${if (hour < 12) "AM" else "PM"}"
}
