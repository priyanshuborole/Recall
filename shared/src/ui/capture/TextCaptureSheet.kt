package com.zerotwodev.recall.ui.capture

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zerotwodev.recall.domain.Mood
import com.zerotwodev.recall.ui.components.MoodChip

/** Quick text capture. Voice and photo capture will reuse the same save path. */
@Composable
fun TextCaptureSheet(
    onDismiss: () -> Unit,
    onSave: (text: String, mood: Mood?) -> Unit,
) {
    var text by rememberSaveable { mutableStateOf("") }
    var moodName by rememberSaveable { mutableStateOf<String?>(null) }
    val mood = moodName?.let { name -> Mood.entries.firstOrNull { it.name == name } }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        // Opens straight to full height (no half-open step).
        sheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        ),
    ) {
        Column(
            Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp).imePadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("What happened?", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp),
                placeholder = { Text("A sentence is enough") },
            )
            Text("How did it feel?", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Mood.entries.forEach { option ->
                    MoodChip(
                        mood = option,
                        selected = mood == option,
                        onClick = { moodName = if (mood == option) null else option.name },
                    )
                }
            }
            Button(
                onClick = { onSave(text, mood) },
                enabled = text.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save memory") }
        }
    }
}
