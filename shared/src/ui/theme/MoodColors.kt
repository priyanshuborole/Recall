package com.zerotwodev.recall.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.zerotwodev.recall.domain.Mood

data class MoodColor(val container: Color, val content: Color)

// Mood colours are the same in every theme ("a mood always looks like itself" — Appearance screen).
// Sad isn't in the design yet and follows the same pattern.
private val lightMoods = mapOf(
    Mood.HAPPY to MoodColor(Color(0xFFF5E2B5), Color(0xFF6A4A08)),
    Mood.CALM to MoodColor(Color(0xFFD8E7DB), Color(0xFF2E5A3D)),
    Mood.PROUD to MoodColor(Color(0xFFE7DBF0), Color(0xFF573A70)),
    Mood.GRATEFUL to MoodColor(Color(0xFFE6E4C4), Color(0xFF535018)),
    Mood.SAD to MoodColor(Color(0xFFD9E2EE), Color(0xFF2F4A6B)),
    Mood.ANXIOUS to MoodColor(Color(0xFFF3D8CC), Color(0xFF7A3420)),
)

private val darkMoods = mapOf(
    Mood.HAPPY to MoodColor(Color(0xFF43361A), Color(0xFFF2D38E)),
    Mood.CALM to MoodColor(Color(0xFF23382A), Color(0xFFA9D3B5)),
    Mood.PROUD to MoodColor(Color(0xFF3A2C46), Color(0xFFD7BFEA)),
    Mood.GRATEFUL to MoodColor(Color(0xFF3A3820), Color(0xFFDDD7A0)),
    Mood.SAD to MoodColor(Color(0xFF233042), Color(0xFFAFC4E0)),
    Mood.ANXIOUS to MoodColor(Color(0xFF47281D), Color(0xFFF1B59E)),
)

@Composable
@ReadOnlyComposable
fun Mood.colors(): MoodColor {
    val table = if (RecallTheme.isDark) darkMoods else lightMoods
    return table.getValue(this)
}
