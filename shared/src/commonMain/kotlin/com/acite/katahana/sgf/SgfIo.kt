package com.acite.katahana.sgf

import com.acite.katahana.domain.AiStyle
import com.acite.katahana.domain.GameConfig
import com.acite.katahana.domain.GameTree
import com.acite.katahana.domain.Move
import com.acite.katahana.domain.Node
import com.acite.katahana.domain.PlayMode
import com.acite.katahana.domain.PlayResult
import com.acite.katahana.domain.Point
import com.acite.katahana.domain.Position
import com.acite.katahana.domain.StoneColor

data class SgfGame(
    val tree: GameTree,
    val config: GameConfig,
    val blackName: String = "Black",
    val whiteName: String = "White",
    val warnings: List<String> = emptyList(),
)

fun Point.toSgf(): String = "${'a' + x}${'a' + y}"

fun Point.Companion.fromSgf(coord: String, boardSize: Int): Point? {
    if (coord.isEmpty() || coord.equals("tt", ignoreCase = true)) return null
    if (coord.length != 2) return null
    val x = coord[0].lowercaseChar() - 'a'
    val y = coord[1].lowercaseChar() - 'a'
    val point = Point(x, y)
    return point.takeIf { it.inBounds(boardSize) }
}

fun writeSgf(
    tree: GameTree,
    config: GameConfig,
    blackName: String = "Black",
    whiteName: String = "White",
): String {
    val sb = StringBuilder()
    sb.append("(;FF[4]GM[1]CA[UTF-8]AP[KataHana]")
    sb.append("SZ[").append(tree.size).append(']')
    sb.append("KM[").append(tree.komi).append(']')
    sb.append("RU[").append(tree.rules.replaceFirstChar { it.uppercaseChar() }).append(']')
    sb.append("PB[").append(escape(blackName)).append(']')
    sb.append("PW[").append(escape(whiteName)).append(']')
    if (config.mode == PlayMode.HumanVsAi) {
        sb.append("GN[").append(escape("HvAI ${config.aiStyle} ${config.rankKyu}")).append(']')
    }
    writeBranches(tree.root, sb)
    sb.append(')')
    return sb.toString()
}

fun parseSgf(text: String): SgfGame {
    val reader = SgfReader(text)
    val rootNode = reader.parseGameTree()
        ?: throw IllegalArgumentException("Empty SGF")
    val warnings = ArrayList<String>()
    val size = rootNode.firstInt("SZ")?.let { if (it == 9 || it == 13 || it == 19) it else 19 } ?: 19
    val komi = rootNode.first("KM")?.toFloatOrNull() ?: 7.5f
    val black = rootNode.first("PB") ?: "Black"
    val white = rootNode.first("PW") ?: "White"
    val ab = rootNode.values("AB").mapNotNull { Point.fromSgf(it, size) }
    val aw = rootNode.values("AW").mapNotNull { Point.fromSgf(it, size) }
    val toPlay = when (rootNode.first("PL")?.uppercase()) {
        "W" -> StoneColor.White
        else -> StoneColor.Black
    }
    val rootPos = Position.of(
        size = size,
        black = ab,
        white = aw,
        toPlay = toPlay,
    )
    val tree = GameTree(size = size, komi = komi, rules = "chinese", rootPosition = rootPos)
    applyMoves(tree, rootNode, warnings)
    val config = GameConfig(
        boardSize = size,
        komi = komi,
        mode = PlayMode.HumanVsHuman,
        aiStyle = AiStyle.Rank,
    )
    return SgfGame(tree, config, black, white, warnings)
}

