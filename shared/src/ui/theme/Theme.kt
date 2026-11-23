package com.zerotwodev.recall.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerotwodev.recall.domain.AppTheme
import com.zerotwodev.recall.domain.ThemeMode
import com.zerotwodev.recall.resources.Res
import com.zerotwodev.recall.resources.dm_sans
import com.zerotwodev.recall.resources.fraunces
import org.jetbrains.compose.resources.Font

/** Maps a design palette onto Material 3 roles, so every M3 component picks up the theme. */
private fun Palette.toColorScheme(dark: Boolean): ColorScheme {
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = accent,
        onPrimary = onAccent,
        primaryContainer = tint,
        onPrimaryContainer = ink,
        secondary = label,
        secondaryContainer = selected,
        onSecondaryContainer = ink,
        background = background,
        onBackground = ink,
        surface = background,
        onSurface = ink,
        surfaceVariant = chip,
        onSurfaceVariant = muted,
        surfaceContainerLowest = card,
        surfaceContainerLow = card,
        surfaceContainer = raised,
        surfaceContainerHigh = card,
        surfaceContainerHighest = chip,
        outline = line,
        outlineVariant = line,
    )
}

// Both fonts are variable. Every axis is set explicitly because Fraunces' file defaults
// to Black weight with the "wonky" letterforms on, which is not the design.
@Composable
private fun frauncesFont(weight: FontWeight): Font = Font(
    resource = Res.font.fraunces,
    weight = weight,
    variationSettings = FontVariation.Settings(
        FontVariation.Setting("wght", weight.weight.toFloat()),
        FontVariation.Setting("opsz", 24f),
        FontVariation.Setting("SOFT", 0f),
        FontVariation.Setting("WONK", 0f),
    ),
)

@Composable
private fun dmSansFont(weight: FontWeight): Font = Font(
    resource = Res.font.dm_sans,
    weight = weight,
    variationSettings = FontVariation.Settings(
        FontVariation.Setting("wght", weight.weight.toFloat()),
        FontVariation.Setting("opsz", 14f),
    ),
)

@Composable
private fun recallTypography(): Typography {
    val serif = FontFamily(frauncesFont(FontWeight.Normal), frauncesFont(FontWeight.Medium))
    val sans = FontFamily(
        dmSansFont(FontWeight.Normal),
        dmSansFont(FontWeight.Medium),
        dmSansFont(FontWeight.SemiBold),
    )
    return remember(serif, sans) {
        Typography(
            headlineMedium = TextStyle(fontFamily = serif, fontWeight = FontWeight.Medium, fontSize = 26.sp, lineHeight = 31.sp),
            titleLarge = TextStyle(fontFamily = serif, fontWeight = FontWeight.Normal, fontSize = 19.sp, lineHeight = 25.sp),
            titleMedium = TextStyle(fontFamily = serif, fontWeight = FontWeight.Medium, fontSize = 18.sp, lineHeight = 23.sp),
            titleSmall = TextStyle(fontFamily = serif, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 21.sp),
            bodyLarge = TextStyle(fontFamily = sans, fontSize = 16.sp, lineHeight = 24.sp),
            bodyMedium = TextStyle(fontFamily = sans, fontSize = 14.sp, lineHeight = 20.sp),
            bodySmall = TextStyle(fontFamily = sans, fontSize = 13.sp, lineHeight = 18.sp),
            labelLarge = TextStyle(fontFamily = sans, fontWeight = FontWeight.Medium, fontSize = 14.sp),
            labelMedium = TextStyle(fontFamily = sans, fontWeight = FontWeight.Medium, fontSize = 12.sp),
            labelSmall = TextStyle(fontFamily = sans, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 1.sp),
        )
    }
}

private val RecallShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
)

private val LocalIsDark = staticCompositionLocalOf { false }

object RecallTheme {
    val isDark: Boolean
        @Composable @ReadOnlyComposable get() = LocalIsDark.current
}

/** The screen colour for a theme and mode, for platform windows drawn behind Compose. */
fun windowBackgroundColor(theme: AppTheme, dark: Boolean): Color = Palettes.of(theme, dark).background

/** Resolves the saved mode to light or dark. */
@Composable
fun ThemeMode.isDark(): Boolean = when (this) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun RecallTheme(
    theme: AppTheme = AppTheme.WARM,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalIsDark provides darkTheme) {
        MaterialTheme(
            colorScheme = Palettes.of(theme, darkTheme).toColorScheme(darkTheme),
            typography = recallTypography(),
            shapes = RecallShapes,
            content = content,
        )
    }
}
