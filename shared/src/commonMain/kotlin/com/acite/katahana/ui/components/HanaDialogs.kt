package com.acite.katahana.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acite.katahana.ui.Copy
import com.acite.katahana.ui.theme.hanaColors
import dev.chrisbanes.haze.HazeState

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

@Composable
fun HanaScrimModal(
    onDismiss: () -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.Center,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.32f))
                .pointerInput(onDismiss) {
                    detectTapGestures(onTap = { onDismiss() })
                },
        )
        val sheet = alignment == Alignment.BottomCenter
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
internal fun HanaDialogCard(
    onDismiss: () -> Unit,
    hazeState: HazeState,
    content: @Composable ColumnScope.() -> Unit,
) {
    HanaScrimModal(
        onDismiss = onDismiss,
        hazeState = hazeState,
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
