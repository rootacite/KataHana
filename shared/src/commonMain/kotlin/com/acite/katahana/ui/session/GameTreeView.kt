package com.acite.katahana.ui.session

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acite.katahana.domain.TreeLayout
import com.acite.katahana.domain.TreeLayoutNode
import com.acite.katahana.ui.Copy
import com.acite.katahana.ui.theme.HanaColors
import com.acite.katahana.ui.theme.hanaAppearance
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
) {
    val tokens = hanaTokens
    Column(
        modifier
            .clip(tokens.card)
            .background(HanaColors.bgPanel)
            .padding(if (compact) 10.dp else 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                Copy.gameTree,
                color = HanaColors.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            if (reviewing) {
                Text(
                    Copy.review,
                    color = HanaColors.accentPink,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        if (reviewing) {
            Spacer(Modifier.height(6.dp))
            Text(Copy.reviewHint, color = HanaColors.textDim, fontSize = 11.sp, lineHeight = 14.sp)
        }
        Spacer(Modifier.height(10.dp))
        GameTreeGraph(
            layout = layout,
            onGoToNode = onGoToNode,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (compact) Modifier.weight(1f) else Modifier.height(220.dp)),
        )
    }
}

@Composable
private fun GameTreeGraph(
    layout: TreeLayout,
    onGoToNode: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val appearance = hanaAppearance
    val hScroll = rememberScrollState()
    val vScroll = rememberScrollState()
    val density = LocalDensity.current
    LaunchedEffect(layout.currentId, layout.cols, layout.rows) {
        val current = layout.nodes.firstOrNull { it.isCurrent } ?: return@LaunchedEffect
        val x = with(density) { (CellW * current.col).roundToPx() }
        val y = with(density) { (CellH * current.row).roundToPx() }
        val hx = (x - hScroll.viewportSize / 3).coerceAtLeast(0)
        val vy = (y - vScroll.viewportSize / 3).coerceAtLeast(0)
        hScroll.animateScrollTo(hx)
        vScroll.animateScrollTo(vy)
    }
    val width = CellW * layout.cols.coerceAtLeast(1)
    val height = CellH * layout.rows.coerceAtLeast(1)
    val byId = layout.nodes.associateBy { it.id }
    Box(
        modifier
            .clipToBounds()
            .clip(hanaTokens.panel)
            .background(HanaColors.bgCard)
            .horizontalScroll(hScroll)
            .verticalScroll(vScroll),
    ) {
        Box(Modifier.size(width, height).padding(2.dp)) {
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
                        HanaColors.accentLilac.copy(alpha = if (node.isFuture) 0.4f else 0.9f)
                    } else {
                        HanaColors.stroke
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
                        node.isCurrent -> HanaColors.accentPink
                        node.color != null -> appearance.swatch(node.color).rim
                        else -> HanaColors.accentLilac
                    },
                    onClick = { onGoToNode(node.id) },
                    modifier = Modifier.offset(x = CellW * node.col, y = CellH * node.row),
                )
            }
        }
    }
}

@Composable
private fun TreeNodeChip(
    node: TreeLayoutNode,
    fill: Color,
    rim: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val alpha = if (node.isFuture) 0.42f else 1f
    val dot: Dp = if (node.isCurrent) 24.dp else 20.dp
    Column(
        modifier
            .size(CellW, CellH)
            .alpha(alpha)
            .clickable(onClick = onClick),
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
                        .background(HanaColors.accentLilac.copy(alpha = 0.85f)),
                )
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(
            node.label,
            color = if (node.isCurrent) HanaColors.accentPink else HanaColors.textDim,
            fontSize = 9.sp,
            fontWeight = if (node.isCurrent) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
        )
    }
}
