package com.troy.sansina

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private const val CARD_W = 96f
private const val CARD_H = 134f

/** One card: product photo on the back, promo value on the front. */
@Composable
fun GameCard(card: Card, theme: SansinaTheme, faceUp: Boolean, modifier: Modifier = Modifier) {
    val rot by animateFloatAsState(if (faceUp) 180f else 0f, tween(620, easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)), label = "flip")
    val shape = RoundedCornerShape(12.dp)
    val ctx = LocalContext.current
    val bmp by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, card.product.file) { value = Catalogue.bitmap(ctx, card.product.file) }
    Box(modifier.size(CARD_W.dp, CARD_H.dp).graphicsLayer { rotationY = rot; cameraDistance = 12f * density }) {
        if (rot <= 90f) {
            Box(
                Modifier.fillMaxSize().shadow(10.dp, shape, ambientColor = Color(0x59000000), spotColor = Color(0x59000000))
                    .background(theme.productCardBg, shape).border(1.dp, theme.productCardBorder, shape).clip(shape).padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                bmp?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit) }
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
                Column(Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(card.promo.label.removeSuffix(" TL"), color = p.text, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.8).sp, lineHeight = 22.sp, softWrap = false)
                            Spacer(Modifier.width(3.dp))
                            Text("TL", color = p.text, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, softWrap = false, modifier = Modifier.padding(bottom = 3.dp))
                        }
                        Box(Modifier.padding(top = 7.dp).size(22.dp, 2.dp).background(p.rule, RoundedCornerShape(1.dp)))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                        Text("avantaj", color = p.cap, fontSize = 8.sp)
                        if (premium) Wordmark(GoldLight.copy(alpha = 0.72f), size = 9, tag = false)
                    }
                }
            }
        }
    }
}

private data class Pose(val x: Float, val y: Float, val scale: Float, val rot: Float)

/**
 * The whole card stage: orbit → fill → flip → shuffle → pick.
 * Positions are dp offsets from the centre of the stage.
 */
@Composable
fun Stage(state: GameState, theme: SansinaTheme, modifier: Modifier = Modifier) {
    val phase = state.phase
    val n = state.cards.size
    val orbiting = phase == Phase.INVITE || phase == Phase.FADE
    val picked = phase.ordinal >= Phase.PICK.ordinal

    val spin = rememberInfiniteTransition(label = "spin").animateFloat(0f, 360f, infiniteRepeatable(tween(70000, easing = LinearEasing)), label = "a")
    val orbitWeight by animateFloatAsState(if (orbiting) 1f else 0f, tween(1300, easing = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)), label = "ow")

    BoxWithConstraints(modifier.fillMaxSize()) {
        val w = maxWidth.value; val h = maxHeight.value
        // Orbit: one ring up to 24 cards, then alternate onto a second inner ring.
        val rings = if (n > 24) 2 else 1
        fun orbitPose(i: Int): Pose {
            val ring = if (rings == 2) i % 2 else 0
            val inRing = if (rings == 2) (n + 1 - ring) / 2 else n
            val k = if (rings == 2) i / 2 else i
            val f = if (ring == 0) 1f else 0.72f
            val a = (spin.value + (if (ring == 1) 180f / inRing else 0f) + 360f * k / inRing) * PI.toFloat() / 180f
            val rx = w * 0.43f * f; val ry = h * 0.335f * f
            val s = (if (ring == 0) 0.86f else 0.7f) * (if (n > 16) 0.9f else 1f)
            return Pose(rx * cos(a), ry * sin(a), s, 0f)
        }
        // Fill: tile the screen with balanced rows (20 → 7/7/6), keeping headroom for the caption.
        val rows = kotlin.math.floor(sqrt(n * (h / w))).toInt().coerceIn(1, n)
        val rowCounts = IntArray(rows) { r -> n / rows + if (r < n % rows) 1 else 0 }
        val rowStart = IntArray(rows).also { var acc = 0; for (r in 0 until rows) { it[r] = acc; acc += rowCounts[r] } }
        val cellH = (h - 200f) / rows
        val cellW = min((w - 64f) / rowCounts.max(), cellH * (CARD_W / CARD_H) * 1.5f)
        val fillScale = min(cellW / (CARD_W + 10f), cellH / (CARD_H + 10f)).coerceIn(0.5f, 1.6f)
        fun gridPose(slot: Int): Pose {
            var r = 0
            while (r < rows - 1 && slot >= rowStart[r + 1]) r++
            val c = slot - rowStart[r]
            val x = (c - (rowCounts[r] - 1) / 2f) * cellW
            val y = (r - (rows - 1) / 2f) * cellH + 8f
            return Pose(x, y, fillScale, 0f)
        }

        state.cards.forEachIndexed { i, card ->
            val isWinner = i == state.winner
            val slot = state.slots.getOrElse(i) { i }
            val g = gridPose(slot)
            val jitter = if (phase == Phase.SHUFFLE) ((i * 37 + state.shuffleStep * 91) % 13 - 6).toFloat() else 0f
            val targetX = if (picked && isWinner) 0f else g.x
            val targetY = if (picked && isWinner) -10f else g.y
            val targetS = if (picked && isWinner) 2.1f else g.scale
            val gx by animateFloatAsState(targetX, spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow), label = "gx")
            val gy by animateFloatAsState(targetY, spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow), label = "gy")
            val gs by animateFloatAsState(targetS, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow), label = "gs")
            val gr by animateFloatAsState(jitter, tween(320), label = "gr")
            val loserAlpha by animateFloatAsState(if (picked && !isWinner) 0f else 1f, tween(600), label = "la")
            val loserScale by animateFloatAsState(if (picked && !isWinner) 0.7f else 1f, tween(600), label = "ls")

            val o = orbitPose(i)
            val ow = orbitWeight
            val x = o.x * ow + gx * (1 - ow)
            val y = o.y * ow + gy * (1 - ow)
            val s = (o.scale * ow + gs * (1 - ow)) * loserScale
            val faceUp = (i in state.flipped) && !(picked && isWinner && !state.revealed) || (picked && isWinner && state.revealed)

            GameCard(
                card, theme, faceUp = faceUp,
                modifier = Modifier.align(Alignment.Center).graphicsLayer {
                    translationX = x * density
                    translationY = y * density
                    scaleX = s; scaleY = s
                    rotationZ = gr
                    alpha = loserAlpha
                }
            )
        }
    }
}
