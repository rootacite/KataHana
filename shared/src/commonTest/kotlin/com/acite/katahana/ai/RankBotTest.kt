package com.acite.katahana.ai

import com.acite.katahana.domain.Move
import com.acite.katahana.domain.Point
import com.acite.katahana.domain.Position
import com.acite.katahana.domain.StoneColor
import com.acite.katahana.engine.MoveInfo
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class RankBotTest {

    @Test
    fun nMovesDiffersAcrossRanksOnEmpty19() {
        val legal = 19 * 19
        val squares = legal
        val n9k = RankBot.nMoves(legal, squares, kyu = 9)
        val n1k = RankBot.nMoves(legal, squares, kyu = 1)
        val n1d = RankBot.nMoves(legal, squares, kyu = 0)
        assertNotEquals(n9k, n1k)
        assertNotEquals(n1k, n1d)
        assertTrue(n9k < n1d, "stronger rank samples more (9k=$n9k, 1d=$n1d)")
        assertTrue(n9k >= 1)
        assertTrue(n1d <= legal)
        assertEquals(75, RankBot.nMoves(361, 361, kyu = 5))
        assertEquals(23, RankBot.nMoves(361, 361, kyu = 15))
    }

    @Test
    fun occupiedHighPolicyIsNotPlayed() {
        val size = 9
        val occupied = Point.fromIndex(40, size)
        val pos = Position.of(size, black = listOf(occupied), toPlay = StoneColor.White)
        val policy = DoubleArray(size * size + 1) { 0.002 }
        policy[40] = 0.9
        policy[0] = 0.08
        val decision = RankBot.choose(policy.toList(), pos, kyu = 5, rng = Random(1))
        val place = assertIs<Move.Place>(decision.move)
        assertTrue(place.point != Point.fromIndex(40, size))
    }

    @Test
    fun obviousMoveOverridePlaysPolicyFirst() {
        val size = 9
        val policy = DoubleArray(size * size + 1) { 0.001 }.also { it[40] = 0.95 }
        val pos = Position.empty(size)
        val decision = RankBot.choose(policy.toList(), pos, kyu = 5, rng = Random(1))
        assertTrue(decision.usedOverride)
        val place = assertIs<Move.Place>(decision.move)
        assertEquals(Point.fromIndex(40, size), place.point)
    }

    @Test
    fun samplingIsDeterministicWithFixedSeed() {
        val size = 9
        val policy = DoubleArray(size * size + 1) { i ->
            if (i == size * size) 0.01 else 0.02 + (i % 17) * 0.001
        }.toList()
        val pos = Position.empty(size)
        val a = RankBot.choose(policy, pos, kyu = 5, rng = Random(42))
        val b = RankBot.choose(policy, pos, kyu = 5, rng = Random(42))
        assertEquals(a.move, b.move)
        assertEquals(a.nMoves, b.nMoves)
        val c = RankBot.choose(policy, pos, kyu = 5, rng = Random(7))
        // Different seed may still collide; just ensure it returns a legal place or pass.
        assertTrue(c.move is Move.Place || c.move is Move.Pass)
    }

    @Test
    fun fullStrengthTakesOrderZero() {
        val infos = listOf(
            MoveInfo(move = "D4", order = 1, scoreLead = 1.0),
            MoveInfo(move = "C3", order = 0, scoreLead = 2.0),
            MoveInfo(move = "pass", order = 2, scoreLead = 0.0),
        )
        val move = FullStrengthBot.choose(infos, boardSize = 9, toPlay = StoneColor.Black)
        val place = assertIs<Move.Place>(move)
        assertEquals("C3", place.point.toGtp(9))
    }
}

class MoveQualityTest {
    @Test
    fun bandsFollowKaTrainThresholds() {
        assertEquals(QualityBand.Shallow, qualityBand(0.1, visits = 20))
        assertEquals(QualityBand.Good, qualityBand(0.2, visits = 100))
        assertEquals(QualityBand.Fair, qualityBand(1.0, visits = 100))
        assertEquals(QualityBand.Inaccuracy, qualityBand(2.0, visits = 100))
        assertEquals(QualityBand.Mistake, qualityBand(4.0, visits = 100))
        assertEquals(QualityBand.BigMistake, qualityBand(8.0, visits = 100))
        assertEquals(QualityBand.Blunder, qualityBand(13.0, visits = 100))
    }

    @Test
    fun recentQualitiesKeepsLastThreePerColor() {
        val marks = (0 until 5).flatMap { i ->
            listOf(
                QualityMark(Point(i, 0), i.toDouble(), 100, QualityBand.Good, StoneColor.Black),
                QualityMark(Point(i, 1), i.toDouble(), 100, QualityBand.Fair, StoneColor.White),
            )
        }
        val recent = recentQualities(marks, 3)
        assertEquals(6, recent.size)
        assertEquals(3, recent.count { it.color == StoneColor.Black })
        assertEquals(3, recent.count { it.color == StoneColor.White })
        assertEquals(setOf(Point(2, 0), Point(3, 0), Point(4, 0)), recent.filter { it.color == StoneColor.Black }.map { it.point }.toSet())
        assertEquals(setOf(Point(2, 1), Point(3, 1), Point(4, 1)), recent.filter { it.color == StoneColor.White }.map { it.point }.toSet())
    }
}
