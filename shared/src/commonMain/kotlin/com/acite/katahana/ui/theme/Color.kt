package com.acite.katahana.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class HanaPalette(
    val bgApp: Color,
    val bgPanel: Color,
    val bgCard: Color,
    val stroke: Color,
    val text: Color,
    val textDim: Color,
    val accentPink: Color,
    val accentBlue: Color,
    val accentLilac: Color,
    val boardBg: Color,
    val grid: Color,
    val star: Color,
    val qualityPurple: Color = Color(0xFFB44AC0),
    val qualityRed: Color = Color(0xFFE85D4C),
    val qualityOrange: Color = Color(0xFFF08A3A),
    val qualityYellow: Color = Color(0xFFF2C14E),
    val qualityMint: Color = Color(0xFF7BC67E),
    val qualityGreen: Color = Color(0xFF3DAA6D),
) {
    companion object {
        val SkySakura = HanaPalette(
            bgApp = Color(0xFF12101A),
            bgPanel = Color(0xFF1B1730),
            bgCard = Color(0xFF252042),
            stroke = Color(0xFF3A3460),
            text = Color(0xFFF4F0FF),
            textDim = Color(0xFFA89BC8),
            accentPink = Color(0xFFFF6BA8),
            accentBlue = Color(0xFF7AB8FF),
            accentLilac = Color(0xFFC9B6FF),
            boardBg = Color(0xFF2A2450),
            grid = Color(0xFF6E64A8),
            star = Color(0xFFFF8EC8),
        )

        val InkPaper = HanaPalette(
            bgApp = Color(0xFF100E18),
            bgPanel = Color(0xFF1A1630),
            bgCard = Color(0xFF262044),
            stroke = Color(0xFF3E3662),
            text = Color(0xFFF4F0FF),
            textDim = Color(0xFFB0A3C8),
            accentPink = Color(0xFFE8B7D2),
            accentBlue = Color(0xFF7A8ED4),
            accentLilac = Color(0xFF8E7AC8),
            boardBg = Color(0xFF241E3A),
            grid = Color(0xFF6A5E90),
            star = Color(0xFFE8B7D2),
        )

        val MidnightSnow = HanaPalette(
            bgApp = Color(0xFF0E0D12),
            bgPanel = Color(0xFF16151C),
            bgCard = Color(0xFF22212A),
            stroke = Color(0xFF3A3848),
            text = Color(0xFFF6F1EA),
            textDim = Color(0xFFA8A0B8),
            accentPink = Color(0xFFE8D5B5),
            accentBlue = Color(0xFF8AA0C8),
            accentLilac = Color(0xFFA8A0B8),
            boardBg = Color(0xFF1C1A28),
            grid = Color(0xFF5A5668),
            star = Color(0xFFF6F1EA),
        )

        val LilacPeach = HanaPalette(
            bgApp = Color(0xFF14101C),
            bgPanel = Color(0xFF221A30),
            bgCard = Color(0xFF2E2444),
            stroke = Color(0xFF4A3A62),
            text = Color(0xFFF8F0FF),
            textDim = Color(0xFFC4B0D4),
            accentPink = Color(0xFFFF9B7A),
            accentBlue = Color(0xFFB39BFF),
            accentLilac = Color(0xFFFFC4A8),
            boardBg = Color(0xFF2A2048),
            grid = Color(0xFF7A68A8),
            star = Color(0xFFFFC4A8),
        )
    }
}

/** Sky & Sakura defaults. Prefer [hanaColors] in composables so the active appearance is used. */
object HanaColors {
    val bgApp = HanaPalette.SkySakura.bgApp
    val bgPanel = HanaPalette.SkySakura.bgPanel
    val bgCard = HanaPalette.SkySakura.bgCard
    val stroke = HanaPalette.SkySakura.stroke
    val text = HanaPalette.SkySakura.text
    val textDim = HanaPalette.SkySakura.textDim
    val accentPink = HanaPalette.SkySakura.accentPink
    val accentBlue = HanaPalette.SkySakura.accentBlue
    val accentLilac = HanaPalette.SkySakura.accentLilac
    val boardBg = HanaPalette.SkySakura.boardBg
    val grid = HanaPalette.SkySakura.grid
    val star = HanaPalette.SkySakura.star
    val qualityPurple = HanaPalette.SkySakura.qualityPurple
    val qualityRed = HanaPalette.SkySakura.qualityRed
    val qualityOrange = HanaPalette.SkySakura.qualityOrange
    val qualityYellow = HanaPalette.SkySakura.qualityYellow
    val qualityMint = HanaPalette.SkySakura.qualityMint
    val qualityGreen = HanaPalette.SkySakura.qualityGreen
}

val LocalHanaPalette = staticCompositionLocalOf { HanaPalette.SkySakura }

val hanaColors: HanaPalette
    @Composable get() = LocalHanaPalette.current
