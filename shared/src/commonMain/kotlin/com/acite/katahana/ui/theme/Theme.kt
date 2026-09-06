package com.acite.katahana.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

@Composable
fun KataHanaTheme(
    appearance: Appearance = Appearance.SkySakura,
    content: @Composable () -> Unit,
) {
    val palette = appearance.palette
    val fonts = rememberHanaFontFamily()
    val typography = remember(fonts) { hanaTypography(fonts) }
    val scheme = remember(palette) {
        darkColorScheme(
            primary = palette.accentPink,
            onPrimary = Color.White,
            primaryContainer = palette.bgCard,
            onPrimaryContainer = palette.text,
            secondary = palette.accentBlue,
            onSecondary = palette.bgApp,
            secondaryContainer = palette.bgPanel,
            onSecondaryContainer = palette.text,
            tertiary = palette.accentLilac,
            onTertiary = palette.bgApp,
            background = palette.bgApp,
            onBackground = palette.text,
            surface = palette.bgPanel,
            onSurface = palette.text,
            surfaceVariant = palette.bgCard,
            onSurfaceVariant = palette.textDim,
            surfaceTint = Color.Transparent,
            outline = palette.stroke,
            outlineVariant = palette.stroke,
            error = palette.qualityRed,
            onError = Color.White,
            inverseSurface = palette.text,
            inverseOnSurface = palette.bgApp,
            inversePrimary = palette.accentPink,
        )
    }
    CompositionLocalProvider(
        LocalHanaTokens provides HanaTokens(),
        LocalAppearance provides appearance,
        LocalHanaPalette provides palette,
        LocalHanaFontFamily provides fonts,
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = typography,
            content = content,
        )
    }
}
