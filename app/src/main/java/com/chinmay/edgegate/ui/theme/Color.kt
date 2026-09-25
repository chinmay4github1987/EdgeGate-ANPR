package com.chinmay.edgegate.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * EdgeGate colour tokens (see the "Design system" artboard).
 * Cool neutral ground, one teal accent for actions, and three status colours
 * reserved for gate decisions. Status colours differ in lightness as well as
 * hue and are always paired with an icon + word (colour-blind safe).
 */
object Palette {
    val Ink = Color(0xFF0F172A)
    val Slate = Color(0xFF4B5565)
    val SlateSoft = Color(0xFF334155)
    val Line = Color(0xFFDCE1E8)
    val LineSoft = Color(0xFFEEF1F5)
    val Ground = Color(0xFFF3F5F8)
    val Surface = Color(0xFFFFFFFF)
    val Night = Color(0xFF0B1220)
    val NightSurface = Color(0xFF111A2B)
    val NightLine = Color(0xFF334155)

    val Teal = Color(0xFF0F766E)
    val TealDark = Color(0xFF0B5F58)
    val TealTint = Color(0xFFCCEBE7)
    val TealWash = Color(0xFFE6F4F2)

    val Allow = Color(0xFF15803D)
    val AllowInk = Color(0xFF14532D)
    val AllowWash = Color(0xFFDCF3E4)

    val Visitor = Color(0xFF1D4ED8)
    val VisitorInk = Color(0xFF1E3A8A)
    val VisitorWash = Color(0xFFDCE7FB)

    val Deny = Color(0xFFB91C1C)
    val DenyInk = Color(0xFF991B1B)
    val DenyWash = Color(0xFFFDE8E8)
    val DenyLine = Color(0xFFF5B5B5)

    val Staff = Color(0xFFB45309)
    val StaffInk = Color(0xFF78350F)
    val StaffWash = Color(0xFFFEF3C7)

    val PlateStrip = Color(0xFF1D4ED8)
}

/** Semantic colours not covered by Material's ColorScheme. */
@Immutable
data class EdgeColors(
    val ground: Color,
    val card: Color,
    val cardLine: Color,
    val divider: Color,
    val textMuted: Color,
    val allow: Color, val allowInk: Color, val allowWash: Color,
    val visitor: Color, val visitorInk: Color, val visitorWash: Color,
    val deny: Color, val denyInk: Color, val denyWash: Color, val denyLine: Color,
    val staffInk: Color, val staffWash: Color,
)

val LightEdgeColors = EdgeColors(
    ground = Palette.Ground, card = Palette.Surface, cardLine = Palette.Line, divider = Palette.LineSoft,
    textMuted = Palette.Slate,
    allow = Palette.Allow, allowInk = Palette.AllowInk, allowWash = Palette.AllowWash,
    visitor = Palette.Visitor, visitorInk = Palette.VisitorInk, visitorWash = Palette.VisitorWash,
    deny = Palette.Deny, denyInk = Palette.DenyInk, denyWash = Palette.DenyWash, denyLine = Palette.DenyLine,
    staffInk = Palette.StaffInk, staffWash = Palette.StaffWash,
)

val DarkEdgeColors = EdgeColors(
    ground = Palette.Night, card = Palette.NightSurface, cardLine = Palette.NightLine, divider = Color(0xFF1E293B),
    textMuted = Color(0xFFA3AEC0),
    allow = Color(0xFF22C55E), allowInk = Color(0xFF86EFAC), allowWash = Color(0xFF12301F),
    visitor = Color(0xFF60A5FA), visitorInk = Color(0xFFBFDBFE), visitorWash = Color(0xFF172A4D),
    deny = Color(0xFFEF4444), denyInk = Color(0xFFFCA5A5), denyWash = Color(0xFF3B1414), denyLine = Color(0xFF7F1D1D),
    staffInk = Color(0xFFFCD34D), staffWash = Color(0xFF3A2A0C),
)

val LocalEdgeColors = staticCompositionLocalOf { LightEdgeColors }
