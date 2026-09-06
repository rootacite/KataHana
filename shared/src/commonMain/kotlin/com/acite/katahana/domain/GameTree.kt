package com.acite.katahana.domain

class Node(
    val id: String,
    val parent: Node?,
    val move: Move?,
    val position: Position,
    val children: MutableList<Node> = mutableListOf(),
) {
    var preferredChild: Int = 0

    val moveNumber: Int = generateSequence(this) { it.parent }.count { it.move != null }

    /** Child indices from the tree root down to this node. */
    fun pathFromRoot(): List<Int> {
        val acc = ArrayDeque<Int>()
        var node = this
        while (true) {
            val parent = node.parent ?: break
            val idx = parent.children.indexOf(node)
            if (idx < 0) break
            acc.addFirst(idx)
            node = parent
        }
        return acc.toList()
    }
}

class GameTree(
    val size: Int,
    val komi: Float = 7.5f,
    val rules: String = "chinese",
    rootPosition: Position? = null,
) {
    val root: Node = Node(
        id = "n0",
        parent = null,
        move = null,
        position = rootPosition ?: Position.empty(size),
    )

    var current: Node = root
        private set

    private var nextId = 1L
    private var lastLeafId: String = root.id

    val ended: Boolean get() = current.position.consecutivePasses >= 2

    val canUndo: Boolean get() = current.parent != null

    val canRedo: Boolean get() = current.children.isNotEmpty()

    /** Off a leaf: remaining moves exist, so this is review rather than live play. */
    val reviewing: Boolean get() = canRedo

    fun lineMoves(): List<Move> = pathMoves(current)

    fun pathMoves(node: Node): List<Move> {
        val acc = ArrayDeque<Move>()
        var walk: Node? = node
        while (walk != null) {
            walk.move?.let { acc.addFirst(it) }
            walk = walk.parent
        }
        return acc.toList()
    }

    /** Root → current → preferredChild leaf. Does not move [current]. */
    fun preferredLine(): List<Node> {
        val acc = nodesFromRoot().toMutableList()
        var node = current
        while (node.children.isNotEmpty()) {
            val i = node.preferredChild.coerceIn(0, node.children.lastIndex)
            node = node.children[i]
            acc += node
        }
        return acc
    }

    /** Most recently occupied leaf, or the preferred continuation if still on an interior node. */
    fun lastLeaf(): Node = nodeById(lastLeafId) ?: leafFrom(current)

    /** Jump to [lastLeaf]. Clears review when that node is a leaf. */
    fun resumeLeaf(): Boolean {
        val target = lastLeaf()
        if (current === target) return false
        return goTo(target.id)
    }

    /** Prefer the continuation leaf from [current] when loading a tree mid-review. */
    fun syncResumeLeaf() {
        lastLeafId = leafFrom(current).id
    }

    private fun leafFrom(start: Node): Node {
        var node = start
        while (node.children.isNotEmpty()) {
            val i = node.preferredChild.coerceIn(0, node.children.lastIndex)
            node = node.children[i]
        }
        return node
    }

    private fun rememberLeaf() {
        if (current.children.isEmpty()) lastLeafId = current.id
    }

    fun play(point: Point): PlayResult {
        val result = Rules.tryPlay(current.position, point)
        if (result is PlayResult.Ok) {
            append(Move.Place(current.position.toPlay, point), result.position)
        }
        return result
    }

    fun pass(): PlayResult {
        val result = Rules.tryPass(current.position)
        if (result is PlayResult.Ok) {
            append(Move.Pass(current.position.toPlay), result.position)
        }
        return result
    }

    fun undo(): Boolean {
        val parent = current.parent ?: return false
        current = parent
        rememberLeaf()
        return true
    }

    fun redo(): Boolean {
        if (current.children.isEmpty()) return false
        val i = current.preferredChild.coerceIn(0, current.children.lastIndex)
        current = current.children[i]
        rememberLeaf()
        return true
    }

    fun undoTo(node: Node): Boolean {
        var guard = 0
        while (current !== node && current.parent != null && guard++ < 10_000) {
            current = current.parent!!
        }
        rememberLeaf()
        return current === node
    }

    fun goTo(id: String): Boolean {
        val target = nodeById(id) ?: return false
        val path = ArrayDeque<Node>()
        var walk: Node? = target
        while (walk != null) {
            path.addFirst(walk)
            walk = walk.parent
        }
        if (path.first() !== root) return false
        for (i in 0 until path.lastIndex) {
            val parent = path[i]
            val child = path[i + 1]
            val idx = parent.children.indexOf(child)
            if (idx < 0) return false
            parent.preferredChild = idx
        }
        current = target
        rememberLeaf()
        return true
    }

    fun cycleVariation(delta: Int): Boolean {
        val parent = current.parent ?: return false
        if (parent.children.size < 2) return false
        val idx = parent.children.indexOf(current)
        if (idx < 0) return false
        val next = (idx + delta).mod(parent.children.size)
        parent.preferredChild = next
        current = parent.children[next]
        rememberLeaf()
        return true
    }

    fun nodesFromRoot(): List<Node> {
        val acc = ArrayDeque<Node>()
        var node: Node? = current
        while (node != null) {
            acc.addFirst(node)
            node = node.parent
        }
        return acc.toList()
    }

    /** Child indices from root to [current], used to restore the viewing node. */
    fun childPath(): List<Int> = current.pathFromRoot()

    fun nodeAtPath(path: List<Int>): Node? {
        var node = root
        for (index in path) {
            node = node.children.getOrNull(index) ?: return null
        }
        return node
    }

    fun applyChildPath(path: List<Int>): Boolean {
        current = root
        for (index in path) {
            if (index !in current.children.indices) {
                syncResumeLeaf()
                return false
            }
            current.preferredChild = index
            current = current.children[index]
        }
        syncResumeLeaf()
        return true
    }

    fun variationIndex(): Int {
        val parent = current.parent ?: return 0
        return parent.children.indexOf(current).coerceAtLeast(0)
    }

    fun variationCount(): Int = current.parent?.children?.size ?: 1

    private fun append(move: Move, position: Position) {
        val existingIndex = current.children.indexOfFirst { child ->
            when (val m = child.move) {
                is Move.Place -> move is Move.Place && m.point == move.point && m.color == move.color
                is Move.Pass -> move is Move.Pass && m.color == move.color
                null -> false
            }
        }
        if (existingIndex >= 0) {
            current.preferredChild = existingIndex
            current = current.children[existingIndex]
            rememberLeaf()
            return
        }
        val node = Node(
            id = "n${nextId++}",
            parent = current,
            move = move,
            position = position,
        )
        current.children.add(node)
        current.preferredChild = current.children.lastIndex
        current = node
        rememberLeaf()
    }
}
