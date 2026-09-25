package com.chinmay.edgegate.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Brand fonts: Space Grotesk (display), IBM Plex Sans (body), IBM Plex Mono (plates, metrics).
 * All three are free under the SIL Open Font License from fonts.google.com.
 *
 * To switch from system fallbacks to the brand fonts:
 *  1. Download the TTFs into app/src/main/res/font/ as
 *     space_grotesk_semibold.ttf, space_grotesk_bold.ttf,
 *     ibm_plex_sans_regular.ttf, ibm_plex_sans_medium.ttf, ibm_plex_sans_semibold.ttf,
 *     ibm_plex_mono_medium.ttf, ibm_plex_mono_semibold.ttf
 *  2. Replace the three vals below with, for example:
 *     val DisplayFont = FontFamily(Font(R.font.space_grotesk_semibold, FontWeight.SemiBold),
 *                                  Font(R.font.space_grotesk_bold, FontWeight.Bold))
 * Every screen picks the change up automatically.
 */
val DisplayFont: FontFamily = FontFamily.SansSerif
val BodyFont: FontFamily = FontFamily.Default
val MonoFont: FontFamily = FontFamily.Monospace

val EdgeTypography = Typography(
    displaySmall = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 38.sp, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-0.5).sp),
    headlineSmall = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = BodyFont, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = BodyFont, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = BodyFont, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 19.sp),
    bodySmall = TextStyle(fontFamily = BodyFont, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = BodyFont, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = BodyFont, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp),
    labelSmall = TextStyle(fontFamily = BodyFont, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.5.sp),
)

/** Plate text at three sizes, plus the mono style for metrics. */
object EdgeText {
    val plateLarge = TextStyle(fontFamily = MonoFont, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, letterSpacing = 2.sp)
    val plateMedium = TextStyle(fontFamily = MonoFont, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, letterSpacing = 1.sp)
    val plateSmall = TextStyle(fontFamily = MonoFont, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, letterSpacing = 0.5.sp)
    val metric = TextStyle(fontFamily = MonoFont, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
    val mono = TextStyle(fontFamily = MonoFont, fontWeight = FontWeight.Medium, fontSize = 12.sp)
    val kpi = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 36.sp)
    val overline = TextStyle(fontFamily = BodyFont, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, letterSpacing = 0.6.sp)
}
