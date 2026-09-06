package com.acite.katahana.ui.board

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import com.acite.katahana.ai.QualityBand
import com.acite.katahana.domain.Point
import com.acite.katahana.engine.OWNERSHIP_SKIP
import com.acite.katahana.settings.OwnershipStyle
import com.acite.katahana.ui.theme.HanaColors
import com.acite.katahana.ui.theme.StoneSwatch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

fun DrawScope.drawLastMoveMark(
    center: Offset,
    radius: Float,
    swatch: StoneSwatch,
    pulse: Float,
    breath: Float,
    alpha: Float = 1f,
) {
    val hot = swatch.rim
    val wash = swatch.fill
    val backing = if (swatch.light) Color(0xFF1A1228) else Color.White
    val a = alpha.coerceIn(0f, 1f)
    fun ripple(t: Float) {
        val u = t.coerceIn(0f, 1f)
        val fade = (1f - u) * (1f - 0.35f * u)
        drawCircle(
            color = wash.copy(alpha = fade * 0.82f * a),
            radius = radius * (1.16f + 0.62f * u),
            center = center,
            style = Stroke(width = (radius * (0.16f - 0.07f * u)).coerceAtLeast(2.0f)),
        )
        drawCircle(
            color = backing.copy(alpha = fade * 0.55f * a),
            radius = radius * (1.16f + 0.62f * u),
            center = center,
            style = Stroke(width = (radius * 0.055f).coerceAtLeast(1.4f)),
        )
    }
    ripple(pulse)
    ripple((pulse + 0.5f) % 1f)

    val ringR = radius * (1.18f + 0.05f * breath)
    val ringW = (radius * (0.18f + 0.05f * breath) * 0.4f).coerceAtLeast(1.12f)
    drawCircle(
        color = backing.copy(alpha = 0.98f * a),
        radius = ringR,
        center = center,
        style = Stroke(width = ringW * 1.7f),
    )
    drawCircle(
        color = hot.copy(alpha = (0.80f + 0.20f * breath) * a),
        radius = ringR,
        center = center,
        style = Stroke(width = ringW),
    )
}

class OwnershipMotion(
    val bounce: Float = 0f,
    val drift: Float = 0f,
    val breath: Float = 1f,
    val twinkle: Float = 0f,
) {
    companion object {
        val Still = OwnershipMotion()
    }
}

private const val TWO_PI = (PI * 2.0).toFloat()

fun DrawScope.drawOwnershipLayer(
    values: FloatArray,
    boardSize: Int,
    gap: Float,
    centerOf: (Point) -> Offset,
    style: OwnershipStyle = OwnershipStyle.Blocks,
    motion: OwnershipMotion = OwnershipMotion.Still,
    ownBlue: Color = HanaColors.accentBlue,
    ownPink: Color = HanaColors.accentPink,
) {
    if (values.size != boardSize * boardSize) return
    when (style) {
        OwnershipStyle.Blocks -> drawOwnershipBlocks(
            values, boardSize, gap, centerOf, motion.bounce, ownBlue, ownPink,
        )
        OwnershipStyle.Fog -> drawOwnershipFog(
            values, boardSize, gap, centerOf, motion.drift, ownBlue, ownPink,
        )
        OwnershipStyle.Constellation -> drawOwnershipConstellation(
            values, boardSize, gap, centerOf, motion.breath, motion.twinkle, ownBlue, ownPink,
        )
    }
}

private const val OWNERSHIP_ALPHA = 0.5f * 0.65f

private fun ownershipTint(v: Float, ownBlue: Color, ownPink: Color): Color =
    if (v >= 0f) ownBlue else ownPink

private fun Color.haze(alpha: Float): Color = copy(alpha = alpha * OWNERSHIP_ALPHA)

