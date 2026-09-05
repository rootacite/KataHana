package com.acite.katahana.ai

import com.acite.katahana.domain.Move
import com.acite.katahana.domain.Point
import com.acite.katahana.domain.Position
import com.acite.katahana.domain.StoneColor
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class HumanBotTest {

    @Test
    fun peakedPolicyUsuallyPlaysTheTopLegal() {
        val size = 9
        val policy = DoubleArray(size * size + 1) { 0.001 }
        policy[40] = 0.90
        val pos = Position.empty(size)
        val hits = (0 until 40).count { seed ->
            val move = HumanBot.choose(policy.toList(), pos, Random(seed))
            move is Move.Place && move.point == Point.fromIndex(40, size)
        }
        assertTrue(hits >= 30, "expected the peak to win most draws, got $hits/40")
    }

    @Test
    fun occupiedHighPolicyIsNotPlayed() {
        val size = 9
        val occupied = Point.fromIndex(40, size)
        val pos = Position.of(size, black = listOf(occupied), toPlay = StoneColor.White)
        val policy = DoubleArray(size * size + 1) { 0.002 }
        policy[40] = 0.9
        policy[0] = 0.08
        val move = HumanBot.choose(policy.toList(), pos, Random(1))
        val place = assertIs<Move.Place>(move)
        assertTrue(place.point != occupied)
    }

    @Test
    fun samplingIsDeterministicWithFixedSeed() {
        val size = 9
        val policy = DoubleArray(size * size + 1) { i ->
            if (i == size * size) 0.01 else 0.02 + (i % 17) * 0.001
        }.toList()
        val pos = Position.empty(size)
        val a = HumanBot.choose(policy, pos, Random(42))
        val b = HumanBot.choose(policy, pos, Random(42))
        assertEquals(a, b)
    }

    @Test
    fun flatPolicyIsNotLockedToASinglePoint() {
        val size = 9
        val policy = DoubleArray(size * size + 1) { 0.01 }.toList()
        val pos = Position.empty(size)
        val distinct = (0 until 30).map { seed ->
            HumanBot.choose(policy, pos, Random(seed))
        }.toSet()
        assertTrue(distinct.size >= 8, "flat human policy should wander, got ${distinct.size}")
    }
}
