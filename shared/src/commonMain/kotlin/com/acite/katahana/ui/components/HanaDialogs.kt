package com.acite.katahana.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.acite.katahana.ui.Copy
import com.acite.katahana.ui.theme.HanaColors
import com.acite.katahana.ui.theme.hanaTokens

@Composable
fun LeaveGameDialog(
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onCancel: () -> Unit,
) {
    HanaDialogCard(onDismiss = onCancel) {
        Text(Copy.unsavedLeave, color = HanaColors.text, fontSize = 18.sp)
        Spacer(Modifier.height(8.dp))
        Text(Copy.unsavedLeaveHint, color = HanaColors.textDim, fontSize = 13.sp)
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
) {
    var name by remember { mutableStateOf(initial) }
    LaunchedEffect(initial) { name = initial }
    HanaDialogCard(onDismiss = onCancel) {
        Text(Copy.saveGame, color = HanaColors.text, fontSize = 18.sp)
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
private fun HanaDialogCard(
    onDismiss: () -> Unit,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .widthIn(max = 380.dp)
                .clip(hanaTokens.card)
                .background(HanaColors.bgPanel)
                .padding(20.dp),
            content = content,
        )
    }
}