private fun DrawScope.drawOwnershipBlocks(
    values: FloatArray,
    boardSize: Int,
    gap: Float,
    centerOf: (Point) -> Offset,
    bounce: Float,
    ownBlue: Color,
    ownPink: Color,
) {
    val cell = gap * 0.82f
    val corner = cell * 0.28f
    for (y in 0 until boardSize) {
        for (x in 0 until boardSize) {
            val v = values[y * boardSize + x]
            if (abs(v) < OWNERSHIP_SKIP) continue
            val phase = fract(x * 0.173f + y * 0.311f + x * y * 0.019f)
            val wave = sin((bounce + phase) * TWO_PI)
            val scale = 1f + 0.035f * wave
            val lift = -gap * 0.0225f * wave
            val s = cell * scale
            val alpha = (0.10f + 0.48f * abs(v)).coerceIn(0.10f, 0.58f)
            val c = centerOf(Point(x, y))
            drawRoundRect(
                color = ownershipTint(v, ownBlue, ownPink).haze(alpha),
                topLeft = Offset(c.x - s / 2f, c.y - s / 2f + lift),
                size = Size(s, s),
                cornerRadius = CornerRadius(corner * scale, corner * scale),
            )
        }
    }
}

private const val FOG_STEPS = 8

private fun DrawScope.drawOwnershipFog(
    values: FloatArray,
    boardSize: Int,
    gap: Float,
    centerOf: (Point) -> Offset,
    drift: Float,
    ownBlue: Color,
    ownPink: Color,
) {
    if (boardSize < 2) return
    drawFogLayer(values, boardSize, gap, centerOf, drift, layer = 0, alphaScale = 1f, ownBlue, ownPink)
    drawFogLayer(values, boardSize, gap, centerOf, drift, layer = 1, alphaScale = 0.42f, ownBlue, ownPink)
}

private fun DrawScope.drawFogLayer(
    values: FloatArray,
    boardSize: Int,
    gap: Float,
    centerOf: (Point) -> Offset,
    drift: Float,
    layer: Int,
    alphaScale: Float,
    ownBlue: Color,
    ownPink: Color,
) {
    val tau = (drift + if (layer == 0) 0f else 0.37f) * TWO_PI
    val swayX = gap * 0.11f * sin(tau + layer * 1.7f)
    val swayY = gap * 0.09f * cos(tau * 0.73f + layer * 2.3f)
    val micro = gap / FOG_STEPS
    val pad = micro * 0.42f
    for (gy in 0 until boardSize - 1) {
        val c00y = centerOf(Point(0, gy)).y
        val c01y = centerOf(Point(0, gy + 1)).y
        for (gx in 0 until boardSize - 1) {
            val c00 = centerOf(Point(gx, gy))
            val c10 = centerOf(Point(gx + 1, gy))
            val originX = c00.x
            val spanX = c10.x - c00.x
            val originY = c00y
            val spanY = c01y - c00y
            for (iy in 0 until FOG_STEPS) {
                val v0 = (iy + 0.5f) / FOG_STEPS
                for (ix in 0 until FOG_STEPS) {
                    val u0 = (ix + 0.5f) / FOG_STEPS
                    val fx = gx + u0
                    val fy = gy + v0
                    val warped = fogWarp(fx, fy, drift, layer)
                    val sample = sampleField(values, boardSize, warped.x, warped.y)
                    val mag = abs(sample)
                    val density = smoothstep(0.05f, 0.82f, mag)
                    if (density <= 0.02f) continue
                    val px = originX + spanX * (ix / FOG_STEPS.toFloat()) + swayX
                    val py = originY + spanY * (iy / FOG_STEPS.toFloat()) + swayY
                    drawRect(
                        color = ownershipTint(sample, ownBlue, ownPink).haze(
                            (0.045f + 0.20f * density) * alphaScale,
                        ),
                        topLeft = Offset(px - pad, py - pad),
                        size = Size(micro + pad * 2f, micro + pad * 2f),
                    )
                }
            }
        }
    }
}

private fun fogWarp(x: Float, y: Float, drift: Float, layer: Int): Offset {
    val tau = drift * TWO_PI
    val phase = if (layer == 0) 0f else 2.15f
    val dx = 0.18f * sin(tau + y * 0.62f + phase) + 0.08f * sin(tau * 0.47f + x * 0.33f)
    val dy = 0.15f * cos(tau * 0.86f + x * 0.50f + phase) + 0.07f * sin(tau * 1.13f + y * 0.41f)
    return Offset(x + dx, y + dy)
}

