package com.acite.katahana.ui.session

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acite.katahana.settings.AnalysisColWeights
import com.acite.katahana.domain.EvalGraphMode
import com.acite.katahana.domain.EvalSample
import com.acite.katahana.domain.QualityStats
import com.acite.katahana.domain.TreeLayout
import com.acite.katahana.domain.advantage
import com.acite.katahana.domain.advantageTint
import com.acite.katahana.domain.yMaxFor
import com.acite.katahana.ui.Copy
import com.acite.katahana.ui.components.FrostedSurface
import com.acite.katahana.ui.theme.StoneSwatch
import com.acite.katahana.ui.theme.hanaAppearance
import com.acite.katahana.ui.theme.hanaColors
import com.acite.katahana.ui.theme.hanaFontFamily
import com.acite.katahana.ui.theme.hanaTokens
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.roundToInt

private val PlotHeight = 128.dp
private val PlotPadL = 40.dp
private val PlotPadR = 14.dp

private enum class AnalysisTab { Tree, Score, Quality }

@Composable
fun SessionAnalysisPane(
    layout: TreeLayout,
    reviewing: Boolean,
    onGoToNode: (String) -> Unit,
    samples: List<EvalSample>,
    currentMoveNumber: Int,
    graphMode: EvalGraphMode,
    onGraphMode: (EvalGraphMode) -> Unit,
    stats: QualityStats,
    modifier: Modifier = Modifier,
    tabbed: Boolean = false,
    columns: Boolean = false,
    weights: AnalysisColWeights = AnalysisColWeights.Default,
    onWeightsChange: (AnalysisColWeights) -> Unit = {},
    onWeightsCommit: () -> Unit = {},
) {
    if (tabbed) {
        TabbedAnalysisPane(
            layout = layout,
            reviewing = reviewing,
            onGoToNode = onGoToNode,
            samples = samples,
            currentMoveNumber = currentMoveNumber,
            graphMode = graphMode,
            onGraphMode = onGraphMode,
            stats = stats,
            modifier = modifier,
        )
        return
    }
    if (columns) {
        AnalysisColumns(
            layout = layout,
            reviewing = reviewing,
            onGoToNode = onGoToNode,
            samples = samples,
            currentMoveNumber = currentMoveNumber,
            graphMode = graphMode,
            onGraphMode = onGraphMode,
            stats = stats,
            weights = weights,
            onWeightsChange = onWeightsChange,
            onWeightsCommit = onWeightsCommit,
            modifier = modifier,
        )
    } else {
        AnalysisRows(
            layout = layout,
            reviewing = reviewing,
            onGoToNode = onGoToNode,
            samples = samples,
            currentMoveNumber = currentMoveNumber,
            graphMode = graphMode,
            onGraphMode = onGraphMode,
            stats = stats,
            weights = weights,
            onWeightsChange = onWeightsChange,
            onWeightsCommit = onWeightsCommit,
            modifier = modifier,
        )
    }
}

