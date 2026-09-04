package com.acite.katahana.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class HanaTokens(
    val radiusCard: Dp = 22.dp,
    val radiusPanel: Dp = 20.dp,
    val radiusSheet: Dp = 24.dp,
    val motionMs: Int = 160,
) {
    val card = RoundedCornerShape(radiusCard)
    val panel = RoundedCornerShape(radiusPanel)
    val sheet = RoundedCornerShape(topStart = radiusSheet, topEnd = radiusSheet)
    val capsule = RoundedCornerShape(50)
}

val LocalHanaTokens = staticCompositionLocalOf { HanaTokens() }

object HanaMotion {
    fun <T> softSpring() = spring<T>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = 900f,
    )
}

val hanaTokens: HanaTokens
    @Composable get() = LocalHanaTokens.current
