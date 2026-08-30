package com.troy.sansina

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import kotlinx.coroutines.delay

// ───────────────────────── World / background ─────────────────────────

/** Background per theme. Every decor drifts very slowly so the idle screen is never fully static. */
@Composable
fun WorldBackground(theme: SansinaTheme, phase: Phase, modifier: Modifier = Modifier) {
    val washed = theme.isWashed(phase)
    val bg by animateColorAsState(if (washed) theme.washBg else theme.bg, tween(600), label = "bg")
    val drift = rememberInfiniteTransition(label = "drift").animateFloat(0f, 1f, infiniteRepeatable(tween(22000, easing = LinearEasing), RepeatMode.Reverse), label = "d")
    // Figma frame 5 "CERCEVE": white border frame around the idle screen only.
    val frameA by animateFloatAsState(if (theme.decor == Decor.TROY_SKY && phase == Phase.INVITE) 1f else 0f, tween(600), label = "cerceve")
    Canvas(modifier.fillMaxSize()) {
        val w = size.width; val h = size.height
        val d = drift.value
        val dx = w * 0.03f * (d - 0.5f); val dy = h * 0.02f * (0.5f - d)
        if (theme.decor == Decor.STORE_WASH && !washed) {
            drawRect(Brush.linearGradient(listOf(Color(0xFF0A62FF), Color(0xFF2E93FF), Color(0xFF57B0FF)), Offset(dx, 0f), Offset(w + dx, h)))
        } else drawRect(bg)
        when (theme.decor) {
            Decor.QUIET_GLOW -> drawOval(
                Brush.radialGradient(listOf(Color.White, Color.White.copy(0f)), Offset(w / 2 + dx, h * 0.46f + dy), w * 0.5f),
                Offset(dx, h * 0.46f - h * 0.5f + dy), Size(w, h)
            )
            Decor.GLASS_FIELD -> {
                drawCircle(Brush.radialGradient(listOf(Color(0x8C0A84FF), Color(0x000A84FF)), Offset(w * (0.26f - 0.02f * d), h * (0.22f + 0.015f * d)), w * 0.36f * (1 + 0.08f * d)), w * 0.36f * (1 + 0.08f * d), Offset(w * (0.26f - 0.02f * d), h * (0.22f + 0.015f * d)))
                drawCircle(Brush.radialGradient(listOf(Color(0x575AC8FA), Color(0x005AC8FA)), Offset(w * 0.78f - dx, h * 0.74f - dy), w * 0.32f), w * 0.32f, Offset(w * 0.78f - dx, h * 0.74f - dy))
                drawRect(Brush.radialGradient(listOf(Color.Transparent, Color.Transparent, Color(0xFF03102A)), Offset(w / 2, h / 2), w * 0.75f))
            }
            Decor.SPOTLIGHT -> drawOval(
                Brush.radialGradient(listOf(Color(0x21FFFFFF), Color(0x00FFFFFF)), Offset(w / 2 + dx, h * 0.44f + dy), w * 0.55f),
                Offset(w / 2 - w * 0.55f + dx, h * 0.44f - h * 0.55f + dy), Size(w * 1.1f, h * 1.1f)
            )
            Decor.TRAY_LIGHT -> {
                drawOval(Brush.radialGradient(listOf(Color(0xF2FFFFFF), Color(0x00FFFFFF)), Offset(w / 2 + dx, -h * 0.25f), w * 0.45f), Offset(w * 0.05f + dx, -h * 0.7f), Size(w * 0.9f, h * 0.9f))
                drawRect(Brush.verticalGradient(listOf(Color(0x00171614), Color(0x14171614)), h * 0.75f, h), Offset(0f, h * 0.75f), Size(w, h * 0.25f))
            }
            Decor.STORE_WASH -> Unit
            Decor.TROY_SKY -> {
                // Figma: base TROY BLUE #4DC0DF with a radial #C4F2FF → #4DC0DF wash.
                drawRect(Color(0xFF4DC0DF))
                drawRect(Brush.radialGradient(listOf(Color(0xFFC4F2FF), Color(0xFF4DC0DF)), Offset(w * 0.5f + dx, h * 0.42f + dy), w * 0.72f))
                drawOval(Brush.radialGradient(listOf(Color(0x2EFBFDFE), Color(0x00FBFDFE)), Offset(w * 0.5f + dx, h * 0.35f + dy), w * 0.55f), Offset(w * 0.5f - w * 0.55f + dx, h * 0.35f - h * 0.5f + dy), Size(w * 1.1f, h))
                // Figma "ARTI": filled white plus marks scattered on the world (sizes 67–139 px on 1920).
                val plus = Color(0x8CFFFFFF)
                fun arti(cx: Float, cy: Float, r: Float) {
                    val t = r * 0.30f
                    drawRoundRect(plus, Offset(cx - t / 2, cy - r), Size(t, r * 2), androidx.compose.ui.geometry.CornerRadius(t / 2))
                    drawRoundRect(plus, Offset(cx - r, cy - t / 2), Size(r * 2, t), androidx.compose.ui.geometry.CornerRadius(t / 2))
                }
                arti(w * 0.08f + dx, h * 0.14f + dy, w * 0.028f)
                arti(w * 0.93f - dx, h * 0.2f - dy, w * 0.02f)
                arti(w * 0.85f + dx, h * 0.82f + dy, w * 0.032f)
                arti(w * 0.12f - dx, h * 0.86f - dy, w * 0.018f)
                // CERCEVE: 1770×1050 on 1920×1200, r50, 6 px white stroke — idle only.
                if (frameA > 0f) {
                    val fw = w * (1770f / 1920f); val fh = h * (1050f / 1200f)
                    drawRoundRect(
                        Color.White.copy(alpha = frameA), Offset((w - fw) / 2, (h - fh) / 2), Size(fw, fh),
                        androidx.compose.ui.geometry.CornerRadius(w * (50f / 1920f)),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * (6f / 1920f))
                    )
                }
            }
        }
        // Faint Troy sparkles that breathe with the drift.
        val sp = theme.primary(phase).copy(alpha = 0.05f + 0.04f * d)
        sparkle(Offset(w * 0.12f + dx, h * 0.18f + dy), w * 0.012f, sp)
        sparkle(Offset(w * 0.88f - dx, h * 0.24f - dy), w * 0.009f, sp)
        sparkle(Offset(w * 0.8f + dx, h * 0.84f + dy), w * 0.014f, sp)
        sparkle(Offset(w * 0.16f - dx, h * 0.8f - dy), w * 0.008f, sp)
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

/** Soft 1 → 1.03 breathing scale, shared by the idle "merhaba" and the CTA. */
@Composable
fun rememberBreath(period: Int = 1800, amount: Float = 0.03f): State<Float> =
    rememberInfiniteTransition(label = "breath").animateFloat(1f, 1f + amount, infiniteRepeatable(tween(period, easing = EaseInOut), RepeatMode.Reverse), label = "bs")

@Composable
fun Cta(theme: SansinaTheme, text: String, onClick: () -> Unit) {
    val shape = if (theme.buttonShape == ButtonShape.PILL) RoundedCornerShape(999.dp) else RoundedCornerShape(8.dp)
    val breathe by rememberBreath()
    Box(
        Modifier
            .graphicsLayer { scaleX = breathe; scaleY = breathe }
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

/** 1. Idle / welcome: the word "merhaba" and the CTA breathe. */
@Composable
fun InviteText(theme: SansinaTheme, visible: Boolean, onStart: () -> Unit) {
    val alpha by animateFloatAsState(if (visible) 1f else 0f, tween(if (visible) 900 else 500), label = "inv")
    val scale by animateFloatAsState(if (visible) 1f else 0.96f, tween(900), label = "invs")
    if (alpha == 0f) return
    val p = Phase.INVITE
    val primary = theme.primary(p)
    val fs = (theme.h1Size * 0.82f).toInt()
    val style = @Composable { t: String, c: Color, m: Modifier ->
        Text(t, color = c, fontSize = fs.sp, lineHeight = (fs * 1.05f).sp, fontWeight = theme.headlineWeight, letterSpacing = (-fs * 0.032f).sp, textAlign = TextAlign.Center, modifier = m, softWrap = false)
    }
    val breathe by rememberBreath(period = 2100, amount = 0.05f)
    Column(
        Modifier.fillMaxSize().graphicsLayer { this.alpha = alpha; scaleX = scale; scaleY = scale },
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
    ) {
        // Figma frame 5: "Bugünkü şansına" (90px black) / "merhaba" (140px white, soft shadow) / "demek ister misin?" (60px).
        style("Bugünkü şansına", primary, Modifier)
        Text(
            "merhaba", color = theme.accent, fontSize = (fs * 1.5f).sp, lineHeight = (fs * 1.6f).sp,
            fontWeight = theme.headlineWeight, letterSpacing = (-fs * 0.045f).sp, softWrap = false,
            style = androidx.compose.ui.text.TextStyle(shadow = androidx.compose.ui.graphics.Shadow(Color(0x40000000), Offset(3f, 3f), 14f)),
            modifier = Modifier.graphicsLayer { scaleX = breathe; scaleY = breathe }
        )
        Text(
            "demek ister misin?", color = primary, fontSize = (fs * 0.67f).sp, lineHeight = (fs * 0.72f).sp,
            fontWeight = FontWeight.SemiBold, letterSpacing = (-fs * 0.02f).sp, softWrap = false
        )
        Spacer(Modifier.height(14.dp))
        Caption("Bir dokunuşla Troy'dan ne kazanacağını öğren.", theme.secondary(p), 19)
        Spacer(Modifier.height(26.dp))
        Cta(theme, "Hemen Dene", onStart)
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Canvas(Modifier.size(18.dp)) {
                val c = theme.secondary(p)
                drawRoundRect(c, Offset(size.width * 0.4f, 0f), Size(size.width * 0.2f, size.height * 0.6f), androidx.compose.ui.geometry.CornerRadius(4f), style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
                drawCircle(c, size.width * 0.32f, Offset(size.width * 0.5f, size.height * 0.5f), style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
            }
            Spacer(Modifier.width(9.dp))
            Text("Ekrana dokun ve oyuna başla!", color = theme.secondary(p), fontSize = 15.sp)
        }
    }
}

/** Invite + the card choreography (2–4), on one surface so the cards never remount. */
@Composable
fun StageScreen(state: GameState, theme: SansinaTheme, cardBack: CardBack, onStart: () -> Unit, onPick: (Int) -> Unit, onFlip: () -> Unit) {
    val phase = state.phase
    Box(Modifier.fillMaxSize()) {
        Stage(state, theme, cardBack, onPick, onFlip, Modifier.fillMaxSize().padding(top = 60.dp))
        InviteText(theme, visible = phase == Phase.INVITE, onStart = onStart)

        // Headline for the current step, above the cards.
        Box(Modifier.align(Alignment.TopCenter).padding(top = 118.dp)) {
            AnimatedContent(
                targetState = when (phase) { Phase.SELECT -> 1; Phase.SHUFFLE -> 2; Phase.READY, Phase.REVEAL -> 3; else -> 0 },
                label = "head", transitionSpec = { (fadeIn(tween(400)) + slideInVertically { -it / 4 }) togetherWith fadeOut(tween(220)) }
            ) { k ->
                val size = (theme.h1Size * 0.62f).toInt()
                when (k) {
                    1 -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Headline(theme, phase, "Bir dokunuşla ", "şansını", " keşfet!", size)
                        Spacer(Modifier.height(8.dp))
                        Caption("Kartlardan birini seç, Troy'dan kazanacağın avantajı öğren.", theme.secondary(phase), 19)
                    }
                    2 -> Caption("Avantajın seçiliyor…", theme.secondary(phase), 24)
                    3 -> Headline(theme, phase, "Hemen çevir, ", "avantajını", " gör!", size)
                    else -> Spacer(Modifier.height(1.dp))
                }
            }
        }
        Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 44.dp)) {
            AnimatedContent(targetState = phase, label = "cap", transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) }) { p ->
                when (p) {
                    Phase.SELECT -> Caption("Bir karta dokun.", theme.secondary(p), 19)
                    Phase.READY -> Caption("Karta dokun ve çevir.", theme.secondary(p), 19)
                    Phase.REVEAL -> Caption("Tebrikler!", theme.primary(p), 30, FontWeight.SemiBold)
                    else -> Spacer(Modifier.height(1.dp))
                }
            }
        }
    }
}

