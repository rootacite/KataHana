package com.acite.katahana.ui.board

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import com.acite.katahana.domain.StoneColor
import com.acite.katahana.ui.theme.Appearance
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
