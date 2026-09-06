package com.acite.katahana.domain

data class TreeLayout(
    val nodes: List<TreeLayoutNode>,
    val currentId: String,
    val cols: Int,
    val rows: Int,
)

data class TreeLayoutNode(
    val id: String,
    val parentId: String?,
    val col: Int,
    val row: Int,
    val moveNumber: Int,
    val color: StoneColor?,
    val label: String,
    val onActiveLine: Boolean,
    val isCurrent: Boolean,
    val isFuture: Boolean,
)

fun GameTree.nodeById(id: String): Node? {
    val stack = ArrayDeque<Node>()
    stack.add(root)
    while (stack.isNotEmpty()) {
        val node = stack.removeLast()
        if (node.id == id) return node
        node.children.forEach { stack.add(it) }
    }
    return null
}

fun GameTree.layout(): TreeLayout {
    val pathToCurrent = nodesFromRoot().map { it.id }.toHashSet()
    val active = LinkedHashSet<String>()
    var walk: Node? = lastLeaf()
    while (walk != null) {
        active.add(walk.id)
        walk = walk.parent
    }

    val out = ArrayList<TreeLayoutNode>()
    var nextRow = 0
    var maxCol = 0

    fun labelOf(node: Node): String = when (val move = node.move) {
        is Move.Place -> move.point.toGtp(size)
        is Move.Pass -> "P"
        null -> "·"
    }

    fun orderedKids(node: Node): List<Node> {
        val kids = node.children
        if (kids.isEmpty()) return emptyList()
        val nextActive = kids.firstOrNull { it.id in active }
        return if (nextActive == null) kids.toList()
        else listOf(nextActive) + kids.filter { it !== nextActive }
    }

    fun place(node: Node, col: Int): Int {
        maxCol = maxOf(maxCol, col)
        val kids = orderedKids(node)
        val row = if (kids.isEmpty()) {
            nextRow++
        } else {
            val firstRow = place(kids[0], col + 1)
            for (i in 1 until kids.size) place(kids[i], col + 1)
            firstRow
        }
        out += TreeLayoutNode(
            id = node.id,
            parentId = node.parent?.id,
            col = col,
            row = row,
            moveNumber = node.moveNumber,
            color = node.move?.color,
            label = labelOf(node),
            onActiveLine = node.id in active,
            isCurrent = node.id == current.id,
            isFuture = node.id in active && node.id !in pathToCurrent,
        )
        return row
    }

    place(root, 0)
    return TreeLayout(
        nodes = out,
        currentId = current.id,
        cols = maxCol + 1,
        rows = maxOf(nextRow, 1),
    )
}
