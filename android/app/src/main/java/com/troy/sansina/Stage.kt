package com.troy.sansina

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

const val CARD_W = 96f
const val CARD_H = 134f

/** Soft "premium" easing shared by the stage. */
val SoftOut = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

/**
 * One card: product photo on the back, promo value on the front.
 * [shine] sweeps a soft highlight across the back so the card reads as touchable.
 */
@Composable
fun GameCard(card: Card, theme: SansinaTheme, faceUp: Boolean, back: CardBack = CardBack.TROY, shine: Boolean = false, modifier: Modifier = Modifier) {
    val rot by animateFloatAsState(if (faceUp) 180f else 0f, tween(Timing.FLIP.toInt(), easing = SoftOut), label = "flip")
    val shape = RoundedCornerShape(12.dp)
    val ctx = LocalContext.current
    val bmp by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, card.product.file, back) {
        value = if (back == CardBack.PRODUCT) Catalogue.bitmap(ctx, card.product.file) else null
    }
    val sweep = rememberInfiniteTransition(label = "sheen").animateFloat(-1.2f, 2.2f, infiniteRepeatable(tween(2600, easing = LinearEasing)), label = "sw")
    Box(modifier.size(CARD_W.dp, CARD_H.dp).graphicsLayer { rotationY = rot; cameraDistance = 12f * density }) {
        if (rot <= 90f) {
            val backFill = if (theme.id == "F")
                Modifier.background(Brush.radialGradient(listOf(Color(0xFFC4F2FF), Color(0xFF4DC0DF)), Offset(CARD_W * 1.5f, CARD_H * 1.2f), CARD_W * 3.4f), shape)
            else Modifier.background(theme.productCardBg, shape).border(1.dp, theme.productCardBorder, shape)
            Box(
                Modifier.fillMaxSize().shadow(10.dp, shape, ambientColor = Color(0x59000000), spotColor = Color(0x59000000))
                    .then(backFill).clip(shape),
                contentAlignment = Alignment.Center
            ) {
                when (back) {
                    CardBack.PRODUCT -> bmp?.let { Image(it, null, Modifier.fillMaxSize().padding(8.dp), contentScale = ContentScale.Fit) }
                    CardBack.TROY -> Wordmark(theme.productInk, size = 22, tag = theme.brandTag)
                }
                if (shine) {
                    val s = sweep.value
                    Box(
                        Modifier.fillMaxSize().background(
                            Brush.linearGradient(
                                listOf(Color.Transparent, Color.White.copy(0.28f), Color.Transparent),
                                Offset(CARD_W * 3 * (s - 0.3f), CARD_H * 3 * (s - 0.3f) * 0.6f),
                                Offset(CARD_W * 3 * (s + 0.3f), CARD_H * 3 * (s + 0.3f) * 0.6f)
                            )
                        )
                    )
                }
            }
        } else {
            val p = card.tier.palette
            val premium = card.tier == Tier.PREMIUM
            Box(
                Modifier.fillMaxSize().graphicsLayer { rotationY = 180f }
                    .shadow(12.dp, shape, ambientColor = Color(0x8C0C1018), spotColor = Color(0x8C0C1018))
                    .background(p.bg, shape).border(1.dp, p.border, shape).clip(shape)
            ) {
                Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color.White.copy(p.sheen), Color.Transparent), Offset(0f, 0f), Offset(200f, 300f))))
                if (theme.brushed) Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0x4DFFFFFF), Color.Transparent, Color(0x1F000000)), Offset(0f, 0f), Offset(140f, 420f))))
                if (premium) Box(Modifier.fillMaxSize().padding(4.dp).border(1.dp, Color(0x24C9A227), RoundedCornerShape(9.dp)))
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Text(card.promo.label.removeSuffix(" TL"), color = p.text, fontSize = 26.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-1).sp, lineHeight = 26.sp, softWrap = false)
                    Spacer(Modifier.width(4.dp))
                    Text("TL", color = p.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, softWrap = false, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
    }
}

/** Pulsing halo behind the centre card while it waits to be flipped. */
@Composable
private fun CardGlow(color: Color, visible: Boolean, scale: Float, modifier: Modifier = Modifier) {
    val pulse = rememberInfiniteTransition(label = "glow").animateFloat(0.55f, 1f, infiniteRepeatable(tween(1500, easing = EaseInOut), RepeatMode.Reverse), label = "gp")
    val on by animateFloatAsState(if (visible) 1f else 0f, tween(500), label = "gon")
    if (on == 0f) return
    Canvas(modifier.size((CARD_W * scale * 2.6f).dp, (CARD_H * scale * 2.2f).dp).graphicsLayer { alpha = on * pulse.value; scaleX = 0.92f + 0.08f * pulse.value; scaleY = 0.92f + 0.08f * pulse.value }) {
        drawOval(Brush.radialGradient(listOf(color.copy(0.55f), color.copy(0.18f), color.copy(0f)), center, size.minDimension / 2 * 1.1f))
    }
}

