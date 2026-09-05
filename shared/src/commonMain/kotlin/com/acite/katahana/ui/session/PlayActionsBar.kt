package com.acite.katahana.ui.session

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.acite.katahana.domain.SessionSnapshot
import com.acite.katahana.ui.Copy
import com.acite.katahana.ui.theme.HanaColors

@Composable
fun PlayActionsBar(
    snapshot: SessionSnapshot,
    hasSelection: Boolean,
    humanTurn: Boolean,
    onPass: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlayGlyphButton(PlayGlyph.Pass, Copy.pass, humanTurn, onPass)
        PlayGlyphButton(PlayGlyph.Undo, Copy.undo, snapshot.humanControls && snapshot.canUndo, onUndo)
        PlayGlyphButton(PlayGlyph.Redo, Copy.redo, snapshot.humanControls && snapshot.canRedo, onRedo)
        if (hasSelection) {
            PlayGlyphButton(PlayGlyph.Confirm, Copy.confirm, humanTurn, onConfirm, emphasized = true)
        }
    }
}

@Composable
fun PlayIconCluster(
    snapshot: SessionSnapshot,
    hasSelection: Boolean,
    humanTurn: Boolean,
    onPass: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    vertical: Boolean = false,
) {
    val buttons: @Composable () -> Unit = {
        PlayGlyphButton(PlayGlyph.Pass, Copy.pass, humanTurn, onPass)
        PlayGlyphButton(PlayGlyph.Undo, Copy.undo, snapshot.humanControls && snapshot.canUndo, onUndo)
        PlayGlyphButton(PlayGlyph.Redo, Copy.redo, snapshot.humanControls && snapshot.canRedo, onRedo)
        if (hasSelection) {
            PlayGlyphButton(PlayGlyph.Confirm, Copy.confirm, humanTurn, onConfirm, emphasized = true)
        }
    }
    if (vertical) {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
            content = { buttons() },
        )
    } else {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            content = { buttons() },
        )
    }
}

private enum class PlayGlyph { Pass, Undo, Redo, Confirm }

@Composable
private fun PlayGlyphButton(
    glyph: PlayGlyph,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    emphasized: Boolean = false,
) {
    val tint = when {
        !enabled -> HanaColors.textDim.copy(alpha = 0.38f)
        emphasized -> Color.White
        else -> HanaColors.accentLilac
    }
    val bg = when {
        emphasized && enabled -> HanaColors.accentPink
        else -> Color.Transparent
    }
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(bg)
            .semantics {
                contentDescription = label
                role = Role.Button
            }
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(18.dp)) {
            drawPlayGlyph(glyph, tint)
        }
    }
}

private fun DrawScope.drawPlayGlyph(glyph: PlayGlyph, color: Color) {
    val s = size.minDimension
    val stroke = Stroke(width = s * 0.12f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    when (glyph) {
        PlayGlyph.Pass -> {
            val bar = Path().apply {
                moveTo(s * 0.18f, s * 0.22f)
                lineTo(s * 0.18f, s * 0.78f)
            }
            drawPath(bar, color, style = stroke)
            val skip = Path().apply {
                moveTo(s * 0.36f, s * 0.22f)
                lineTo(s * 0.84f, s * 0.50f)
                lineTo(s * 0.36f, s * 0.78f)
                close()
            }
            drawPath(skip, color)
        }
        PlayGlyph.Undo -> drawTurnArrow(color, stroke, flip = false)
        PlayGlyph.Redo -> drawTurnArrow(color, stroke, flip = true)
        PlayGlyph.Confirm -> {
            val check = Path().apply {
                moveTo(s * 0.18f, s * 0.52f)
                lineTo(s * 0.42f, s * 0.76f)
                lineTo(s * 0.84f, s * 0.24f)
            }
            drawPath(check, color, style = stroke)
        }
    }
}

private fun DrawScope.drawTurnArrow(color: Color, stroke: Stroke, flip: Boolean) {
    val s = size.minDimension
    fun x(v: Float) = if (flip) s * (1f - v) else s * v
    val arc = Path().apply {
        addArc(
            oval = Rect(s * 0.18f, s * 0.22f, s * 0.82f, s * 0.86f),
            startAngleDegrees = if (flip) 20f else 160f,
            sweepAngleDegrees = if (flip) 200f else -200f,
        )
    }
    drawPath(arc, color, style = stroke)
    val tip = Path().apply {
        moveTo(x(0.18f), s * 0.18f)
        lineTo(x(0.18f), s * 0.48f)
        lineTo(x(0.46f), s * 0.48f)
        close()
    }
    drawPath(tip, color)
}
