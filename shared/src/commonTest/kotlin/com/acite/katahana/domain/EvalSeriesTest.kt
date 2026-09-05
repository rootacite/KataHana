package com.acite.katahana.domain

import com.acite.katahana.ai.QualityBand
import com.acite.katahana.ai.QualityMark
import com.acite.katahana.sgf.parseSgf
import com.acite.katahana.sgf.writeSgf
import kotlin.test.Test
import kotlin.test.assertEquals


class EvalSeriesTest {

    @Test
    fun niceAbsUsesFloorAndOneTwoFive() {
        assertEquals(5.0, niceAbs(0.0, 5.0))
        assertEquals(5.0, niceAbs(3.0, 5.0))
        assertEquals(10.0, niceAbs(7.0, 5.0))
        assertEquals(15.0, niceAbs(11.0, 5.0))
        assertEquals(10.0, niceAbs(8.0, 10.0))
        assertEquals(20.0, niceAbs(16.0, 10.0))
    }

    @Test
    fun yMaxIsSymmetricFloorForEmptyAndPeaked() {
        assertEquals(5.0, yMaxFor(emptyList(), EvalGraphMode.Score))
        assertEquals(10.0, yMaxFor(emptyList(), EvalGraphMode.Winrate))
        val samples = listOf(
            EvalSample(1, 0.8, 12.0, "n1"),
            EvalSample(2, 0.2, -3.0, "n2"),
        )
        assertEquals(15.0, yMaxFor(samples, EvalGraphMode.Score))
        assertEquals(50.0, yMaxFor(samples, EvalGraphMode.Winrate))
    }

    @Test
    fun samplesSkipMissingAndViewlessNodes() {
        val tree = GameTree(9)
        tree.play(Point(2, 2))
        tree.play(Point(3, 3))
        val line = tree.preferredLine()
        val views = mapOf(
            line[0].id to EvalView(0.5, 0.0, hasView = true),
            line[2].id to EvalView(0.0, 0.0, hasView = false),
        )
        val samples = samplesFrom(line) { views[it] }
        assertEquals(1, samples.size)
        assertEquals(0, samples[0].moveNumber)
        assertEquals(line[0].id, samples[0].nodeId)
    }

    @Test
    fun advantageIsSignedBlackLead() {
        val sample = EvalSample(4, 0.7, -2.5, "n")
        assertEquals(-2.5, sample.advantage(EvalGraphMode.Score))
        assertEquals(20.0, sample.advantage(EvalGraphMode.Winrate), 1e-9)
    }

    @Test
    fun qualityCountsSplitByColorAndSkipShallow() {
        val marks = listOf(
            mark(QualityBand.Good, StoneColor.Black),
            mark(QualityBand.Good, StoneColor.Black),
            mark(QualityBand.Inaccuracy, StoneColor.White),
            mark(QualityBand.Blunder, StoneColor.White),
            mark(QualityBand.Shallow, StoneColor.Black),
        )
        val stats = qualityCounts(marks)
        assertEquals(2, stats.count(StoneColor.Black, QualityBand.Good))
        assertEquals(0, stats.count(StoneColor.White, QualityBand.Good))
        assertEquals(1, stats.count(StoneColor.White, QualityBand.Inaccuracy))
        assertEquals(1, stats.count(StoneColor.White, QualityBand.Blunder))
        assertEquals(0, stats.count(StoneColor.Black, QualityBand.Shallow))
    }

    @Test
    fun pathSurvivesSgfRoundTrip() {
        val tree = GameTree(9)
        tree.play(Point(2, 2))
        tree.undo()
        tree.play(Point(4, 4))
        tree.play(Point(5, 5))
        val path = tree.current.pathFromRoot()
        val sgf = writeSgf(tree, GameConfig(boardSize = 9))
        val loaded = parseSgf(sgf).tree
        val node = requireNotNull(loaded.nodeAtPath(path))
        val place = node.move as Move.Place
        assertEquals(Point(5, 5), place.point)
        assertEquals(path, node.pathFromRoot())
    }

    private fun mark(band: QualityBand, color: StoneColor) = QualityMark(
        point = Point(0, 0),
        pointsLost = 0.0,
        visits = 100,
        band = band,
        color = color,
    )
}