/**
 * 5 + 6. Reward result: amount scales up, "avantajına merhaba" follows, a short confetti burst,
 * then the QR and its instruction fade in 0.5 s later and the screen holds still.
 */
@Composable
fun ResultScreen(state: GameState, theme: SansinaTheme, onRestart: () -> Unit) {
    val p = Phase.RESULT
    val prize = state.winningCard.promo
    // Figma: amounts are TROY BLUE with a white outline stroke.
    val big = when (theme.id) { "E" -> theme.accent; "F" -> Color(0xFF4DC0DF); else -> theme.primary(p) }

    val amount = remember { Animatable(0.6f) }
    val hello = remember { Animatable(0f) }
    val qr = remember { Animatable(0f) }
    var confetti by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        confetti = true
        amount.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow))
        hello.animateTo(1f, tween(450, easing = SoftOut))
        delay(Timing.RESULT_QR_DELAY)
        qr.animateTo(1f, tween(600, easing = SoftOut))
    }

    Box(Modifier.fillMaxSize()) {
        // Final design (TROY_ROSYLAB 8–11): amount → "Avantajına merhaba" → copy → QR, one centred column.
        Column(Modifier.align(Alignment.Center).padding(horizontal = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.graphicsLayer { scaleX = amount.value; scaleY = amount.value; alpha = ((amount.value - 0.6f) / 0.3f).coerceIn(0f, 1f) }) {
                val sz = (theme.bigSize * 0.82f)
                val outline = if (theme.id == "F") androidx.compose.ui.text.TextStyle(
                    shadow = androidx.compose.ui.graphics.Shadow(Color(0x40000000), Offset(4f, 4f), 18f)
                ) else androidx.compose.ui.text.TextStyle.Default
                Box {
                    if (theme.id == "F") Text(
                        prize.label.removeSuffix(" TL"), color = Color.White, fontSize = sz.sp, lineHeight = (sz * 0.96f).sp, fontWeight = theme.headlineWeight, letterSpacing = (-sz * 0.04f).sp,
                        style = androidx.compose.ui.text.TextStyle(drawStyle = androidx.compose.ui.graphics.drawscope.Stroke(width = 16f, join = androidx.compose.ui.graphics.StrokeJoin.Round))
                    )
                    Text(prize.label.removeSuffix(" TL"), color = big, fontSize = sz.sp, lineHeight = (sz * 0.96f).sp, fontWeight = theme.headlineWeight, letterSpacing = (-sz * 0.04f).sp, style = outline)
                }
                Spacer(Modifier.width(10.dp))
                Box {
                    if (theme.id == "F") Text(
                        "TL", color = Color.White, fontSize = (sz * 0.36f).sp, fontWeight = theme.headlineWeight, modifier = Modifier.padding(bottom = (sz * 0.14f).dp),
                        style = androidx.compose.ui.text.TextStyle(drawStyle = androidx.compose.ui.graphics.drawscope.Stroke(width = 10f, join = androidx.compose.ui.graphics.StrokeJoin.Round))
                    )
                    Text("TL", color = big, fontSize = (sz * 0.36f).sp, fontWeight = theme.headlineWeight, modifier = Modifier.padding(bottom = (sz * 0.14f).dp))
                }
            }
            Spacer(Modifier.height(6.dp))
            Box(Modifier.graphicsLayer { alpha = hello.value; translationY = (1 - hello.value) * 18f * density }) {
                Headline(theme, p, "Avantajına ", "merhaba", "", if (theme.id == "C" || theme.id == "E") 44 else 38, accentOn = theme.id != "E" && theme.id != "F")
            }
            Spacer(Modifier.height(18.dp))
            Column(Modifier.graphicsLayer { alpha = qr.value; translationY = (1 - qr.value) * 14f * density }, horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Troy mağazalarında kullanabileceğin avantajı kazandın.\nQR'ı okut ve avantajın tadını çıkar.",
                    color = theme.secondary(p), fontSize = 19.sp, lineHeight = 26.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 520.dp)
                )
                Spacer(Modifier.height(20.dp))
                QrPlate(theme)
            }
        }
        Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 36.dp).graphicsLayer { alpha = qr.value }) {
            val shape = RoundedCornerShape(26.dp)
            Box(
                Modifier.then(if (theme.glassButton) Modifier.border(1.dp, Color(0x3DFFFFFF), shape) else Modifier)
                    .background(theme.buttonBg, shape).clip(shape).clickable(onClick = onRestart).height(52.dp).padding(horizontal = 30.dp),
                contentAlignment = Alignment.Center
            ) { Text("Yeniden oyna", color = theme.buttonText, fontSize = 17.sp, fontWeight = FontWeight.SemiBold) }
        }
        Confetti(active = confetti, colors = theme.confetti, modifier = Modifier.fillMaxSize())
    }
}

