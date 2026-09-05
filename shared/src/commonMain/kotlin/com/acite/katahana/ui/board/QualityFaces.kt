package com.acite.katahana.ui.board

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.acite.katahana.ai.QualityBand
import com.acite.katahana.ui.theme.HanaColors

fun DrawScope.drawQualityFace(center: Offset, stoneRadius: Float, band: QualityBand) {
    val r = stoneRadius * 0.96f
    val cap = StrokeCap.Round
    val join = StrokeJoin.Round
    val colorW = (r * 0.18f).coerceAtLeast(2.6f)
    val extra = ((r * 0.07f).coerceAtLeast(2.2f)) * 0.40f
    val haloW = colorW + extra
    val outline = FacePen(
        color = Color.White,
        stroke = Stroke(width = haloW, cap = cap, join = join),
        pad = extra * 0.5f,
        fill = false,
    )
    val ink = FacePen(
        color = qualityDotColor(band),
        stroke = Stroke(width = colorW, cap = cap, join = join),
        pad = 0f,
        fill = true,
    )
    paintFace(center, r, band, outline)
    paintFace(center, r, band, ink)
}

private class FacePen(
    val color: Color,
    val stroke: Stroke,
    val pad: Float,
    val fill: Boolean,
)

private fun DrawScope.paintFace(center: Offset, r: Float, band: QualityBand, pen: FacePen) {
    when (band) {
        QualityBand.Good -> drawHappyChevrons(center, r, pen)
        QualityBand.Fair -> drawCaretSmile(center, r, pen)
        QualityBand.Inaccuracy -> drawThinkFace(center, r, pen)
        QualityBand.Mistake -> drawFlatFace(center, r, pen)
        QualityBand.BigMistake -> drawUnamusedFace(center, r, pen)
        QualityBand.Blunder -> drawSickFace(center, r, pen)
        QualityBand.Shallow -> drawDottedFace(center, r, pen)
    }
}

private fun DrawScope.pt(c: Offset, r: Float, x: Float, y: Float) =
    Offset(c.x + x * r, c.y + y * r)

private fun DrawScope.seg(
    c: Offset,
    r: Float,
    x1: Float,
    y1: Float,
    x2: Float,
    y2: Float,
    pen: FacePen,
) {
    drawLine(
        color = pen.color,
        start = pt(c, r, x1, y1),
        end = pt(c, r, x2, y2),
        strokeWidth = pen.stroke.width,
        cap = pen.stroke.cap,
    )
}

private fun DrawScope.dot(
    c: Offset,
    r: Float,
    x: Float,
    y: Float,
    pen: FacePen,
    radius: Float,
    alpha: Float = 1f,
) {
    drawCircle(pen.color.copy(alpha = alpha), radius + pen.pad, pt(c, r, x, y))
}

/** >v< */
private fun DrawScope.drawHappyChevrons(c: Offset, r: Float, pen: FacePen) {
    seg(c, r, -0.46f, -0.30f, -0.22f, -0.10f, pen)
    seg(c, r, -0.22f, -0.10f, -0.46f, 0.10f, pen)
    seg(c, r, 0.46f, -0.30f, 0.22f, -0.10f, pen)
    seg(c, r, 0.22f, -0.10f, 0.46f, 0.10f, pen)
    seg(c, r, -0.20f, 0.28f, 0f, 0.46f, pen)
    seg(c, r, 0f, 0.46f, 0.20f, 0.28f, pen)
}

/** ^_^ */
private fun DrawScope.drawCaretSmile(c: Offset, r: Float, pen: FacePen) {
    seg(c, r, -0.42f, -0.04f, -0.28f, -0.28f, pen)
    seg(c, r, -0.28f, -0.28f, -0.14f, -0.04f, pen)
    seg(c, r, 0.14f, -0.04f, 0.28f, -0.28f, pen)
    seg(c, r, 0.28f, -0.28f, 0.42f, -0.04f, pen)
    seg(c, r, -0.16f, 0.30f, 0.16f, 0.30f, pen)
}

/** thinking: offset gaze + thought bubbles */
private fun DrawScope.drawThinkFace(c: Offset, r: Float, pen: FacePen) {
    dot(c, r, -0.22f, -0.10f, pen, r * 0.09f)
    dot(c, r, 0.18f, -0.20f, pen, r * 0.09f)
    val mouth = Path().apply {
        moveTo(pt(c, r, -0.12f, 0.28f).x, pt(c, r, -0.12f, 0.28f).y)
        quadraticTo(pt(c, r, 0.04f, 0.40f).x, pt(c, r, 0.04f, 0.40f).y, pt(c, r, 0.20f, 0.26f).x, pt(c, r, 0.20f, 0.26f).y)
    }
    drawPath(mouth, pen.color, style = pen.stroke)
    dot(c, r, 0.52f, -0.38f, pen, r * 0.07f, 0.55f)
    dot(c, r, 0.68f, -0.58f, pen, r * 0.10f, 0.70f)
    dot(c, r, 0.86f, -0.82f, pen, r * 0.14f, 0.85f)
}

/** -_- */
private fun DrawScope.drawFlatFace(c: Offset, r: Float, pen: FacePen) {
    seg(c, r, -0.42f, -0.12f, -0.16f, -0.12f, pen)
    seg(c, r, 0.16f, -0.12f, 0.42f, -0.12f, pen)
    seg(c, r, -0.18f, 0.30f, 0.18f, 0.30f, pen)
}