private fun applyMoves(tree: GameTree, sgfRoot: SgfNode, warnings: MutableList<String>) {
    fun playColor(color: StoneColor, coord: String) {
        if (tree.current.position.toPlay != color) {
            warnings += "Expected ${tree.current.position.toPlay} at $coord"
        }
        val point = Point.fromSgf(coord, tree.size)
        val result = if (point == null) tree.pass() else tree.play(point)
        if (result is PlayResult.Illegal) {
            warnings += "Skipped illegal ${color.name} $coord"
        }
    }

    fun walk(node: SgfNode, isRoot: Boolean) {
        if (!isRoot || node.hasMove()) {
            node.values("B").firstOrNull()?.let { playColor(StoneColor.Black, it) }
            node.values("W").firstOrNull()?.let { playColor(StoneColor.White, it) }
        }
        if (node.children.isEmpty()) return
        val here = tree.current
        node.children.forEachIndexed { index, child ->
            if (index > 0) tree.undoTo(here)
            walk(child, isRoot = false)
        }
        here.preferredChild = 0
    }
    walk(sgfRoot, isRoot = true)
    while (tree.undo()) {
        /* review starts at root */
    }
}

private fun writeBranches(node: Node, sb: StringBuilder) {
    val kids = node.children
    if (kids.isEmpty()) return
    if (kids.size == 1) {
        sb.append(moveProp(kids[0].move))
        writeBranches(kids[0], sb)
        return
    }
    for (child in kids) {
        sb.append('(')
        sb.append(moveProp(child.move))
        writeBranches(child, sb)
        sb.append(')')
    }
}

private fun moveProp(move: Move?): String = when (move) {
    is Move.Place -> {
        val tag = if (move.color == StoneColor.Black) "B" else "W"
        ";$tag[${move.point.toSgf()}]"
    }
    is Move.Pass -> {
        val tag = if (move.color == StoneColor.Black) "B" else "W"
        ";$tag[]"
    }
    null -> ""
}

private fun escape(value: String): String =
    value.replace("\\", "\\\\").replace("]", "\\]")

private class SgfNode(
    val props: MutableMap<String, MutableList<String>> = mutableMapOf(),
    val children: MutableList<SgfNode> = mutableListOf(),
) {
    fun first(id: String): String? = props[id]?.firstOrNull()
    fun firstInt(id: String): Int? = first(id)?.toIntOrNull()
    fun values(id: String): List<String> = props[id] ?: emptyList()
    fun hasMove(): Boolean = props.containsKey("B") || props.containsKey("W")
}

private class SgfReader(private val text: String) {
    private var i = 0

    fun parseGameTree(): SgfNode? {
        skipWs()
        if (!eat('(')) return null
        val first = parseNode() ?: run {
            skipUntilClose()
            return SgfNode()
        }
        var last = first
        while (true) {
            skipWs()
            when {
                peek() == ';' -> {
                    val next = parseNode() ?: break
                    last.children.add(next)
                    last = next
                }
                peek() == '(' -> {
                    while (peek() == '(') {
                        parseGameTree()?.let { last.children.add(it) }
                        skipWs()
                    }
                    break
                }
                else -> break
            }
        }
        skipUntilClose()
        return first
    }

    private fun parseNode(): SgfNode? {
        skipWs()
        if (!eat(';')) return null
        val node = SgfNode()
        while (true) {
            skipWs()
            if (i >= text.length || !text[i].isUpperCase()) break
            val ident = readIdent()
            val values = mutableListOf<String>()
            while (true) {
                skipWs()
                if (!eat('[')) break
                values += readValue()
            }
            if (ident.isNotEmpty()) {
                node.props.getOrPut(ident) { mutableListOf() }.addAll(values)
            }
        }
        return node
    }

    private fun readIdent(): String {
        val start = i
        while (i < text.length && text[i].isUpperCase()) i++
        return text.substring(start, i)
    }

    private fun readValue(): String {
        val sb = StringBuilder()
        while (i < text.length) {
            val c = text[i++]
            when {
                c == '\\' && i < text.length -> sb.append(text[i++])
                c == ']' -> return sb.toString()
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }

    private fun skipWs() {
        while (i < text.length && text[i].isWhitespace()) i++
    }

    private fun peek(): Char? = text.getOrNull(i)
    private fun eat(c: Char): Boolean {
        skipWs()
        if (peek() != c) return false
        i++
        return true
    }

    private fun skipUntilClose() {
        var depth = 1
        while (i < text.length && depth > 0) {
            when (text[i++]) {
                '[' -> {
                    i--
                    eat('[')
                    readValue()
                }
                '(' -> depth++
                ')' -> depth--
            }
        }
    }
}
