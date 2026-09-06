package com.acite.katahana.ui.board

import androidx.compose.ui.geometry.Offset
import com.acite.katahana.domain.Point
import kotlin.math.min
import kotlin.math.roundToInt

internal const val TAP_MAX_GAPS = 0.62f
internal const val SLIDE_MAX_GAPS = 0.85f
internal const val STONE_RADIUS_GAPS = 0.46f
internal const val COORD_FONT_GAP_FRACTION = 0.48f
internal const val COORD_THIN_GLYPH_FRACTION = 0.9f

internal fun coordFontPx(gap: Float): Float = gap * COORD_FONT_GAP_FRACTION

internal fun coordsNeedThinning(widestGlyphPx: Float, gap: Float): Boolean =
    widestGlyphPx > gap * COORD_THIN_GLYPH_FRACTION

internal fun shouldDrawCoordIndex(index: Int, boardSize: Int, thin: Boolean): Boolean {
    if (!thin) return true
    return index == 0 || index == boardSize - 1 || index % 2 == 0
}

internal data class BoardLayout(
    val canvasW: Float,
    val canvasH: Float,
    val boardSize: Int,
    val showCoords: Boolean,
    val edgePadPx: Float = 0f,
    val gridPadPx: Float = 0f,
) {
    val side: Float = min(canvasW, canvasH)
    val originX: Float = (canvasW - side) / 2f
    val originY: Float = (canvasH - side) / 2f
    private val last: Int = (boardSize - 1).coerceAtLeast(1)
    val gap: Float
    val fontPx: Float
    val stoneR: Float
    val inset: Float
    val coordCenter: Float

    init {
        val n = last.toFloat()
        val edge = edgePadPx.coerceAtLeast(0f)
        val inner = gridPadPx.coerceAtLeast(0f)
        val frac = STONE_RADIUS_GAPS + if (showCoords) COORD_FONT_GAP_FRACTION else 0f
        val pad = if (showCoords) edge + inner else edge
        gap = ((side - 2f * pad) / (n + 2f * frac)).coerceAtLeast(1f)
        fontPx = if (showCoords) COORD_FONT_GAP_FRACTION * gap else 0f
        stoneR = STONE_RADIUS_GAPS * gap
        inset = if (showCoords) edge + fontPx + stoneR + inner else stoneR + edge
        coordCenter = edge + fontPx / 2f
    }

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
