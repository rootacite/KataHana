package com.acite.katahana.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acite.katahana.ui.Copy
import com.acite.katahana.ui.theme.hanaColors
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeSourceSelection
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LeaveGameDialog(
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onCancel: () -> Unit,
    hazeState: HazeState,
) {
    HanaDialogCard(onDismiss = onCancel, hazeState = hazeState) {
        val colors = hanaColors
        Text(Copy.unsavedLeave, color = colors.text, fontSize = 18.sp)
        Spacer(Modifier.height(8.dp))
        Text(Copy.unsavedLeaveHint, color = colors.textDim, fontSize = 13.sp)
        Spacer(Modifier.height(16.dp))
        CapsuleButton(
            Copy.save,
            onClick = onSave,
            modifier = Modifier.fillMaxWidth(),
            emphasized = true,
        )
        Spacer(Modifier.height(8.dp))
        CapsuleButton(
            Copy.dontSave,
            onClick = onDiscard,
            modifier = Modifier.fillMaxWidth(),
        )
        QuietTextButton(Copy.cancel, modifier = Modifier.fillMaxWidth(), onClick = onCancel)
    }
}

@Composable
fun SaveNameDialog(
    initial: String,
    onConfirm: (String) -> Unit,
    onCancel: () -> Unit,
    hazeState: HazeState,
) {
    var name by remember { mutableStateOf(initial) }
    LaunchedEffect(initial) { name = initial }
    HanaDialogCard(onDismiss = onCancel, hazeState = hazeState) {
        val colors = hanaColors
        Text(Copy.saveGame, color = colors.text, fontSize = 18.sp)
        Spacer(Modifier.height(12.dp))
        HanaField(Copy.gameName, name, onChange = { name = it })
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CapsuleButton(
                Copy.cancel,
                onClick = onCancel,
                modifier = Modifier.weight(1f),
            )
            CapsuleButton(
                Copy.save,
                onClick = { onConfirm(name) },
                modifier = Modifier.weight(1f),
                emphasized = true,
            )
        }
    }
}

private val ScrimFadeMs = 180
private val SheetSlideMs = 260

@Composable
fun HanaScrimModal(
    onDismiss: () -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.Center,
    slideFromBottom: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (slideFromBottom) {
        CinematicScrimModal(
            onDismiss = onDismiss,
            hazeState = hazeState,
            modifier = modifier,
            alignment = alignment,
            content = content,
        )
    } else {
        InstantScrimModal(
            onDismiss = onDismiss,
            hazeState = hazeState,
            modifier = modifier,
            alignment = alignment,
            content = content,
        )
    }
}

@Composable
private fun InstantScrimModal(
    onDismiss: () -> Unit,
    hazeState: HazeState,
    modifier: Modifier,
    alignment: Alignment,
    content: @Composable ColumnScope.() -> Unit,
) {
    val sheet = alignment == Alignment.BottomCenter
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.32f))
                .pointerInput(onDismiss) {
                    detectTapGestures(onTap = { onDismiss() })
                },
        )
        FrostedSurface(
            modifier = modifier
                .align(alignment)
                .padding(if (sheet) 0.dp else 20.dp)
                .then(if (sheet) Modifier.fillMaxWidth() else Modifier)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
            hazeState = hazeState,
            content = content,
        )
    }
}

@Composable
private fun CinematicScrimModal(
    onDismiss: () -> Unit,
    hazeState: HazeState,
    modifier: Modifier,
    alignment: Alignment,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scope = rememberCoroutineScope()
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val hide: () -> Unit = {
        if (visible) {
            visible = false
            scope.launch {
                delay(SheetSlideMs.toLong())
                onDismiss()
            }
        }
    }
    val dim = Color.Black.copy(alpha = 0.32f)
    val scrimBlur = remember(dim) {
        HazeBlurStyle {
            blurRadius(20.dp)
            backgroundColor(dim)
            colorEffects(listOf(HazeColorEffect.tint(dim)))
            fallbackColorEffect(HazeColorEffect.tint(dim))
        }
    }
    val sheet = alignment == Alignment.BottomCenter
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val screenH = constraints.maxHeight
        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.fillMaxSize(),
            enter = fadeIn(tween(ScrimFadeMs)),
            exit = fadeOut(tween(ScrimFadeMs)),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .hazeBlur(
                        input = HazeInput.Sources(
                            state = hazeState,
                            selection = HazeSourceSelection.All,
                        ),
                        style = scrimBlur,
                    )
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { hide() })
                    },
            )
        }
        AnimatedVisibility(
            visible = visible,
            modifier = modifier
                .align(alignment)
                .padding(if (sheet) 0.dp else 20.dp)
                .then(if (sheet) Modifier.fillMaxWidth() else Modifier),
            enter = slideInVertically(
                animationSpec = tween(SheetSlideMs, easing = FastOutSlowInEasing),
                initialOffsetY = { screenH },
            ),
            exit = slideOutVertically(
                animationSpec = tween(SheetSlideMs, easing = FastOutSlowInEasing),
                targetOffsetY = { screenH },
            ),
        ) {
            FrostedSurface(
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
                hazeState = hazeState,
                blurRadius = 48.dp,
                panelAlpha = 0.64f,
                cardAlpha = 0.50f,
                content = content,
            )
        }
    }
}

@Composable
internal fun HanaDialogCard(
    onDismiss: () -> Unit,
    hazeState: HazeState,
    slideFromBottom: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    HanaScrimModal(
        onDismiss = onDismiss,
        hazeState = hazeState,
        slideFromBottom = slideFromBottom,
        modifier = Modifier
            .widthIn(max = 380.dp)
            .fillMaxWidth(),
    ) {
        Column(
            Modifier.padding(20.dp),
            content = content,
        )
    }
}
