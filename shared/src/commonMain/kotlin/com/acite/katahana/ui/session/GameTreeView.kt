package com.acite.katahana.ui.session

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.floor
import kotlin.math.roundToInt
import com.acite.katahana.domain.TreeLayout
import com.acite.katahana.domain.TreeLayoutNode
import com.acite.katahana.ui.Copy
import com.acite.katahana.ui.components.FrostedSurface
import com.acite.katahana.ui.theme.hanaAppearance
import com.acite.katahana.ui.theme.hanaColors
import com.acite.katahana.ui.theme.hanaTokens

private val CellW = 40.dp
private val CellH = 48.dp

@Composable
fun GameTreeCard(
    layout: TreeLayout,
    reviewing: Boolean,
    onGoToNode: (String) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    framed: Boolean = true,
) {
    val colors = hanaColors
    val body: @Composable () -> Unit = {
        Column(
            Modifier
                .then(if (compact) Modifier.fillMaxHeight() else Modifier)
                .padding(if (compact && framed) 10.dp else if (framed) 14.dp else 0.dp),
        ) {
            if (framed || reviewing) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (framed) {
                        Text(
                            Copy.gameTree,
                            color = colors.text,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                        )
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                    if (reviewing) {
                        Text(
                            Copy.review,
                            color = colors.accentPink,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                        )
                    }
                }
            }
            if (reviewing) {
                Spacer(Modifier.height(6.dp))
                Text(Copy.reviewHint, color = colors.textDim, fontSize = 11.sp, lineHeight = 14.sp)
            }
            if (framed || reviewing) {
                Spacer(Modifier.height(10.dp))
            }
            GameTreeGraph(
                layout = layout,
                onGoToNode = onGoToNode,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (compact) Modifier.weight(1f) else Modifier.height(220.dp)),
            )
        }
    }
    if (framed) {
        FrostedSurface(modifier.then(if (compact) Modifier.fillMaxHeight() else Modifier)) {
            body()
        }
    } else {
        Box(modifier) { body() }
    }
}

