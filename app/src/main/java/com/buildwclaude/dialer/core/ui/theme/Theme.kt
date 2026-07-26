@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.buildwclaude.dialer.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buildwclaude.dialer.R

/**
 * Placeholder tokens shared with the Messages app (Inter, blue accent). These
 * will be replaced with the exact values from the Figma design in Milestone 2.
 */
data class Palette(
    val Accent: Color,
    val Positive: Color,
    val Negative: Color,
    val TextPrimary: Color,
    val TextSecondary: Color,
    val Muted: Color,
    val Surface: Color,
    val SurfaceSubtle: Color,
    val KeyBg: Color,
    val Divider: Color,
)

// Shared with the Messages app so Phone and Messages read as one pair:
// blue accent, navy text, light/dark following the system setting.
val LightPalette = Palette(
    Accent = Color(0xFF2F80ED),        // brand blue — tabs, highlights, actions
    Positive = Color(0xFF27AE60),      // call / answer
    Negative = Color(0xFFEB5757),      // end / decline
    TextPrimary = Color(0xFF1B1A57),
    TextSecondary = Color(0xFF4F5E7B),
    Muted = Color(0xFFA1A1BC),
    Surface = Color(0xFFFFFFFF),
    SurfaceSubtle = Color(0xFFF8F9FC),
    KeyBg = Color(0xFFF7F7F7),         // dial keys, elevated chips
    Divider = Color(0xFFEDEDED),
)

val DarkPalette = Palette(
    Accent = Color(0xFF3D8BF2),
    Positive = Color(0xFF34C77B),
    Negative = Color(0xFFFF6B6B),
    TextPrimary = Color(0xFFF2F2F8),
    TextSecondary = Color(0xFF9C9CB2),
    Muted = Color(0xFF8A8AA0),
    Surface = Color(0xFF101014),
    SurfaceSubtle = Color(0xFF17171D),
    KeyBg = Color(0xFF1E1E26),
    Divider = Color(0xFF26262E),
)

val LocalPalette = staticCompositionLocalOf { LightPalette }

val palette: Palette
    @Composable @ReadOnlyComposable get() = LocalPalette.current

private val Inter = FontFamily(
    Font(R.font.inter, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.inter, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.inter, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.inter, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
)

object DesignType {
    val screenTitle = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
    val itemTitle = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    val body = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp)
    val dialDigit = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 30.sp)
    val label = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 12.sp)
}

private val Type = Typography(
    titleLarge = DesignType.screenTitle,
    titleMedium = DesignType.itemTitle,
    bodyMedium = DesignType.body,
    labelMedium = DesignType.label,
)

private val Shapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
)

@Composable
fun PhoneTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // Follows the system light/dark setting, same as the Messages app.
    val p = if (darkTheme) DarkPalette else LightPalette
    val scheme = (if (darkTheme) darkColorScheme() else lightColorScheme()).copy(
        primary = p.Accent,
        background = p.Surface,
        surface = p.Surface,
        onBackground = p.TextPrimary,
        onSurface = p.TextPrimary,
        error = p.Negative,
    )
    CompositionLocalProvider(LocalPalette provides p) {
        MaterialTheme(colorScheme = scheme, typography = Type, shapes = Shapes, content = content)
    }
}
