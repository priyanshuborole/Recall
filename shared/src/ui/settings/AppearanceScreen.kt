package com.zerotwodev.recall.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zerotwodev.recall.domain.AppTheme
import com.zerotwodev.recall.domain.Mood
import com.zerotwodev.recall.domain.ThemeMode
import com.zerotwodev.recall.presentation.SettingsViewModel
import com.zerotwodev.recall.ui.RecallIcons
import com.zerotwodev.recall.ui.components.MoodChip
import com.zerotwodev.recall.ui.theme.Palette
import com.zerotwodev.recall.ui.theme.Palettes
import com.zerotwodev.recall.ui.theme.RecallTheme

/** Settings › Theme. Changes apply immediately; there is nothing to save. */
@Composable
fun AppearanceScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp),
    ) {
        // Pulled left so the arrow lines up with the content edge, as in the design.
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.offset(x = (-12).dp)) {
            IconButton(onClick = onBack) {
                Icon(RecallIcons.Back, contentDescription = "Back to settings")
            }
            Text("Appearance", style = MaterialTheme.typography.headlineMedium)
        }

        Label("THEME", top = 20.dp)
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.selectableGroup(),
        ) {
            AppTheme.entries.forEach { theme ->
                ThemeCard(
                    theme = theme,
                    selected = theme == settings.theme,
                    onClick = { viewModel.setTheme(theme) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Label("MODE", top = 26.dp)
        ModeSelector(selected = settings.themeMode, onSelect = viewModel::setThemeMode)
        Text(
            "System follows your device’s dark mode setting.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, top = 10.dp),
        )

        Surface(
            modifier = Modifier.fillMaxWidth().padding(top = 26.dp),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Mood colours stay the same in every theme, so a mood always looks like itself.",
                    style = MaterialTheme.typography.bodySmall,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(Mood.HAPPY, Mood.CALM, Mood.PROUD, Mood.ANXIOUS).forEach { MoodChip(it) }
                }
            }
        }

        Spacer(Modifier.height(32.dp))
        Text(
            "Changes apply right away — there’s nothing to save.",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun Label(text: String, top: Dp) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp, letterSpacing = 0.8.sp),
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(start = 4.dp, top = top, bottom = 10.dp),
    )
}

private val AppTheme.title: String
    get() = when (this) {
        AppTheme.WARM -> "Warm paper"
        AppTheme.FRESH -> "Fresh sage"
    }

private val AppTheme.description: String
    get() = when (this) {
        AppTheme.WARM -> "Cream & terracotta"
        AppTheme.FRESH -> "Mist & teal"
    }

/** "Warm paper · follows system", used on the Settings row. */
fun themeSummary(theme: AppTheme, mode: ThemeMode): String {
    val modeText = when (mode) {
        ThemeMode.SYSTEM -> "follows system"
        ThemeMode.LIGHT -> "always light"
        ThemeMode.DARK -> "always dark"
    }
    return "${theme.title} · $modeText"
}

/** A card showing the theme's light and dark versions side by side. */
@Composable
private fun ThemeCard(
    theme: AppTheme,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.primary
    Surface(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = if (selected) BorderStroke(2.dp, accent) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(Modifier.padding(6.dp)) {
            Row(Modifier.fillMaxWidth().height(112.dp).clip(RoundedCornerShape(10.dp))) {
                PreviewHalf(Palettes.of(theme, dark = false), Modifier.weight(1f))
                PreviewHalf(Palettes.of(theme, dark = true), Modifier.weight(1f))
            }
            Row(
                Modifier.padding(start = 4.dp, end = 4.dp, top = 10.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(theme.title, style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.5.sp))
                    Text(
                        theme.description,
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Normal),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                RadioDot(selected)
            }
        }
    }
}

/** A tiny sketch of the Home screen in [palette]: two cards and the mic button. */
@Composable
private fun PreviewHalf(palette: Palette, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxHeight().background(palette.background).padding(horizontal = 8.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(2) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(palette.card).padding(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(Modifier.fillMaxWidth(0.8f).height(5.dp).clip(RoundedCornerShape(3.dp)).background(palette.sketch))
                Box(Modifier.fillMaxWidth(0.5f).height(4.dp).alpha(0.6f).clip(RoundedCornerShape(2.dp)).background(palette.sketch))
            }
        }
        Spacer(Modifier.weight(1f))
        Box(Modifier.size(22.dp).align(Alignment.CenterHorizontally).clip(CircleShape).background(palette.accent))
    }
}

@Composable
private fun RadioDot(selected: Boolean) {
    val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        Modifier.size(20.dp).border(2.dp, color, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) Box(Modifier.size(10.dp).clip(CircleShape).background(color))
    }
}

/** System / Light / Dark pill selector. */
@Composable
private fun ModeSelector(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    val colors = MaterialTheme.colorScheme
    // Selected pill: the card colour in light mode, the "selected" colour in dark mode (as in the design).
    val selectedBg = if (RecallTheme.isDark) colors.secondaryContainer else colors.surfaceContainerLowest
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(colors.surfaceVariant)
            .padding(4.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ThemeMode.entries.forEach { mode ->
            val isSelected = mode == selected
            val pill = RoundedCornerShape(22.dp)
            Box(
                Modifier
                    .weight(1f)
                    .height(44.dp)
                    .then(if (isSelected) Modifier.shadow(1.dp, pill) else Modifier)
                    .clip(pill)
                    .background(if (isSelected) selectedBg else Color.Transparent)
                    .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(mode) }),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    when (mode) {
                        ThemeMode.SYSTEM -> "System"
                        ThemeMode.LIGHT -> "Light"
                        ThemeMode.DARK -> "Dark"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = colors.onSurface,
                )
            }
        }
    }
}
