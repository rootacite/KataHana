package com.acite.katahana.sgf

import com.acite.katahana.domain.GameConfig
import com.acite.katahana.domain.GameTree
import com.acite.katahana.domain.Move
import com.acite.katahana.domain.Point
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SgfIoTest {

    @Test
    fun roundTripTwoMovesOn9() {
        val tree = GameTree(9)
        tree.play(Point(3, 3))
        tree.play(Point(5, 5))
        val text = writeSgf(tree, GameConfig(boardSize = 9))
        assertTrue(text.contains("SZ[9]"))
        assertTrue(text.contains("B[dd]"))
        assertTrue(text.contains("W[ff]"))
        val loaded = parseSgf(text)
        assertEquals(9, loaded.tree.size)
        assertTrue(loaded.tree.redo())
        val first = assertIs<Move.Place>(loaded.tree.current.move)
        assertEquals(Point(3, 3), first.point)
        assertTrue(loaded.tree.redo())
        val second = assertIs<Move.Place>(loaded.tree.current.move)
        assertEquals(Point(5, 5), second.point)
    }

    @Test
    fun roundTripVariationAndPass() {
        val tree = GameTree(9)
        tree.play(Point(2, 2))
        tree.undo()
        tree.play(Point(4, 4))
        tree.pass()
        val text = writeSgf(tree, GameConfig(boardSize = 9))
        val loaded = parseSgf(text)
        assertEquals(2, loaded.tree.root.children.size)
        assertTrue(loaded.tree.redo())
        assertEquals(Point(2, 2), (loaded.tree.current.move as Move.Place).point)
        assertTrue(loaded.tree.cycleVariation(1))
        assertEquals(Point(4, 4), (loaded.tree.current.move as Move.Place).point)
        assertTrue(loaded.tree.redo())
        assertIs<Move.Pass>(loaded.tree.current.move)
    }

    @Test
    fun skipIFileCoordsAreSgfLetters() {
        val tree = GameTree(19)
        tree.play(Point(8, 0))
        val text = writeSgf(tree, GameConfig(boardSize = 19))
        assertTrue(text.contains("B[ia]"), text)
        val loaded = parseSgf(text)
        loaded.tree.redo()
        assertEquals(Point(8, 0), (loaded.tree.current.move as Move.Place).point)
    }
}
