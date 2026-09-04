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

private val FaceInk = Color(0xFF1A1228)
private val Tongue = Color(0xFFE85D7A)
private val Thought = Color(0xFF3A3460)

fun DrawScope.drawQualityFace(center: Offset, stoneRadius: Float, band: QualityBand) {
    val r = stoneRadius * 0.70f
    val fill = qualityDotColor(band)
    drawCircle(color = fill, radius = r, center = center)
    drawCircle(
        color = Color(0xFF1A1228).copy(alpha = 0.35f),
        radius = r,
        center = center,
        style = Stroke(width = (r * 0.07f).coerceAtLeast(1.2f)),
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.95f),
        radius = r,
        center = center,
        style = Stroke(width = (r * 0.13f).coerceAtLeast(2f)),
    )
    val ink = FaceInk.copy(alpha = if (band == QualityBand.Shallow) 0.55f else 0.92f)
    val stroke = Stroke(
        width = (r * 0.11f).coerceAtLeast(1.6f),
        cap = StrokeCap.Round,
        join = StrokeJoin.Round,
    )
    when (band) {
        QualityBand.Good -> drawHappyChevrons(center, r, ink, stroke)
        QualityBand.Fair -> drawCaretSmile(center, r, ink, stroke)
        QualityBand.Inaccuracy -> drawThinkFace(center, r, ink, stroke)
        QualityBand.Mistake -> drawFlatFace(center, r, ink, stroke)
        QualityBand.BigMistake -> drawUnamusedFace(center, r, ink, stroke)
        QualityBand.Blunder -> drawSickFace(center, r, ink, stroke)
        QualityBand.Shallow -> drawDottedFace(center, r, ink, stroke)
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
    color: Color,
    stroke: Stroke,
) {
    drawLine(
        color = color,
        start = pt(c, r, x1, y1),
        end = pt(c, r, x2, y2),
        strokeWidth = stroke.width,
        cap = stroke.cap,
    )
}

/** >v< */
private fun DrawScope.drawHappyChevrons(c: Offset, r: Float, ink: Color, stroke: Stroke) {
    seg(c, r, -0.46f, -0.30f, -0.22f, -0.10f, ink, stroke)
    seg(c, r, -0.22f, -0.10f, -0.46f, 0.10f, ink, stroke)
    seg(c, r, 0.46f, -0.30f, 0.22f, -0.10f, ink, stroke)
    seg(c, r, 0.22f, -0.10f, 0.46f, 0.10f, ink, stroke)
    seg(c, r, -0.20f, 0.28f, 0f, 0.46f, ink, stroke)
    seg(c, r, 0f, 0.46f, 0.20f, 0.28f, ink, stroke)
}

/** ^_^ */
private fun DrawScope.drawCaretSmile(c: Offset, r: Float, ink: Color, stroke: Stroke) {
    seg(c, r, -0.42f, -0.04f, -0.28f, -0.28f, ink, stroke)
    seg(c, r, -0.28f, -0.28f, -0.14f, -0.04f, ink, stroke)
    seg(c, r, 0.14f, -0.04f, 0.28f, -0.28f, ink, stroke)
    seg(c, r, 0.28f, -0.28f, 0.42f, -0.04f, ink, stroke)
    seg(c, r, -0.16f, 0.30f, 0.16f, 0.30f, ink, stroke)
}

/** thinking: offset gaze + thought bubbles */
private fun DrawScope.drawThinkFace(c: Offset, r: Float, ink: Color, stroke: Stroke) {
    drawCircle(ink, r * 0.09f, pt(c, r, -0.22f, -0.10f))
    drawCircle(ink, r * 0.09f, pt(c, r, 0.18f, -0.20f))
    val mouth = Path().apply {
        moveTo(pt(c, r, -0.12f, 0.28f).x, pt(c, r, -0.12f, 0.28f).y)
        quadraticTo(pt(c, r, 0.04f, 0.40f).x, pt(c, r, 0.04f, 0.40f).y, pt(c, r, 0.20f, 0.26f).x, pt(c, r, 0.20f, 0.26f).y)
    }
    drawPath(mouth, ink, style = stroke)
    drawCircle(Thought.copy(alpha = 0.55f), r * 0.07f, pt(c, r, 0.52f, -0.38f))
    drawCircle(Thought.copy(alpha = 0.70f), r * 0.10f, pt(c, r, 0.68f, -0.58f))
    drawCircle(Thought.copy(alpha = 0.85f), r * 0.14f, pt(c, r, 0.86f, -0.82f))
}

