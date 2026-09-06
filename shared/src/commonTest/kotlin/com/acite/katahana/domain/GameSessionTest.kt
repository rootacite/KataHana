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
        assertTrue(snap.humanControls)
    }

    @Test
    fun bothAiSeatsMoveAtEveryLiveLeaf() {
        val session = GameSession(
            GameConfig(
                boardSize = 9,
                black = PlayerSeat(SeatKind.Rank, 5),
                white = PlayerSeat(SeatKind.HumanLike, 3),
            ),
        )
        assertTrue(session.aiShouldMove())
        assertFalse(session.snapshot().humanControls)
        session.play(Point(3, 3))
        assertTrue(session.aiShouldMove())
        session.undo()
        assertTrue(session.reviewing)
        assertFalse(session.aiShouldMove())
        assertTrue(session.snapshot().humanControls)
    }

    @Test
    fun takingOverCurrentSeatStopsAi() {
        val session = GameSession(
            GameConfig(
                boardSize = 9,
                black = PlayerSeat(SeatKind.Full),
                white = PlayerSeat(SeatKind.Rank, 8),
            ),
        )
        assertTrue(session.aiShouldMove())
        session.setSeat(StoneColor.Black, PlayerSeat())
        assertFalse(session.aiShouldMove())
        assertTrue(session.snapshot().humanControls)
        assertEquals(SeatKind.Rank, session.config.white.kind)
    }

    @Test
    fun humanControlsAtGameOverEvenIfNextSeatIsAi() {
        val session = GameSession(
            GameConfig(boardSize = 9, mode = PlayMode.HumanVsAi, humanPlaysBlack = true),
        )
        session.pass()
        session.pass()
        val snap = session.snapshot()
        assertTrue(snap.ended)
        assertFalse(snap.aiToPlay)
        assertTrue(snap.humanControls)
    }

    @Test
    fun exitReviewReturnsToLastLeaf() {
        val session = GameSession(GameConfig(boardSize = 9))
        session.play(Point(3, 3))
        session.play(Point(5, 5))
        val leaf = session.tree.current.id
        session.undo()
        assertTrue(session.reviewing)
        assertTrue(session.exitReview())
        assertEquals(leaf, session.tree.current.id)
        assertFalse(session.reviewing)
    }

    @Test
    fun sessionSeedsResumeLeafWhenLoadedMidReview() {
        val tree = GameTree(9)
        tree.play(Point(4, 4))
        tree.play(Point(3, 3))
        val leaf = tree.current.id
        tree.applyChildPath(listOf(0))
        assertTrue(tree.reviewing)
        val session = GameSession(GameConfig(boardSize = 9), tree)
        assertEquals(leaf, session.tree.lastLeaf().id)
        assertTrue(session.exitReview())
        assertEquals(leaf, session.tree.current.id)
    }
}
