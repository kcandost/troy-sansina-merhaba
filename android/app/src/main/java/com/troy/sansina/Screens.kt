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

// ───────────────────────── Cards ─────────────────────────

@Composable
fun GameCard(card: Card, theme: SansinaTheme, faceUp: Boolean, big: Boolean, modifier: Modifier = Modifier) {
    val rot by animateFloatAsState(if (faceUp) 180f else 0f, tween(620, easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)), label = "flip")
    val scale by animateFloatAsState(if (big) 1.58f else 1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow), label = "s")
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier
            .size(146.dp, 206.dp)
            .graphicsLayer { rotationY = rot; cameraDistance = 12f * density; scaleX = scale; scaleY = scale }
    ) {
        if (rot <= 90f) {
            Box(Modifier.fillMaxSize().shadow(14.dp, shape, ambientColor = Color(0x66000000), spotColor = Color(0x66000000)).background(theme.productCardBg, shape).border(1.dp, theme.productCardBorder, shape)) {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    ProductFace(card.product, theme.productInk, Modifier.size(90.dp))
                    Spacer(Modifier.height(14.dp))
                    Text(card.product.label, color = theme.productInk.copy(alpha = 0.6f), fontSize = 13.sp)
                }
            }
        } else {
            val p = card.tier.palette
            val premium = card.tier == Tier.PREMIUM
            Box(
                Modifier.fillMaxSize().graphicsLayer { rotationY = 180f }
                    .shadow(if (premium) 18.dp else 16.dp, shape, ambientColor = Color(0x8C0C1018), spotColor = Color(0x8C0C1018))
                    .background(p.bg, shape).border(1.dp, p.border, shape)
            ) {
                // sheen
                Box(Modifier.fillMaxSize().clip(shape).background(Brush.linearGradient(listOf(Color.White.copy(p.sheen), Color.Transparent), Offset(0f, 0f), Offset(280f, 400f))))
                if (theme.brushed) Box(Modifier.fillMaxSize().clip(shape).background(Brush.linearGradient(listOf(Color(0x4DFFFFFF), Color.Transparent, Color(0x1F000000)), Offset(0f, 0f), Offset(200f, 600f))))
                if (premium) Box(Modifier.fillMaxSize().padding(5.dp).border(1.dp, Color(0x24C9A227), RoundedCornerShape(12.dp)))
                Column(Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 18.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(card.promo.label.removeSuffix(" TL"), color = p.text, fontSize = 34.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-1.4).sp, lineHeight = 34.sp, softWrap = false)
                            Spacer(Modifier.width(5.dp))
                            Text("TL", color = p.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, softWrap = false, modifier = Modifier.padding(bottom = 4.dp))
                        }
                        Box(Modifier.padding(top = 12.dp).size(34.dp, 2.dp).background(p.rule, RoundedCornerShape(1.dp)))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                        Text("avantaj", color = p.cap, fontSize = 11.sp, letterSpacing = 0.3.sp)
                        if (premium) Wordmark(GoldLight.copy(alpha = 0.72f), size = 14, tag = false)
                    }
                }
            }
        }
    }
}

