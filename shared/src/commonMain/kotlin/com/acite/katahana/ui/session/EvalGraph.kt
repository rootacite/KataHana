package com.acite.katahana.ui.session

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acite.katahana.domain.EvalGraphMode
import com.acite.katahana.domain.EvalSample
import com.acite.katahana.domain.QualityStats
import com.acite.katahana.domain.TreeLayout
import com.acite.katahana.domain.advantage
import com.acite.katahana.domain.advantageTint
import com.acite.katahana.domain.yMaxFor
import com.acite.katahana.ui.Copy
import com.acite.katahana.ui.theme.HanaColors
import com.acite.katahana.ui.theme.StoneSwatch
import com.acite.katahana.ui.theme.hanaAppearance
import com.acite.katahana.ui.theme.hanaTokens
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.roundToInt

private val PlotHeight = 128.dp

@Composable
fun SessionTreeColumn(
    layout: TreeLayout,
    reviewing: Boolean,
    onGoToNode: (String) -> Unit,
    samples: List<EvalSample>,
    currentMoveNumber: Int,
    graphMode: EvalGraphMode,
    onGraphMode: (EvalGraphMode) -> Unit,
    stats: QualityStats,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        GameTreeCard(
            layout = layout,
            reviewing = reviewing,
            onGoToNode = onGoToNode,
            compact = true,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        )
        EvalGraphCard(
            samples = samples,
            currentMoveNumber = currentMoveNumber,
            mode = graphMode,
            onMode = onGraphMode,
            onSeek = onGoToNode,
            compact = true,
            modifier = Modifier.fillMaxWidth(),
        )
        QualityStatsCard(
            stats = stats,
            compact = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun EvalGraphCard(
    samples: List<EvalSample>,
    currentMoveNumber: Int,
    mode: EvalGraphMode,
    onMode: (EvalGraphMode) -> Unit,
    onSeek: (String) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val tokens = hanaTokens
    Column(
        modifier
            .clip(tokens.card)
            .background(HanaColors.bgPanel)
            .padding(if (compact) 10.dp else 14.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                if (mode == EvalGraphMode.Score) Copy.score else Copy.winrate,
                color = HanaColors.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            ModePill(Copy.score, selected = mode == EvalGraphMode.Score) {
                onMode(EvalGraphMode.Score)
            }
            Spacer(Modifier.width(6.dp))
            ModePill(Copy.winrate, selected = mode == EvalGraphMode.Winrate) {
                onMode(EvalGraphMode.Winrate)
            }
        }
        Spacer(Modifier.height(10.dp))
        EvalPlot(
            samples = samples,
            currentMoveNumber = currentMoveNumber,
            mode = mode,
            onSeek = onSeek,
            modifier = Modifier
                .fillMaxWidth()
                .height(PlotHeight)
                .clip(tokens.panel)
                .background(HanaColors.bgCard),
        )
    }
}

@Composable
private fun ModePill(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) HanaColors.accentPink else HanaColors.bgCard
    val fg = if (selected) Color.White else HanaColors.textDim
    Box(
        Modifier
            .clip(hanaTokens.capsule)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = fg, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun EvalPlot(
    samples: List<EvalSample>,
    currentMoveNumber: Int,
    mode: EvalGraphMode,
    onSeek: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val appearance = hanaAppearance
    val blackInk = curveInk(appearance.first)
    val whiteInk = curveInk(appearance.second)
    val measurer = rememberTextMeasurer()
    val labelStyle = remember {
        TextStyle(color = HanaColors.textDim, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
    val yMax = yMaxFor(samples, mode)
    val xMax = maxOf(
        samples.maxOfOrNull { it.moveNumber } ?: 0,
        currentMoveNumber,
        1,
    )
    Canvas(
        modifier.pointerInput(samples, xMax) {
            detectTapGestures { offset ->
                if (samples.isEmpty()) return@detectTapGestures
                val padL = 28.dp.toPx()
                val padR = 10.dp.toPx()
                val plotW = (size.width - padL - padR).coerceAtLeast(1f)
                val t = ((offset.x - padL) / plotW).coerceIn(0f, 1f)
                val move = (t * xMax).roundToInt()
                val nearest = samples.minByOrNull { abs(it.moveNumber - move) } ?: return@detectTapGestures
                onSeek(nearest.nodeId)
            }
        },
    ) {
        val padL = 28.dp.toPx()
        val padR = 10.dp.toPx()
        val padT = 8.dp.toPx()
        val padB = 18.dp.toPx()
        val plotW = (size.width - padL - padR).coerceAtLeast(1f)
        val plotH = (size.height - padT - padB).coerceAtLeast(1f)
        val zeroY = padT + plotH / 2f
        fun xOf(move: Int): Float = padL + (move.toFloat() / xMax) * plotW
        fun yOf(adv: Double): Float {
            val t = (adv / yMax).toFloat().coerceIn(-1f, 1f)
            return zeroY - t * (plotH / 2f)
        }
        fun tint(adv: Double): Color = lerp(whiteInk, blackInk, advantageTint(adv, yMax))

        drawLine(
            color = HanaColors.accentLilac.copy(alpha = 0.45f),
            start = Offset(padL, zeroY),
            end = Offset(padL + plotW, zeroY),
            strokeWidth = 1.2.dp.toPx(),
        )
        val currentX = xOf(currentMoveNumber)
        drawLine(
            color = HanaColors.accentPink.copy(alpha = 0.55f),
            start = Offset(currentX, padT),
            end = Offset(currentX, padT + plotH),
            strokeWidth = 1.1.dp.toPx(),
        )

        if (samples.size >= 2) {
            val stroke = 2.2.dp.toPx()
            val stepPx = 2.dp.toPx()
            for (i in 0 until samples.lastIndex) {
                val a = samples[i]
                val b = samples[i + 1]
                val ya = a.advantage(mode)
                val yb = b.advantage(mode)
                val p0 = Offset(xOf(a.moveNumber), yOf(ya))
                val p1 = Offset(xOf(b.moveNumber), yOf(yb))
                drawAdvantageSegment(
                    p0 = p0,
                    p1 = p1,
                    adv0 = ya,
                    adv1 = yb,
                    yMax = yMax,
                    zeroY = zeroY,
                    blackInk = blackInk,
                    whiteInk = whiteInk,
                    stroke = stroke,
                    stepPx = stepPx,
                )
            }
        } else if (samples.size == 1) {
            val s = samples[0]
            val p = Offset(xOf(s.moveNumber), yOf(s.advantage(mode)))
            drawCircle(tint(s.advantage(mode)), radius = 3.5.dp.toPx(), center = p)
        }

        val here = samples.lastOrNull { it.moveNumber <= currentMoveNumber }
            ?: samples.firstOrNull { it.moveNumber >= currentMoveNumber }
        if (here != null) {
            val p = Offset(xOf(here.moveNumber), yOf(here.advantage(mode)))
            drawCircle(Color.White.copy(alpha = 0.9f), radius = 5.dp.toPx(), center = p)
            drawCircle(tint(here.advantage(mode)), radius = 3.2.dp.toPx(), center = p)
            drawCircle(
                color = Color.White.copy(alpha = 0.7f),
                radius = 5.dp.toPx(),
                center = p,
                style = Stroke(width = 1.1.dp.toPx()),
            )
        }

        fun stamp(text: String, x: Float, y: Float, centerX: Boolean = false, centerY: Boolean = true) {
            val layout = measurer.measure(text, labelStyle)
            val left = if (centerX) x - layout.size.width / 2f else x
            val top = if (centerY) y - layout.size.height / 2f else y
            drawText(layout, topLeft = Offset(left, top))
        }
        stamp(formatAxis(yMax, mode), 4.dp.toPx(), padT + 2.dp.toPx(), centerY = false)
        stamp(formatAxis(0.0, mode), 4.dp.toPx(), zeroY)
        stamp(formatAxis(-yMax, mode), 4.dp.toPx(), padT + plotH - 2.dp.toPx() - 10.sp.toPx(), centerY = false)
        stamp("0", xOf(0), padT + plotH + 2.dp.toPx(), centerX = true, centerY = false)
        stamp(xMax.toString(), xOf(xMax), padT + plotH + 2.dp.toPx(), centerX = true, centerY = false)

        if (samples.isEmpty()) {
            val empty = measurer.measure(Copy.waitingForAnalysis, labelStyle)
            drawText(
                empty,
                topLeft = Offset(
                    padL + (plotW - empty.size.width) / 2f,
                    padT + (plotH - empty.size.height) / 2f,
                ),
            )
        }
    }
}

private fun DrawScope.drawAdvantageSegment(
    p0: Offset,
    p1: Offset,
    adv0: Double,
    adv1: Double,
    yMax: Double,
    zeroY: Float,
    blackInk: Color,
    whiteInk: Color,
    stroke: Float,
    stepPx: Float,
) {
    val dist = hypot((p1.x - p0.x).toDouble(), (p1.y - p0.y).toDouble()).toFloat()
    val steps = maxOf(1, ceil(dist / stepPx.coerceAtLeast(1f)).toInt())
    var prev = p0
    for (s in 1..steps) {
        val u = s / steps.toFloat()
        val cur = Offset(p0.x + (p1.x - p0.x) * u, p0.y + (p1.y - p0.y) * u)
        val adv = adv0 + (adv1 - adv0) * u
        val color = lerp(whiteInk, blackInk, advantageTint(adv, yMax))
        val fill = Path().apply {
            moveTo(prev.x, zeroY)
            lineTo(prev.x, prev.y)
            lineTo(cur.x, cur.y)
            lineTo(cur.x, zeroY)
            close()
        }
        drawPath(fill, color.copy(alpha = 0.18f))
        drawLine(color, prev, cur, strokeWidth = stroke, cap = StrokeCap.Round)
        prev = cur
    }
}

private fun curveInk(swatch: StoneSwatch): Color {
    val fill = swatch.fill
    val contrast = abs(luminance(fill) - luminance(HanaColors.bgCard))
    if (contrast >= 0.18f) return fill
    return if (swatch.light) swatch.rim else lerp(fill, swatch.hi, 0.65f)
}

private fun luminance(c: Color): Float =
    0.2126f * c.red + 0.7152f * c.green + 0.0722f * c.blue

private fun formatAxis(value: Double, mode: EvalGraphMode): String {
    if (abs(value) < 0.05) return if (mode == EvalGraphMode.Winrate) "0%" else "0"
    val sign = if (value > 0) "+" else "−"
    val mag = abs(value)
    val body = if (mode == EvalGraphMode.Winrate) {
        "${mag.roundToInt()}%"
    } else if (abs(mag - mag.roundToInt()) < 0.05) {
        mag.roundToInt().toString()
    } else {
        val tenths = (mag * 10.0).roundToInt()
        "${tenths / 10}.${tenths % 10}"
    }
    return sign + body
}
