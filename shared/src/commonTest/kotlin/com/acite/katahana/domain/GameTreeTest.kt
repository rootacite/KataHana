package com.acite.katahana.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GameTreeTest {

    @Test
    fun undoRestoresStonesToPlayAndCaptures() {
        val tree = GameTree(9)
        tree.play(Point(1, 0))
        tree.play(Point(0, 0))
        assertIs<PlayResult.Ok>(tree.play(Point(0, 1)))
        assertEquals(1, tree.current.position.capturedByBlack)
        assertEquals(StoneColor.White, tree.current.position.toPlay)

        assertTrue(tree.undo())
        assertEquals(0, tree.current.position.capturedByBlack)
        assertEquals(StoneColor.Black, tree.current.position.toPlay)
        assertEquals(StoneColor.White, tree.current.position.stoneAt(0, 0))
        assertEquals(null, tree.current.position.stoneAt(0, 1))

        assertTrue(tree.undo())
        assertTrue(tree.undo())
        assertEquals(StoneColor.Black, tree.current.position.toPlay)
        assertEquals(null, tree.current.position.stoneAt(1, 0))
        assertFalse(tree.undo())
    }

    @Test
    fun redoReplaysUndoneMove() {
        val tree = GameTree(9)
        tree.play(Point(4, 4))
        tree.undo()
        assertTrue(tree.redo())
        assertEquals(StoneColor.Black, tree.current.position.stoneAt(4, 4))
        assertEquals(StoneColor.White, tree.current.position.toPlay)
    }

    @Test
    fun playAfterUndoKeepsSiblingVariation() {
        val tree = GameTree(9)
        tree.play(Point(4, 4))
        tree.undo()
        tree.play(Point(3, 3))
        assertEquals(StoneColor.Black, tree.current.position.stoneAt(3, 3))
        assertEquals(null, tree.current.position.stoneAt(4, 4))
        assertEquals(2, tree.variationCount())
        assertFalse(tree.redo())
        assertTrue(tree.cycleVariation(1))
        assertEquals(StoneColor.Black, tree.current.position.stoneAt(4, 4))
        assertEquals(null, tree.current.position.stoneAt(3, 3))
    }

    @Test
    fun sessionSnapshotTracksLastMove() {
        val session = GameSession(GameConfig(boardSize = 13, komi = 7.5f))
        session.play(Point(6, 6))
        val snap = session.snapshot()
        assertEquals(13, snap.size)
        assertEquals(Point(6, 6), snap.lastMove)
        assertEquals(1, snap.moveNumber)
        assertEquals(StoneColor.White, snap.toPlay)
    }

    @Test
    fun lineMovesWalksRootToCurrent() {
        val tree = GameTree(9)
        assertEquals(emptyList(), tree.lineMoves())
        tree.play(Point(2, 2))
        tree.pass()
        val moves = tree.lineMoves()
        assertEquals(2, moves.size)
        assertIs<Move.Place>(moves[0])
        assertIs<Move.Pass>(moves[1])
        tree.undo()
        assertEquals(1, tree.lineMoves().size)
    }

    @Test
    fun goToSetsPreferredChildSoRedoFollowsThatLine() {
        val tree = GameTree(9)
        tree.play(Point(4, 4))
        val first = tree.current.id
        tree.undo()
        tree.play(Point(3, 3))
        val second = tree.current.id
        assertTrue(tree.goTo(first))
        assertEquals(first, tree.current.id)
        tree.undo()
        assertTrue(tree.redo())
        assertEquals(first, tree.current.id)
        assertTrue(tree.goTo(second))
        assertEquals(second, tree.current.id)
    }

    @Test
    fun preferredLineFollowsPreferredChildPastCurrent() {
        val tree = GameTree(9)
        tree.play(Point(4, 4))
        tree.play(Point(3, 3))
        val leaf = tree.current.id
        tree.undo()
        val mid = tree.current.id
        assertEquals(listOf("n0", mid, leaf), tree.preferredLine().map { it.id })
        assertEquals(mid, tree.current.id)
    }

    @Test
    fun pathMovesDoesNotMoveCurrent() {
        val tree = GameTree(9)
        tree.play(Point(4, 4))
        val first = tree.current
        tree.play(Point(3, 3))
        val currentId = tree.current.id
        val moves = tree.pathMoves(first)
        assertEquals(1, moves.size)
        assertIs<Move.Place>(moves[0])
        assertEquals(Point(4, 4), (moves[0] as Move.Place).point)
        assertEquals(currentId, tree.current.id)
    }

    @Test
    fun reviewingIsTrueExactlyWhenNotAtALeaf() {
        val tree = GameTree(9)
        assertFalse(tree.reviewing)
        tree.play(Point(4, 4))
        assertFalse(tree.reviewing)
        tree.undo()
        assertTrue(tree.reviewing)
        tree.redo()
        assertFalse(tree.reviewing)
    }

    @Test
    fun childPathRoundTripsThroughVariation() {
        val tree = GameTree(9)
        tree.play(Point(2, 2))
        tree.undo()
        tree.play(Point(4, 4))
        tree.play(Point(5, 5))
        val path = tree.childPath()
        assertEquals(listOf(1, 0), path)
        tree.applyChildPath(emptyList())
        assertEquals(tree.root.id, tree.current.id)
        assertTrue(tree.applyChildPath(path))
        assertEquals(Point(5, 5), (tree.current.move as Move.Place).point)
        assertEquals(Point(4, 4), (tree.current.parent?.move as Move.Place).point)
    }
}