/** Card row with deal / flip / shuffle / reveal choreography driven by [GameState]. */
@Composable
fun Deck(state: GameState, theme: SansinaTheme, modifier: Modifier = Modifier) {
    val phase = state.phase
    val n = state.cards.size
    val spacing = if (n <= 5) 168f else 150f
    Box(modifier.fillMaxWidth().height(340.dp), contentAlignment = Alignment.Center) {
        state.cards.forEachIndexed { i, card ->
            val dealt = phase.ordinal > Phase.DEAL.ordinal || (phase == Phase.DEAL && i < state.dealtCount)
            val faceUp = phase.ordinal > Phase.FLIP.ordinal || (phase == Phase.FLIP && i < state.flippedCount)
            val isWinner = i == state.winner
            val picked = phase.ordinal >= Phase.PICK.ordinal
            val visible = dealt && (!picked || isWinner)

            // shuffle target positions
            val home = (i - (n - 1) / 2f) * spacing
            val (tx, ty, rz) = when (state.shuffleStep) {
                1 -> Triple(home * 1.15f, -20f * kotlin.math.abs(i - (n - 1) / 2f), (i - (n - 1) / 2f) * 9f)          // fan
                2 -> Triple(if (i < n / 2 + 1) -200f else 200f, i * -8f, 0f)  // split
                3 -> Triple(if (i < n / 2 + 1) 180f else -180f, 0f, if (i < n / 2 + 1) -4f else 4f)             // cross
                4 -> Triple(0f, i * -3f, (i - (n - 1) / 2f) * 2f)                                           // collapse
                5 -> Triple(if (i % 2 == 0) -70f else 70f, i * -3f, 0f)                          // cut
                6 -> Triple(home, 0f, 0f)                                                        // re-deal
                else -> if (picked && isWinner) Triple(0f, -8f, 0f) else Triple(home, 0f, 0f)
            }
            val ax by animateFloatAsState(tx, tween(440, easing = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)), label = "x")
            val ay by animateFloatAsState(ty, tween(440, easing = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)), label = "y")
            val ar by animateFloatAsState(rz, tween(440), label = "r")
            val alpha by animateFloatAsState(if (visible) 1f else 0f, tween(if (dealt) 400 else 0), label = "a")
            val dealY by animateFloatAsState(if (dealt) 0f else -330f, tween(660, easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)), label = "dy")
            val dealS by animateFloatAsState(if (dealt) 1f else 0.9f, tween(660), label = "ds")

            val revealed = picked && isWinner && state.revealed
            GameCard(
                card, theme, faceUp = faceUp && !(picked && isWinner && !state.revealed) || (picked && isWinner && state.revealed),
                big = revealed,
                modifier = Modifier.graphicsLayer {
                    translationX = ax * density
                    translationY = (ay + dealY) * density
                    rotationZ = ar
                    this.alpha = alpha
                    scaleX = dealS; scaleY = dealS
                }
            )
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
fun InviteScreen(theme: SansinaTheme, onStart: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 96.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Headline(theme, Phase.INVITE, "Bugünkü şansına ", "merhaba", " demek ister misin?", theme.h1Size, Modifier.widthIn(max = 760.dp))
        Spacer(Modifier.height(18.dp))
        Caption("Bir dokunuşla Troy'dan ne kazanacağını öğren.", theme.secondary(Phase.INVITE), 21)
        Spacer(Modifier.height(34.dp))
        Cta(theme, "Şansını Dene", onStart)
        Spacer(Modifier.height(26.dp))
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

@Composable
fun HelloScreen(theme: SansinaTheme) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        val scale = remember { Animatable(0.9f) }
        LaunchedEffect(Unit) { scale.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)) }
        Box(Modifier.graphicsLayer { scaleX = scale.value; scaleY = scale.value }) {
            Headline(theme, Phase.MERHABA, if (theme.id == "A") "Şansına " else "Şansına\n", "merhaba!", "", theme.helloSize, accentOn = theme.helloAccent)
        }
        Spacer(Modifier.height(16.dp))
        Caption("Avantajını seçmeye hazır ol…", theme.secondary(Phase.MERHABA), 21)
    }
}

@Composable
fun DeckScreen(state: GameState, theme: SansinaTheme) {
    val phase = state.phase
    Box(Modifier.fillMaxSize()) {
        Deck(state, theme, Modifier.align(Alignment.Center).offset(y = (-10).dp))
        Column(Modifier.align(Alignment.BottomCenter).padding(bottom = 64.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            AnimatedContent(targetState = phase, label = "cap", transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) }) { p ->
                when (p) {
                    Phase.DEAL -> Caption("Avantajını seçmeye hazır ol…", theme.secondary(p), 21)
                    Phase.FLIP -> Caption("250 TL'den 1.500 TL'ye kadar", theme.secondary(p), 21)
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

