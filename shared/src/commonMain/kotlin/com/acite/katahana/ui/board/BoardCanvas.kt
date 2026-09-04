package com.acite.katahana.ui.board

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acite.katahana.domain.Point
import com.acite.katahana.domain.SessionSnapshot
import com.acite.katahana.domain.StoneColor
import com.acite.katahana.domain.hoshiPoints
import com.acite.katahana.domain.stoneLinks
import com.acite.katahana.ai.QualityMark
import com.acite.katahana.engine.Candidate
import com.acite.katahana.engine.formatScoreLoss
import com.acite.katahana.engine.lerpOwnership
import com.acite.katahana.settings.OwnershipStyle
import com.acite.katahana.ui.theme.HanaColors
import com.acite.katahana.ui.theme.HanaMotion
import com.acite.katahana.ui.theme.hanaAppearance
import kotlin.math.min

@Composable
fun BoardCanvas(
    snapshot: SessionSnapshot,
    showCoords: Boolean,
    preview: Point?,
    onHover: (Point?) -> Unit,
    onActivate: (Point, isTouch: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    candidates: List<Candidate> = emptyList(),
    qualities: List<QualityMark> = emptyList(),
    showConnections: Boolean = false,
    ownership: List<Double> = emptyList(),
    showOwnership: Boolean = true,
    ownershipStyle: OwnershipStyle = OwnershipStyle.Blocks,
) {
    val squash = remember { Animatable(1f) }
    var displayedNumber by remember { mutableIntStateOf(snapshot.moveNumber) }
    val pendingPlace = snapshot.moveNumber > displayedNumber &&
        snapshot.lastMove != null &&
        showConnections
    val linkGrow = remember(snapshot.moveNumber) {
        Animatable(if (pendingPlace) 0f else 1f)
    }
    val lastPulse = rememberInfiniteTransition(label = "lastMove")
    val lastRipple = lastPulse.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "lastRipple",
    )
    val lastBreath = lastPulse.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 760, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "lastBreath",
    )
    val ownershipPulse = rememberInfiniteTransition(label = "ownership")
    val blockBounce = ownershipPulse.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "blockBounce",
    )
    val fogDrift = ownershipPulse.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "fogDrift",
    )
    val starBreath = ownershipPulse.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "starBreath",
    )
    val starTwinkle = ownershipPulse.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "starTwinkle",
    )
    LaunchedEffect(snapshot.lastMove, snapshot.moveNumber) {
        if (snapshot.lastMove == null) {
            squash.snapTo(1f)
            return@LaunchedEffect
        }
        squash.snapTo(0.84f)
        squash.animateTo(1f, HanaMotion.softSpring())
    }
    LaunchedEffect(snapshot.moveNumber, showConnections) {
        displayedNumber = snapshot.moveNumber
        if (linkGrow.value < 0.999f && showConnections) {
            linkGrow.animateTo(1f, tween(durationMillis = 320, easing = FastOutSlowInEasing))
        } else {
            linkGrow.snapTo(1f)
        }
    }
    val measurer = rememberTextMeasurer()
    val boardSize = snapshot.size
    val appearance = hanaAppearance
    val ownershipMorph = remember { OwnershipMorph() }
    val morph = remember { Animatable(1f) }
    LaunchedEffect(ownership, boardSize) {
        val n = boardSize * boardSize
        if (ownership.size != n) return@LaunchedEffect
        val incoming = FloatArray(n) { i -> ownership[i].toFloat() }
        val t = morph.value
        ownershipMorph.from = when {
            ownershipMorph.to.size == n && ownershipMorph.from.size == n ->
                lerpOwnership(ownershipMorph.from, ownershipMorph.to, t)
            else -> FloatArray(n)
        }
        ownershipMorph.to = incoming
        morph.snapTo(0f)
        morph.animateTo(1f, tween(durationMillis = 520, easing = FastOutSlowInEasing))
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(boardSize, showCoords, snapshot.ended) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.lastOrNull() ?: continue
                        val isTouch = change.type == PointerType.Touch
                        val point = hitPoint(
                            change.position,
                            size.width.toFloat(),
                            size.height.toFloat(),
                            boardSize,
                            showCoords,
                        )
                        when (event.type) {
                            PointerEventType.Move, PointerEventType.Enter -> {
                                if (!isTouch && !snapshot.ended) onHover(point)
                            }
                            PointerEventType.Exit -> {
                                if (!isTouch) onHover(null)
                            }
                            PointerEventType.Release -> {
                                if (snapshot.ended) continue
                                if (change.changedToUpIgnoreConsumed()) {
                                    change.consume()
                                    if (point != null) onActivate(point, isTouch)
                                }
                            }
                            else -> Unit
                        }
                    }
                }
            },
    ) {
        val layout = BoardLayout(this.size.width, this.size.height, boardSize, showCoords)
        drawRoundRect(
            color = HanaColors.boardBg,
            topLeft = Offset(layout.originX, layout.originY),
            size = Size(layout.side, layout.side),
            cornerRadius = CornerRadius(22.dp.toPx(), 22.dp.toPx()),
        )
        val gridColor = HanaColors.grid.copy(alpha = 0.45f)
        val stroke = (1.2.dp.toPx()).coerceAtLeast(1f)
        for (i in 0 until boardSize) {
            val x = layout.xOf(i)
            val y = layout.yOf(i)
            drawLine(
                color = gridColor,
                start = Offset(x, layout.yOf(0)),
                end = Offset(x, layout.yOf(boardSize - 1)),
                strokeWidth = stroke,
            )
            drawLine(
                color = gridColor,
                start = Offset(layout.xOf(0), y),
                end = Offset(layout.xOf(boardSize - 1), y),
                strokeWidth = stroke,
            )
        }
        if (showOwnership) {
            val t = morph.value
            val mixed = when {
                ownershipMorph.to.size != boardSize * boardSize -> null
                ownershipMorph.from.size != boardSize * boardSize || t >= 0.999f -> ownershipMorph.to
                else -> lerpOwnership(ownershipMorph.from, ownershipMorph.to, t)
            }
            if (mixed != null) {
                drawOwnershipLayer(
                    values = mixed,
                    boardSize = boardSize,
                    gap = layout.gap,
                    centerOf = { layout.center(it) },
                    style = ownershipStyle,
                    motion = OwnershipMotion(
                        bounce = blockBounce.value,
                        drift = fogDrift.value,
                        breath = starBreath.value,
                        twinkle = starTwinkle.value,
                    ),
                )
            }
        }
        val starR = (layout.gap * 0.09f).coerceAtLeast(2.5f)
        for (h in hoshiPoints(boardSize)) {
            drawHoshi(layout.center(h), starR)
        }
        if (showCoords) {
            val style = TextStyle(
                color = HanaColors.textDim,
                fontSize = (layout.gap * 0.42f).coerceIn(13f, 18f).sp,
            )
            val letters = gtpLetters(boardSize)
            val topBand = (layout.originY + layout.yOf(0)) / 2f
            val botBand = (layout.yOf(boardSize - 1) + layout.originY + layout.side) / 2f
            val leftBand = (layout.originX + layout.xOf(0)) / 2f
            val rightBand = (layout.xOf(boardSize - 1) + layout.originX + layout.side) / 2f
            for (i in 0 until boardSize) {
                val letter = measurer.measure(letters[i], style)
                val lx = layout.xOf(i) - letter.size.width / 2f
                drawText(letter, topLeft = Offset(lx, topBand - letter.size.height / 2f))
                drawText(letter, topLeft = Offset(lx, botBand - letter.size.height / 2f))
                val number = measurer.measure((boardSize - i).toString(), style)
                val ny = layout.yOf(i) - number.size.height / 2f
                drawText(number, topLeft = Offset(leftBand - number.size.width / 2f, ny))
                drawText(number, topLeft = Offset(rightBand - number.size.width / 2f, ny))
            }
        }
        val stoneR = layout.gap * 0.46f
        for (y in 0 until boardSize) {
            for (x in 0 until boardSize) {
                val color = snapshot.stoneAt(x, y) ?: continue
                val p = Point(x, y)
                val squashY = if (p == snapshot.lastMove) squash.value else 1f
                drawStone(color, appearance, layout.center(p), stoneR, squashY = squashY)
            }
        }
        if (showConnections) {
            drawStoneLinks(
                links = stoneLinks(snapshot),
                centerOf = { layout.center(it) },
                stoneR = stoneR,
                appearance = appearance,
                growFrom = snapshot.lastMove,
                progress = linkGrow.value,
            )
        }
        for (mark in qualities) {
            val onBoard = snapshot.stoneAt(mark.point.x, mark.point.y)
            if (onBoard != mark.color) continue
            drawQualityFace(layout.center(mark.point), stoneR, mark.band)
        }
        snapshot.lastMove?.let { point ->
            val color = snapshot.stoneAt(point.x, point.y) ?: return@let
            drawLastMoveMark(
                center = layout.center(point),
                radius = stoneR,
                swatch = appearance.swatch(color),
                pulse = lastRipple.value,
                breath = lastBreath.value,
            )
        }
        candidates.forEach { candidate ->
            val point = candidate.point ?: return@forEach
            if (snapshot.stoneAt(point.x, point.y) != null) return@forEach
            val tightness = (1.0 - (candidate.pointsLost / 1.5).coerceIn(0.0, 1.0)).toFloat()
            val radius = stoneR * (0.58f + 0.20f * tightness)
            val fill = candidateColorForLoss(candidate.pointsLost)
            val labelStyle = TextStyle(
                color = candidateLabelColor(candidate.pointsLost),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
            )
            val label = measurer.measure(formatScoreLoss(candidate.pointsLost), labelStyle)
            drawCandidate(layout.center(point), radius, fill, label)
        }
        if (preview != null && snapshot.stoneAt(preview.x, preview.y) == null) {
            drawStone(snapshot.toPlay, appearance, layout.center(preview), stoneR, alpha = 0.42f)
        }
    }
}

private class OwnershipMorph {
    var from: FloatArray = FloatArray(0)
    var to: FloatArray = FloatArray(0)
}

private data class BoardLayout(
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

private fun hitPoint(
    offset: Offset,
    canvasW: Float,
    canvasH: Float,
    boardSize: Int,
    showCoords: Boolean,
): Point? {
    val layout = BoardLayout(canvasW, canvasH, boardSize, showCoords)
    val limit = layout.gap * 0.45f
    var best: Point? = null
    var bestD = limit * limit
    for (y in 0 until boardSize) {
        for (x in 0 until boardSize) {
            val c = layout.center(Point(x, y))
            val dx = offset.x - c.x
            val dy = offset.y - c.y
            val d = dx * dx + dy * dy
            if (d <= bestD) {
                bestD = d
                best = Point(x, y)
            }
        }
    }
    return best
}

private fun gtpLetters(size: Int): List<String> {
    val out = ArrayList<String>(size)
    var c = 'A'
    while (out.size < size) {
        if (c != 'I') out.add(c.toString())
        c++
    }
    return out
}
