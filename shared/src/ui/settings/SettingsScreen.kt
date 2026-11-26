package com.zerotwodev.recall.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zerotwodev.recall.presentation.SettingsViewModel
import com.zerotwodev.recall.util.minuteOfDayText

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onOpenAppearance: () -> Unit,
    onMessage: (String) -> Unit = {},
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var pickTime by remember { mutableStateOf(false) }
    val itemColors = ListItemDefaults.colors(containerColor = Color.Transparent)
    LaunchedEffect(viewModel) { viewModel.messages.collect { onMessage(it) } }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 16.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(horizontal = 20.dp))

        SectionTitle("Reminder")
        ListItem(
            headlineContent = { Text("Nightly question") },
            supportingContent = { Text("“What’s one thing from today worth remembering?”") },
            trailingContent = { Switch(checked = settings.reminderEnabled, onCheckedChange = viewModel::setReminderEnabled) },
            colors = itemColors,
        )
        ListItem(
            headlineContent = { Text("Time") },
            trailingContent = { Text(minuteOfDayText(settings.reminderMinuteOfDay)) },
            modifier = Modifier.clickable(enabled = settings.reminderEnabled) { pickTime = true },
            colors = itemColors,
        )

        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        SectionTitle("Privacy")
        ListItem(
            headlineContent = { Text("App lock") },
            supportingContent = { Text("Ask for your fingerprint or phone PIN when you come back") },
            trailingContent = { Switch(checked = settings.appLockEnabled, onCheckedChange = viewModel::setAppLockEnabled) },
            colors = itemColors,
        )

        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        SectionTitle("Appearance")
        ListItem(
            headlineContent = { Text("Theme") },
            supportingContent = { Text(themeSummary(settings.theme, settings.themeMode)) },
            trailingContent = { Text("›", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) },
            modifier = Modifier.clickable(onClick = onOpenAppearance),
            colors = itemColors,
        )

        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        SectionTitle("Your data")
        ListItem(headlineContent = { Text("Export memories") }, supportingContent = { Text("Coming soon") }, colors = itemColors)
        ListItem(headlineContent = { Text("Import memories") }, supportingContent = { Text("Coming soon") }, colors = itemColors)

        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        SectionTitle("About")
        ListItem(
            headlineContent = { Text("Your memories stay on this device") },
            supportingContent = { Text("Nothing is sent to our servers.") },
            colors = itemColors,
        )
    }

    if (pickTime) {
        val state = rememberTimePickerState(
            initialHour = settings.reminderMinuteOfDay / 60,
            initialMinute = settings.reminderMinuteOfDay % 60,
        )
        AlertDialog(
            onDismissRequest = { pickTime = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setReminderTime(state.hour * 60 + state.minute)
                    pickTime = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { pickTime = false }) { Text("Cancel") } },
            text = { TimePicker(state = state) },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 4.dp),
    )
}
