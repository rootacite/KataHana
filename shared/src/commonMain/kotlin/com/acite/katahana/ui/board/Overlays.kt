package com.acite.katahana.ui.board

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import com.acite.katahana.ai.QualityBand
import com.acite.katahana.ui.theme.HanaColors

fun DrawScope.drawLastMoveMark(
    center: Offset,
    radius: Float,
    lightStone: Boolean,
    pulse: Float,
    breath: Float,
) {
    val hot = HanaColors.accentPink
    val backing = if (lightStone) Color(0xFF1A1228) else Color.White
    fun ripple(t: Float) {
        val u = t.coerceIn(0f, 1f)
        val fade = (1f - u) * (1f - 0.35f * u)
        drawCircle(
            color = hot.copy(alpha = fade * 0.82f),
            radius = radius * (1.16f + 0.62f * u),
            center = center,
            style = Stroke(width = (radius * (0.16f - 0.07f * u)).coerceAtLeast(2.0f)),
        )
        drawCircle(
            color = backing.copy(alpha = fade * 0.55f),
            radius = radius * (1.16f + 0.62f * u),
            center = center,
            style = Stroke(width = (radius * 0.055f).coerceAtLeast(1.4f)),
        )
    }
    ripple(pulse)
    ripple((pulse + 0.5f) % 1f)

    val ringR = radius * (1.18f + 0.05f * breath)
    val ringW = (radius * (0.18f + 0.05f * breath)).coerceAtLeast(2.8f)
    drawCircle(
        color = backing.copy(alpha = 0.98f),
        radius = ringR,
        center = center,
        style = Stroke(width = ringW * 1.7f),
    )
    drawCircle(
        color = hot.copy(alpha = 0.80f + 0.20f * breath),
        radius = ringR,
        center = center,
        style = Stroke(width = ringW),
    )
}

fun DrawScope.drawHoshi(center: Offset, radius: Float) {
    drawCircle(
        color = HanaColors.star,
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

fun qualityDotColor(band: QualityBand): Color = when (band) {
    QualityBand.Blunder -> HanaColors.qualityPurple
    QualityBand.BigMistake -> HanaColors.qualityRed
    QualityBand.Mistake -> HanaColors.qualityOrange
    QualityBand.Inaccuracy -> HanaColors.qualityYellow
    QualityBand.Fair -> HanaColors.qualityMint
    QualityBand.Good -> HanaColors.qualityGreen
    QualityBand.Shallow -> HanaColors.accentLilac.copy(alpha = 0.55f)
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
