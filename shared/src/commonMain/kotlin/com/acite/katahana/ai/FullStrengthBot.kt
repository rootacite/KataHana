package com.acite.katahana.ai

import com.acite.katahana.domain.Move
import com.acite.katahana.domain.Point
import com.acite.katahana.domain.StoneColor
import com.acite.katahana.engine.MoveInfo

object FullStrengthBot {
    fun choose(moveInfos: List<MoveInfo>, boardSize: Int, toPlay: StoneColor): Move {
        val best = moveInfos.minByOrNull { it.order } ?: return Move.Pass(toPlay)
        if (best.move.equals("pass", ignoreCase = true)) return Move.Pass(toPlay)
        val point = Point.fromGtp(best.move, boardSize) ?: return Move.Pass(toPlay)
        return Move.Place(toPlay, point)
    }
}
