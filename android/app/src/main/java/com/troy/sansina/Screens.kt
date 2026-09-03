package com.troy.sansina

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// ───────────────────────── World / background ─────────────────────────

/** One scattered ARTI plus mark: centre x/y and size, in Figma's 1920×1200 frame px. */
private data class Mark(val x: Float, val y: Float, val s: Float)

// Exact positions parsed from TROY_ROZYLABS frames 5 / 6 / 7. Frames 8–11 carry none (confetti instead).
private val InviteMarks = listOf(
    Mark(1659f, 256f, 139f), Mark(210f, 1015f, 97f), Mark(429f, 510f, 139f),
    Mark(1292f, 396f, 67f), Mark(281f, 169f, 95f), Mark(1747f, 841f, 95f),
)
private val SelectMarks = listOf(
    Mark(1802f, 177f, 139f), Mark(164f, 1057f, 97f), Mark(765f, 1078f, 139f), Mark(1398f, 341f, 67f),
    Mark(158f, 135f, 95f), Mark(1416f, 1022f, 95f), Mark(1736f, 1112f, 95f),
)
private val ReadyMarks = listOf(
    Mark(1802f, 177f, 139f), Mark(1742f, 750f, 139f), Mark(1411f, 611f, 109f), Mark(164f, 1057f, 97f),
    Mark(456f, 747f, 139f), Mark(240f, 461f, 139f), Mark(1430f, 452f, 67f), Mark(158f, 135f, 95f),
    Mark(1490f, 1017f, 95f), Mark(1736f, 1112f, 95f),
)

/**
 * Figma "ARTI": sharp-cornered plus built from four arms with a hollow centre square.
 * Arm thickness is 13.67% of the mark size (9.57/70 in the source vectors).
 */
fun DrawScope.artiCross(center: Offset, s: Float, color: Color) {
    val t = s * 0.1367f
    val a = s / 2f
    val t2 = t / 2f
    drawRect(color, Offset(center.x - t2, center.y - a), Size(t, a - t2))
    drawRect(color, Offset(center.x - t2, center.y + t2), Size(t, a - t2))
    drawRect(color, Offset(center.x - a, center.y - t2), Size(a - t2, t))
    drawRect(color, Offset(center.x + t2, center.y - t2), Size(a - t2, t))
}

