package com.acite.katahana.domain

class GameSession(config: GameConfig, tree: GameTree? = null) {
    var config: GameConfig = config
        private set

    val tree: GameTree = tree ?: GameTree(size = config.boardSize, komi = config.komi)

    init {
        if (this.tree.reviewing) this.tree.syncResumeLeaf()
    }

    val position: Position get() = tree.current.position

    val lastPlace: Point?
        get() = (tree.current.move as? Move.Place)?.point

    fun play(point: Point): PlayResult = tree.play(point)

    fun pass(): PlayResult = tree.pass()

    fun undo(): Boolean = tree.undo()

    fun redo(): Boolean = tree.redo()

    fun cycleVariation(delta: Int): Boolean = tree.cycleVariation(delta)

    fun goTo(id: String): Boolean = tree.goTo(id)

    fun exitReview(): Boolean = tree.resumeLeaf()

    val reviewing: Boolean get() = tree.reviewing

    fun setSeat(color: StoneColor, seat: PlayerSeat) {
        config = if (color == StoneColor.Black) {
            config.copy(black = seat)
        } else {
            config.copy(white = seat)
        }
    }

    /**
     * AI moves only at a leaf, on an AI seat's color, while the game is live.
     * Undo / jump into the interior is review: the human owns the next stone.
     */
    fun aiShouldMove(): Boolean {
        if (tree.ended) return false
        if (tree.reviewing) return false
        return config.seat(position.toPlay).isAi
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
            black = config.black,
            white = config.white,
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
    val black: PlayerSeat = PlayerSeat(),
    val white: PlayerSeat = PlayerSeat(),
    val variationIndex: Int = 0,
    val variationCount: Int = 1,
    val aiToPlay: Boolean = false,
) {
    val reviewing: Boolean get() = canRedo

    val mode: PlayMode
        get() = if (black.isAi || white.isAi) PlayMode.HumanVsAi else PlayMode.HumanVsHuman

    val rankKyu: Int
        get() = when {
            white.isAi -> white.rankKyu
            black.isAi -> black.rankKyu
            else -> 5
        }

    val humanPlaysBlack: Boolean get() = !black.isAi

    val aiStyle: AiStyle
        get() = when {
            white.isAi -> white.kind.toAiStyle()
            black.isAi -> black.kind.toAiStyle()
            else -> AiStyle.Human
        }

    /** Not an AI leaf that is about to move. Review and game-over still belong to the human. */
    val humanControls: Boolean
        get() = ended || reviewing || !seat(toPlay).isAi

    fun seat(color: StoneColor): PlayerSeat =
        if (color == StoneColor.Black) black else white

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
            black == other.black &&
            white == other.white &&
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
        result = 31 * result + black.hashCode()
        result = 31 * result + white.hashCode()
        result = 31 * result + variationIndex
        result = 31 * result + variationCount
        result = 31 * result + aiToPlay.hashCode()
        return result
    }
}
