package com.acite.katahana.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acite.katahana.ui.theme.hanaAppearance
import com.acite.katahana.ui.theme.hanaColors
import com.acite.katahana.ui.theme.hanaTokens

@Composable
fun PorcelainButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
    enabled: Boolean = true,
    accent: Color = hanaColors.accentPink,
) {
    val tokens = hanaTokens
    val colors = hanaColors
    val bg = when {
        !enabled -> Color.White.copy(alpha = 0.03f)
        emphasized -> accent.copy(alpha = 0.22f)
        else -> Color.White.copy(alpha = 0.05f)
    }
    val border = when {
        !enabled -> Color.White.copy(alpha = 0.06f)
        emphasized -> accent.copy(alpha = 0.40f)
        else -> Color.White.copy(alpha = 0.10f)
    }
    val fg = if (enabled) colors.text else colors.textDim
    Box(
        modifier
            .height(52.dp)
            .clip(tokens.capsule)
            .background(bg)
            .border(1.dp, border, tokens.capsule)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = fg,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun HanaChoiceRow(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val tokens = hanaTokens
    val colors = hanaColors
    Row(
        modifier
            .alpha(if (enabled) 1f else 0.45f)
            .fillMaxWidth()
            .clip(tokens.panel)
            .background(
                if (selected) colors.accentPink.copy(alpha = 0.16f)
                else Color.White.copy(alpha = 0.05f),
            )
            .border(
                1.dp,
                if (selected) colors.accentPink.copy(alpha = 0.28f)
                else Color.White.copy(alpha = 0.08f),
                tokens.panel,
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
fun CapsuleButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
    enabled: Boolean = true,
    compact: Boolean = false,
) {
    val tokens = hanaTokens
    val colors = hanaColors
    if (compact) {
        val bg = when {
            !enabled -> colors.bgCard.copy(alpha = 0.5f)
            emphasized -> colors.accentPink
            else -> colors.bgCard
        }
        val fg = when {
            !enabled -> colors.textDim
            emphasized -> Color.White
            else -> colors.text
        }
        Box(
            modifier
                .height(36.dp)
                .clip(tokens.capsule)
                .background(bg)
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text,
                color = fg,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        }
        return
    }
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = tokens.capsule,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (emphasized) colors.accentPink else colors.bgCard,
            contentColor = if (emphasized) Color.White else colors.text,
            disabledContainerColor = colors.bgCard.copy(alpha = 0.5f),
            disabledContentColor = colors.textDim,
        ),
        contentPadding = ButtonDefaults.ContentPadding,
        modifier = modifier.height(52.dp),
    ) {
        Text(
            text,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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
    val colors = hanaColors
    val bg = if (selected) colors.accentPink.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.05f)
    val border = if (selected) colors.accentPink.copy(alpha = 0.40f) else Color.White.copy(alpha = 0.10f)
    val fg = colors.text
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(tokens.capsule)
            .background(bg)
            .border(1.dp, border, tokens.capsule)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = fg, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun EngineDot(online: Boolean, modifier: Modifier = Modifier) {
    val colors = hanaColors
    Box(
        modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(if (online) colors.accentPink else colors.accentPink.copy(alpha = 0.28f)),
    )
}

@Composable
fun WinrateTrack(
    modifier: Modifier = Modifier,
    blackWinrate: Float? = null,
    enabled: Boolean = true,
    vertical: Boolean = false,
) {
    val tokens = hanaTokens
    val appearance = hanaAppearance
    val colors = hanaColors
    val black = (blackWinrate ?: 0.5f).coerceIn(0f, 1f)
    val white = 1f - black
    val alpha = if (enabled) 1f else 0.38f
    val percentColor = colors.text.copy(alpha = alpha)
    val blackBox = Modifier.background(appearance.first.fill.copy(alpha = alpha))
    val whiteBox = Modifier.background(appearance.second.fill.copy(alpha = alpha))
    val trackShape = tokens.capsule
    val trackBorder = Modifier
        .clip(trackShape)
        .background(colors.bgCard.copy(alpha = alpha))
        .border(1.dp, colors.stroke.copy(alpha = 0.6f * alpha), trackShape)
    if (vertical) {
        Column(
            modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (blackWinrate != null) {
                Text(
                    "${(black * 100f).toInt()}%",
                    color = percentColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }
            Column(Modifier.weight(1f).width(8.dp).then(trackBorder)) {
                Box(
                    Modifier
                        .weight(black.coerceAtLeast(0.0001f))
                        .fillMaxWidth()
                        .then(blackBox),
                )
                Box(
                    Modifier
                        .weight(white.coerceAtLeast(0.0001f))
                        .fillMaxWidth()
                        .then(whiteBox),
                )
            }
        }
    } else {
        Row(
            modifier,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (blackWinrate != null) {
                Text(
                    "${(black * 100f).toInt()}%",
                    color = percentColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
            Row(Modifier.weight(1f).height(8.dp).then(trackBorder)) {
                Box(
                    Modifier
                        .weight(black.coerceAtLeast(0.0001f))
                        .fillMaxHeight()
                        .then(blackBox),
                )
                Box(
                    Modifier
                        .weight(white.coerceAtLeast(0.0001f))
                        .fillMaxHeight()
                        .then(whiteBox),
                )
            }
        }
    }
}

@Composable
fun QuietTextButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = hanaColors
    TextButton(onClick = onClick, modifier = modifier) {
        Text(text, color = colors.accentLilac, fontWeight = FontWeight.Medium)
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

@Composable
fun HanaField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    placeholder: String? = null,
    keyboard: KeyboardType = KeyboardType.Text,
    minLines: Int = 1,
) {
    val colors = hanaColors
    val single = minLines <= 1
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it, color = colors.textDim) } },
        singleLine = single,
        minLines = if (single) 1 else minLines,
        maxLines = if (single) 1 else 4,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        shape = hanaTokens.panel,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.accentPink,
            unfocusedBorderColor = Color.White.copy(alpha = 0.10f),
            focusedLabelColor = colors.accentLilac,
            unfocusedLabelColor = colors.textDim,
            focusedTextColor = colors.text,
            unfocusedTextColor = colors.text,
            cursorColor = colors.accentPink,
            focusedContainerColor = colors.bgCard.copy(alpha = 0.42f),
            unfocusedContainerColor = colors.bgCard.copy(alpha = 0.28f),
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}