/** Background per theme. Every decor drifts very slowly so the idle screen is never fully static. */
@Composable
fun WorldBackground(theme: SansinaTheme, phase: Phase, modifier: Modifier = Modifier) {
    val washed = theme.isWashed(phase)
    val bg by animateColorAsState(if (washed) theme.washBg else theme.bg, tween(600), label = "bg")
    val drift = rememberInfiniteTransition(label = "drift").animateFloat(0f, 1f, infiniteRepeatable(tween(22000, easing = LinearEasing), RepeatMode.Reverse), label = "d")
    // Frame 5 dressing (SEKIL waves + CERCEVE border) shows on the idle screen only.
    val frameA by animateFloatAsState(if (theme.decor == Decor.TROY_SKY && phase == Phase.INVITE) 1f else 0f, tween(600), label = "cerceve")
    // Figma: the diagonal light beam sits at 10% opacity on frame 5 and full strength on frames 6–11.
    val beamA by animateFloatAsState(if (phase == Phase.INVITE) 0.10f else 1f, tween(700), label = "beam")
    val sekil = painterResource(R.drawable.sekil)
    val cerceve = painterResource(R.drawable.cerceve_screen)
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
                // Figma: flat TROY BLUE #4DC0DF under a diagonal top-left → bottom-right "light beam"
                // wash (7 stops, white core at 48.6%). Frame 5 = 10% wash; frames 6–11 = full wash.
                drawRect(Color(0xFF4DC0DF))
                drawRect(
                    Brush.linearGradient(
                        colorStops = arrayOf(
                            0f to Color(0xFF4EC0DF), 0.121f to Color(0xFF79CFE7), 0.243f to Color(0xFFA5DFEF),
                            0.364f to Color(0xFFD0EEF6), 0.486f to Color(0xFFFBFDFE), 0.743f to Color(0xFFC6EDF3),
                            1f to Color(0xFF90DCE7),
                        ),
                        start = Offset(dx * 2f, dy * 2f), end = Offset(w + dx * 2f, h + dy * 2f)
                    ),
                    alpha = beamA
                )
                if (frameA > 0.004f) {
                    // SEKIL: the indigo Troy-curve pattern along the bottom (2174×796 at y=500 on 1920×1200).
                    translate(0f, h * (500f / 1200f)) {
                        with(sekil) { draw(Size(w * (2174f / 1920f), h * (796f / 1200f)), alpha = frameA) }
                    }
                    // CERCEVE: white border frame (1770×1050, r50, 6px stroke) with a gap at top centre…
                    translate(w * (72f / 1920f), h * (72f / 1200f)) {
                        with(cerceve) { draw(Size(w * (1776f / 1920f), h * (1056f / 1200f)), alpha = frameA) }
                    }
                    // …where a solid white ARTI sits on the border line.
                    artiCross(Offset(w * (960f / 1920f), h * (75f / 1200f)), w * (70f / 1920f), Color.White.copy(frameA))
                }
                // Scattered ARTI marks, white at 50%, exact per frame.
                val marks = when (phase) {
                    Phase.INVITE -> InviteMarks
                    Phase.SELECT, Phase.SHUFFLE -> SelectMarks
                    Phase.READY, Phase.REVEAL -> ReadyMarks
                    Phase.RESULT -> emptyList()
                }
                marks.forEach { m ->
                    artiCross(Offset(w * m.x / 1920f + dx, h * m.y / 1200f + dy), w * m.s / 1920f, Color.White.copy(0.5f))
                }
            }
        }
        // Faint Troy sparkles that breathe with the drift — not part of the campaign look.
        if (theme.decor != Decor.TROY_SKY) {
            val sp = theme.primary(phase).copy(alpha = 0.05f + 0.04f * d)
            sparkle(Offset(w * 0.12f + dx, h * 0.18f + dy), w * 0.012f, sp)
            sparkle(Offset(w * 0.88f - dx, h * 0.24f - dy), w * 0.009f, sp)
            sparkle(Offset(w * 0.8f + dx, h * 0.84f + dy), w * 0.014f, sp)
            sparkle(Offset(w * 0.16f - dx, h * 0.8f - dy), w * 0.008f, sp)
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
        fontFamily = theme.fontFamily,
        letterSpacing = (-size * 0.032f).sp, textAlign = TextAlign.Center, modifier = modifier
    )
}

/** Soft 1 → 1.03 breathing scale, shared by the idle "merhaba" and the CTA. */
@Composable
fun rememberBreath(period: Int = 1800, amount: Float = 0.03f): State<Float> =
    rememberInfiniteTransition(label = "breath").animateFloat(1f, 1f + amount, infiniteRepeatable(tween(period, easing = EaseInOut), RepeatMode.Reverse), label = "bs")

@Composable
fun Cta(theme: SansinaTheme, text: String, onClick: () -> Unit) {
    val f = theme.id == "F"
    val shape = if (theme.buttonShape == ButtonShape.PILL) RoundedCornerShape(999.dp) else RoundedCornerShape(8.dp)
    val breathe by rememberBreath()
    Box(
        Modifier
            .graphicsLayer { scaleX = breathe; scaleY = breathe }
            .then(
                when {
                    theme.glassButton -> Modifier.border(1.dp, Color(0x3DFFFFFF), shape)
                    // Figma Input: flat white pill, no drop shadow.
                    f -> Modifier
                    else -> Modifier.shadow(12.dp, shape, ambientColor = Color(0x61000000), spotColor = Color(0x61000000))
                }
            )
            .background(theme.buttonBg, shape)
            .clip(shape)
            .clickable(onClick = onClick)
            .height(theme.buttonHeight)
            .padding(horizontal = if (f) 36.dp else if (theme.buttonHeight > 60.dp) 46.dp else 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text, color = theme.buttonText,
            fontSize = if (f) 30.sp else if (theme.buttonHeight > 60.dp) 21.sp else 19.sp,
            fontWeight = if (f) FontWeight.ExtraBold else if (theme.headlineWeight == FontWeight.Bold) FontWeight.Bold else FontWeight.SemiBold,
            fontFamily = theme.fontFamily
        )
    }
}

