package com.acite.katahana.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.acite.katahana.domain.StoneColor

data class StoneSwatch(
    val fill: Color,
    val rim: Color,
    val hi: Color,
    val light: Boolean,
)

data class Appearance(
    val id: String,
    val label: String,
    val blurb: String,
    val first: StoneSwatch,
    val second: StoneSwatch,
) {
    fun swatch(color: StoneColor): StoneSwatch = when (color) {
        StoneColor.Black -> first
        StoneColor.White -> second
    }

    companion object {
        const val DEFAULT_ID = "sky_sakura"

        val SkySakura = Appearance(
            id = DEFAULT_ID,
            label = "Sky & Sakura",
            blurb = "Clear-sky blue and cherry blossom",
            first = StoneSwatch(
                fill = Color(0xFF3EA4F8),
                rim = Color(0xFF1B7BD4),
                hi = Color(0xFFD7EFFF),
                light = false,
            ),
            second = StoneSwatch(
                fill = Color(0xFFFFB7C9),
                rim = Color(0xFFE87A9C),
                hi = Color(0xFFFFFFFF),
                light = true,
            ),
        )

        val InkPaper = Appearance(
            id = "ink_paper",
            label = "Ink & Paper",
            blurb = "Deep indigo and sakura paper",
            first = StoneSwatch(
                fill = Color(0xFF2B2148),
                rim = Color(0xFF5B4D8A),
                hi = Color(0xFF8E7AC8),
                light = false,
            ),
            second = StoneSwatch(
                fill = Color(0xFFFFEAF4),
                rim = Color(0xFFE8B7D2),
                hi = Color(0xFFFFFFFF),
                light = true,
            ),
        )

        val MidnightSnow = Appearance(
            id = "midnight_snow",
            label = "Midnight & Snow",
            blurb = "Night charcoal and warm snow",
            first = StoneSwatch(
                fill = Color(0xFF1C1A28),
                rim = Color(0xFF4A4660),
                hi = Color(0xFF8A84A8),
                light = false,
            ),
            second = StoneSwatch(
                fill = Color(0xFFF6F1EA),
                rim = Color(0xFFD4C8B8),
                hi = Color(0xFFFFFFFF),
                light = true,
            ),
        )

        val LilacPeach = Appearance(
            id = "lilac_peach",
            label = "Lilac & Peach",
            blurb = "Soft lilac and ripe peach",
            first = StoneSwatch(
                fill = Color(0xFFB39BFF),
                rim = Color(0xFF7B62D4),
                hi = Color(0xFFEDE4FF),
                light = false,
            ),
            second = StoneSwatch(
                fill = Color(0xFFFFC4A8),
                rim = Color(0xFFE88962),
                hi = Color(0xFFFFF3EC),
                light = true,
            ),
        )

        val all: List<Appearance> = listOf(SkySakura, InkPaper, MidnightSnow, LilacPeach)

        fun byId(id: String): Appearance = all.firstOrNull { it.id == id } ?: SkySakura
    }
}

val LocalAppearance = staticCompositionLocalOf { Appearance.SkySakura }

val hanaAppearance: Appearance
    @Composable get() = LocalAppearance.current
