package com.acite.katahana.domain

class GameSession(val config: GameConfig, tree: GameTree? = null) {
    val tree: GameTree = tree ?: GameTree(size = config.boardSize, komi = config.komi)

    val position: Position get() = tree.current.position

    val lastPlace: Point?
        get() = (tree.current.move as? Move.Place)?.point

    fun play(point: Point): PlayResult = tree.play(point)

    fun pass(): PlayResult = tree.pass()

    fun undo(): Boolean = tree.undo()

    fun redo(): Boolean = tree.redo()

    fun cycleVariation(delta: Int): Boolean = tree.cycleVariation(delta)

    fun goTo(id: String): Boolean = tree.goTo(id)

    val reviewing: Boolean get() = tree.reviewing

    /**
     * AI moves only at a leaf, on its color, in HvAI, while the game is live.
     * Undo / jump into the interior is review: the human owns the next stone.
     */
    fun aiShouldMove(): Boolean {
        if (config.mode != PlayMode.HumanVsAi) return false
        if (tree.ended) return false
        if (tree.reviewing) return false
        val aiIsBlack = !config.humanPlaysBlack
        return (position.toPlay == StoneColor.Black) == aiIsBlack
    }

    fun snapshot(): SessionSnapshot {
        val pos = tree.current.position
        return SessionSnapshot(
            size = pos.size,
            cells = pos.copyCells(),
            toPlay = pos.toPlay,
            lastMove = lastPlace,
            lastWasPass = tree.current.move is Move.Pass,
            capturedByBlack = pos.capturedByBlack,
            capturedByWhite = pos.capturedByWhite,
            canUndo = tree.canUndo,
            canRedo = tree.canRedo,
            ended = tree.ended,
            moveNumber = tree.current.moveNumber,
            komi = tree.komi,
            mode = config.mode,
            rankKyu = config.rankKyu,
            humanPlaysBlack = config.humanPlaysBlack,
            aiStyle = config.aiStyle,
            variationIndex = tree.variationIndex(),
            variationCount = tree.variationCount(),
            aiToPlay = aiShouldMove(),
        )
    }
}

data class SessionSnapshot(
    val size: Int,
    val cells: IntArray,
    val toPlay: StoneColor,
    val lastMove: Point?,
    val lastWasPass: Boolean,
    val capturedByBlack: Int,
    val capturedByWhite: Int,
    val canUndo: Boolean,
    val canRedo: Boolean,
    val ended: Boolean,
    val moveNumber: Int,
    val komi: Float,
    val mode: PlayMode,
    val rankKyu: Int,
    val humanPlaysBlack: Boolean,
    val aiStyle: AiStyle = AiStyle.Rank,
    val variationIndex: Int = 0,
    val variationCount: Int = 1,
    val aiToPlay: Boolean = false,
) {
    val reviewing: Boolean get() = canRedo

    fun stoneAt(x: Int, y: Int): StoneColor? =
        StoneColor.fromCell(cells[y * size + x])

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SessionSnapshot) return false
        return size == other.size &&
            cells.contentEquals(other.cells) &&
            toPlay == other.toPlay &&
            lastMove == other.lastMove &&
            lastWasPass == other.lastWasPass &&
            capturedByBlack == other.capturedByBlack &&
            capturedByWhite == other.capturedByWhite &&
            canUndo == other.canUndo &&
            canRedo == other.canRedo &&
            ended == other.ended &&
            moveNumber == other.moveNumber &&
            komi == other.komi &&
            mode == other.mode &&
            rankKyu == other.rankKyu &&
            humanPlaysBlack == other.humanPlaysBlack &&
            aiStyle == other.aiStyle &&
            variationIndex == other.variationIndex &&
            variationCount == other.variationCount &&
            aiToPlay == other.aiToPlay
    }

    override fun hashCode(): Int {
        var result = size
        result = 31 * result + cells.contentHashCode()
        result = 31 * result + toPlay.hashCode()
        result = 31 * result + (lastMove?.hashCode() ?: 0)
        result = 31 * result + lastWasPass.hashCode()
        result = 31 * result + capturedByBlack
        result = 31 * result + capturedByWhite
        result = 31 * result + canUndo.hashCode()
        result = 31 * result + canRedo.hashCode()
        result = 31 * result + ended.hashCode()
        result = 31 * result + moveNumber
        result = 31 * result + komi.hashCode()
        result = 31 * result + mode.hashCode()
        result = 31 * result + rankKyu
        result = 31 * result + humanPlaysBlack.hashCode()
        result = 31 * result + aiStyle.hashCode()
        result = 31 * result + variationIndex
        result = 31 * result + variationCount
        result = 31 * result + aiToPlay.hashCode()
        return result
    }
}
