package com.troy.sansina

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ───────────────────────── World / background ─────────────────────────

@Composable
fun WorldBackground(theme: SansinaTheme, phase: Phase, modifier: Modifier = Modifier) {
    val washed = theme.isWashed(phase)
    val bg by animateColorAsState(if (washed) theme.washBg else theme.bg, tween(600), label = "bg")
    val drift = rememberInfiniteTransition(label = "drift").animateFloat(0f, 1f, infiniteRepeatable(tween(22000, easing = LinearEasing), RepeatMode.Reverse), label = "d")
    Canvas(modifier.fillMaxSize()) {
        val w = size.width; val h = size.height
        if (theme.decor == Decor.STORE_WASH && !washed) {
            drawRect(Brush.linearGradient(listOf(Color(0xFF0A62FF), Color(0xFF2E93FF), Color(0xFF57B0FF)), Offset(0f, 0f), Offset(w, h)))
        } else drawRect(bg)
        when (theme.decor) {
            Decor.QUIET_GLOW -> drawOval(
                Brush.radialGradient(listOf(Color.White, Color.White.copy(0f)), Offset(w / 2, h * 0.46f), w * 0.5f),
                Offset(0f, h * 0.46f - h * 0.5f), Size(w, h)
            )
            Decor.GLASS_FIELD -> {
                val d = drift.value
                drawCircle(Brush.radialGradient(listOf(Color(0x8C0A84FF), Color(0x000A84FF)), Offset(w * (0.26f - 0.02f * d), h * (0.22f + 0.015f * d)), w * 0.36f * (1 + 0.08f * d)), w * 0.36f * (1 + 0.08f * d), Offset(w * (0.26f - 0.02f * d), h * (0.22f + 0.015f * d)))
                drawCircle(Brush.radialGradient(listOf(Color(0x575AC8FA), Color(0x005AC8FA)), Offset(w * 0.78f, h * 0.74f), w * 0.32f), w * 0.32f, Offset(w * 0.78f, h * 0.74f))
                drawRect(Brush.radialGradient(listOf(Color.Transparent, Color.Transparent, Color(0xFF03102A)), Offset(w / 2, h / 2), w * 0.75f))
            }
            Decor.SPOTLIGHT -> drawOval(
                Brush.radialGradient(listOf(Color(0x21FFFFFF), Color(0x00FFFFFF)), Offset(w / 2, h * 0.44f), w * 0.55f),
                Offset(w / 2 - w * 0.55f, h * 0.44f - h * 0.55f), Size(w * 1.1f, h * 1.1f)
            )
            Decor.TRAY_LIGHT -> {
                drawOval(Brush.radialGradient(listOf(Color(0xF2FFFFFF), Color(0x00FFFFFF)), Offset(w / 2, -h * 0.25f), w * 0.45f), Offset(w * 0.05f, -h * 0.7f), Size(w * 0.9f, h * 0.9f))
                drawRect(Brush.verticalGradient(listOf(Color(0x00171614), Color(0x14171614)), h * 0.75f, h), Offset(0f, h * 0.75f), Size(w, h * 0.25f))
            }
            Decor.STORE_WASH -> Unit
        }
    }
}

// ───────────────────────── Text helpers ─────────────────────────

@Composable
fun Headline(theme: SansinaTheme, phase: Phase, before: String, accent: String, after: String, size: Int, modifier: Modifier = Modifier, accentOn: Boolean = true) {
    val primary = theme.primary(phase)
    Text(
        buildAnnotatedString {
            append(before)
            withStyle(SpanStyle(color = if (accentOn) theme.accent else primary)) { append(accent) }
            append(after)
        },
        color = primary, fontSize = size.sp, lineHeight = (size * 1.05f).sp, fontWeight = theme.headlineWeight,
        letterSpacing = (-size * 0.032f).sp, textAlign = TextAlign.Center, modifier = modifier
    )
}

