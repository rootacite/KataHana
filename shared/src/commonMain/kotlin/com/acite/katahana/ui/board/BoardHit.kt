package com.acite.katahana.ui.board

import androidx.compose.ui.geometry.Offset
import com.acite.katahana.domain.Point
import kotlin.math.min
import kotlin.math.roundToInt

internal const val TAP_MAX_GAPS = 0.62f
internal const val SLIDE_MAX_GAPS = 0.85f
/** Wood past the stone (stones are `0.46` gaps). */
internal const val BOARD_MARGIN_GAPS = 0.75f
/** Extra strip outside the wood for GTP labels. */
internal const val COORD_BAND_GAPS = 0.58f
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
) {
    val side: Float = min(canvasW, canvasH)
    val originX: Float = (canvasW - side) / 2f
    val originY: Float = (canvasH - side) / 2f
    private val last: Int = (boardSize - 1).coerceAtLeast(1)
    private val marginGaps: Float =
        BOARD_MARGIN_GAPS + if (showCoords) COORD_BAND_GAPS else 0f
    val gap: Float = side / (last + 2f * marginGaps)
    val coordBand: Float = if (showCoords) gap * COORD_BAND_GAPS else 0f
    val inset: Float = gap * marginGaps

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
