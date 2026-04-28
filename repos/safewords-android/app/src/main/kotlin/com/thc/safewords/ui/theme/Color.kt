package com.thc.safewords.ui.theme

import androidx.compose.ui.graphics.Color

// Ink theme — editorial, near-mono with a single ember accent.
// Ported from the design handoff bundle (Safewords App.html).
object Ink {
    val bg       = Color(0xFF0B0B0C)
    val bgElev   = Color(0xFF151517)
    val bgInset  = Color(0xFF1C1C1F)
    val fg       = Color(0xFFF5F2EC)
    val fgMuted  = Color(0xFFF5F2EC).copy(alpha = 0.55f)
    val fgFaint  = Color(0xFFF5F2EC).copy(alpha = 0.32f)
    val rule     = Color(0xFFF5F2EC).copy(alpha = 0.08f)
    val accent   = Color(0xFFE8553A)
    val accentInk = Color(0xFF0B0B0C)
    val ok       = Color(0xFF9DBF8A)
    val warn     = Color(0xFFE8A13A)
    val tickFill = Color(0xFFE8553A).copy(alpha = 0.18f)
}

// High-visibility palette for Plain / accessibility mode.
object A11y {
    val bg        = Color(0xFF0B1220)
    val bgElev    = Color(0xFF18243C)
    val bgInset   = Color(0xFF24354F)
    val fg        = Color.White
    val fgMuted   = Color(0xFFCBD5E1)
    val fgFaint   = Color(0xFF94A3B8)
    val rule      = Color.White.copy(alpha = 0.22f)
    val accent    = Color(0xFFFFD23F)
    val accentInk = Color(0xFF0B1220)
    val ok        = Color(0xFF4ADE80)
    val danger    = Color(0xFFFF6B6B)
    val tickFill  = Color(0xFFFFD23F).copy(alpha = 0.22f)
}

// Avatar palette (matches iOS DotPalette).
val DotPalette = listOf(
    Color(0xFFE8553A),
    Color(0xFF6E94E7),
    Color(0xFF9DBF8A),
    Color(0xFFE89B5E),
    Color(0xFFB47AE8),
)

// Back-compat aliases so existing pre-design screens still compile.
val Background = Ink.bg
val Surface = Ink.bgElev
val SurfaceVariant = Ink.bgInset
val SurfaceBright = Ink.bgInset
val Teal = Ink.accent
val TealDark = Ink.accent
val TealMuted = Ink.tickFill
val Amber = Ink.accent
val AmberLight = Ink.accent
val TextPrimary = Ink.fg
val TextSecondary = Ink.fgMuted
val TextMuted = Ink.fgMuted
val TextSubtle = Ink.fgFaint
val Error = Ink.accent
val Success = Ink.ok
val AvatarColors = DotPalette