@Composable
fun Cta(theme: SansinaTheme, text: String, onClick: () -> Unit) {
    val shape = if (theme.buttonShape == ButtonShape.PILL) RoundedCornerShape(999.dp) else RoundedCornerShape(8.dp)
    val breathe = rememberInfiniteTransition(label = "b").animateFloat(1f, 1.024f, infiniteRepeatable(tween(1800, easing = EaseInOut), RepeatMode.Reverse), label = "bs")
    Box(
        Modifier
            .graphicsLayer { if (!theme.glassButton) { scaleX = breathe.value; scaleY = breathe.value } }
            .then(if (theme.glassButton) Modifier.border(1.dp, Color(0x3DFFFFFF), shape) else Modifier.shadow(12.dp, shape, ambientColor = Color(0x61000000), spotColor = Color(0x61000000)))
            .background(theme.buttonBg, shape)
            .clip(shape)
            .clickable(onClick = onClick)
            .height(theme.buttonHeight)
            .padding(horizontal = if (theme.buttonHeight > 60.dp) 46.dp else 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = theme.buttonText, fontSize = if (theme.buttonHeight > 60.dp) 21.sp else 19.sp, fontWeight = if (theme.headlineWeight == FontWeight.Bold) FontWeight.Bold else FontWeight.SemiBold)
    }
}

// ───────────────────────── Screens ─────────────────────────

@Composable
fun InviteText(theme: SansinaTheme, visible: Boolean, onStart: () -> Unit) {
    val alpha by animateFloatAsState(if (visible) 1f else 0f, tween(1100), label = "inv")
    val scale by animateFloatAsState(if (visible) 1f else 0.94f, tween(1100), label = "invs")
    if (alpha == 0f) return
    Column(
        Modifier.fillMaxSize().padding(horizontal = 220.dp).graphicsLayer { this.alpha = alpha; scaleX = scale; scaleY = scale },
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
    ) {
        Headline(theme, Phase.INVITE, "Bugünkü şansına ", "merhaba", " demek ister misin?", (theme.h1Size * 0.78f).toInt(), Modifier.widthIn(max = 640.dp))
        Spacer(Modifier.height(14.dp))
        Caption("Bir dokunuşla Troy'dan ne kazanacağını öğren.", theme.secondary(Phase.INVITE), 19)
        Spacer(Modifier.height(26.dp))
        Cta(theme, "Şansını Dene", onStart)
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Canvas(Modifier.size(18.dp)) {
                val c = theme.secondary(Phase.INVITE)
                drawRoundRect(c, Offset(size.width * 0.4f, 0f), Size(size.width * 0.2f, size.height * 0.6f), androidx.compose.ui.geometry.CornerRadius(4f), style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
                drawCircle(c, size.width * 0.32f, Offset(size.width * 0.5f, size.height * 0.5f), style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
            }
            Spacer(Modifier.width(9.dp))
            Text("Ekrana dokun ve oyuna başla!", color = theme.secondary(Phase.INVITE), fontSize = 15.sp)
        }
    }
}

/** Invite + the whole card choreography, on one surface so the cards never remount. */
@Composable
fun StageScreen(state: GameState, theme: SansinaTheme, onStart: () -> Unit) {
    val phase = state.phase
    Box(Modifier.fillMaxSize()) {
        Stage(state, theme, Modifier.fillMaxSize())
        InviteText(theme, visible = phase == Phase.INVITE, onStart = onStart)
        val helloAlpha by animateFloatAsState(if (phase == Phase.FADE) 1f else 0f, tween(if (phase == Phase.FADE) 900 else 400), label = "hello")
        if (helloAlpha > 0f) Box(Modifier.fillMaxSize().graphicsLayer { alpha = helloAlpha }, contentAlignment = Alignment.Center) {
            Headline(theme, Phase.FADE, "Şansına ", "merhaba!", "", theme.helloSize.coerceAtMost(72), accentOn = theme.helloAccent)
        }
        Column(Modifier.align(Alignment.BottomCenter).padding(bottom = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            AnimatedContent(targetState = phase, label = "cap", transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) }) { p ->
                when (p) {
                    Phase.FILL -> Caption("Avantajını seçmeye hazır ol…", theme.secondary(p), 21)
                    Phase.FLIP -> Caption("${state.config.promos.minOf { it.amount }.let { Promo(it, 0).label }}'den ${state.config.promos.maxOf { it.amount }.let { Promo(it, 0).label }}'ye kadar", theme.secondary(p), 21)
                    Phase.SHUFFLE -> Caption("Avantajın seçiliyor…", theme.secondary(p), 21)
                    Phase.PICK -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Caption("Tebrikler!", theme.primary(p), if (theme.id == "E") 36 else 30, FontWeight.SemiBold)
                        Caption("Avantajın hazır.", theme.secondary(p), 21)
                    }
                    else -> Spacer(Modifier.height(1.dp))
                }
            }
        }
        Confetti(active = phase == Phase.PICK && state.revealed, colors = theme.confetti, modifier = Modifier.fillMaxSize())
    }
}

