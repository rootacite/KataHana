package com.acite.katahana.ui.board

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.acite.katahana.domain.LinkKind
import com.acite.katahana.domain.Point
import com.acite.katahana.domain.StoneLink
import com.acite.katahana.ui.theme.Appearance
import kotlin.math.hypot

fun DrawScope.drawStoneLinks(
    links: List<StoneLink>,
    centerOf: (Point) -> Offset,
    stoneR: Float,
    appearance: Appearance,
    growFrom: Point? = null,
    progress: Float = 1f,
) {
    for (link in links) {
        val swatch = appearance.swatch(link.color)
        val t = if (growFrom != null && (link.a == growFrom || link.b == growFrom)) {
            progress.coerceIn(0f, 1f)
        } else {
            1f
        }
        if (t <= 0.001f) continue
        val origin: Point
        val dest: Point
        if (growFrom != null && (link.a == growFrom || link.b == growFrom)) {
            origin = if (link.a == growFrom) link.b else link.a
            dest = growFrom
        } else {
            origin = link.a
            dest = link.b
        }
        val from = centerOf(origin)
        val to = centerOf(dest)
        when (link.kind) {
            LinkKind.Nobi -> drawNobi(from, to, stoneR, swatch.fill, swatch.hi, t)
            LinkKind.Kosumi -> drawKosumi(from, to, stoneR, swatch.fill, swatch.rim, t)
            LinkKind.Tobi -> {
                val mid = link.via.firstOrNull()?.let(centerOf)
                drawTobi(from, to, mid, stoneR, swatch.fill, swatch.hi, t)
            }
            LinkKind.Keima -> {
                val elbow = link.via.firstOrNull()?.let(centerOf)
                drawKeima(from, to, elbow, stoneR, swatch.fill, swatch.rim, t)
            }
        }
    }
}

private fun DrawScope.drawNobi(
    a: Offset,
    b: Offset,
    stoneR: Float,
    fill: Color,
    hi: Color,
    t: Float,
) {
    val (from, to) = trimmed(a, b, stoneR * 0.92f)
    val path = Path().apply {
        moveTo(from.x, from.y)
        lineTo(to.x, to.y)
    }
    drawPartial(path, fill.copy(alpha = 0.22f), stoneR * 0.42f, t)
    drawPartial(path, fill.copy(alpha = 0.88f), stoneR * 0.16f, t)
    drawPartial(path, hi.copy(alpha = 0.55f), stoneR * 0.06f, t)
}

private fun DrawScope.drawKosumi(
    a: Offset,
    b: Offset,
    stoneR: Float,
    fill: Color,
    rim: Color,
    t: Float,
) {
    val (from, to) = trimmed(a, b, stoneR * 0.88f)
    val path = Path().apply {
        moveTo(from.x, from.y)
        lineTo(to.x, to.y)
    }
    val dash = PathEffect.dashPathEffect(floatArrayOf(stoneR * 0.28f, stoneR * 0.20f), 0f)
    drawPartial(path, fill.copy(alpha = 0.90f), stoneR * 0.12f, t, dash)
    drawPartial(path, rim.copy(alpha = 0.55f), stoneR * 0.06f, t)
}

private fun DrawScope.drawTobi(
    a: Offset,
    b: Offset,
    mid: Offset?,
    stoneR: Float,
    fill: Color,
    hi: Color,
    t: Float,
) {
    val (from, to) = trimmed(a, b, stoneR * 0.90f)
    val peak = tobiPeak(from, to, mid, stoneR)
    val path = Path().apply {
        moveTo(from.x, from.y)
        quadraticTo(peak.x, peak.y, to.x, to.y)
    }
    drawPartial(path, fill.copy(alpha = 0.28f), stoneR * 0.28f, t)
    drawPartial(path, fill.copy(alpha = 0.92f), stoneR * 0.10f, t)
    if (mid != null && t > 0.55f) {
        val fade = ((t - 0.55f) / 0.45f).coerceIn(0f, 1f)
        drawCircle(fill.copy(alpha = 0.55f * fade), stoneR * 0.16f, mid, style = Stroke(stoneR * 0.07f))
        drawCircle(hi.copy(alpha = 0.50f * fade), stoneR * 0.06f, mid)
    }
}

private fun DrawScope.drawKeima(
    a: Offset,
    b: Offset,
    elbow: Offset?,
    stoneR: Float,
    fill: Color,
    rim: Color,
    t: Float,
) {
    val joint = elbow ?: Offset((a.x + b.x) / 2f, (a.y + b.y) / 2f)
    val dash = PathEffect.dashPathEffect(floatArrayOf(stoneR * 0.22f, stoneR * 0.16f), 0f)
    val (a2, j1) = trimmed(a, joint, stoneR * 0.88f)
    val (j2, b2) = trimmed(joint, b, stoneR * 0.88f)
    val path = Path().apply {
        moveTo(a2.x, a2.y)
        lineTo(j1.x, j1.y)
        lineTo(j2.x, j2.y)
        lineTo(b2.x, b2.y)
    }
    drawPartial(path, fill.copy(alpha = 0.90f), stoneR * 0.11f, t, dash, StrokeJoin.Round)
    if (t > 0.45f) {
        val fade = ((t - 0.45f) / 0.55f).coerceIn(0f, 1f)
        drawCircle(fill.copy(alpha = 0.90f * fade), stoneR * 0.14f, joint)
        drawCircle(rim.copy(alpha = 0.95f * fade), stoneR * 0.07f, joint)
    }
}

private fun DrawScope.drawPartial(
    path: Path,
    color: Color,
    width: Float,
    t: Float,
    dash: PathEffect? = null,
    join: StrokeJoin = StrokeJoin.Round,
) {
    val stroke = Stroke(width = width, cap = StrokeCap.Round, join = join, pathEffect = dash)
    if (t >= 0.999f) {
        drawPath(path, color, style = stroke)
        return
    }
    val measure = PathMeasure()
    measure.setPath(path, false)
    val dest = Path()
    measure.getSegment(0f, measure.length * t, dest, true)
    drawPath(dest, color, style = stroke)
}

private fun tobiPeak(from: Offset, to: Offset, mid: Offset?, stoneR: Float): Offset {
    val bulge = mid ?: Offset((from.x + to.x) / 2f, (from.y + to.y) / 2f)
    var dx = to.x - from.x
    var dy = to.y - from.y
    if (dx < 0f || (dx == 0f && dy < 0f)) {
        dx = -dx
        dy = -dy
    }
    val len = hypot(dx, dy).coerceAtLeast(1f)
    val nx = dy / len
    val ny = -dx / len
    return Offset(bulge.x + nx * stoneR * 0.55f, bulge.y + ny * stoneR * 0.55f)
}

private fun trimmed(a: Offset, b: Offset, inset: Float): Pair<Offset, Offset> {
    val dx = b.x - a.x
    val dy = b.y - a.y
    val len = hypot(dx, dy).coerceAtLeast(1f)
    val ux = dx / len
    val uy = dy / len
    val cut = inset.coerceAtMost(len * 0.42f)
    return Offset(a.x + ux * cut, a.y + uy * cut) to Offset(b.x - ux * cut, b.y - uy * cut)
}