// ───────────────────────── Screens ─────────────────────────

/** 1. Idle / welcome: the word "merhaba" and the CTA breathe. */
@Composable
fun InviteText(theme: SansinaTheme, visible: Boolean, onStart: () -> Unit) {
    val alpha by animateFloatAsState(if (visible) 1f else 0f, tween(if (visible) 900 else 500), label = "inv")
    val scale by animateFloatAsState(if (visible) 1f else 0.96f, tween(900), label = "invs")
    if (alpha == 0f) return
    if (theme.id == "F") { FigmaInvite(theme, alpha, scale, onStart); return }
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

/** Every promo hit its grant cap: hold this static screen until new config arrives. */
@Composable
fun CampaignEnded(theme: SansinaTheme) {
    Column(
        Modifier.fillMaxSize().background(Color(0xFFF6FBFD)),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
    ) {
        Text("Kampanya sona erdi", color = Color(0xFF171614), fontSize = 64.sp, fontWeight = FontWeight.ExtraBold, fontFamily = theme.fontFamily)
        Spacer(Modifier.height(18.dp))
        Text("Yeni avantajlar için bizi takip et.", color = Color(0xFF5C5A56), fontSize = 26.sp, fontFamily = theme.fontFamily)
    }
}

/**
 * Frame 5, laid out by frame fractions: big white logo lockup at 16.5%,
 * "Bugünkü şansına" (90px) / "merhaba" (140px white, soft shadow) / "demek ister misin?" (60px),
 * then the white HEMEN DENE pill at 74.8%.
 */
@Composable
private fun FigmaInvite(theme: SansinaTheme, alpha: Float, scale: Float, onStart: () -> Unit) {
    val breathe by rememberBreath(period = 2100, amount = 0.05f)
    val density = LocalDensity.current.density
    BoxWithConstraints(Modifier.fillMaxSize().graphicsLayer { this.alpha = alpha; scaleX = scale; scaleY = scale }) {
        val h = maxHeight
        Image(
            painterResource(R.drawable.troy_logo), null,
            Modifier.align(Alignment.TopCenter).offset(y = h * (198f / 1200f)).height(h * (214f / 1200f))
        )
        Text(
            "Bugünkü şansına", color = Color.Black, fontSize = 54.sp, lineHeight = 57.sp,
            fontWeight = FontWeight.ExtraBold, fontFamily = theme.fontFamily, softWrap = false,
            modifier = Modifier.align(Alignment.TopCenter).offset(y = h * (497f / 1200f))
        )
        Text(
            "merhaba", color = Color.White, fontSize = 84.sp, lineHeight = 88.sp,
            fontWeight = FontWeight.ExtraBold, fontFamily = theme.fontFamily, softWrap = false,
            style = androidx.compose.ui.text.TextStyle(
                shadow = androidx.compose.ui.graphics.Shadow(Color(0x40000000), Offset(4.9f * density, 4.9f * density), 16.7f * density)
            ),
            modifier = Modifier.align(Alignment.TopCenter).offset(y = h * (572f / 1200f)).graphicsLayer { scaleX = breathe; scaleY = breathe }
        )
        Text(
            "demek ister misin?", color = Color.Black, fontSize = 36.sp, lineHeight = 38.sp,
            fontWeight = FontWeight.Bold, fontFamily = theme.fontFamily, softWrap = false,
            modifier = Modifier.align(Alignment.TopCenter).offset(y = h * (754f / 1200f))
        )
        Box(Modifier.align(Alignment.TopCenter).offset(y = h * (898f / 1200f))) {
            Cta(theme, "HEMEN DENE", onStart)
        }
    }
}

/** Invite + the card choreography (2–4), on one surface so the cards never remount. */
@Composable
fun StageScreen(state: GameState, theme: SansinaTheme, cardBack: CardBack, onStart: () -> Unit, onPick: (Int) -> Unit, onFlip: () -> Unit) {
    val phase = state.phase
    val f = theme.id == "F"
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val h = maxHeight
        Stage(state, theme, cardBack, onPick, onFlip, Modifier.fillMaxSize().padding(top = 60.dp))
        InviteText(theme, visible = phase == Phase.INVITE, onStart = onStart)

        // Headline for the current step, above the cards. Figma: SELECT headline at 87/1200,
        // READY headline lower at 171/1200; all black, no accent span.
        Box(Modifier.align(Alignment.TopCenter).padding(top = if (f) h * (87f / 1200f) else 118.dp)) {
            AnimatedContent(
                targetState = when (phase) { Phase.SELECT -> 1; Phase.SHUFFLE -> 2; Phase.READY, Phase.REVEAL -> 3; else -> 0 },
                label = "head", transitionSpec = { (fadeIn(tween(400)) + slideInVertically { -it / 4 }) togetherWith fadeOut(tween(220)) }
            ) { k ->
                val size = if (f) 54 else (theme.h1Size * 0.62f).toInt()
                when (k) {
                    1 -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (f) {
                            Headline(theme, phase, "Bir dokunuşla şansını keşfet!", "", "", size)
                            Spacer(Modifier.height(10.dp))
                            Caption("Kartlardan birini seç, Troy’dan kazanacağın avantajı öğren.", Color.Black, 30, FontWeight.Medium, fontFamily = theme.fontFamily)
                        } else {
                            Headline(theme, phase, "Bir dokunuşla ", "şansını", " keşfet!", size)
                            Spacer(Modifier.height(8.dp))
                            Caption("Kartlardan birini seç, Troy'dan kazanacağın avantajı öğren.", theme.secondary(phase), 19)
                        }
                    }
                    2 -> Caption("Avantajın seçiliyor…", if (f) Color.Black else theme.secondary(phase), if (f) 30 else 24, if (f) FontWeight.Medium else FontWeight.Normal, fontFamily = theme.fontFamily)
                    3 -> Box(Modifier.offset(y = if (f) h * (84f / 1200f) else 0.dp)) {
                        Headline(theme, phase, "Hemen çevir, ", "avantajını", " gör!", size, accentOn = !f)
                    }
                    else -> Spacer(Modifier.height(1.dp))
                }
            }
        }
        // Bottom guidance captions exist only outside the campaign look.
        if (!f) Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 44.dp)) {
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
    val f = theme.id == "F"

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

    if (f) {
        // Frames 8–11, laid out by frame fractions: 200px amount at y199, 90px "Avantajına merhaba"
        // at y442, 45px bold copy at y652, 320px QR plate at y804. All centred, all black-on-wash.
        val density = LocalDensity.current.density
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val h = maxHeight
            Box(
                Modifier.align(Alignment.TopCenter).offset(y = h * (186f / 1200f))
                    .graphicsLayer { scaleX = amount.value; scaleY = amount.value; alpha = ((amount.value - 0.6f) / 0.3f).coerceIn(0f, 1f) }
            ) {
                val label = "${prize.amount} TL"
                // White contour + TROY BLUE fill + soft shadow, as in the agency frames.
                Text(
                    label, color = Color.White, fontSize = 120.sp, lineHeight = 124.sp, fontWeight = FontWeight.ExtraBold,
                    fontFamily = theme.fontFamily, softWrap = false,
                    style = androidx.compose.ui.text.TextStyle(drawStyle = androidx.compose.ui.graphics.drawscope.Stroke(width = 7f * density, join = androidx.compose.ui.graphics.StrokeJoin.Round))
                )
                Text(
                    label, color = TroyBlue, fontSize = 120.sp, lineHeight = 124.sp, fontWeight = FontWeight.ExtraBold,
                    fontFamily = theme.fontFamily, softWrap = false,
                    style = androidx.compose.ui.text.TextStyle(
                        shadow = androidx.compose.ui.graphics.Shadow(Color(0x40000000), Offset(4.9f * density, 4.9f * density), 16.7f * density)
                    )
                )
            }
            Text(
                "Avantajına merhaba", color = Color.Black, fontSize = 54.sp, lineHeight = 57.sp,
                fontWeight = FontWeight.ExtraBold, fontFamily = theme.fontFamily, softWrap = false,
                modifier = Modifier.align(Alignment.TopCenter).offset(y = h * (440f / 1200f))
                    .graphicsLayer { alpha = hello.value; translationY = (1 - hello.value) * 18f * density }
            )
            Column(
                Modifier.align(Alignment.TopCenter).offset(y = h * (648f / 1200f))
                    .graphicsLayer { alpha = qr.value; translationY = (1 - qr.value) * 14f * density },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Troy mağazalarında kullanabileceğin avantajı kazandın.\nQR’ı okut ve avantajın tadını çıkar.",
                    color = Color.Black, fontSize = 27.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold,
                    fontFamily = theme.fontFamily, textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(h * (44f / 1200f)))
                QrPlate(theme)
            }
            // Kept out of the Figma composition's centre column: restart lives in the corner.
            Box(Modifier.align(Alignment.BottomEnd).padding(end = 28.dp, bottom = 20.dp).graphicsLayer { alpha = qr.value }) {
                val shape = RoundedCornerShape(26.dp)
                Box(
                    Modifier.background(Color.White, shape).clip(shape).clickable(onClick = onRestart).height(52.dp).padding(horizontal = 30.dp),
                    contentAlignment = Alignment.Center
                ) { Text("Yeniden oyna", color = TroyBlue, fontSize = 17.sp, fontWeight = FontWeight.Bold, fontFamily = theme.fontFamily) }
            }
            Confetti(active = confetti, colors = theme.confetti, modifier = Modifier.fillMaxSize())
        }
        return
    }

    val big = if (theme.id == "E") theme.accent else theme.primary(p)
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.align(Alignment.Center).padding(horizontal = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.graphicsLayer { scaleX = amount.value; scaleY = amount.value; alpha = ((amount.value - 0.6f) / 0.3f).coerceIn(0f, 1f) }) {
                val sz = (theme.bigSize * 0.82f)
                Text(prize.label.removeSuffix(" TL"), color = big, fontSize = sz.sp, lineHeight = (sz * 0.96f).sp, fontWeight = theme.headlineWeight, letterSpacing = (-sz * 0.04f).sp)
                Spacer(Modifier.width(10.dp))
                Text("TL", color = big, fontSize = (sz * 0.36f).sp, fontWeight = theme.headlineWeight, modifier = Modifier.padding(bottom = (sz * 0.14f).dp))
            }
            Spacer(Modifier.height(6.dp))
            Box(Modifier.graphicsLayer { alpha = hello.value; translationY = (1 - hello.value) * 18f * density }) {
                Headline(theme, p, "Avantajına ", "merhaba", "", if (theme.id == "C" || theme.id == "E") 44 else 38, accentOn = theme.id != "E")
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
    if (theme.id == "F") {
        // Figma "QR": 320px white plate, r29 (9.1%), 5px TROY BLUE stroke inset, soft drop shadow.
        val radius = 17.dp
        Box(
            Modifier.size(192.dp)
                .graphicsLayer { val s = 1f + 0.02f * pulse.value; scaleX = s; scaleY = s }
                .shadow(20.dp, RoundedCornerShape(radius), ambientColor = Color(0x40000000), spotColor = Color(0x40000000))
                .background(Color.White, RoundedCornerShape(radius))
                .border(3.dp, TroyBlue, RoundedCornerShape(radius))
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) { QrGrid(Color.Black, Color.White, Modifier.fillMaxSize()) }
        return
    }
    val radius = 22.dp
    val frameColor = if (theme.id == "E" || theme.id == "A" || theme.id == "D") theme.accent else Color.White
    Box(contentAlignment = Alignment.Center) {
        Box(
            Modifier.matchParentSize().graphicsLayer { val s = 1f + 0.035f * pulse.value; scaleX = s; scaleY = s; alpha = 0.25f + 0.45f * pulse.value }
                .border(2.dp, frameColor, RoundedCornerShape(radius + 6.dp))
        )
        val plate = when (theme.qrStyle) {
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

/**
 * Invisible staff entry to the settings: a silent 44dp touch target in the corner.
 * Customers never see it; staff tap it and enter the PIN.
 */
@Composable
fun SettingsButton(theme: SansinaTheme, phase: Phase, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.size(44.dp).clip(CircleShape)
            .clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null, onClick = onClick)
    )
}