@Composable
fun PrizeScreen(state: GameState, theme: SansinaTheme) {
    val p = Phase.PRIZE
    val prize = state.winningCard.promo
    val big = if (theme.id == "E") theme.accent else theme.primary(p)
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        if (theme.id == "C") {
            Box(Modifier.background(theme.accent, RoundedCornerShape(8.dp)).padding(horizontal = 14.dp, vertical = 8.dp)) {
                Text("TEBRİKLER!", color = Color(0xFF0E1116), fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
            }
        } else Caption("Tebrikler!", theme.primary(p), if (theme.id == "E") 40 else 26, if (theme.id == "E") FontWeight.Bold else FontWeight.Normal)
        Spacer(Modifier.height(10.dp))
        val s = remember { Animatable(0.7f) }
        LaunchedEffect(Unit) { s.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)) }
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.graphicsLayer { scaleX = s.value; scaleY = s.value }) {
            Text(prize.label.removeSuffix(" TL"), color = big, fontSize = theme.bigSize.sp, lineHeight = (theme.bigSize * 0.96f).sp, fontWeight = theme.headlineWeight, letterSpacing = (-theme.bigSize * 0.04f).sp)
            Spacer(Modifier.width(10.dp))
            Text("TL", color = big, fontSize = (theme.bigSize * 0.36f).sp, fontWeight = theme.headlineWeight, modifier = Modifier.padding(bottom = (theme.bigSize * 0.14f).dp))
        }
        if (theme.id == "A") Box(Modifier.padding(top = 8.dp).size(190.dp, 3.dp).background(theme.accent, RoundedCornerShape(2.dp)))
        Spacer(Modifier.height(10.dp))
        Headline(theme, p, "avantajına ", "merhaba!", "", if (theme.id == "C" || theme.id == "E") 44 else 36, accentOn = theme.id != "E")
    }
}

@Composable
fun QrScreen(state: GameState, theme: SansinaTheme, onRestart: () -> Unit) {
    val p = Phase.QR
    val prize = state.winningCard.promo
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.align(Alignment.Center)) {
            when (theme.qrStyle) {
                QrStyle.BOARDING_PASS -> Column(Modifier.width(440.dp).shadow(30.dp, RoundedCornerShape(26.dp), ambientColor = Color(0x80171614), spotColor = Color(0x80171614)).clip(RoundedCornerShape(26.dp))) {
                    Row(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFFE4C79C), Color(0xFFB4915F)))).padding(horizontal = 26.dp, vertical = 20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Wordmark(Color(0xFF3B2E17), size = 26, tag = false)
                        Text(prize.label, color = Color(0xFF3B2E17), fontSize = 26.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp)
                    }
                    Column(Modifier.fillMaxWidth().background(Color.White).padding(start = 26.dp, end = 26.dp, top = 28.dp, bottom = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("QR'ı okut, kodunu al ve\nTroy mağazasında kullan.", color = Color(0xFF171614), fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(16.dp))
                        QrGrid(theme.qrModule, Color.White, Modifier.size(176.dp))
                    }
                }
                else -> {
                    val cardMod = when (theme.qrStyle) {
                        QrStyle.PLAIN_CARD -> Modifier.shadow(26.dp, RoundedCornerShape(24.dp), ambientColor = Color(0x4D1D1D1F), spotColor = Color(0x4D1D1D1F)).background(Color.White, RoundedCornerShape(24.dp)).padding(horizontal = 52.dp, vertical = 44.dp)
                        QrStyle.GLASS_CARD -> Modifier.background(Color(0x21FFFFFF), RoundedCornerShape(32.dp)).border(1.dp, Color(0x3DFFFFFF), RoundedCornerShape(32.dp)).padding(horizontal = 48.dp, vertical = 42.dp)
                        else -> Modifier.padding(horizontal = 40.dp)
                    }
                    Row(cardMod, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(56.dp)) {
                        Column(Modifier.widthIn(max = 380.dp)) {
                            PrizeChip(theme, prize)
                            Spacer(Modifier.height(14.dp))
                            Text("QR'ı okut,", color = theme.primary(p), fontSize = if (theme.id == "E") 46.sp else 40.sp, lineHeight = 48.sp, fontWeight = theme.headlineWeight, letterSpacing = (-1).sp)
                            Text("kodunu al ve Troy mağazasında kullan.", color = theme.secondary(p), fontSize = if (theme.id == "E") 46.sp else 40.sp, lineHeight = 48.sp, fontWeight = FontWeight.Medium, letterSpacing = (-1).sp)
                        }
                        val plate = when (theme.qrStyle) {
                            QrStyle.SHADOW_BOX -> Modifier.shadow(28.dp, RoundedCornerShape(26.dp), ambientColor = Color(0x57060A12), spotColor = Color(0x57060A12)).background(Color.White, RoundedCornerShape(26.dp)).padding(26.dp)
                            QrStyle.BARE_PLATE -> Modifier.background(Color.White, RoundedCornerShape(18.dp)).padding(16.dp)
                            QrStyle.GLASS_CARD -> Modifier.background(Color.White, RoundedCornerShape(16.dp)).padding(14.dp)
                            else -> Modifier.padding(8.dp)
                        }
                        QrGrid(theme.qrModule, Color.White, plate.size(if (theme.id == "E") 208.dp else 196.dp))
                    }
                }
            }
        }
        Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 40.dp)) {
            val shape = RoundedCornerShape(26.dp)
            Box(
                Modifier.then(if (theme.glassButton) Modifier.border(1.dp, Color(0x3DFFFFFF), shape) else Modifier)
                    .background(theme.buttonBg, shape).clip(shape).clickable(onClick = onRestart).height(52.dp).padding(horizontal = 30.dp),
                contentAlignment = Alignment.Center
            ) { Text("Yeniden oyna", color = theme.buttonText, fontSize = 17.sp, fontWeight = FontWeight.SemiBold) }
        }
    }
}

