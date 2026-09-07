package com.acite.katahana.ui.session

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.acite.katahana.settings.ANALYSIS_SIDE_WIDTH_DP_DEFAULT
import com.acite.katahana.settings.ANALYSIS_SIDE_WIDTH_DP_MIN
import com.acite.katahana.settings.AnalysisArrangement
import com.acite.katahana.settings.AnalysisColWeights
import com.acite.katahana.settings.AnalysisLayoutMode

internal val SessionRailWidth = 64.dp
internal val SessionTreeMinWidth = ANALYSIS_SIDE_WIDTH_DP_MIN.dp
internal val SessionTreeDefaultWidth = ANALYSIS_SIDE_WIDTH_DP_DEFAULT.dp
internal val SessionTreeRowMinHeight = 200.dp
internal val SessionTabbedPortraitMaxWidth = 560.dp
internal val SessionTabbedLandscapeMinHeight = 420.dp
internal val SessionTabbedSideMinWidth = 240.dp
internal val SessionResizeHandleWidth = 8.dp
internal val SessionTreeColMinWidth = 96.dp
internal val SessionTreeRowMinSlice = 72.dp
internal val SessionDrawerWidth = 280.dp
internal const val SessionDrawerAnimMs = 280

internal data class SessionLayout(
    val showRail: Boolean,
    val showSideTree: Boolean,
    val showTopTree: Boolean,
    val tabbedAnalysis: Boolean,
    val analysisColumns: Boolean,
    val boardSide: Dp,
    val treeW: Dp,
    val treeH: Dp,
    val maxTreeW: Dp,
)

internal fun autoTabbedAnalysis(
    maxWidth: Dp,
    maxHeight: Dp,
    boardSide: Dp,
    treeW: Dp,
    showTopTree: Boolean,
    showSideTree: Boolean,
): Boolean {
    if (!showTopTree && !showSideTree) return false
    val portrait = maxHeight >= maxWidth
    return if (portrait) {
        maxWidth < SessionTabbedPortraitMaxWidth
    } else {
        boardSide < SessionTabbedLandscapeMinHeight || treeW < SessionTabbedSideMinWidth
    }
}

internal fun resolveTabbedAnalysis(
    mode: AnalysisLayoutMode,
    auto: Boolean,
    showing: Boolean,
): Boolean {
    if (!showing) return false
    return when (mode) {
        AnalysisLayoutMode.Auto -> auto
        AnalysisLayoutMode.Compact -> true
        AnalysisLayoutMode.Expanded -> false
    }
}

internal fun resolveAnalysisColumns(
    arrangement: AnalysisArrangement,
    portrait: Boolean,
): Boolean = when (arrangement) {
    AnalysisArrangement.Auto -> portrait
    AnalysisArrangement.Rows -> false
    AnalysisArrangement.Columns -> true
}

internal fun clampAnalysisSideWidth(
    preferred: Dp,
    leftoverW: Dp,
    railTaken: Dp,
    gap: Dp = SessionResizeHandleWidth,
): Dp {
    val maxW = leftoverW - railTaken - gap
    if (maxW < SessionTreeMinWidth) return 0.dp
    return preferred.coerceIn(SessionTreeMinWidth, maxW)
}

internal fun applyAnalysisSplitterDrag(
    weights: AnalysisColWeights,
    splitter: Int,
    deltaPx: Float,
    totalPx: Float,
    minPx: Float,
): AnalysisColWeights {
    if (splitter !in 0..1 || totalPx <= 0f) return weights
    val sum = weights.tree + weights.graph + weights.quality
    if (sum <= 0f) return AnalysisColWeights.Default
    val px = floatArrayOf(
        weights.tree / sum * totalPx,
        weights.graph / sum * totalPx,
        weights.quality / sum * totalPx,
    )
    px[splitter] += deltaPx
    px[splitter + 1] -= deltaPx
    if (px[splitter] < minPx) {
        val d = minPx - px[splitter]
        px[splitter] = minPx
        px[splitter + 1] -= d
    }
    if (px[splitter + 1] < minPx) {
        val d = minPx - px[splitter + 1]
        px[splitter + 1] = minPx
        px[splitter] -= d
    }
    if (px.any { it < minPx - 0.5f }) return weights
    return AnalysisColWeights(px[0], px[1], px[2])
}

internal fun computeSessionLayout(
    maxWidth: Dp,
    maxHeight: Dp,
    mobile: Boolean,
    chromePad: Dp,
    analysisMode: AnalysisLayoutMode = AnalysisLayoutMode.Auto,
    preferredTreeW: Dp = SessionTreeDefaultWidth,
    arrangement: AnalysisArrangement = AnalysisArrangement.Default,
): SessionLayout {
    val showRail = mobile && maxWidth > maxHeight
    val portrait = maxHeight >= maxWidth
    val fullSquare = minOf(maxWidth, maxHeight).coerceAtLeast(120.dp)
    val railTaken = if (showRail) SessionRailWidth + chromePad else 0.dp
    val boardSide =
        if (showRail && maxWidth - fullSquare < railTaken) {
            minOf(maxHeight, maxWidth - railTaken).coerceAtLeast(120.dp)
        } else {
            fullSquare
        }
    val leftoverW = maxWidth - boardSide
    val maxTreeW = (leftoverW - railTaken - SessionResizeHandleWidth).coerceAtLeast(0.dp)
    val treeW =
        if (!portrait) {
            clampAnalysisSideWidth(
                preferred = preferredTreeW,
                leftoverW = leftoverW,
                railTaken = railTaken,
            )
        } else {
            0.dp
        }
    val leftoverH = maxHeight - boardSide
    val showTopTree = portrait && leftoverH >= SessionTreeRowMinHeight
    val showSideTree = treeW > 0.dp
    val auto = autoTabbedAnalysis(
        maxWidth = maxWidth,
        maxHeight = maxHeight,
        boardSide = boardSide,
        treeW = treeW,
        showTopTree = showTopTree,
        showSideTree = showSideTree,
    )
    return SessionLayout(
        showRail = showRail,
        showSideTree = showSideTree,
        showTopTree = showTopTree,
        tabbedAnalysis = resolveTabbedAnalysis(
            mode = analysisMode,
            auto = auto,
            showing = showTopTree || showSideTree,
        ),
        analysisColumns = resolveAnalysisColumns(arrangement, portrait),
        boardSide = boardSide,
        treeW = treeW,
        treeH = if (showTopTree) leftoverH else 0.dp,
        maxTreeW = if (showSideTree) maxTreeW else 0.dp,
    )
}
