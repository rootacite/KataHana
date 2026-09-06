package com.acite.katahana.ui.session

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal val SessionRailWidth = 64.dp
internal val SessionTreeMinWidth = 168.dp
internal val SessionTreeMaxWidth = 280.dp
internal val SessionTreeRowMinHeight = 200.dp

internal data class SessionLayout(
    val showRail: Boolean,
    val showSideTree: Boolean,
    val showTopTree: Boolean,
    val boardSide: Dp,
    val treeW: Dp,
    val treeH: Dp,
)

internal fun computeSessionLayout(
    maxWidth: Dp,
    maxHeight: Dp,
    mobile: Boolean,
    chromePad: Dp,
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
    return SessionLayout(
        showRail = showRail,
        showSideTree = treeW > 0.dp,
        showTopTree = showTopTree,
        boardSide = boardSide,
        treeW = treeW,
        treeH = if (showTopTree) leftoverH else 0.dp,
    )
}