/** Small reminder of the won discount on the QR page. */
@Composable
fun PrizeChip(theme: SansinaTheme, prize: Promo) {
    val p = Phase.QR
    val bg = when (theme.id) { "B" -> Color(0x26FFFFFF); "C" -> theme.accent; else -> theme.primary(p).copy(alpha = 0.08f) }
    val fg = when (theme.id) { "C" -> Color(0xFF0E1116); else -> theme.primary(p) }
    Row(
        Modifier.background(bg, RoundedCornerShape(999.dp)).then(if (theme.id == "B") Modifier.border(1.dp, Color(0x3DFFFFFF), RoundedCornerShape(999.dp)) else Modifier)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(10.dp).background(if (theme.id == "C") fg else theme.accent, androidx.compose.foundation.shape.CircleShape))
        Spacer(Modifier.width(10.dp))
        Text("Kazandığın avantaj: ", color = fg.copy(alpha = 0.75f), fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Text(prize.label, color = fg, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

// ───────────────────────── Brand + settings ─────────────────────────

@Composable
fun BrandLockup(theme: SansinaTheme, phase: Phase, modifier: Modifier = Modifier) {
    val color by animateColorAsState(theme.primary(phase), tween(600), label = "brand")
    val dim = if (phase == Phase.QR || (phase == Phase.PRIZE && theme.id == "B")) 0.5f else 1f
    Row(modifier.graphicsLayer { alpha = dim }, verticalAlignment = Alignment.CenterVertically) {
        Wordmark(color, size = 34, tag = theme.brandTag)
        if (!theme.brandTag) {
            Box(Modifier.padding(horizontal = 12.dp).size(1.dp, 30.dp).background(color.copy(0.35f)))
            Column {
                Text("PREMIUM", color = color.copy(0.55f), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp, lineHeight = 12.sp)
                Text("PARTNER", color = color.copy(0.55f), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp, lineHeight = 12.sp)
            }
        }
    }
}

@Composable
fun SettingsButton(theme: SansinaTheme, phase: Phase, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val color by animateColorAsState(theme.primary(phase), tween(600), label = "gear")
    Box(
        modifier.size(44.dp).clip(CircleShape).background(color.copy(0.08f)).border(1.dp, color.copy(0.18f), CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(20.dp)) {
            val c = center
            val r = size.minDimension / 2
            for (k in 0 until 8) {
                val a = Math.toRadians(k * 45.0)
                drawLine(color, c, Offset(c.x + (r * Math.cos(a)).toFloat(), c.y + (r * Math.sin(a)).toFloat()), strokeWidth = r * 0.34f)
            }
            drawCircle(color, r * 0.66f, c)
            drawCircle(theme.isWashed(phase).let { if (it) theme.washBg else theme.bg }, r * 0.28f, c)
        }
    }
}