private fun sampleField(values: FloatArray, size: Int, x: Float, y: Float): Float {
    val max = (size - 1).toFloat()
    val sx = x.coerceIn(0f, max)
    val sy = y.coerceIn(0f, max)
    val x0 = sx.toInt().coerceIn(0, size - 1)
    val y0 = sy.toInt().coerceIn(0, size - 1)
    val x1 = (x0 + 1).coerceAtMost(size - 1)
    val y1 = (y0 + 1).coerceAtMost(size - 1)
    val tx = sx - x0
    val ty = sy - y0
    val a = values[y0 * size + x0]
    val b = values[y0 * size + x1]
    val c = values[y1 * size + x0]
    val d = values[y1 * size + x1]
    val top = a + (b - a) * tx
    val bot = c + (d - c) * tx
    return top + (bot - top) * ty
}

private fun DrawScope.drawOwnershipConstellation(
    values: FloatArray,
    boardSize: Int,
    gap: Float,
    centerOf: (Point) -> Offset,
    breath: Float,
    twinkle: Float,
    ownBlue: Color,
    ownPink: Color,
) {
    val faceCut = 0.36f
    val lineCut = 0.26f
    val starCut = 0.12f
    val inhale = 0.86f + 0.14f * breath

    for (y in 0 until boardSize - 1) {
        for (x in 0 until boardSize - 1) {
            val a = values[y * boardSize + x]
            val b = values[y * boardSize + x + 1]
            val c = values[(y + 1) * boardSize + x]
            val d = values[(y + 1) * boardSize + x + 1]
            val mag = min(min(abs(a), abs(b)), min(abs(c), abs(d)))
            if (mag < faceCut) continue
            if (a * b <= 0f || a * c <= 0f || a * d <= 0f) continue
            val p0 = centerOf(Point(x, y))
            val p1 = centerOf(Point(x + 1, y + 1))
            drawRoundRect(
                color = ownershipTint(a, ownBlue, ownPink).haze((0.07f + 0.16f * mag) * inhale),
                topLeft = Offset(min(p0.x, p1.x), min(p0.y, p1.y)),
                size = Size(abs(p1.x - p0.x), abs(p1.y - p0.y)),
                cornerRadius = CornerRadius(gap * 0.18f, gap * 0.18f),
            )
        }
    }

    fun link(x0: Int, y0: Int, x1: Int, y1: Int) {
        val a = values[y0 * boardSize + x0]
        val b = values[y1 * boardSize + x1]
        if (a * b <= 0f) return
        val mag = min(abs(a), abs(b))
        if (mag < lineCut) return
        val phase = fract(x0 * 0.21f + y0 * 0.34f + x1 * 0.17f + y1 * 0.09f)
        val pulse = inhale * (0.82f + 0.18f * flash(twinkle, phase))
        drawLine(
            color = ownershipTint(a, ownBlue, ownPink).haze((0.22f + 0.50f * mag) * pulse),
            start = centerOf(Point(x0, y0)),
            end = centerOf(Point(x1, y1)),
            strokeWidth = ((1.3f + 1.7f * mag) * inhale).coerceAtLeast(1.1f),
            cap = StrokeCap.Round,
        )
    }
    for (y in 0 until boardSize) {
        for (x in 0 until boardSize) {
            if (x + 1 < boardSize) link(x, y, x + 1, y)
            if (y + 1 < boardSize) link(x, y, x, y + 1)
            if (x + 1 < boardSize && y + 1 < boardSize) {
                val a = values[y * boardSize + x]
                val d = values[(y + 1) * boardSize + x + 1]
                val b = values[y * boardSize + x + 1]
                val c = values[(y + 1) * boardSize + x]
                if (abs(a) + abs(d) > abs(b) + abs(c)) link(x, y, x + 1, y + 1)
            }
        }
    }

    for (y in 0 until boardSize) {
        for (x in 0 until boardSize) {
            val v = values[y * boardSize + x]
            val mag = abs(v)
            if (mag < starCut) continue
            val c = centerOf(Point(x, y))
            val tint = ownershipTint(v, ownBlue, ownPink)
            val phase = fract(x * 0.41f + y * 0.27f + 0.13f * x * y)
            val spark = flash(twinkle, phase)
            val r = gap * (0.07f + 0.16f * mag) * (0.88f + 0.16f * breath)
            val glow = spark * inhale
            drawCircle(color = tint.haze(0.20f * glow), radius = r * 2.6f, center = c)
            drawCircle(
                color = tint.haze((0.50f + 0.45f * mag) * glow),
                radius = r * (0.92f + 0.16f * spark),
                center = c,
            )
            if (mag >= 0.58f) {
                val arm = r * (1.7f + 0.8f * spark)
                val white = Color.White.haze((0.28f + 0.50f * mag) * spark * inhale)
                val w = (r * 0.35f).coerceAtLeast(1f)
                drawLine(white, Offset(c.x - arm, c.y), Offset(c.x + arm, c.y), w, StrokeCap.Round)
                drawLine(white, Offset(c.x, c.y - arm), Offset(c.x, c.y + arm), w, StrokeCap.Round)
            }
        }
    }
}