@Composable
private fun AnalysisRows(
    layout: TreeLayout,
    reviewing: Boolean,
    onGoToNode: (String) -> Unit,
    samples: List<EvalSample>,
    currentMoveNumber: Int,
    graphMode: EvalGraphMode,
    onGraphMode: (EvalGraphMode) -> Unit,
    stats: QualityStats,
    weights: AnalysisColWeights,
    onWeightsChange: (AnalysisColWeights) -> Unit,
    onWeightsCommit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val latestWeights by rememberUpdatedState(weights)
    val latestChange by rememberUpdatedState(onWeightsChange)
    BoxWithConstraints(modifier) {
        val totalPx = with(density) { (maxHeight - SessionResizeHandleWidth * 2).toPx() }
        val minPx = with(density) { SessionTreeRowMinSlice.toPx() }
        Column(Modifier.fillMaxSize()) {
            GameTreeCard(
                layout = layout,
                reviewing = reviewing,
                onGoToNode = onGoToNode,
                compact = true,
                modifier = Modifier
                    .weight(weights.tree)
                    .fillMaxWidth(),
            )
            AnalysisResizeHandle(
                vertical = true,
                onDrag = { dy ->
                    latestChange(
                        applyAnalysisSplitterDrag(latestWeights, 0, dy, totalPx, minPx),
                    )
                },
                onDragEnd = onWeightsCommit,
            )
            EvalGraphCard(
                samples = samples,
                currentMoveNumber = currentMoveNumber,
                mode = graphMode,
                onMode = onGraphMode,
                onSeek = onGoToNode,
                compact = true,
                expandPlot = true,
                modifier = Modifier
                    .weight(weights.graph)
                    .fillMaxWidth(),
            )
            AnalysisResizeHandle(
                vertical = true,
                onDrag = { dy ->
                    latestChange(
                        applyAnalysisSplitterDrag(latestWeights, 1, dy, totalPx, minPx),
                    )
                },
                onDragEnd = onWeightsCommit,
            )
            QualityStatsCard(
                stats = stats,
                compact = true,
                modifier = Modifier
                    .weight(weights.quality)
                    .fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun AnalysisColumns(
    layout: TreeLayout,
    reviewing: Boolean,
    onGoToNode: (String) -> Unit,
    samples: List<EvalSample>,
    currentMoveNumber: Int,
    graphMode: EvalGraphMode,
    onGraphMode: (EvalGraphMode) -> Unit,
    stats: QualityStats,
    weights: AnalysisColWeights,
    onWeightsChange: (AnalysisColWeights) -> Unit,
    onWeightsCommit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val latestWeights by rememberUpdatedState(weights)
    val latestChange by rememberUpdatedState(onWeightsChange)
    BoxWithConstraints(modifier) {
        val totalPx = with(density) { (maxWidth - SessionResizeHandleWidth * 2).toPx() }
        val minPx = with(density) { SessionTreeColMinWidth.toPx() }
        Row(Modifier.fillMaxSize()) {
            GameTreeCard(
                layout = layout,
                reviewing = reviewing,
                onGoToNode = onGoToNode,
                compact = true,
                modifier = Modifier
                    .weight(weights.tree)
                    .fillMaxHeight(),
            )
            AnalysisResizeHandle(
                onDrag = { dx ->
                    latestChange(
                        applyAnalysisSplitterDrag(latestWeights, 0, dx, totalPx, minPx),
                    )
                },
                onDragEnd = onWeightsCommit,
            )
            EvalGraphCard(
                samples = samples,
                currentMoveNumber = currentMoveNumber,
                mode = graphMode,
                onMode = onGraphMode,
                onSeek = onGoToNode,
                compact = true,
                expandPlot = true,
                modifier = Modifier
                    .weight(weights.graph)
                    .fillMaxHeight(),
            )
            AnalysisResizeHandle(
                onDrag = { dx ->
                    latestChange(
                        applyAnalysisSplitterDrag(latestWeights, 1, dx, totalPx, minPx),
                    )
                },
                onDragEnd = onWeightsCommit,
            )
            QualityStatsCard(
                stats = stats,
                compact = true,
                modifier = Modifier
                    .weight(weights.quality)
                    .fillMaxHeight(),
            )
        }
    }
}

@Composable
internal fun AnalysisResizeHandle(
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier,
    vertical: Boolean = false,
) {
    val tokens = hanaTokens
    val latestDrag by rememberUpdatedState(onDrag)
    val latestEnd by rememberUpdatedState(onDragEnd)
    Box(
        modifier
            .then(
                if (vertical) {
                    Modifier
                        .fillMaxWidth()
                        .height(SessionResizeHandleWidth)
                } else {
                    Modifier
                        .width(SessionResizeHandleWidth)
                        .fillMaxHeight()
                },
            )
            .pointerInput(vertical) {
                if (vertical) {
                    detectVerticalDragGestures(
                        onDragEnd = { latestEnd() },
                        onDragCancel = { latestEnd() },
                        onVerticalDrag = { change, dy ->
                            change.consume()
                            latestDrag(dy)
                        },
                    )
                } else {
                    detectHorizontalDragGestures(
                        onDragEnd = { latestEnd() },
                        onDragCancel = { latestEnd() },
                        onHorizontalDrag = { change, dx ->
                            change.consume()
                            latestDrag(dx)
                        },
                    )
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .then(
                    if (vertical) {
                        Modifier
                            .width(28.dp)
                            .height(2.dp)
                    } else {
                        Modifier
                            .width(2.dp)
                            .height(28.dp)
                    },
                )
                .clip(tokens.capsule)
                .background(hanaColors.accentPink.copy(alpha = 0.55f)),
        )
    }
}

@Composable
private fun TabbedAnalysisPane(
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
    var tab by rememberSaveable { mutableStateOf(AnalysisTab.Tree) }
    FrostedSurface(modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 10.dp, end = 10.dp, top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ModePill(
                Copy.gameTree,
                selected = tab == AnalysisTab.Tree,
                modifier = Modifier.weight(1f),
            ) {
                tab = AnalysisTab.Tree
            }
            ModePill(
                Copy.score,
                selected = tab == AnalysisTab.Score,
                modifier = Modifier.weight(1f),
            ) {
                tab = AnalysisTab.Score
            }
            ModePill(
                Copy.qualityTab,
                selected = tab == AnalysisTab.Quality,
                modifier = Modifier.weight(1f),
            ) {
                tab = AnalysisTab.Quality
            }
        }
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(start = 10.dp, end = 10.dp, bottom = 10.dp),
        ) {
            when (tab) {
                AnalysisTab.Tree -> GameTreeCard(
                    layout = layout,
                    reviewing = reviewing,
                    onGoToNode = onGoToNode,
                    compact = true,
                    framed = false,
                    modifier = Modifier.fillMaxSize(),
                )
                AnalysisTab.Score -> EvalGraphCard(
                    samples = samples,
                    currentMoveNumber = currentMoveNumber,
                    mode = graphMode,
                    onMode = onGraphMode,
                    onSeek = onGoToNode,
                    compact = true,
                    expandPlot = true,
                    framed = false,
                    modifier = Modifier.fillMaxSize(),
                )
                AnalysisTab.Quality -> QualityStatsCard(
                    stats = stats,
                    compact = true,
                    framed = false,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
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
    expandPlot: Boolean = false,
    framed: Boolean = true,
) {
    val tokens = hanaTokens
    val colors = hanaColors
    val body: @Composable () -> Unit = {
        Column(
            Modifier
                .then(if (expandPlot) Modifier.fillMaxHeight() else Modifier)
                .padding(if (compact && framed) 10.dp else if (framed) 14.dp else 0.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (framed) {
                    Text(
                        if (mode == EvalGraphMode.Score) Copy.score else Copy.winrate,
                        color = colors.text,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
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
                    .then(if (expandPlot) Modifier.weight(1f) else Modifier.height(PlotHeight))
                    .clip(tokens.panel)
                    .background(colors.bgCard),
            )
        }
    }
    if (framed) {
        FrostedSurface(modifier) { body() }
    } else {
        Box(modifier) { body() }
    }
}

@Composable
private fun ModePill(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = hanaColors
    val bg = if (selected) colors.accentPink.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.05f)
    val border = if (selected) colors.accentPink.copy(alpha = 0.40f) else Color.White.copy(alpha = 0.10f)
    val fg = colors.text
    Box(
        modifier
            .clip(hanaTokens.capsule)
            .background(bg)
            .border(1.dp, border, hanaTokens.capsule)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = fg,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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
    val colors = hanaColors
    val blackInk = curveInk(appearance.first, colors.bgCard)
    val whiteInk = curveInk(appearance.second, colors.bgCard)
    val measurer = rememberTextMeasurer()
    val fonts = hanaFontFamily
    val labelStyle = TextStyle(
        color = colors.textDim,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        fontFamily = fonts,
    )
    val tagStyle = TextStyle(
        color = colors.text,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = fonts,
    )
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
                val padL = PlotPadL.toPx()
                val padR = PlotPadR.toPx()
                val plotW = (size.width - padL - padR).coerceAtLeast(1f)
                val t = ((offset.x - padL) / plotW).coerceIn(0f, 1f)
                val move = (t * xMax).roundToInt()
                val nearest = samples.minByOrNull { abs(it.moveNumber - move) } ?: return@detectTapGestures
                onSeek(nearest.nodeId)
            }
        },
    ) {
        val padL = PlotPadL.toPx()
        val padR = PlotPadR.toPx()
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
            color = colors.accentLilac.copy(alpha = 0.45f),
            start = Offset(padL, zeroY),
            end = Offset(padL + plotW, zeroY),
            strokeWidth = 1.2.dp.toPx(),
        )
        val currentX = xOf(currentMoveNumber)
        drawLine(
            color = colors.accentPink.copy(alpha = 0.55f),
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
            drawLiveTag(
                text = formatLive(here, mode),
                anchor = p,
                measurer = measurer,
                style = tagStyle,
                zeroY = zeroY,
                plotLeft = padL,
                plotRight = padL + plotW,
                plotTop = padT,
                plotBottom = padT + plotH,
                card = colors.bgCard,
                stroke = colors.stroke,
            )
        }

        fun stamp(text: String, x: Float, y: Float, centerX: Boolean = false, centerY: Boolean = true) {
            val layout = measurer.measure(text, labelStyle)
            val left = if (centerX) x - layout.size.width / 2f else x
            val top = if (centerY) y - layout.size.height / 2f else y
            drawText(layout, topLeft = Offset(left, top))
        }
        val topTick = if (mode == EvalGraphMode.Winrate) "B 100%" else formatAxis(yMax)
        val midTick = if (mode == EvalGraphMode.Winrate) "50%" else formatAxis(0.0)
        val botTick = if (mode == EvalGraphMode.Winrate) "W 100%" else formatAxis(-yMax)
        stamp(topTick, 4.dp.toPx(), padT + 2.dp.toPx(), centerY = false)
        stamp(midTick, 4.dp.toPx(), zeroY)
        stamp(botTick, 4.dp.toPx(), padT + plotH - 2.dp.toPx() - 10.sp.toPx(), centerY = false)
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

private fun curveInk(swatch: StoneSwatch, card: Color): Color {
    val fill = swatch.fill
    val contrast = abs(luminance(fill) - luminance(card))
    if (contrast >= 0.18f) return fill
    return if (swatch.light) swatch.rim else lerp(fill, swatch.hi, 0.65f)
}

private fun luminance(c: Color): Float =
    0.2126f * c.red + 0.7152f * c.green + 0.0722f * c.blue

private fun DrawScope.drawLiveTag(
    text: String,
    anchor: Offset,
    measurer: TextMeasurer,
    style: TextStyle,
    zeroY: Float,
    plotLeft: Float,
    plotRight: Float,
    plotTop: Float,
    plotBottom: Float,
    card: Color,
    stroke: Color,
) {
    val layout = measurer.measure(text, style)
    val padX = 6.dp.toPx()
    val padY = 3.dp.toPx()
    val tagW = layout.size.width + padX * 2
    val tagH = layout.size.height + padY * 2
    val gap = 8.dp.toPx()
    var x = anchor.x - tagW / 2f
    if (x < plotLeft) x = plotLeft
    if (x + tagW > plotRight) x = (plotRight - tagW).coerceAtLeast(plotLeft)
    val preferAbove = anchor.y <= zeroY
    val aboveY = anchor.y - gap - tagH
    val belowY = anchor.y + gap
    var y = if (preferAbove) aboveY else belowY
    if (y < plotTop) y = belowY
    if (y + tagH > plotBottom) y = aboveY
    if (y < plotTop) y = plotTop
    if (y + tagH > plotBottom) y = plotBottom - tagH
    val origin = Offset(x, y)
    val box = Size(tagW, tagH)
    val radius = CornerRadius(6.dp.toPx())
    drawRoundRect(card.copy(alpha = 0.92f), origin, box, radius)
    drawRoundRect(
        color = stroke.copy(alpha = 0.55f),
        topLeft = origin,
        size = box,
        cornerRadius = radius,
        style = Stroke(width = 1.dp.toPx()),
    )
    drawText(layout, topLeft = Offset(x + padX, y + padY))
}

private fun formatLive(sample: EvalSample, mode: EvalGraphMode): String = when (mode) {
    EvalGraphMode.Winrate -> "${(sample.blackWinrate * 100.0).roundToInt().coerceIn(0, 100)}%"
    EvalGraphMode.Score -> formatAxis(sample.blackScoreLead)
}

private fun formatAxis(value: Double): String {
    if (abs(value) < 0.05) return "0"
    val sign = if (value > 0) "+" else "−"
    val mag = abs(value)
    val body = if (abs(mag - mag.roundToInt()) < 0.05) {
        mag.roundToInt().toString()
    } else {
        val tenths = (mag * 10.0).roundToInt()
        "${tenths / 10}.${tenths % 10}"
    }
    return sign + body
}
