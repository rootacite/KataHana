package com.acite.katahana.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acite.katahana.ui.theme.HanaColors
import com.acite.katahana.ui.theme.hanaAppearance
import com.acite.katahana.ui.theme.hanaTokens

@Composable
fun CapsuleButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
    enabled: Boolean = true,
) {
    val tokens = hanaTokens
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = tokens.capsule,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (emphasized) HanaColors.accentPink else HanaColors.bgCard,
            contentColor = if (emphasized) Color.White else HanaColors.text,
            disabledContainerColor = HanaColors.bgCard.copy(alpha = 0.5f),
            disabledContentColor = HanaColors.textDim,
        ),
        modifier = modifier.height(52.dp),
    ) {
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun CapsuleChoice(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = hanaTokens
    val bg = if (selected) HanaColors.accentPink else HanaColors.bgCard
    val fg = if (selected) Color.White else HanaColors.text
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(tokens.capsule)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = fg, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun EngineDot(online: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(if (online) HanaColors.accentPink else HanaColors.accentPink.copy(alpha = 0.28f)),
    )
}

@Composable
fun WinrateTrack(
    modifier: Modifier = Modifier,
    blackWinrate: Float? = null,
    enabled: Boolean = true,
) {
    val tokens = hanaTokens
    val appearance = hanaAppearance
    val black = (blackWinrate ?: 0.5f).coerceIn(0f, 1f)
    val white = 1f - black
    val alpha = if (enabled) 1f else 0.38f
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (blackWinrate != null) {
            Text(
                "${(black * 100f).toInt()}%",
                color = HanaColors.text.copy(alpha = alpha),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(end = 8.dp),
            )
        }
        Row(
            Modifier
                .weight(1f)
                .height(8.dp)
                .clip(tokens.capsule)
                .background(HanaColors.bgCard.copy(alpha = alpha))
                .border(1.dp, HanaColors.stroke.copy(alpha = 0.6f * alpha), tokens.capsule),
        ) {
            Box(
                Modifier
                    .weight(black.coerceAtLeast(0.0001f))
                    .fillMaxHeight()
                    .background(appearance.first.fill.copy(alpha = alpha)),
            )
            Box(
                Modifier
                    .weight(white.coerceAtLeast(0.0001f))
                    .fillMaxHeight()
                    .background(appearance.second.fill.copy(alpha = alpha)),
            )
        }
    }
}

@Composable
fun QuietTextButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = modifier) {
        Text(text, color = HanaColors.accentLilac, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun ChoiceRow(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}