private fun fract(v: Float): Float {
    val r = v - kotlin.math.floor(v.toDouble()).toFloat()
    return if (r < 0f) r + 1f else r
}

private fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
    val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

private fun flash(t: Float, phase: Float): Float {
    val u = fract(t + phase)
    return 0.72f + 0.28f * peak(u, 0.06f)
}

private fun peak(u: Float, width: Float): Float {
    val x = (1f - u / width).coerceIn(0f, 1f)
    return x * x
}

fun DrawScope.drawHoshi(center: Offset, radius: Float, color: Color = HanaColors.star) {
    drawCircle(
        color = color,
        radius = radius,
        center = center,
    )
}

/** KaTrain eval_thresholds: 12, 6, 3, 1.5, 0.5, 0 — purple → green. */
fun candidateColorForLoss(pointsLost: Double): Color = when {
    pointsLost >= 12.0 -> HanaColors.qualityPurple
    pointsLost >= 6.0 -> HanaColors.qualityRed
    pointsLost >= 3.0 -> HanaColors.qualityOrange
    pointsLost >= 1.5 -> HanaColors.qualityYellow
    pointsLost >= 0.5 -> HanaColors.qualityMint
    else -> HanaColors.qualityGreen
}

fun candidateLabelColor(pointsLost: Double): Color =
    if (pointsLost >= 0.5 && pointsLost < 3.0) Color(0xFF1A1228) else Color.White

fun qualityDotColor(band: QualityBand, shallow: Color = HanaColors.accentLilac): Color = when (band) {
    QualityBand.Blunder -> HanaColors.qualityPurple
    QualityBand.BigMistake -> HanaColors.qualityRed
    QualityBand.Mistake -> HanaColors.qualityOrange
    QualityBand.Inaccuracy -> HanaColors.qualityYellow
    QualityBand.Fair -> HanaColors.qualityMint
    QualityBand.Good -> HanaColors.qualityGreen
    QualityBand.Shallow -> shallow.copy(alpha = 0.55f)
}

fun DrawScope.drawQualityDot(center: Offset, stoneRadius: Float, color: Color) {
    val r = stoneRadius * 0.46f
    drawCircle(color = color.copy(alpha = 0.92f), radius = r, center = center)
    drawCircle(
        color = Color.White.copy(alpha = 0.95f),
        radius = r,
        center = center,
        style = Stroke(width = (r * 0.12f).coerceAtLeast(1.8f)),
    )
}

fun DrawScope.drawCandidate(
    center: Offset,
    radius: Float,
    color: Color,
    label: TextLayoutResult,
) {
    drawCircle(color = color.copy(alpha = 0.78f), radius = radius, center = center)
    drawCircle(
        color = color.copy(alpha = 0.95f),
        radius = radius,
        center = center,
        style = Stroke(width = (radius * 0.08f).coerceAtLeast(1.2f)),
    )
    drawText(
        textLayoutResult = label,
        topLeft = Offset(
            center.x - label.size.width / 2f,
            center.y - label.size.height / 2f,
        ),
    )
}
