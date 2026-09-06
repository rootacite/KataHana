package com.acite.katahana.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class AppearancePaletteTest {
    @Test
    fun skySakuraKeepsCurrentTokens() {
        val palette = Appearance.SkySakura.palette
        assertEquals(Color(0xFF12101A), palette.bgApp)
        assertEquals(Color(0xFF1B1730), palette.bgPanel)
        assertEquals(Color(0xFF252042), palette.bgCard)
        assertEquals(Color(0xFFFF6BA8), palette.accentPink)
        assertEquals(Color(0xFF7AB8FF), palette.accentBlue)
        assertEquals(Color(0xFFC9B6FF), palette.accentLilac)
        assertEquals(Color(0xFF2A2450), palette.boardBg)
        assertEquals(HanaColors.bgApp, palette.bgApp)
        assertEquals(HanaColors.accentPink, palette.accentPink)
        assertEquals("Local Go · pink night", Appearance.SkySakura.tagline)
    }

    @Test
    fun otherAppearancesHaveDistinctAccents() {
        val sky = Appearance.SkySakura.palette.accentPink
        assertNotEquals(sky, Appearance.InkPaper.palette.accentPink)
        assertNotEquals(sky, Appearance.MidnightSnow.palette.accentPink)
        assertNotEquals(sky, Appearance.LilacPeach.palette.accentPink)
        assertNotEquals(
            Appearance.InkPaper.palette.bgApp,
            Appearance.MidnightSnow.palette.bgApp,
        )
        assertEquals(Appearance.InkPaper.palette, Appearance.byId("ink_paper").palette)
        assertEquals("Local Go · ink wash", Appearance.InkPaper.tagline)
        assertEquals("Local Go · midnight", Appearance.MidnightSnow.tagline)
        assertEquals("Local Go · dusk", Appearance.LilacPeach.tagline)
    }

    @Test
    fun qualityBandsStayShared() {
        val palettes = Appearance.all.map { it.palette }
        palettes.forEach { palette ->
            assertEquals(HanaColors.qualityRed, palette.qualityRed)
            assertEquals(HanaColors.qualityGreen, palette.qualityGreen)
        }
    }
}
