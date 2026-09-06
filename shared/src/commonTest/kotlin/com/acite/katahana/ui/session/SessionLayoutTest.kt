package com.acite.katahana.ui.session

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SessionLayoutTest {
    @Test
    fun phonePortraitShowsTopTreeAndNoRail() {
        val layout = computeSessionLayout(390.dp, 844.dp, mobile = true, chromePad = 8.dp)
        assertFalse(layout.showRail)
        assertFalse(layout.showSideTree)
        assertTrue(layout.showTopTree)
        assertEquals(390.dp, layout.boardSide)
        assertEquals(454.dp, layout.treeH)
    }

    @Test
    fun phoneLandscapeKeepsRailAndSideTree() {
        val layout = computeSessionLayout(844.dp, 390.dp, mobile = true, chromePad = 4.dp)
        assertTrue(layout.showRail)
        assertTrue(layout.showSideTree)
        assertFalse(layout.showTopTree)
        assertEquals(390.dp, layout.boardSide)
        assertEquals(280.dp, layout.treeW)
    }

    @Test
    fun desktopNarrowShowsTopTreeWithoutRail() {
        val layout = computeSessionLayout(800.dp, 1200.dp, mobile = false, chromePad = 8.dp)
        assertFalse(layout.showRail)
        assertFalse(layout.showSideTree)
        assertTrue(layout.showTopTree)
        assertEquals(800.dp, layout.boardSide)
    }

    @Test
    fun desktopWideShowsSideTreeWithoutRail() {
        val layout = computeSessionLayout(1400.dp, 800.dp, mobile = false, chromePad = 8.dp)
        assertFalse(layout.showRail)
        assertTrue(layout.showSideTree)
        assertFalse(layout.showTopTree)
        assertEquals(800.dp, layout.boardSide)
        assertEquals(280.dp, layout.treeW)
    }

    @Test
    fun shortPortraitSkipsTopTree() {
        val layout = computeSessionLayout(390.dp, 560.dp, mobile = true, chromePad = 8.dp)
        assertFalse(layout.showTopTree)
        assertFalse(layout.showSideTree)
        assertFalse(layout.showRail)
        assertEquals(390.dp, layout.boardSide)
        assertEquals(0.dp, layout.treeH)
    }
}
