package com.androbito.tilfaz.core.designsystem.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Palette inspired by the dark navy IPTV look the user is used to: near-black blue backgrounds,
 * deep blue tiles, a cyan accent for focus and selection, and saturated gradients for the three hero tiles.
 */
object TilfazColors {
    val Background = Color(0xFF070C18)
    val Surface = Color(0xFF0F1A33)
    val SurfaceVariant = Color(0xFF162648)
    val SurfaceBright = Color(0xFF1F3566)
    val Accent = Color(0xFF27A9E1)
    val AccentSoft = Color(0xFF1C6FA0)
    val OnAccent = Color.White
    val OnBackground = Color(0xFFE9EEF8)
    val OnSurfaceMuted = Color(0xFF9AA8C7)
    val Danger = Color(0xFFE5484D)
    val Success = Color(0xFF2EC27E)
    val Badge = Color(0xFF2196F3)
    val Scrim = Color(0xCC050914)

    val LiveGradient = Brush.verticalGradient(listOf(Color(0xFF34D6A4), Color(0xFF1F6FE0)))
    val MoviesGradient = Brush.linearGradient(listOf(Color(0xFFEE3B6C), Color(0xFFF2A14A)))
    val SeriesGradient = Brush.linearGradient(listOf(Color(0xFF7D4DFF), Color(0xFF24C4E8)))
    val PillGradient = Brush.horizontalGradient(listOf(Color(0xFF26B7A6), Color(0xFF1E8FA9)))
    val FocusGradient = Brush.linearGradient(listOf(Color(0xFF2E4F8F), Color(0xFF1B3366)))
}