/** unamused: heavy lids + frown */
private fun DrawScope.drawUnamusedFace(c: Offset, r: Float, pen: FacePen) {
    seg(c, r, -0.44f, -0.18f, -0.14f, -0.10f, pen)
    seg(c, r, 0.14f, -0.10f, 0.44f, -0.18f, pen)
    dot(c, r, -0.28f, -0.02f, pen, r * 0.055f)
    dot(c, r, 0.28f, -0.02f, pen, r * 0.055f)
    val frown = Path().apply {
        moveTo(pt(c, r, -0.20f, 0.36f).x, pt(c, r, -0.20f, 0.36f).y)
        quadraticTo(pt(c, r, 0f, 0.18f).x, pt(c, r, 0f, 0.18f).y, pt(c, r, 0.20f, 0.36f).x, pt(c, r, 0.20f, 0.36f).y)
    }
    drawPath(frown, pen.color, style = pen.stroke)
}

/** sick: X eyes, open mouth, tongue */
private fun DrawScope.drawSickFace(c: Offset, r: Float, pen: FacePen) {
    seg(c, r, -0.40f, -0.28f, -0.16f, -0.04f, pen)
    seg(c, r, -0.40f, -0.04f, -0.16f, -0.28f, pen)
    seg(c, r, 0.16f, -0.28f, 0.40f, -0.04f, pen)
    seg(c, r, 0.16f, -0.04f, 0.40f, -0.28f, pen)
    val mouth = Rect(pt(c, r, -0.22f, 0.10f), pt(c, r, 0.22f, 0.42f))
    if (pen.fill) {
        drawOval(pen.color.copy(alpha = 0.18f), topLeft = mouth.topLeft, size = mouth.size)
    }
    drawOval(pen.color, topLeft = mouth.topLeft, size = mouth.size, style = pen.stroke)
    val tongue = Path().apply {
        val a = pt(c, r, -0.10f, 0.32f)
        val b = pt(c, r, 0.10f, 0.32f)
        val tip = pt(c, r, 0.02f, 0.62f)
        moveTo(a.x, a.y)
        quadraticTo(tip.x, tip.y, b.x, b.y)
        close()
    }
    if (pen.fill) {
        drawPath(tongue, pen.color.copy(alpha = 0.85f), style = Fill)
    }
    drawPath(tongue, pen.color, style = pen.stroke)
}

fun DrawScope.drawDeadFace(center: Offset, stoneRadius: Float, alpha: Float = 1f) {
    val a = alpha.coerceIn(0f, 1f)
    if (a <= 0.01f) return
    val r = stoneRadius * 0.96f
    val cap = StrokeCap.Round
    val join = StrokeJoin.Round
    val colorW = (r * 0.18f).coerceAtLeast(2.6f)
    val haloW = (colorW + r * 0.07f).coerceAtLeast(colorW + 2.2f)
    val outline = FacePen(
        color = Color.White.copy(alpha = a),
        stroke = Stroke(width = haloW, cap = cap, join = join),
        pad = (haloW - colorW) * 0.5f,
        fill = false,
    )
    val ink = FacePen(
        color = HanaColors.accentLilac.copy(alpha = a),
        stroke = Stroke(width = colorW, cap = cap, join = join),
        pad = 0f,
        fill = true,
    )
    paintHaloFace(center, r, outline)
    paintHaloFace(center, r, ink)
}

/** Dot eyes, small o mouth, and a compact halo just above the eyes. */
private fun DrawScope.paintHaloFace(c: Offset, r: Float, pen: FacePen) {
    val haloCenter = pt(c, r, 0f, -0.46f)
    drawCircle(
        color = pen.color,
        radius = r * 0.16f + pen.pad,
        center = haloCenter,
        style = Stroke(width = pen.stroke.width, cap = pen.stroke.cap, join = pen.stroke.join),
    )
    val eyeA = pen.color.alpha
    dot(c, r, -0.22f, -0.02f, pen, r * 0.08f, eyeA)
    dot(c, r, 0.22f, -0.02f, pen, r * 0.08f, eyeA)
    val mouth = Rect(pt(c, r, -0.11f, 0.22f), pt(c, r, 0.11f, 0.42f))
    drawOval(pen.color, topLeft = mouth.topLeft, size = mouth.size, style = pen.stroke)
}

private fun DrawScope.drawDottedFace(c: Offset, r: Float, pen: FacePen) {
    dot(c, r, -0.22f, -0.10f, pen, r * 0.08f)
    dot(c, r, 0.22f, -0.10f, pen, r * 0.08f)
    val wave = Path().apply {
        moveTo(pt(c, r, -0.22f, 0.28f).x, pt(c, r, -0.22f, 0.28f).y)
        quadraticTo(pt(c, r, -0.08f, 0.18f).x, pt(c, r, -0.08f, 0.18f).y, pt(c, r, 0f, 0.28f).x, pt(c, r, 0f, 0.28f).y)
        quadraticTo(pt(c, r, 0.08f, 0.38f).x, pt(c, r, 0.08f, 0.38f).y, pt(c, r, 0.22f, 0.28f).x, pt(c, r, 0.22f, 0.28f).y)
    }
    drawPath(wave, pen.color, style = pen.stroke)
}