/** The QR itself never moves; a soft frame around it pulses to draw the eye. */
@Composable
private fun QrPlate(theme: SansinaTheme) {
    val pulse = rememberInfiniteTransition(label = "qrf").animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = EaseInOut), RepeatMode.Reverse), label = "qp")
    val radius = 22.dp
    val frameColor = if (theme.id == "E" || theme.id == "A" || theme.id == "D") theme.accent else Color.White
    Box(contentAlignment = Alignment.Center) {
        Box(
            Modifier.matchParentSize().graphicsLayer { val s = 1f + 0.035f * pulse.value; scaleX = s; scaleY = s; alpha = 0.25f + 0.45f * pulse.value }
                .border(2.dp, frameColor, RoundedCornerShape(radius + 6.dp))
        )
        val plate = if (theme.id == "F")
            // Figma "QR": white plate, ~9% corner radius, TROY BLUE stroke, soft shadow.
            Modifier.shadow(20.dp, RoundedCornerShape(radius), ambientColor = Color(0x40000000), spotColor = Color(0x40000000))
                .background(Color.White, RoundedCornerShape(radius)).border(2.5.dp, Color(0xFF4DC0DF), RoundedCornerShape(radius)).padding(18.dp)
        else when (theme.qrStyle) {
            QrStyle.GLASS_CARD -> Modifier.background(Color.White, RoundedCornerShape(radius)).padding(18.dp)
            QrStyle.BARE_PLATE -> Modifier.background(Color.White, RoundedCornerShape(radius)).padding(18.dp)
            QrStyle.BOARDING_PASS -> Modifier.shadow(24.dp, RoundedCornerShape(radius), ambientColor = Color(0x66171614), spotColor = Color(0x66171614)).background(Color.White, RoundedCornerShape(radius)).border(1.dp, Color(0xFFE4C79C), RoundedCornerShape(radius)).padding(22.dp)
            else -> Modifier.shadow(24.dp, RoundedCornerShape(radius), ambientColor = Color(0x4D060A12), spotColor = Color(0x4D060A12)).background(Color.White, RoundedCornerShape(radius)).padding(22.dp)
        }
        Box(Modifier.padding(10.dp).then(plate)) { QrGrid(theme.qrModule, Color.White, Modifier.size(164.dp)) }
    }
}

// ───────────────────────── Brand + settings ─────────────────────────

@Composable
fun BrandLockup(theme: SansinaTheme, phase: Phase, modifier: Modifier = Modifier) {
    // Figma: the campaign look keeps the wordmark white on the blue world.
    val color by animateColorAsState(if (theme.id == "F") Color.White else theme.primary(phase), tween(600), label = "brand")
    val dim = if (phase == Phase.RESULT && theme.id != "F") 0.6f else 1f
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
