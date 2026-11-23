package com.zerotwodev.recall.ui.theme

import androidx.compose.ui.graphics.Color
import com.zerotwodev.recall.domain.AppTheme

/**
 * One theme's colours in one mode. Values come from the design canvas
 * ("Warm paper" and "Fresh sage" boards, light and dark).
 */
data class Palette(
    val background: Color,   // screen
    val card: Color,         // cards, sheets
    val raised: Color,       // bottom bar
    val tint: Color,         // prompt card
    val chip: Color,         // "On device" pill, segmented control track
    val selected: Color,     // selected nav item, selected segment (dark)
    val line: Color,         // card borders
    val ink: Color,          // main text
    val muted: Color,        // secondary text
    val label: Color,        // section labels like "TODAY'S QUESTION"
    val accent: Color,       // mic button, switches, links
    val onAccent: Color,     // icon/text on the accent
    val sketch: Color,       // text lines in the Appearance preview cards
)

internal object Palettes {
    val warmLight = Palette(
        background = Color(0xFFF6EFE4), card = Color(0xFFFFFAF3), raised = Color(0xFFFBF6EE),
        tint = Color(0xFFF1E2CF), chip = Color(0xFFEFE4D5), selected = Color(0xFFEBDCC8),
        line = Color(0xFFEADFCF), ink = Color(0xFF2B211A), muted = Color(0xFF6F6257),
        label = Color(0xFF7A4A2E), accent = Color(0xFFA94A2C), onAccent = Color(0xFFFFF8F0),
        sketch = Color(0xFFCDBEAA),
    )

    val warmDark = Palette(
        background = Color(0xFF1E1814), card = Color(0xFF2A221C), raised = Color(0xFF241D18),
        tint = Color(0xFF3A2A1F), chip = Color(0xFF2F2620), selected = Color(0xFF4A3A2E),
        line = Color(0xFF3A2F27), ink = Color(0xFFF2E8DB), muted = Color(0xFFB8AA99),
        label = Color(0xFFE8A580), accent = Color(0xFFE0855F), onAccent = Color(0xFF1E1814),
        sketch = Color(0xFF5C4A3C),
    )

    val freshLight = Palette(
        background = Color(0xFFF1F6F3), card = Color(0xFFFBFDFC), raised = Color(0xFFF7FBF9),
        tint = Color(0xFFDFEEE8), chip = Color(0xFFE3EEE9), selected = Color(0xFFD1E6DD),
        line = Color(0xFFDCE8E2), ink = Color(0xFF1E2D2A), muted = Color(0xFF566A64),
        label = Color(0xFF2C6255), accent = Color(0xFF2F7A6B), onAccent = Color(0xFFF6FCF9),
        sketch = Color(0xFFBACEC6),
    )

    val freshDark = Palette(
        background = Color(0xFF111A18), card = Color(0xFF1A2624), raised = Color(0xFF152120),
        tint = Color(0xFF1C3430), chip = Color(0xFF1E2B28), selected = Color(0xFF2B4943),
        line = Color(0xFF293835), ink = Color(0xFFE5F0EC), muted = Color(0xFF9DB3AC),
        label = Color(0xFF8ED4C1), accent = Color(0xFF6CC3AB), onAccent = Color(0xFF111A18),
        sketch = Color(0xFF34524B),
    )

    fun of(theme: AppTheme, dark: Boolean): Palette = when (theme) {
        AppTheme.WARM -> if (dark) warmDark else warmLight
        AppTheme.FRESH -> if (dark) freshDark else freshLight
    }
}
