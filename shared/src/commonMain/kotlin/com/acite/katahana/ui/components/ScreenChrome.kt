package com.acite.katahana.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acite.katahana.generated.AppInfo
import com.acite.katahana.ui.Copy
import com.acite.katahana.ui.theme.HanaColors
import com.acite.katahana.ui.theme.hanaTokens
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur
import katahana.shared.generated.resources.Res
import katahana.shared.generated.resources.app_icon
import org.jetbrains.compose.resources.painterResource

@Composable
fun BrandMark(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val icon = if (compact) 44.dp else 64.dp
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Image(
            painter = painterResource(Res.drawable.app_icon),
            contentDescription = Copy.appName,
            modifier = Modifier
                .size(icon)
                .clip(CircleShape),
        )
        Column {
            Text(
                Copy.appName,
                color = HanaColors.text,
                fontSize = if (compact) 22.sp else 28.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = if (compact) 26.sp else 32.sp,
            )
            Text(
                "v${AppInfo.version}  ·  ${AppInfo.gitHash}",
                color = HanaColors.textDim,
                fontSize = 12.sp,
            )
            Text(
                Copy.tagline,
                color = HanaColors.accentLilac,
                fontSize = 13.sp,
            )
        }
    }
}

@Composable
fun ScreenHeader(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    subtitle: String? = "${Copy.appName}  ·  v${AppInfo.version}",
) {
    Column(modifier.fillMaxWidth()) {
        if (onBack != null) QuietTextButton(Copy.back, onClick = onBack)
        Text(title, color = HanaColors.text, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
        if (subtitle != null) {
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = HanaColors.textDim, fontSize = 13.sp)
        }
    }
}

@Composable
fun HanaSection(
    title: String,
    modifier: Modifier = Modifier,
    hint: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = hanaTokens
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, color = HanaColors.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        if (hint != null) {
            Text(hint, color = HanaColors.textDim, fontSize = 13.sp)
        }
        Column(
            Modifier
                .fillMaxWidth()
                .clip(tokens.card)
                .background(HanaColors.bgCard)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content,
        )
    }
}

@Composable
fun PorcelainCard(
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = hanaTokens
    val tint = HanaColors.bgPanel.copy(alpha = 0.58f)
    Column(
        modifier
            .fillMaxWidth()
            .clip(tokens.card)
            .hazeBlur(
                input = HazeInput.Sources(hazeState),
                style = HazeBlurStyle {
                    blurRadius(22.dp)
                    backgroundColor(tint)
                    colorEffects(listOf(HazeColorEffect.tint(HanaColors.bgCard.copy(alpha = 0.42f))))
                    noiseFactor(0.05f)
                    fallbackColorEffect(HazeColorEffect.tint(tint))
                },
            )
            .border(1.dp, Color.White.copy(alpha = 0.10f), tokens.card)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (title != null) {
            Text(title, color = HanaColors.textDim, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
        content()
    }
}

@Composable
fun HomeNavTile(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = HanaColors.accentPink,
    emphasized: Boolean = false,
) {
    val tokens = hanaTokens
    Row(
        modifier
            .clip(tokens.panel)
            .background(if (emphasized) accent.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.05f))
            .border(
                1.dp,
                if (emphasized) accent.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.08f),
                tokens.panel,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(accent),
        )
        Text(
            label,
            color = HanaColors.text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
