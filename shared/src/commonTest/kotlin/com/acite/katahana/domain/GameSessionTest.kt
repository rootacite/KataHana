package com.acite.katahana.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GameSessionTest {

    @Test
    fun humanBlackAiMovesOnlyAtLeafAfterHuman() {
        val session = GameSession(
            GameConfig(boardSize = 9, mode = PlayMode.HumanVsAi, humanPlaysBlack = true),
        )
        assertFalse(session.aiShouldMove())
        assertFalse(session.reviewing)
        session.play(Point(3, 3))
        assertTrue(session.aiShouldMove())
        session.play(Point(5, 5))
        assertFalse(session.aiShouldMove())
        session.undo()
        assertTrue(session.reviewing)
        assertFalse(session.aiShouldMove())
        assertEquals(StoneColor.White, session.position.toPlay)
    }

    @Test
    fun undoAiMoveThenHumanBranchLeavesAiWaitingUntilLeaf() {
        val session = GameSession(
            GameConfig(boardSize = 9, mode = PlayMode.HumanVsAi, humanPlaysBlack = true),
        )
        session.play(Point(3, 3))
        session.play(Point(5, 5))
        session.undo()
        assertTrue(session.reviewing)
        assertFalse(session.aiShouldMove())
        session.play(Point(4, 4))
        assertFalse(session.reviewing)
        assertEquals(2, session.tree.root.children[0].children.size)
        assertFalse(session.aiShouldMove())
        assertEquals(StoneColor.Black, session.position.toPlay)
    }

    @Test
    fun humanWhiteAiMovesAtRootLeafUntilUndo() {
        val session = GameSession(
            GameConfig(boardSize = 9, mode = PlayMode.HumanVsAi, humanPlaysBlack = false),
        )
        assertTrue(session.aiShouldMove())
        session.play(Point(3, 3))
        assertFalse(session.aiShouldMove())
        session.undo()
        assertTrue(session.reviewing)
        assertFalse(session.aiShouldMove())
        assertEquals(StoneColor.Black, session.position.toPlay)
    }

    @Test
    fun humanVsHumanNeverAsksAi() {
        val session = GameSession(GameConfig(boardSize = 9, mode = PlayMode.HumanVsHuman))
        assertFalse(session.aiShouldMove())
        session.play(Point(3, 3))
        assertFalse(session.aiShouldMove())
        session.undo()
        assertFalse(session.aiShouldMove())
    }

    @Test
    fun snapshotMarksAiToPlayAndReviewing() {
        val session = GameSession(
            GameConfig(boardSize = 9, mode = PlayMode.HumanVsAi, humanPlaysBlack = true),
        )
        assertFalse(session.snapshot().aiToPlay)
        assertFalse(session.snapshot().reviewing)
        session.play(Point(2, 2))
        assertTrue(session.snapshot().aiToPlay)
        session.undo()
        val snap = session.snapshot()
        assertTrue(snap.reviewing)
        assertFalse(snap.aiToPlay)
        assertTrue(snap.canRedo)
    }
}
