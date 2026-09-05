package com.acite.katahana.ui.session

import com.acite.katahana.domain.AiStyle
import com.acite.katahana.domain.GameConfig
import com.acite.katahana.domain.GameTree
import com.acite.katahana.domain.PlayMode
import com.acite.katahana.domain.Point
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SessionScreenSerializeTest {

    @Test
    fun gameConfigRoundTripsThroughJavaSerialization() {
        val original = GameConfig(
            boardSize = 13,
            komi = 0.5f,
            mode = PlayMode.HumanVsAi,
            rankKyu = 5,
            humanPlaysBlack = false,
            aiStyle = AiStyle.Full,
        )
        val restored = roundTrip(original)
        assertIs<GameConfig>(restored)
        assertEquals(original, restored)
    }

    @Test
    fun sessionScreenWithConfigSerializes() {
        val screen = SessionScreen(
            GameConfig(boardSize = 9, mode = PlayMode.HumanVsHuman),
        )
        val restored = roundTrip(screen)
        assertIs<SessionScreen>(restored)
        assertEquals(screen.key, restored.key)
    }

    @Test
    fun sessionScreenWithLoadedTreeSerializesWithoutTheTree() {
        val tree = GameTree(9)
        tree.play(Point(3, 3))
        val screen = SessionScreen(
            config = GameConfig(boardSize = 9),
            loadedTree = tree,
            recentId = "abc",
            recentTitle = "night game",
        )
        val restored = roundTrip(screen)
        assertIs<SessionScreen>(restored)
        assertEquals(screen.key, restored.key)
    }

    private fun roundTrip(value: Any): Any {
        val bytes = ByteArrayOutputStream()
        ObjectOutputStream(bytes).use { it.writeObject(value) }
        return ObjectInputStream(ByteArrayInputStream(bytes.toByteArray())).use { it.readObject() }
    }
}
