package com.acite.katahana.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import katahana.shared.generated.resources.Res
import katahana.shared.generated.resources.nunito_bold
import katahana.shared.generated.resources.nunito_medium
import katahana.shared.generated.resources.nunito_regular
import katahana.shared.generated.resources.nunito_semibold
import org.jetbrains.compose.resources.Font

val LocalHanaFontFamily = staticCompositionLocalOf<FontFamily> { FontFamily.Default }

val hanaFontFamily: FontFamily
    @Composable get() = LocalHanaFontFamily.current

@Composable
fun rememberHanaFontFamily(): FontFamily {
    val regular = Font(Res.font.nunito_regular, FontWeight.Normal)
    val medium = Font(Res.font.nunito_medium, FontWeight.Medium)
    val semibold = Font(Res.font.nunito_semibold, FontWeight.SemiBold)
    val bold = Font(Res.font.nunito_bold, FontWeight.Bold)
    return remember(regular, medium, semibold, bold) {
        FontFamily(regular, medium, semibold, bold)
    }
}

fun hanaTypography(fontFamily: FontFamily): Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 42.sp,
        lineHeight = 46.sp,
        letterSpacing = (-0.8).sp,
        color = HanaColors.text,
    ),
    headlineMedium = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        color = HanaColors.text,
    ),
    titleLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        color = HanaColors.text,
    ),
    titleMedium = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        color = HanaColors.text,
    ),
    bodyLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        color = HanaColors.text,
    ),
    bodyMedium = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = HanaColors.text,
    ),
    labelLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        color = HanaColors.text,
    ),
    labelSmall = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.4.sp,
        color = HanaColors.textDim,
    ),
)
