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

// Client QR (brand/reference/figma-assets/qr-plate.svg) — encodes https://appy.to/troy1000.
private val QrRows = arrayOf(
    "11111110101011000010101111111",
    "10000010001011101011001000001",
    "10111010000010100011101011101",
    "10111010011001001101101011101",
    "10111010101110100001101011101",
    "10000010101101111010001000001",
    "11111110101010101010101111111",
    "00000000000001000000000000000",
    "01111111011001001010100110001",
    "10111001000110100100001010001",
    "11100010100001010010111010100",
    "11001100101001001000011011010",
    "11001111110110111110110100111",
    "01010001101100000111111011001",
    "00110110001011001100110100000",
    "01011101001001100110010011010",
    "01101010001100101000010101100",
    "11000100001111010110101011111",
    "10111011100010011101001111100",
    "10001000100101110011101011010",
    "10111011100101110101111111111",
    "00000000111010100000100011001",
    "11111110111000111011101010000",
    "10000010100011101001100011001",
    "10111010100100100010111111101",
    "10111010100110111001110100100",
    "10111010100010100001111111010",
    "10000010111110001010010010010",
    "11111110011000100100010011100",
)

/** The client's redemption QR, drawn module-for-module from their Figma asset. */
@Composable
fun QrGrid(dark: Color, light: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.background(light, RoundedCornerShape(12.dp)).padding(10.dp)) {
        val n = QrRows.size
        val m = size.minDimension / n
        for (y in 0 until n) for (x in 0 until n) {
            if (QrRows[y][x] == '1') drawRect(dark, Offset(x * m, y * m), Size(m + 0.5f, m + 0.5f))
        }
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
fun Caption(text: String, color: Color, size: Int = 18, weight: FontWeight = FontWeight.Normal, modifier: Modifier = Modifier, fontFamily: androidx.compose.ui.text.font.FontFamily? = null) {
    Text(text, color = color, fontSize = size.sp, fontWeight = weight, fontFamily = fontFamily, textAlign = TextAlign.Center, modifier = modifier)
}
