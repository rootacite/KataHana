package com.acite.katahana.ui.session

import androidx.compose.ui.unit.dp
import com.acite.katahana.settings.AnalysisArrangement
import com.acite.katahana.settings.AnalysisColWeights
import com.acite.katahana.settings.AnalysisLayoutMode
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
        assertTrue(layout.tabbedAnalysis)
        assertEquals(390.dp, layout.boardSide)
        assertEquals(454.dp, layout.treeH)
    }

    @Test
    fun phoneLandscapeKeepsRailAndSideTree() {
        val layout = computeSessionLayout(844.dp, 390.dp, mobile = true, chromePad = 4.dp)
        assertTrue(layout.showRail)
        assertTrue(layout.showSideTree)
        assertFalse(layout.showTopTree)
        assertTrue(layout.tabbedAnalysis)
        assertEquals(390.dp, layout.boardSide)
        assertEquals(280.dp, layout.treeW)
    }

    @Test
    fun desktopNarrowShowsTopTreeWithoutRail() {
        val layout = computeSessionLayout(800.dp, 1200.dp, mobile = false, chromePad = 8.dp)
        assertFalse(layout.showRail)
        assertFalse(layout.showSideTree)
        assertTrue(layout.showTopTree)
        assertFalse(layout.tabbedAnalysis)
        assertEquals(800.dp, layout.boardSide)
    }

    @Test
    fun desktopWideShowsSideTreeWithoutRail() {
        val layout = computeSessionLayout(1400.dp, 800.dp, mobile = false, chromePad = 8.dp)
        assertFalse(layout.showRail)
        assertTrue(layout.showSideTree)
        assertFalse(layout.showTopTree)
        assertFalse(layout.tabbedAnalysis)
        assertEquals(800.dp, layout.boardSide)
        assertEquals(280.dp, layout.treeW)
    }

    @Test
    fun shortPortraitSkipsTopTree() {
        val layout = computeSessionLayout(390.dp, 560.dp, mobile = true, chromePad = 8.dp)
        assertFalse(layout.showTopTree)
        assertFalse(layout.showSideTree)
        assertFalse(layout.showRail)
        assertFalse(layout.tabbedAnalysis)
        assertEquals(390.dp, layout.boardSide)
        assertEquals(0.dp, layout.treeH)
    }

    @Test
    fun tabletLandscapeTabsWhenSideColumnIsNarrow() {
        val layout = computeSessionLayout(1024.dp, 768.dp, mobile = true, chromePad = 4.dp)
        assertTrue(layout.showRail)
        assertTrue(layout.showSideTree)
        assertTrue(layout.tabbedAnalysis)
        assertTrue(layout.treeW < SessionTabbedSideMinWidth)
    }

    @Test
    fun compactOverrideTabsOnWideDesktop() {
        val layout = computeSessionLayout(
            1400.dp,
            800.dp,
            mobile = false,
            chromePad = 8.dp,
            analysisMode = AnalysisLayoutMode.Compact,
        )
        assertTrue(layout.showSideTree)
        assertTrue(layout.tabbedAnalysis)
    }

    @Test
    fun expandedOverrideKeepsThreeCardsOnPhone() {
        val layout = computeSessionLayout(
            390.dp,
            844.dp,
            mobile = true,
            chromePad = 8.dp,
            analysisMode = AnalysisLayoutMode.Expanded,
        )
        assertTrue(layout.showTopTree)
        assertFalse(layout.tabbedAnalysis)
    }

    @Test
    fun preferredSideWidthUsesLeftoverWithoutShrinkingBoard() {
        val wide = computeSessionLayout(
            1400.dp,
            800.dp,
            mobile = false,
            chromePad = 8.dp,
            preferredTreeW = 400.dp,
        )
        assertEquals(800.dp, wide.boardSide)
        assertEquals(400.dp, wide.treeW)
        assertTrue(wide.maxTreeW >= 400.dp)

        val clamped = computeSessionLayout(
            1400.dp,
            800.dp,
            mobile = false,
            chromePad = 8.dp,
            preferredTreeW = 900.dp,
        )
        assertEquals(800.dp, clamped.boardSide)
        assertEquals(clamped.maxTreeW, clamped.treeW)
        assertTrue(clamped.treeW < 900.dp)
    }

    @Test
    fun preferredSideWidthDoesNotGoBelowMin() {
        val layout = computeSessionLayout(
            1400.dp,
            800.dp,
            mobile = false,
            chromePad = 8.dp,
            preferredTreeW = 100.dp,
        )
        assertEquals(SessionTreeMinWidth, layout.treeW)
    }

    @Test
    fun autoArrangementIsColumnsInPortraitAndRowsInLandscape() {
        assertTrue(resolveAnalysisColumns(AnalysisArrangement.Auto, portrait = true))
        assertFalse(resolveAnalysisColumns(AnalysisArrangement.Auto, portrait = false))
        assertFalse(resolveAnalysisColumns(AnalysisArrangement.Rows, portrait = true))
        assertTrue(resolveAnalysisColumns(AnalysisArrangement.Columns, portrait = false))
    }

    @Test
    fun defaultArrangementIsRowsInBothOrientations() {
        val portrait = computeSessionLayout(800.dp, 1200.dp, mobile = false, chromePad = 8.dp)
        val landscape = computeSessionLayout(1400.dp, 800.dp, mobile = false, chromePad = 8.dp)
        assertFalse(portrait.analysisColumns)
        assertFalse(landscape.analysisColumns)
        assertEquals(AnalysisArrangement.Rows, AnalysisArrangement.Default)
    }

    @Test
    fun parseColWeightsFallsBackOnJunk() {
        assertEquals(AnalysisColWeights.Default, AnalysisColWeights.parse(null))
        assertEquals(AnalysisColWeights.Default, AnalysisColWeights.parse(""))
        assertEquals(AnalysisColWeights.Default, AnalysisColWeights.parse("1,2"))
        assertEquals(AnalysisColWeights.Default, AnalysisColWeights.parse("1,0,1"))
        assertEquals(AnalysisColWeights(2f, 3f, 4f), AnalysisColWeights.parse(" 2, 3 ,4 "))
        val roundTrip = AnalysisColWeights(2f, 3f, 4f)
        assertEquals(roundTrip, AnalysisColWeights.parse(AnalysisColWeights.format(roundTrip)))
    }

    @Test
    fun splitterDragKeepsEachColumnAboveMin() {
        val start = AnalysisColWeights(100f, 100f, 100f)
        val moved = applyAnalysisSplitterDrag(
            weights = start,
            splitter = 0,
            deltaPx = 80f,
            totalPx = 300f,
            minPx = 20f,
        )
        assertEquals(180f, moved.tree)
        assertEquals(20f, moved.graph)
        assertEquals(100f, moved.quality)

        val blocked = applyAnalysisSplitterDrag(
            weights = start,
            splitter = 0,
            deltaPx = 200f,
            totalPx = 300f,
            minPx = 96f,
        )
        assertEquals(104f, blocked.tree)
        assertEquals(96f, blocked.graph)
        assertEquals(100f, blocked.quality)
    }
}
