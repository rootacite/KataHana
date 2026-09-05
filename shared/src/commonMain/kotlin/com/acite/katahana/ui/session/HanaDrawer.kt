package com.acite.katahana.ui.session

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.acite.katahana.ui.theme.HanaColors
import com.acite.katahana.ui.theme.hanaTokens
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur

private val DrawerWidth = 280.dp

@Composable
fun HanaDrawer(
    open: Boolean,
    onOpenChange: (Boolean) -> Unit,
    acrylic: Float,
    hazeState: HazeState,
    drawerContent: @Composable () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    val drawerState = rememberDrawerState(if (open) DrawerValue.Open else DrawerValue.Closed)
    LaunchedEffect(open) {
        if (open && drawerState.isClosed) drawerState.open()
        else if (!open && drawerState.isOpen) drawerState.close()
    }
    LaunchedEffect(drawerState.currentValue) {
        onOpenChange(drawerState.isOpen)
    }
    val t = acrylic.coerceIn(0f, 1f)
    val tint = HanaColors.bgPanel.copy(alpha = 0.38f + 0.34f * t)
    val blurStyle = HazeBlurStyle {
        blurEnabled(t > 0.02f)
        blurRadius((4f + 20f * t).dp)
        backgroundColor(tint)
        colorEffects(listOf(HazeColorEffect.tint(tint)))
        noiseFactor(0.06f * t)
        fallbackColorEffect(HazeColorEffect.tint(tint))
    }
    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        scrimColor = Color.Black.copy(alpha = 0.16f),
        drawerContent = {
            Box(
                Modifier
                    .fillMaxHeight()
                    .width(DrawerWidth)
                    .clip(RoundedCornerShape(topEnd = 22.dp, bottomEnd = 22.dp))
                    .hazeBlur(
                        input = HazeInput.Sources(hazeState),
                        style = blurStyle,
                    ),
            ) {
                drawerContent()
            }
        },
    ) {
        Box(Modifier.fillMaxSize()) {
            content()
            if (drawerState.isClosed) {
                DrawerHandle(
                    modifier = Modifier.align(Alignment.CenterStart),
                    onClick = { onOpenChange(true) },
                )
            }
        }
    }
}

@Composable
internal fun DrawerHandle(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = hanaTokens
    Box(
        modifier
            .padding(start = 4.dp)
            .width(18.dp)
            .height(72.dp)
            .clip(RoundedCornerShape(topEnd = 14.dp, bottomEnd = 14.dp))
            .background(HanaColors.bgPanel.copy(alpha = 0.72f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .width(3.dp)
                .height(28.dp)
                .clip(tokens.capsule)
                .background(HanaColors.accentPink.copy(alpha = 0.85f)),
        )
    }
}
