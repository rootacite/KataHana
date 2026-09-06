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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxSize
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import com.acite.katahana.domain.Forecast
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
import com.acite.katahana.ui.theme.HanaMotion
import com.acite.katahana.ui.theme.hanaAppearance
import com.acite.katahana.ui.theme.hanaColors

@Composable
fun BoardCanvas(
    snapshot: SessionSnapshot,
    showCoords: Boolean,
    preview: Point?,
    onHover: (Point?) -> Unit,
    onActivate: (Point, isTouch: Boolean) -> Unit,
    onAim: (Point?) -> Unit = {},
    onForecast: (Point) -> Unit = {},
    modifier: Modifier = Modifier,
    candidates: List<Candidate> = emptyList(),
    qualities: List<QualityMark> = emptyList(),
    showConnections: Boolean = false,
    ownership: List<Double> = emptyList(),
    showOwnership: Boolean = true,
    ownershipStyle: OwnershipStyle = OwnershipStyle.Blocks,
    deadPoints: Set<Point> = emptySet(),
    forecast: Forecast? = null,
    forecastRevealed: Int = 0,
) {
    val squash = remember { Animatable(1f) }
    val captureFlight = remember { Animatable(1f) }
    var departing by remember { mutableStateOf(emptyList<DepartingStone>()) }
    val captureMemory = remember { CaptureMemory() }
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
    LaunchedEffect(
        snapshot.moveNumber,
        snapshot.variationIndex,
        snapshot.capturedByBlack,
        snapshot.capturedByWhite,
        snapshot.size,
        snapshot.lastMove,
    ) {
        val size = snapshot.size
        val previous = captureMemory.cells
        val stepForward = size == captureMemory.size &&
            previous.size == size * size &&
            snapshot.cells.size == size * size &&
            snapshot.moveNumber == captureMemory.moveNumber + 1
        val vanished = if (stepForward) {
            vanishedStones(previous, snapshot.cells, size)
        } else {
            emptyList()
        }
        captureMemory.cells = snapshot.cells.copyOf()
        captureMemory.size = size
        captureMemory.moveNumber = snapshot.moveNumber
        if (vanished.isEmpty()) {
            departing = emptyList()
            if (captureFlight.value < 1f) captureFlight.snapTo(1f)
            return@LaunchedEffect
        }
        departing = vanished
        captureFlight.snapTo(0f)
        captureFlight.animateTo(
            1f,
            tween(durationMillis = 820, easing = FastOutSlowInEasing),
        )
        departing = emptyList()
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
    val colors = hanaColors
    val boardInWindow = remember { mutableStateOf(Offset.Zero) }
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
            .onGloballyPositioned { coords ->
                boardInWindow.value = coords.localToWindow(Offset.Zero)
            }
            .pointerInput(
                boardSize,
                showCoords,
                snapshot.ended,
                snapshot.aiToPlay,
                snapshot.moveNumber,
                snapshot.reviewing,
            ) {
                var secondaryDown = false
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.lastOrNull() ?: continue
                        if (change.type != PointerType.Mouse) continue
                        val layout = BoardLayout(
                            size.width.toFloat(),
                            size.height.toFloat(),
                            boardSize,
                            showCoords,
                        )
                        val point = nearestIntersection(change.position, layout, TAP_MAX_GAPS)
                        when (event.type) {
                            PointerEventType.Press -> {
                                if (event.buttons.isSecondaryPressed) {
                                    secondaryDown = true
                                    change.consume()
                                }
                            }
                            PointerEventType.Move, PointerEventType.Enter -> {
                                if (!snapshot.ended && !snapshot.aiToPlay) onHover(point)
                            }
                            PointerEventType.Exit -> onHover(null)
                            PointerEventType.Release -> {
                                val wasSecondary = secondaryDown
                                secondaryDown = false
                                if (!change.changedToUpIgnoreConsumed()) continue
                                change.consume()
                                if (wasSecondary) {
                                    if (snapshot.reviewing && point != null) onForecast(point)
                                } else if (!snapshot.ended && !snapshot.aiToPlay && point != null) {
                                    onActivate(point, false)
                                }
                            }
                            else -> Unit
                        }
                    }
                }
            }
            .pointerInput(
                boardSize,
                showCoords,
                snapshot.ended,
                snapshot.aiToPlay,
                snapshot.moveNumber,
                snapshot.reviewing,
            ) {
                val edgePx = 24.dp.toPx()
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (down.type == PointerType.Mouse) return@awaitEachGesture
                    if (snapshot.ended || snapshot.aiToPlay) return@awaitEachGesture
                    val canvasW = size.width.toFloat()
                    val canvasH = size.height.toFloat()
                    fun layout() = BoardLayout(canvasW, canvasH, boardSize, showCoords)
                    fun snap(at: Offset, maxGaps: Float): Point? {
                        val point = nearestIntersection(at, layout(), maxGaps) ?: return null
                        return if (snapshot.stoneAt(point.x, point.y) != null) null else point
                    }
                    val startedInEdge = boardInWindow.value.x + down.position.x < edgePx
                    val slop = viewConfiguration.touchSlop
                    val start = down.position
                    var dragged = false
                    var lastAim: Point? = null
                    if (snapshot.reviewing) {
                        var slopAt: Offset? = null
                        val finished = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis.toLong()) {
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id }
                                    ?: return@withTimeoutOrNull
                                val travel = change.position - start
                                if (change.changedToUpIgnoreConsumed()) {
                                    val point = snap(change.position, TAP_MAX_GAPS)
                                    if (point != null) onActivate(point, true)
                                    return@withTimeoutOrNull
                                }
                                if (travel.getDistance() >= slop) {
                                    slopAt = change.position
                                    return@withTimeoutOrNull
                                }
                            }
                        }
                        if (finished == null && slopAt == null) {
                            val origin = snap(down.position, TAP_MAX_GAPS)
                            if (origin != null) onForecast(origin)
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                change.consume()
                                if (change.changedToUpIgnoreConsumed()) break
                            }
                            return@awaitEachGesture
                        }
                        val slopPos = slopAt ?: return@awaitEachGesture
                        val travel = slopPos - start
                        if (startedInEdge && abs(travel.x) > abs(travel.y) && travel.x > 0f) {
                            return@awaitEachGesture
                        }
                        dragged = true
                        val aimed = snap(slopPos, SLIDE_MAX_GAPS)
                        if (aimed != null) {
                            lastAim = aimed
                            onAim(aimed)
                        }
                    }
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        val travel = change.position - start
                        val pastSlop = travel.getDistance() >= slop
                        if (change.changedToUpIgnoreConsumed()) {
                            val point = snap(change.position, TAP_MAX_GAPS) ?: lastAim
                            if (dragged) {
                                if (point != null) onAim(point) else onAim(null)
                            } else if (point != null) {
                                onActivate(point, true)
                            }
                            break
                        }
                        if (!pastSlop) continue
                        if (!dragged && startedInEdge && abs(travel.x) > abs(travel.y) && travel.x > 0f) {
                            return@awaitEachGesture
                        }
                        dragged = true
                        change.consume()
                        val point = snap(change.position, SLIDE_MAX_GAPS)
                        if (point != null) {
                            lastAim = point
                            onAim(point)
                        } else if (nearestIntersection(change.position, layout(), SLIDE_MAX_GAPS) == null) {
                            lastAim = null
                            onAim(null)
                        }
                    }
                }
            },
    ) {
        val layout = BoardLayout(this.size.width, this.size.height, boardSize, showCoords)
        drawRoundRect(
            color = colors.boardBg,
            topLeft = Offset(layout.originX, layout.originY),
            size = Size(layout.side, layout.side),
            cornerRadius = CornerRadius(22.dp.toPx(), 22.dp.toPx()),
        )
        val gridColor = colors.grid.copy(alpha = 0.45f)
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
                    ownBlue = colors.accentBlue,
                    ownPink = colors.accentPink,
                )
            }
        }
        val starR = (layout.gap * 0.09f).coerceAtLeast(2.5f)
        for (h in hoshiPoints(boardSize)) {
            drawHoshi(layout.center(h), starR, colors.star)
        }
        if (showCoords) {
            val style = TextStyle(
                color = colors.textDim,
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
        val shownForecast = forecast?.revealed(forecastRevealed)
        val captured = shownForecast?.captured.orEmpty()
        val virtualStones = shownForecast?.stones.orEmpty()
        val virtualPoints = virtualStones.map { it.point }.toHashSet()
        val forecastLoading = forecast != null && forecastRevealed <= 0
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
        for (y in 0 until boardSize) {
            for (x in 0 until boardSize) {
                val color = snapshot.stoneAt(x, y) ?: continue
                val p = Point(x, y)
                val squashY = if (p == snapshot.lastMove) squash.value else 1f
                val alpha = when {
                    p in captured -> 0.35f
                    p in deadPoints -> 0.40f
                    else -> 1f
                }
                drawStone(color, appearance, layout.center(p), stoneR, squashY = squashY, alpha = alpha)
                if (p in captured) {
                    drawForecastCaptureMark(
                        layout.center(p),
                        stoneR,
                        appearance.swatch(color).light,
                    )
                }
            }
        }
        for (mark in qualities) {
            if (mark.point in deadPoints) continue
            if (mark.point in captured || mark.point in virtualPoints) continue
            val onBoard = snapshot.stoneAt(mark.point.x, mark.point.y)
            if (onBoard != mark.color) continue
            drawQualityFace(layout.center(mark.point), stoneR, mark.band, colors.accentLilac)
        }
        for (point in deadPoints) {
            if (point in captured || point in virtualPoints) continue
            if (snapshot.stoneAt(point.x, point.y) == null) continue
            drawDeadFace(layout.center(point), stoneR, accent = colors.accentLilac)
        }
        val flightT = captureFlight.value
        if (departing.isNotEmpty() && flightT < 0.999f) {
            val fade = (1f - flightT).coerceIn(0f, 1f)
            val lift = flightT * layout.gap * 1.45f
            val shrink = 1f - 0.18f * flightT
            for (ghost in departing) {
                val origin = layout.center(ghost.point)
                val c = Offset(origin.x, origin.y - lift)
                scale(scale = shrink, pivot = c) {
                    drawStone(
                        ghost.color,
                        appearance,
                        c,
                        stoneR,
                        alpha = 0.40f * fade,
                    )
                    drawDeadFace(c, stoneR, alpha = fade, accent = colors.accentLilac)
                }
            }
        }
        snapshot.lastMove?.let { point ->
            val color = snapshot.stoneAt(point.x, point.y) ?: return@let
            drawLastMoveMark(
                center = layout.center(point),
                radius = stoneR,
                swatch = appearance.swatch(color),
                pulse = lastRipple.value,
                breath = lastBreath.value,
                alpha = if (point in deadPoints) 0.70f else 1f,
            )
        }
        candidates.forEach { candidate ->
            val point = candidate.point ?: return@forEach
            if (point in virtualPoints) return@forEach
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
        if (
            preview != null &&
            !snapshot.ended &&
            !snapshot.aiToPlay &&
            preview !in virtualPoints &&
            !(forecastLoading && preview == forecast?.origin) &&
            snapshot.stoneAt(preview.x, preview.y) == null
        ) {
            drawStone(snapshot.toPlay, appearance, layout.center(preview), stoneR, alpha = 0.42f)
        }
        if (forecastLoading) {
            forecast?.let {
                drawForecastLoading(
                    layout.center(it.origin),
                    stoneR,
                    lastBreath.value,
                    accent = colors.accentPink,
                )
            }
        }
        val plyStyleBase = (stoneR * 0.72f).coerceIn(11f, 18f).sp
        val originLoss = forecast?.originLoss
        for (stone in virtualStones) {
            val swatch = appearance.swatch(stone.color)
            val isOrigin = stone.ply == 1
            val labelColor = if (swatch.light) Color(0xFF1A1228) else Color.White
            val text = if (isOrigin && originLoss != null) {
                formatScoreLoss(originLoss)
            } else {
                stone.ply.toString()
            }
            val fontSize = if (isOrigin && originLoss != null) {
                (stoneR * 0.48f).coerceIn(9f, 13f).sp
            } else {
                plyStyleBase
            }
            val label = measurer.measure(
                text,
                TextStyle(
                    color = labelColor,
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold,
                ),
            )
            drawForecastStone(
                color = stone.color,
                appearance = appearance,
                center = layout.center(stone.point),
                radius = stoneR,
                label = label,
                origin = isOrigin,
            )
        }
    }
}

private class OwnershipMorph {
    var from: FloatArray = FloatArray(0)
    var to: FloatArray = FloatArray(0)
}

private class CaptureMemory {
    var cells: IntArray = IntArray(0)
    var size: Int = 0
    var moveNumber: Int = -1
}

internal data class DepartingStone(val point: Point, val color: StoneColor)

internal fun vanishedStones(previous: IntArray, current: IntArray, size: Int): List<DepartingStone> {
    val n = size * size
    if (previous.size != n || current.size != n) return emptyList()
    val out = ArrayList<DepartingStone>()
    for (i in 0 until n) {
        val was = StoneColor.fromCell(previous[i]) ?: continue
        if (current[i] != StoneColor.EMPTY_CELL) continue
        out += DepartingStone(Point.fromIndex(i, size), was)
    }
    return out
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
