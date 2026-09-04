package com.acite.katahana.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TreeLayoutTest {

    @Test
    fun emptyTreeIsSingleRoot() {
        val layout = GameTree(9).layout()
        assertEquals(1, layout.nodes.size)
        assertEquals(1, layout.cols)
        assertEquals(1, layout.rows)
        val root = layout.nodes.single()
        assertTrue(root.isCurrent)
        assertFalse(root.isFuture)
        assertEquals("·", root.label)
    }

    @Test
    fun activeLineSharesARowAndVariationGetsAnother() {
        val tree = GameTree(9)
        tree.play(Point(3, 3))
        tree.play(Point(5, 5))
        val mainSecond = tree.current.id
        tree.undo()
        tree.play(Point(4, 4))
        val layout = tree.layout()
        assertEquals(2, layout.rows)
        val current = layout.nodes.first { it.isCurrent }
        val root = layout.nodes.first { it.parentId == null }
        val other = layout.nodes.first { it.id == mainSecond }
        assertEquals(root.row, current.row)
        assertTrue(other.row != current.row)
        assertTrue(current.onActiveLine)
        assertFalse(other.onActiveLine)
        assertFalse(current.isFuture)
    }

    @Test
    fun futureNodesArePreferredContinuationPastCurrent() {
        val tree = GameTree(9)
        tree.play(Point(3, 3))
        tree.play(Point(5, 5))
        tree.undo()
        val layout = tree.layout()
        val current = layout.nodes.first { it.isCurrent }
        val future = layout.nodes.first { it.isFuture }
        assertEquals(1, current.moveNumber)
        assertEquals(2, future.moveNumber)
        assertTrue(future.onActiveLine)
        assertEquals(current.row, future.row)
    }
}
