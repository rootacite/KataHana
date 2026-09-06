package com.acite.katahana.ui.session

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.acite.katahana.settings.AnalysisLayoutMode

internal val SessionRailWidth = 64.dp
internal val SessionTreeMinWidth = 168.dp
internal val SessionTreeMaxWidth = 280.dp
internal val SessionTreeRowMinHeight = 200.dp
internal val SessionTabbedPortraitMaxWidth = 560.dp
internal val SessionTabbedLandscapeMinHeight = 420.dp
internal val SessionTabbedSideMinWidth = 240.dp

internal data class SessionLayout(
    val showRail: Boolean,
    val showSideTree: Boolean,
    val showTopTree: Boolean,
    val tabbedAnalysis: Boolean,
    val boardSide: Dp,
    val treeW: Dp,
    val treeH: Dp,
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

internal fun computeSessionLayout(
    maxWidth: Dp,
    maxHeight: Dp,
    mobile: Boolean,
    chromePad: Dp,
    analysisMode: AnalysisLayoutMode = AnalysisLayoutMode.Auto,
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
    val treeW =
        if (!portrait && leftoverW - railTaken >= SessionTreeMinWidth) {
            (leftoverW - railTaken).coerceAtMost(SessionTreeMaxWidth)
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
        boardSide = boardSide,
        treeW = treeW,
        treeH = if (showTopTree) leftoverH else 0.dp,
    )
}
