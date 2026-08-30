package com.troy.sansina

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class ButtonShape { PILL, SQUARE }
enum class Decor { QUIET_GLOW, GLASS_FIELD, SPOTLIGHT, TRAY_LIGHT, STORE_WASH, TROY_SKY }
enum class QrStyle { PLAIN_CARD, GLASS_CARD, BARE_PLATE, BOARDING_PASS, SHADOW_BOX }

/** One of the five worlds. The tier ladder is shared (see [TierPalette]). */
data class SansinaTheme(
    val id: String,
    val letter: String,
    val name: String,
    val subtitle: String,
    val bg: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val headlineWeight: FontWeight,
    val h1Size: Int,
    val helloSize: Int,
    val bigSize: Int,
    val buttonBg: Color,
    val buttonText: Color,
    val buttonShape: ButtonShape,
    val buttonHeight: Dp,
    val glassButton: Boolean,
    val productCardBg: Color,
    val productCardBorder: Color,
    val productInk: Color,
    val brushed: Boolean,
    val decor: Decor,
    val qrStyle: QrStyle,
    val qrModule: Color,
    val confetti: List<Color>,
    val brandTag: Boolean,          // "+artısı var" (true) vs "Premium Partner" (false)
    val helloAccent: Boolean,       // colour "merhaba!" on hello screen
    /** E only: on invite/result the world is a light wash; otherwise blue. */
    val washSteps: Set<Phase> = emptySet(),
    val washBg: Color = bg,
    val washTextPrimary: Color = textPrimary,
    val washTextSecondary: Color = textSecondary,
)

fun SansinaTheme.isWashed(phase: Phase) = phase in washSteps
fun SansinaTheme.primary(phase: Phase) = if (isWashed(phase)) washTextPrimary else textPrimary
fun SansinaTheme.secondary(phase: Phase) = if (isWashed(phase)) washTextSecondary else textSecondary

data class TierPalette(val bg: Color, val text: Color, val border: Color, val rule: Color, val cap: Color, val sheen: Float)

val Tier.palette: TierPalette
    get() = when (this) {
        Tier.ALUMINIUM -> TierPalette(Color(0xFFE9EBEF), Color(0xFF0E1116), Color(0xFFD2D7DF), Color(0xFF0071E3), Color(0x800E1116), 0.55f)
        Tier.BRONZE -> TierPalette(Color(0xFFA9714B), Color(0xFFFFFFFF), Color(0xFF8A5A38), Color(0xB8FFFFFF), Color(0xA8FFFFFF), 0.34f)
        Tier.SILVER -> TierPalette(Color(0xFF9AA3AE), Color(0xFF0E1116), Color(0xFF7F8996), Color(0x730E1116), Color(0x940E1116), 0.50f)
        Tier.GOLD -> TierPalette(Color(0xFFC9A227), Color(0xFF231A00), Color(0xFFA88516), Color(0x80231A00), Color(0x9E231A00), 0.46f)
        Tier.PREMIUM -> TierPalette(Color(0xFF0F2340), Color(0xFFE8C55F), Color(0xFFC9A227), Color(0xFFC9A227), Color(0x99E8C55F), 0.20f)
    }

val Gold = Color(0xFFC9A227)
val GoldLight = Color(0xFFE8C55F)

val ThemeA = SansinaTheme(
    id = "A", letter = "A", name = "Sessiz Vitrin", subtitle = "HIG deference, near-white, one blue",
    bg = Color(0xFFF5F5F7), textPrimary = Color(0xFF1D1D1F), textSecondary = Color(0xFF6E6E73), accent = Color(0xFF0071E3),
    headlineWeight = FontWeight.SemiBold, h1Size = 56, helloSize = 72, bigSize = 104,
    buttonBg = Color(0xFF0071E3), buttonText = Color.White, buttonShape = ButtonShape.PILL, buttonHeight = 58.dp, glassButton = false,
    productCardBg = Color.White, productCardBorder = Color(0xFFE6E7EB), productInk = Color(0xFF1D1D1F),
    brushed = false, decor = Decor.QUIET_GLOW, qrStyle = QrStyle.PLAIN_CARD, qrModule = Color(0xFF1D1D1F),
    confetti = listOf(Color(0xFF0071E3), Color(0xFF1D1D1F), Color(0xFFC7C7CC), Gold),
    brandTag = false, helloAccent = true,
)

val ThemeB = SansinaTheme(
    id = "B", letter = "B", name = "Cam ve Derinlik", subtitle = "Glass material and 3D depth",
    bg = Color(0xFF03102A), textPrimary = Color.White, textSecondary = Color(0xB3FFFFFF), accent = Color(0xFF7FC0FF),
    headlineWeight = FontWeight.SemiBold, h1Size = 56, helloSize = 80, bigSize = 112,
    buttonBg = Color(0x21FFFFFF), buttonText = Color.White, buttonShape = ButtonShape.PILL, buttonHeight = 58.dp, glassButton = true,
    productCardBg = Color(0x1FFFFFFF), productCardBorder = Color(0x42FFFFFF), productInk = Color(0xD1FFFFFF),
    brushed = false, decor = Decor.GLASS_FIELD, qrStyle = QrStyle.GLASS_CARD, qrModule = Color(0xFF03102A),
    confetti = listOf(Color(0xFF7FC0FF), Color.White, Color(0xFF0A84FF), Gold, Color(0xFFDCE5F1)),
    brandTag = true, helloAccent = true,
)

