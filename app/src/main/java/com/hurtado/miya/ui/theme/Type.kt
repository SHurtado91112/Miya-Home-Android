package com.hurtado.miya.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.hurtado.miya.R

/**
 * Typography — the Android counterpart of `Miya/Extensions/Font.swift`, which overrides every
 * standard SwiftUI text role with Snell Roundhand (script, titles) at 48/36 and Times New Roman
 * (serif, body) at 24/16/12. Android ships neither font, so per the port plan these are
 * substituted with the closest free/redistributable equivalents:
 *   - Tinos: metrically compatible with Times New Roman -> serif/body role
 *   - Yellowtail: a free script face -> title role
 * Both are bundled as static font files rather than fetched via Downloadable Fonts, so the
 * typography renders identically offline (this app has an offline fixture mode).
 */
private val serifFontFamily = FontFamily(
    Font(R.font.tinos_regular, FontWeight.Normal),
    Font(R.font.tinos_bold, FontWeight.Bold),
)

private val scriptFontFamily = FontFamily(
    Font(R.font.yellowtail_regular, FontWeight.Normal),
)

/** Mirrors `.largeTitle` (Snell Roundhand 48). */
val MiyaLargeTitle = TextStyle(fontFamily = scriptFontFamily, fontSize = 48.sp)

/** Mirrors `.title` (Snell Roundhand 36). */
val MiyaTitle = TextStyle(fontFamily = scriptFontFamily, fontSize = 36.sp)

/** Mirrors `.headline` (Times New Roman 24, bold). */
val MiyaHeadline = TextStyle(fontFamily = serifFontFamily, fontSize = 24.sp, fontWeight = FontWeight.Bold)

/** Mirrors `.body` (Times New Roman 16). */
val MiyaBody = TextStyle(fontFamily = serifFontFamily, fontSize = 16.sp)

/** Mirrors `.caption` (Times New Roman 12). */
val MiyaCaption = TextStyle(fontFamily = serifFontFamily, fontSize = 12.sp)

val MiyaTypography = Typography(
    displayLarge = MiyaLargeTitle,
    displayMedium = MiyaTitle,
    headlineMedium = MiyaHeadline,
    bodyLarge = MiyaBody,
    bodySmall = MiyaCaption,
)
