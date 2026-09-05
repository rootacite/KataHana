package com.acite.katahana.ui.board

import androidx.compose.ui.geometry.Offset
import com.acite.katahana.domain.Point
import kotlin.math.min
import kotlin.math.roundToInt

internal const val TAP_MAX_GAPS = 0.62f
internal const val SLIDE_MAX_GAPS = 0.85f

internal data class BoardLayout(
    val canvasW: Float,
    val canvasH: Float,
    val boardSize: Int,
    val showCoords: Boolean,
) {
    val side: Float = min(canvasW, canvasH)
    val originX: Float = (canvasW - side) / 2f
    val originY: Float = (canvasH - side) / 2f
    val coordBand: Float = if (showCoords) (side * 0.028f).coerceIn(16f, 24f) else 0f
    val inset: Float = side * 0.04f + coordBand
    val gap: Float = (side - inset * 2f) / (boardSize - 1).coerceAtLeast(1)

    fun xOf(x: Int): Float = originX + inset + x * gap
    fun yOf(y: Int): Float = originY + inset + y * gap
    fun center(p: Point): Offset = Offset(xOf(p.x), yOf(p.y))
}

internal fun nearestIntersection(
    x: Float,
    y: Float,
    layout: BoardLayout,
    maxGaps: Float,
): Point? {
    if (layout.gap <= 0f || layout.boardSize <= 0) return null
    val gx = (x - layout.originX - layout.inset) / layout.gap
    val gy = (y - layout.originY - layout.inset) / layout.gap
    val last = layout.boardSize - 1
    val ix = gx.roundToInt().coerceIn(0, last)
    val iy = gy.roundToInt().coerceIn(0, last)
    val dx = gx - ix
    val dy = gy - iy
    if (dx * dx + dy * dy > maxGaps * maxGaps) return null
    return Point(ix, iy)
}

internal fun nearestIntersection(
    offset: Offset,
    layout: BoardLayout,
    maxGaps: Float,
): Point? = nearestIntersection(offset.x, offset.y, layout, maxGaps)
