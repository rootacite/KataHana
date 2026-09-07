package com.acite.katahana.ui.session

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.acite.katahana.ui.theme.hanaColors
import com.acite.katahana.ui.theme.hanaTokens
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur

enum class HanaDrawerEdge { End, Bottom }

@Composable
fun HanaDrawer(
    open: Boolean,
    onOpenChange: (Boolean) -> Unit,
    acrylic: Float,
    hazeState: HazeState,
    edge: HanaDrawerEdge,
    progress: Float,
    drawerContent: @Composable () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    val t = acrylic.coerceIn(0f, 1f)
    val p = progress.coerceIn(0f, 1f)
    val tint = hanaColors.bgPanel.copy(alpha = 0.38f + 0.34f * t)
    val panelBlur = HazeBlurStyle {
        blurEnabled(t > 0.02f)
        blurRadius((4f + 20f * t).dp)
        backgroundColor(tint)
        colorEffects(listOf(HazeColorEffect.tint(tint)))
        noiseFactor(0.06f * t)
        fallbackColorEffect(HazeColorEffect.tint(tint))
    }
    val bottom = edge == HanaDrawerEdge.Bottom
    BoxWithConstraints(Modifier.fillMaxSize().clipToBounds()) {
        val sheetH = maxHeight * 0.5f
        val panelH = if (bottom) sheetH else maxHeight
        val panelW = if (bottom) maxWidth else SessionDrawerWidth
        content()
        if (p > 0.01f) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.32f * p))
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { onOpenChange(false) })
                    },
            )
            val shape = if (bottom) {
                RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
            } else {
                RoundedCornerShape(topStart = 22.dp, bottomStart = 22.dp)
            }
            Box(
                Modifier
                    .align(if (bottom) Alignment.BottomCenter else Alignment.CenterEnd)
                    .then(
                        if (bottom) {
                            Modifier
                                .fillMaxWidth()
                                .height(panelH)
                                .offset(y = panelH * (1f - p))
                        } else {
                            Modifier
                                .fillMaxHeight()
                                .width(panelW)
                                .offset(x = panelW * (1f - p))
                        },
                    )
                    .clip(shape)
                    .hazeBlur(
                        input = HazeInput.Sources(hazeState),
                        style = panelBlur,
                    ),
            ) {
                drawerContent()
            }
        }
        val handleMod = if (bottom) {
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = panelH * p)
                .fillMaxWidth()
                .height(24.dp)
        } else {
            Modifier
                .align(Alignment.CenterEnd)
                .padding(end = panelW * p)
                .fillMaxHeight()
                .width(24.dp)
        }
        Box(
            handleMod.pointerInput(bottom, open) {
                val slop = viewConfiguration.touchSlop
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val start = down.position
                    drag(down.id) { change ->
                        val towardOpen = if (bottom) {
                            start.y - change.position.y > slop
                        } else {
                            start.x - change.position.x > slop
                        }
                        val towardClose = if (bottom) {
                            change.position.y - start.y > slop
                        } else {
                            change.position.x - start.x > slop
                        }
                        if (towardOpen) {
                            onOpenChange(true)
                            change.consume()
                        } else if (towardClose) {
                            onOpenChange(false)
                            change.consume()
                        }
                    }
                }
            },
            contentAlignment = if (bottom) Alignment.BottomCenter else Alignment.CenterEnd,
        ) {
            DrawerHandle(
                onClick = { onOpenChange(!open) },
                bottom = bottom,
                edgeInset = 4.dp * (1f - p),
            )
        }
    }
}

@Composable
internal fun DrawerHandle(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    bottom: Boolean = false,
    edgeInset: Dp = 4.dp,
) {
    val tokens = hanaTokens
    if (bottom) {
        Box(
            modifier
                .padding(bottom = edgeInset)
                .width(72.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                .background(hanaColors.bgPanel.copy(alpha = 0.72f))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .width(28.dp)
                    .height(3.dp)
                    .clip(tokens.capsule)
                    .background(hanaColors.accentPink.copy(alpha = 0.85f)),
            )
        }
    } else {
        Box(
            modifier
                .padding(end = edgeInset)
                .width(18.dp)
                .height(72.dp)
                .clip(RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp))
                .background(hanaColors.bgPanel.copy(alpha = 0.72f))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .width(3.dp)
                    .height(28.dp)
                    .clip(tokens.capsule)
                    .background(hanaColors.accentPink.copy(alpha = 0.85f)),
            )
        }
    }
}
