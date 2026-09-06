package com.acite.katahana.ui.components

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acite.katahana.generated.AppInfo
import com.acite.katahana.ui.Copy
import com.acite.katahana.ui.theme.hanaAppearance
import com.acite.katahana.ui.theme.hanaColors
import com.acite.katahana.ui.theme.hanaTokens
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeSourceSelection
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
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
        val colors = hanaColors
        Column {
            Text(
                Copy.appName,
                color = colors.text,
                fontSize = if (compact) 22.sp else 28.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = if (compact) 26.sp else 32.sp,
            )
            Text(
                "${AppInfo.version}  ·  ${AppInfo.gitHash}",
                color = colors.textDim,
                fontSize = 12.sp,
            )
            Text(
                hanaAppearance.tagline,
                color = colors.accentLilac,
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
    subtitle: String? = "${Copy.appName}  ·  ${AppInfo.version}",
) {
    val colors = hanaColors
    Column(modifier.fillMaxWidth()) {
        if (onBack != null) QuietTextButton(Copy.back, onClick = onBack)
        Text(title, color = colors.text, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
        if (subtitle != null) {
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = colors.textDim, fontSize = 13.sp)
        }
    }
}

@Composable
fun GlowOrbs(modifier: Modifier = Modifier) {
    val colors = hanaColors
    Canvas(modifier.fillMaxSize()) {
        fun orb(center: Offset, radius: Float, color: Color, core: Float, mid: Float) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        color.copy(alpha = core),
                        color.copy(alpha = mid),
                        Color.Transparent,
                    ),
                    center = center,
                    radius = radius,
                ),
                radius = radius,
                center = center,
            )
        }
        orb(
            center = Offset(80.dp.toPx(), 100.dp.toPx()),
            radius = 252.dp.toPx(),
            color = colors.accentPink,
            core = 0.55f,
            mid = 0.16f,
        )
        orb(
            center = Offset(size.width - 70.dp.toPx(), 170.dp.toPx()),
            radius = 240.dp.toPx(),
            color = colors.accentBlue,
            core = 0.48f,
            mid = 0.14f,
        )
        orb(
            center = Offset(110.dp.toPx(), size.height - 90.dp.toPx()),
            radius = 224.dp.toPx(),
            color = colors.accentLilac,
            core = 0.42f,
            mid = 0.12f,
        )
    }
}

@Composable
fun HanaBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable (HazeState) -> Unit,
) {
    val hazeState = rememberHazeState()
    val colors = hanaColors
    Box(
        modifier
            .fillMaxSize()
            .background(colors.bgApp),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .hazeSource(hazeState),
        ) {
            GlowOrbs()
        }
        content(hazeState)
    }
}

@Composable
fun HanaSection(
    title: String,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    hint: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = hanaColors
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, color = colors.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        if (hint != null) {
            Text(hint, color = colors.textDim, fontSize = 13.sp)
        }
        PorcelainCard(hazeState, content = content)
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
    val colors = hanaColors
    val tint = colors.bgPanel.copy(alpha = 0.58f)
    Column(
        modifier
            .fillMaxWidth()
            .clip(tokens.card)
            .hazeBlur(
                input = HazeInput.Sources(hazeState),
                style = HazeBlurStyle {
                    blurRadius(22.dp)
                    backgroundColor(tint)
                    colorEffects(listOf(HazeColorEffect.tint(colors.bgCard.copy(alpha = 0.42f))))
                    noiseFactor(0.05f)
                    fallbackColorEffect(HazeColorEffect.tint(tint))
                },
            )
            .border(1.dp, Color.White.copy(alpha = 0.10f), tokens.card)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (title != null) {
            Text(title, color = colors.textDim, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
        content()
    }
}

@Composable
fun FrostedSurface(
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = hanaTokens
    val colors = hanaColors
    val tint = colors.bgPanel.copy(alpha = 0.58f)
    val fallback = colors.bgPanel.copy(alpha = 0.78f)
    val cardTint = colors.bgCard.copy(alpha = 0.42f)
    val blurStyle = remember(tint, fallback, cardTint) {
        HazeBlurStyle {
            blurRadius(24.dp)
            backgroundColor(tint)
            colorEffects(listOf(HazeColorEffect.tint(cardTint)))
            noiseFactor(0.05f)
            fallbackColorEffect(HazeColorEffect.tint(fallback))
        }
    }
    val frost = if (hazeState != null) {
        Modifier.hazeBlur(
            input = HazeInput.Sources(
                state = hazeState,
                selection = HazeSourceSelection.All,
            ),
            style = blurStyle,
        )
    } else {
        Modifier.background(tint)
    }
    Column(
        modifier
            .clip(tokens.card)
            .then(frost)
            .border(1.dp, Color.White.copy(alpha = 0.10f), tokens.card),
        content = content,
    )
}

@Composable
fun HomeNavTile(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = hanaColors.accentPink,
    emphasized: Boolean = false,
) {
    val tokens = hanaTokens
    val colors = hanaColors
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
            color = colors.text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
    }
}