val ThemeC = SansinaTheme(
    id = "C", letter = "C", name = "Sahne", subtitle = "Keynote stage, single spot, volt accent",
    bg = Color.Black, textPrimary = Color.White, textSecondary = Color(0xB8FFFFFF), accent = Color(0xFFD6FF4B),
    headlineWeight = FontWeight.Bold, h1Size = 64, helloSize = 96, bigSize = 136,
    buttonBg = Color(0xFFD6FF4B), buttonText = Color(0xFF0E1116), buttonShape = ButtonShape.SQUARE, buttonHeight = 58.dp, glassButton = false,
    productCardBg = Color(0xFF16181C), productCardBorder = Color(0x29FFFFFF), productInk = Color(0xE6FFFFFF),
    brushed = false, decor = Decor.SPOTLIGHT, qrStyle = QrStyle.BARE_PLATE, qrModule = Color.Black,
    confetti = listOf(Color(0xFFD6FF4B), Color.White, Gold, Color(0xFF9AA3AE)),
    brandTag = false, helloAccent = true,
)

val ThemeD = SansinaTheme(
    id = "D", letter = "D", name = "Vitrin Tepsisi", subtitle = "Warm tray, brushed-metal cards",
    bg = Color(0xFFF2EFEA), textPrimary = Color(0xFF171614), textSecondary = Color(0xFF6B6660), accent = Color(0xFF9A7233),
    headlineWeight = FontWeight.SemiBold, h1Size = 54, helloSize = 74, bigSize = 104,
    buttonBg = Color(0xFF171614), buttonText = Color.White, buttonShape = ButtonShape.PILL, buttonHeight = 58.dp, glassButton = false,
    productCardBg = Color(0xFFFBFAF8), productCardBorder = Color(0xFFE3DED4), productInk = Color(0xFF171614),
    brushed = true, decor = Decor.TRAY_LIGHT, qrStyle = QrStyle.BOARDING_PASS, qrModule = Color(0xFF171614),
    confetti = listOf(Color(0xFFB4915F), Gold, Color(0xFF171614), Color(0xFFD8D3CB), Color(0xFF9A7233), Color(0xFFE4C79C)),
    brandTag = false, helloAccent = true,
)

val ThemeE = SansinaTheme(
    id = "E", letter = "E", name = "Mağaza Işığı", subtitle = "Signage scale, closest to the deck",
    bg = Color(0xFF0A62FF), textPrimary = Color.White, textSecondary = Color(0xE6FFFFFF), accent = Color(0xFF0A62FF),
    headlineWeight = FontWeight.Bold, h1Size = 62, helloSize = 90, bigSize = 128,
    buttonBg = Color(0xFF0A62FF), buttonText = Color.White, buttonShape = ButtonShape.PILL, buttonHeight = 66.dp, glassButton = false,
    productCardBg = Color.White, productCardBorder = Color(0x1A0B1B33), productInk = Color(0xFF0B1B33),
    brushed = false, decor = Decor.STORE_WASH, qrStyle = QrStyle.SHADOW_BOX, qrModule = Color(0xFF0B1B33),
    confetti = listOf(Color.White, Color(0xFFFFC93F), Color(0xFF0A62FF), Color(0xFF9FD2FF), Gold),
    brandTag = true, helloAccent = false,
    washSteps = setOf(Phase.INVITE, Phase.RESULT),
    washBg = Color(0xFFF7FAFF), washTextPrimary = Color(0xFF0B1B33), washTextSecondary = Color(0xFF4A5A72),
)

/**
 * F — the final campaign look, tokens extracted from the agency Figma source
 * (brand/reference/TROY_ROZYLABS.fig): TROY BLUE #4DC0DF, card/bg radial #C4F2FF→#4DC0DF,
 * black headlines, white "merhaba", amounts in TROY BLUE with a white outline.
 */
val TroyBlue = Color(0xFF4DC0DF)
val TroyBlueLight = Color(0xFFC4F2FF)
val ThemeF = SansinaTheme(
    id = "F", letter = "F", name = "Troy Mavi", subtitle = "Final kampanya tasarımı (ajans)",
    bg = TroyBlue, textPrimary = Color(0xFF000000), textSecondary = Color(0xD9000000), accent = Color.White,
    headlineWeight = FontWeight.Bold, h1Size = 73, helloSize = 84, bigSize = 162,
    buttonBg = Color.White, buttonText = TroyBlue, buttonShape = ButtonShape.PILL, buttonHeight = 58.dp, glassButton = false,
    productCardBg = Color(0xFF8CD9EE), productCardBorder = Color(0x00FFFFFF), productInk = Color.White,
    brushed = false, decor = Decor.TROY_SKY, qrStyle = QrStyle.PLAIN_CARD, qrModule = Color(0xFF000000),
    confetti = listOf(Color.White, TroyBlueLight, TroyBlue, Color(0xFF9BDCF3), Color(0xFF046ED9)),
    brandTag = true, helloAccent = true,
)

val AllThemes = listOf(ThemeF, ThemeA, ThemeB, ThemeC, ThemeD, ThemeE)
