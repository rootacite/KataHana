package com.acite.katahana.ui.board

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import com.acite.katahana.domain.StoneColor
import com.acite.katahana.ui.theme.Appearance
import com.acite.katahana.ui.theme.HanaColors
import com.acite.katahana.ui.theme.StoneSwatch

fun DrawScope.drawStone(
    color: StoneColor,
    appearance: Appearance,
    center: Offset,
    radius: Float,
    squashY: Float = 1f,
    alpha: Float = 1f,
) {
    drawStoneSwatch(appearance.swatch(color), center, radius, squashY, alpha)
}

fun DrawScope.drawStoneSwatch(
    swatch: StoneSwatch,
    center: Offset,
    radius: Float,
    squashY: Float = 1f,
    alpha: Float = 1f,
) {
    scale(scaleX = 1f, scaleY = squashY, pivot = center) {
        val hi = Offset(center.x - radius * 0.30f, center.y - radius * 0.35f)
        val hiMix = if (swatch.light) 1f else 0.55f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    swatch.hi.copy(alpha = hiMix * alpha),
                    swatch.fill.copy(alpha = alpha),
                ),
                center = hi,
                radius = radius * 1.4f,
            ),
            radius = radius,
            center = center,
        )
        drawCircle(
            color = swatch.rim.copy(alpha = 0.92f * alpha),
            radius = radius,
            center = center,
            style = Stroke(width = radius * 0.07f),
        )
        drawOval(
            color = swatch.hi.copy(alpha = (if (swatch.light) 0.7f else 0.38f) * alpha),
            topLeft = Offset(center.x - radius * 0.47f, center.y - radius * 0.57f),
            size = Size(radius * 0.71f, radius * 0.41f),
        )
    }
}

fun DrawScope.drawForecastStone(
    color: StoneColor,
    appearance: Appearance,
    center: Offset,
    radius: Float,
    label: TextLayoutResult,
    origin: Boolean,
) {
    val swatch = appearance.swatch(color)
    val r = radius * 0.90f
    val fillAlpha = if (origin) 0.34f else 0.28f
    val strokeW = r * (if (origin) 0.12f else 0.08f)
    drawCircle(color = swatch.fill.copy(alpha = fillAlpha), radius = r, center = center)
    drawCircle(
        color = swatch.rim.copy(alpha = 0.92f),
        radius = r,
        center = center,
        style = Stroke(width = strokeW.coerceAtLeast(1.6f)),
    )
    drawCircle(
        color = (if (swatch.light) Color(0xFF1A1228) else Color.White).copy(alpha = 0.22f),
        radius = r * 0.72f,
        center = center,
        style = Stroke(width = (r * 0.035f).coerceAtLeast(1f)),
    )
    drawText(
        textLayoutResult = label,
        topLeft = Offset(
            center.x - label.size.width / 2f,
            center.y - label.size.height / 2f,
        ),
    )
}

fun DrawScope.drawForecastCaptureMark(center: Offset, radius: Float, lightStone: Boolean) {
    val ink = if (lightStone) Color(0xFF1A1228) else Color.White
    val arm = radius * 0.38f
    val stroke = Stroke(width = (radius * 0.10f).coerceAtLeast(1.6f), cap = StrokeCap.Round)
    drawLine(
        color = ink.copy(alpha = 0.72f),
        start = Offset(center.x - arm, center.y - arm),
        end = Offset(center.x + arm, center.y + arm),
        strokeWidth = stroke.width,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = ink.copy(alpha = 0.72f),
        start = Offset(center.x + arm, center.y - arm),
        end = Offset(center.x - arm, center.y + arm),
        strokeWidth = stroke.width,
        cap = StrokeCap.Round,
    )
}

fun DrawScope.drawForecastLoading(center: Offset, radius: Float, pulse: Float) {
    val t = pulse.coerceIn(0f, 1f)
    val r = radius * (0.78f + 0.10f * t)
    drawCircle(
        color = HanaColors.accentPink.copy(alpha = 0.18f + 0.16f * t),
        radius = r,
        center = center,
    )
    drawCircle(
        color = HanaColors.accentPink.copy(alpha = 0.85f),
        radius = r,
        center = center,
        style = Stroke(width = (radius * 0.10f).coerceAtLeast(1.8f)),
    )
}