/** A short white flash that expands from the card at the moment it turns. */
@Composable
private fun LightBurst(active: Boolean, color: Color, modifier: Modifier = Modifier) {
    val t = remember(active) { Animatable(0f) }
    LaunchedEffect(active) { if (active) { delay(Timing.FLIP / 2); t.animateTo(1f, tween(650, easing = LinearOutSlowInEasing)) } }
    Canvas(modifier.fillMaxSize()) {
        val tt = t.value
        if (tt == 0f || tt == 1f) return@Canvas
        val r = size.minDimension * (0.15f + 0.75f * tt)
        val a = (1 - tt) * 0.9f
        drawCircle(Brush.radialGradient(listOf(Color.White.copy(a), color.copy(a * 0.45f), Color.Transparent), center, r), r, center)
        // Eight short rays.
        for (k in 0 until 8) {
            val ang = Math.toRadians(k * 45.0 + 22.5)
            val inner = r * 0.55f; val outer = r * (0.9f + 0.3f * tt)
            drawLine(Color.White.copy(a * 0.7f), Offset(center.x + inner * Math.cos(ang).toFloat(), center.y + inner * Math.sin(ang).toFloat()), Offset(center.x + outer * Math.cos(ang).toFloat(), center.y + outer * Math.sin(ang).toFloat()), strokeWidth = 3f)
        }
    }
}

private suspend fun delay(ms: Long) = kotlinx.coroutines.delay(ms)

/**
 * The whole card stage: enter → shuffle → collapse → glow → flip.
 * Positions are dp offsets from the centre of the stage.
 */
@Composable
fun Stage(state: GameState, theme: SansinaTheme, cardBack: CardBack, onPick: (Int) -> Unit, onFlip: () -> Unit, modifier: Modifier = Modifier) {
    val phase = state.phase
    val n = state.cards.size
    val selecting = phase == Phase.SELECT
    val collapsed = state.collapsed || phase.ordinal >= Phase.READY.ordinal
    val centred = collapsed || phase.ordinal >= Phase.READY.ordinal

    BoxWithConstraints(modifier.fillMaxSize()) {
        val w = maxWidth.value
        val rowScale = (w / (n * (CARD_W + 44f))).coerceIn(1.2f, 2.0f)
        val gap = (CARD_W + 44f) * rowScale
        val centreScale = rowScale * 1.28f

        CardGlow(theme.accent, visible = phase == Phase.READY, scale = centreScale, Modifier.align(Alignment.Center))

        state.cards.forEachIndexed { i, card ->
            val isWinner = i == state.winner
            val slot = state.slots.getOrElse(i) { i }
            val visible = i in state.entered || phase.ordinal >= Phase.SHUFFLE.ordinal
            val gone = centred && !isWinner

            val targetX = if (centred && isWinner) 0f else (slot - (n - 1) / 2f) * gap
            val targetY = if (centred && isWinner) 0f else 16f
            val targetS = if (centred && isWinner) centreScale else rowScale
            // Slight fan while selecting (like the agency's card row), jitter while shuffling.
            val jitter = when {
                phase == Phase.SHUFFLE && !collapsed -> ((i * 37 + state.shuffleStep * 91) % 9 - 4).toFloat()
                selecting -> (slot - (n - 1) / 2f) * 3.2f
                else -> 0f
            }

            val moveSpec: AnimationSpec<Float> = if (phase == Phase.SHUFFLE && !collapsed) spring(0.78f, Spring.StiffnessMedium) else tween(Timing.COLLAPSE.toInt(), easing = SoftOut)
            val gx by animateFloatAsState(targetX, moveSpec, label = "gx")
            val gy by animateFloatAsState(targetY, moveSpec, label = "gy")
            val gs by animateFloatAsState(targetS, tween(Timing.COLLAPSE.toInt(), easing = SoftOut), label = "gs")
            val gr by animateFloatAsState(jitter, tween(300), label = "gr")

            // Entrance: fade + slide up, staggered by GameState.entered.
            val enter by animateFloatAsState(if (visible) 1f else 0f, tween(Timing.CARD_ENTER.toInt(), easing = SoftOut), label = "en")
            val loser by animateFloatAsState(if (gone) 0f else 1f, tween(Timing.COLLAPSE.toInt()), label = "la")

            val faceUp = isWinner && state.revealed
            val tappable = (selecting && i in state.entered) || (phase == Phase.READY && isWinner)

            GameCard(
                card, theme, faceUp = faceUp, back = cardBack, shine = selecting,
                modifier = Modifier.align(Alignment.Center).graphicsLayer {
                    translationX = gx * density
                    translationY = (gy + 40f * (1 - enter)) * density
                    scaleX = gs * (0.7f + 0.3f * loser); scaleY = gs * (0.7f + 0.3f * loser)
                    rotationZ = gr
                    alpha = enter * loser
                }.clickable(enabled = tappable, interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    if (selecting) onPick(i) else onFlip()
                }
            )
        }

        LightBurst(active = phase == Phase.REVEAL, color = theme.accent, Modifier.align(Alignment.Center))
    }
}