@Composable
private fun GameTreeGraph(
    layout: TreeLayout,
    onGoToNode: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val appearance = hanaAppearance
    val colors = hanaColors
    val density = LocalDensity.current
    val goTo by rememberUpdatedState(onGoToNode)
    val tree by rememberUpdatedState(layout)
    var pan by remember { mutableStateOf(Offset.Zero) }
    val width = CellW * layout.cols.coerceAtLeast(1)
    val height = CellH * layout.rows.coerceAtLeast(1)
    val byId = layout.nodes.associateBy { it.id }
    BoxWithConstraints(
        modifier
            .clipToBounds()
            .clip(hanaTokens.panel)
            .background(colors.bgCard),
    ) {
        val maxX = (with(density) { width.toPx() } - constraints.maxWidth).coerceAtLeast(0f)
        val maxY = (with(density) { height.toPx() } - constraints.maxHeight).coerceAtLeast(0f)
        LaunchedEffect(maxX, maxY) {
            pan = Offset(pan.x.coerceIn(0f, maxX), pan.y.coerceIn(0f, maxY))
        }
        LaunchedEffect(layout.currentId, layout.cols, layout.rows, maxX, maxY) {
            val current = layout.nodes.firstOrNull { it.isCurrent } ?: return@LaunchedEffect
            val x = with(density) { (CellW * current.col).toPx() }
            val y = with(density) { (CellH * current.row).toPx() }
            pan = Offset(
                (x - constraints.maxWidth / 3f).coerceIn(0f, maxX),
                (y - constraints.maxHeight / 3f).coerceIn(0f, maxY),
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(maxX, maxY) {
                    val cellW = CellW.toPx()
                    val cellH = CellH.toPx()
                    val pad = 2.dp.toPx()
                    val slop = viewConfiguration.touchSlop
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        var dragged = false
                        val start = down.position
                        drag(down.id) { change ->
                            if ((change.position - start).getDistance() >= slop) {
                                dragged = true
                            }
                            if (dragged) {
                                val delta = change.positionChange()
                                val cur = pan
                                pan = Offset(
                                    (cur.x - delta.x).coerceIn(0f, maxX),
                                    (cur.y - delta.y).coerceIn(0f, maxY),
                                )
                                change.consume()
                            }
                        }
                        if (!dragged) {
                            val origin = pan
                            val col = floor((down.position.x + origin.x - pad) / cellW).toInt()
                            val row = floor((down.position.y + origin.y - pad) / cellH).toInt()
                            tree.nodes.firstOrNull { it.col == col && it.row == row }
                                ?.let { goTo(it.id) }
                        }
                    }
                }
                .pointerInput(maxX, maxY) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.type != PointerEventType.Scroll) continue
                            val change = event.changes.firstOrNull() ?: continue
                            val scroll = change.scrollDelta
                            val cur = pan
                            pan = Offset(
                                (cur.x + scroll.x).coerceIn(0f, maxX),
                                (cur.y + scroll.y).coerceIn(0f, maxY),
                            )
                            change.consume()
                        }
                    }
                },
        ) {
            Box(
                Modifier
                    .offset { IntOffset(-pan.x.roundToInt(), -pan.y.roundToInt()) }
                    .size(width, height)
                    .padding(2.dp),
            ) {
                Canvas(Modifier.fillMaxSize()) {
                val cellW = CellW.toPx()
                val cellH = CellH.toPx()
                fun center(node: TreeLayoutNode): Offset {
                    val dot = if (node.isCurrent) 24.dp.toPx() else 20.dp.toPx()
                    return Offset(
                        (node.col + 0.5f) * cellW,
                        node.row * cellH + dot / 2f,
                    )
                }
                for (node in layout.nodes) {
                    val parent = node.parentId?.let { byId[it] } ?: continue
                    val from = center(parent)
                    val to = center(node)
                    val color = if (node.onActiveLine) {
                        colors.accentLilac.copy(alpha = 0.9f)
                    } else {
                        colors.stroke
                    }
                    val stroke = if (node.onActiveLine) 2.4.dp.toPx() else 1.4.dp.toPx()
                    if (parent.row == node.row) {
                        drawLine(color, from, to, stroke, StrokeCap.Round)
                    } else {
                        val midX = (from.x + to.x) / 2f
                        val elbow = Offset(midX, from.y)
                        val drop = Offset(midX, to.y)
                        drawLine(color, from, elbow, stroke, StrokeCap.Round)
                        drawLine(color, elbow, drop, stroke, StrokeCap.Round)
                        drawLine(color, drop, to, stroke, StrokeCap.Round)
                    }
                }
            }
                for (node in layout.nodes) {
                    TreeNodeChip(
                        node = node,
                        fill = node.color?.let { appearance.swatch(it).fill } ?: Color.Transparent,
                        rim = when {
                            node.isCurrent -> colors.accentPink
                            node.color != null -> appearance.swatch(node.color).rim
                            else -> colors.accentLilac
                        },
                        modifier = Modifier.offset(x = CellW * node.col, y = CellH * node.row),
                    )
                }
            }
        }
    }
}

@Composable
private fun TreeNodeChip(
    node: TreeLayoutNode,
    fill: Color,
    rim: Color,
    modifier: Modifier = Modifier,
) {
    val colors = hanaColors
    val dot: Dp = if (node.isCurrent) 24.dp else 20.dp
    Column(
        modifier.size(CellW, CellH),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(dot)
                .border(if (node.isCurrent) 2.dp else 1.dp, rim, CircleShape)
                .background(fill, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (node.color == null) {
                Box(
                    Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(colors.accentLilac.copy(alpha = 0.85f)),
                )
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(
            node.label,
            color = if (node.isCurrent) colors.accentPink else colors.textDim,
            fontSize = 9.sp,
            fontWeight = if (node.isCurrent) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
        )
    }
}