/** -_- */
private fun DrawScope.drawFlatFace(c: Offset, r: Float, ink: Color, stroke: Stroke) {
    seg(c, r, -0.42f, -0.12f, -0.16f, -0.12f, ink, stroke)
    seg(c, r, 0.16f, -0.12f, 0.42f, -0.12f, ink, stroke)
    seg(c, r, -0.18f, 0.30f, 0.18f, 0.30f, ink, stroke)
}

/** unamused: heavy lids + frown */
private fun DrawScope.drawUnamusedFace(c: Offset, r: Float, ink: Color, stroke: Stroke) {
    seg(c, r, -0.44f, -0.18f, -0.14f, -0.10f, ink, stroke)
    seg(c, r, 0.14f, -0.10f, 0.44f, -0.18f, ink, stroke)
    drawCircle(ink, r * 0.055f, pt(c, r, -0.28f, -0.02f))
    drawCircle(ink, r * 0.055f, pt(c, r, 0.28f, -0.02f))
    val frown = Path().apply {
        moveTo(pt(c, r, -0.20f, 0.36f).x, pt(c, r, -0.20f, 0.36f).y)
        quadraticTo(pt(c, r, 0f, 0.18f).x, pt(c, r, 0f, 0.18f).y, pt(c, r, 0.20f, 0.36f).x, pt(c, r, 0.20f, 0.36f).y)
    }
    drawPath(frown, ink, style = stroke)
}

/** sick: X eyes, open mouth, tongue */
private fun DrawScope.drawSickFace(c: Offset, r: Float, ink: Color, stroke: Stroke) {
    seg(c, r, -0.40f, -0.28f, -0.16f, -0.04f, ink, stroke)
    seg(c, r, -0.40f, -0.04f, -0.16f, -0.28f, ink, stroke)
    seg(c, r, 0.16f, -0.28f, 0.40f, -0.04f, ink, stroke)
    seg(c, r, 0.16f, -0.04f, 0.40f, -0.28f, ink, stroke)
    val mouth = Rect(pt(c, r, -0.22f, 0.10f), pt(c, r, 0.22f, 0.42f))
    drawOval(ink.copy(alpha = 0.18f), topLeft = mouth.topLeft, size = mouth.size)
    drawOval(ink, topLeft = mouth.topLeft, size = mouth.size, style = stroke)
    val tongue = Path().apply {
        val a = pt(c, r, -0.10f, 0.32f)
        val b = pt(c, r, 0.10f, 0.32f)
        val tip = pt(c, r, 0.02f, 0.62f)
        moveTo(a.x, a.y)
        quadraticTo(tip.x, tip.y, b.x, b.y)
        close()
    }
    drawPath(tongue, Tongue, style = Fill)
    drawPath(tongue, ink, style = stroke)
}

private fun DrawScope.drawDottedFace(c: Offset, r: Float, ink: Color, stroke: Stroke) {
    drawCircle(ink, r * 0.08f, pt(c, r, -0.22f, -0.10f))
    drawCircle(ink, r * 0.08f, pt(c, r, 0.22f, -0.10f))
    val wave = Path().apply {
        moveTo(pt(c, r, -0.22f, 0.28f).x, pt(c, r, -0.22f, 0.28f).y)
        quadraticTo(pt(c, r, -0.08f, 0.18f).x, pt(c, r, -0.08f, 0.18f).y, pt(c, r, 0f, 0.28f).x, pt(c, r, 0f, 0.28f).y)
        quadraticTo(pt(c, r, 0.08f, 0.38f).x, pt(c, r, 0.08f, 0.38f).y, pt(c, r, 0.22f, 0.28f).x, pt(c, r, 0.22f, 0.28f).y)
    }
    drawPath(wave, ink, style = stroke)
}
