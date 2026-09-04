package com.acite.katahana.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val HanaColorScheme = darkColorScheme(
    primary = HanaColors.accentPink,
    onPrimary = Color.White,
    primaryContainer = HanaColors.bgCard,
    onPrimaryContainer = HanaColors.text,
    secondary = HanaColors.accentBlue,
    onSecondary = HanaColors.bgApp,
    secondaryContainer = HanaColors.bgPanel,
    onSecondaryContainer = HanaColors.text,
    tertiary = HanaColors.accentLilac,
    onTertiary = HanaColors.bgApp,
    background = HanaColors.bgApp,
    onBackground = HanaColors.text,
    surface = HanaColors.bgPanel,
    onSurface = HanaColors.text,
    surfaceVariant = HanaColors.bgCard,
    onSurfaceVariant = HanaColors.textDim,
    surfaceTint = Color.Transparent,
    outline = HanaColors.stroke,
    outlineVariant = HanaColors.stroke,
    error = HanaColors.qualityRed,
    onError = Color.White,
    inverseSurface = HanaColors.text,
    inverseOnSurface = HanaColors.bgApp,
    inversePrimary = HanaColors.accentPink,
)

@Composable
fun KataHanaTheme(
    appearance: Appearance = Appearance.SkySakura,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalHanaTokens provides HanaTokens(),
        LocalAppearance provides appearance,
    ) {
        MaterialTheme(
            colorScheme = HanaColorScheme,
            typography = HanaTypography,
            content = content,
        )
    }
}
