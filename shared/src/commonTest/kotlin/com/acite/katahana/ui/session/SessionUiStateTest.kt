package com.acite.katahana.ui.session

import com.acite.katahana.domain.PlayMode
import com.acite.katahana.domain.Point
import com.acite.katahana.domain.Position
import com.acite.katahana.domain.SessionSnapshot
import com.acite.katahana.domain.StoneColor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SessionUiStateTest {

    @Test
    fun previewUsesHoverOnHumanTurn() {
        val ui = SessionUiState(snapshot = snap(aiToPlay = false), hover = Point(3, 3))
        assertEquals(Point(3, 3), ui.preview)
    }

    @Test
    fun previewPrefersSelectedOverHover() {
        val ui = SessionUiState(
            snapshot = snap(aiToPlay = false),
            selected = Point(1, 1),
            hover = Point(3, 3),
        )
        assertEquals(Point(1, 1), ui.preview)
    }

    @Test
    fun previewHiddenWhileAiToPlay() {
        val ui = SessionUiState(
            snapshot = snap(aiToPlay = true, toPlay = StoneColor.White),
            hover = Point(3, 3),
            selected = Point(2, 2),
        )
        assertNull(ui.preview)
    }

    @Test
    fun previewHiddenWhenGameEnded() {
        val ui = SessionUiState(snapshot = snap(ended = true), hover = Point(3, 3))
        assertNull(ui.preview)
    }

    private fun snap(
        aiToPlay: Boolean = false,
        ended: Boolean = false,
        toPlay: StoneColor = StoneColor.Black,
    ): SessionSnapshot {
        val pos = Position.of(9)
        return SessionSnapshot(
            size = 9,
            cells = pos.copyCells(),
            toPlay = toPlay,
            lastMove = null,
            lastWasPass = false,
            capturedByBlack = 0,
            capturedByWhite = 0,
            canUndo = false,
            canRedo = false,
            ended = ended,
            moveNumber = 1,
            komi = 6.5f,
            mode = PlayMode.HumanVsAi,
            rankKyu = 5,
            humanPlaysBlack = true,
            aiToPlay = aiToPlay,
        )
    }
}
