package com.troy.sansina

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** TROY wordmark with the "+artısı var" tag. Text-based stand-in for the SVG. */
@Composable
fun Wordmark(color: Color, size: Int = 34, tag: Boolean = true, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.Top) {
        Text(
            "TROY",
            color = color,
            fontSize = size.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-1.5).sp,
            lineHeight = size.sp
        )
        if (tag) {
            Column(Modifier.padding(start = 3.dp, top = 2.dp)) {
                Text("+artısı", color = color, fontSize = (size * 0.30f).sp, fontWeight = FontWeight.Medium, lineHeight = (size * 0.32f).sp)
                Text("  var", color = color, fontSize = (size * 0.30f).sp, fontWeight = FontWeight.Medium, lineHeight = (size * 0.32f).sp)
            }
        }
    }
}

/** Four-point TROY sparkle. */
fun DrawScope.sparkle(center: Offset, r: Float, color: Color) {
    val p = Path()
    val k = r * 0.22f
    p.moveTo(center.x, center.y - r)
    p.quadraticTo(center.x + k, center.y - k, center.x + r, center.y)
    p.quadraticTo(center.x + k, center.y + k, center.x, center.y + r)
    p.quadraticTo(center.x - k, center.y + k, center.x - r, center.y)
    p.quadraticTo(center.x - k, center.y - k, center.x, center.y - r)
    p.close()
    drawPath(p, color)
}

/** Drawn module grid; placeholder for a scannable QR. */
@Composable
fun QrGrid(dark: Color, light: Color, modifier: Modifier = Modifier) {
    val rnd = remember { Random(7) }
    val bits = remember { Array(21) { BooleanArray(21) { rnd.nextFloat() < 0.45f } } }
    Canvas(modifier.background(light, RoundedCornerShape(12.dp)).padding(10.dp)) {
        val n = 21
        val m = size.minDimension / n
        fun finder(ox: Int, oy: Int) {
            for (y in 0 until 7) for (x in 0 until 7) {
                val edge = x == 0 || y == 0 || x == 6 || y == 6
                val core = x in 2..4 && y in 2..4
                if (edge || core) drawRect(dark, Offset((ox + x) * m, (oy + y) * m), Size(m, m))
            }
        }
        for (y in 0 until n) for (x in 0 until n) {
            val inFinder = (x < 8 && y < 8) || (x > n - 9 && y < 8) || (x < 8 && y > n - 9)
            if (!inFinder && bits[y][x]) drawRect(dark, Offset(x * m, y * m), Size(m, m))
        }
        finder(0, 0); finder(n - 7, 0); finder(0, n - 7)
    }
}

private data class Piece(val x: Float, val y0: Float, val vy: Float, val vx: Float, val rot: Float, val color: Color, val w: Float, val h: Float)

/** Confetti burst, fires once when [active] becomes true. */
@Composable
fun Confetti(active: Boolean, colors: List<Color>, modifier: Modifier = Modifier) {
    val pieces = remember(active) {
        val r = Random(active.hashCode())
        List(if (active) 90 else 0) {
            val a = r.nextFloat() * Math.PI.toFloat() * 2
            val sp = 0.35f + r.nextFloat() * 0.65f
            Piece(0.5f, 0.45f, sin(a) * sp, cos(a) * sp * 0.6f, r.nextFloat() * 360f, colors[r.nextInt(colors.size)], 6f + r.nextFloat() * 8f, 10f + r.nextFloat() * 10f)
        }
    }
    val t = remember(active) { Animatable(0f) }
    LaunchedEffect(active) { if (active) t.animateTo(1f, tween(1800, easing = LinearOutSlowInEasing)) }
    Canvas(modifier) {
        val tt = t.value
        if (tt == 0f) return@Canvas
        pieces.forEach { p ->
            val x = (p.x + p.vx * tt) * size.width
            val y = (p.y0 + p.vy * tt + 0.9f * tt * tt) * size.height
            rotate(p.rot + tt * 540f, Offset(x, y)) {
                drawRect(p.color.copy(alpha = (1 - tt).coerceIn(0f, 1f)), Offset(x - p.w, y - p.h), Size(p.w * 2, p.h * 2))
            }
        }
    }
}

@Composable
fun Caption(text: String, color: Color, size: Int = 18, weight: FontWeight = FontWeight.Normal, modifier: Modifier = Modifier) {
    Text(text, color = color, fontSize = size.sp, fontWeight = weight, textAlign = TextAlign.Center, modifier = modifier)
}
