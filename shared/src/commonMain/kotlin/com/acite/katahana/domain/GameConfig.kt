package com.acite.katahana.domain

import cafe.adriel.voyager.core.lifecycle.JavaSerializable

enum class PlayMode {
    HumanVsHuman,
    HumanVsAi,
}

enum class AiStyle {
    Human,
    Rank,
    Full,
}

enum class SeatKind {
    Human,
    Rank,
    HumanLike,
    Full,
}

data class PlayerSeat(
    val kind: SeatKind = SeatKind.Human,
    val rankKyu: Int = 5,
) : JavaSerializable {
    init {
        require(rankKyu in -2..15)
    }

    val isAi: Boolean get() = kind != SeatKind.Human
}

data class GameConfig(
    val boardSize: Int = 19,
    val komi: Float = 7.5f,
    val black: PlayerSeat = PlayerSeat(),
    val white: PlayerSeat = PlayerSeat(),
) : JavaSerializable {
    init {
        require(boardSize == 9 || boardSize == 13 || boardSize == 19)
        require(black.rankKyu in -2..15)
        require(white.rankKyu in -2..15)
    }

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

    fun seat(color: StoneColor): PlayerSeat =
        if (color == StoneColor.Black) black else white
}

/**
 * Old HvH / HvAI constructor. [mode] is required so this does not clash with
 * the data-class constructor that takes [black] / [white].
 */
fun GameConfig(
    boardSize: Int = 19,
    komi: Float = 7.5f,
    mode: PlayMode,
    rankKyu: Int = 5,
    humanPlaysBlack: Boolean = true,
    aiStyle: AiStyle = AiStyle.Human,
): GameConfig {
    val (black, white) = seatsFromLegacy(mode, humanPlaysBlack, aiStyle, rankKyu)
    return GameConfig(boardSize = boardSize, komi = komi, black = black, white = white)
}

fun seatsFromLegacy(
    mode: PlayMode,
    humanPlaysBlack: Boolean,
    aiStyle: AiStyle,
    rankKyu: Int,
): Pair<PlayerSeat, PlayerSeat> {
    if (mode != PlayMode.HumanVsAi) return PlayerSeat() to PlayerSeat()
    val ai = PlayerSeat(kind = aiStyle.toSeatKind(), rankKyu = rankKyu)
    val human = PlayerSeat()
    return if (humanPlaysBlack) human to ai else ai to human
}

fun rankLabel(kyu: Int): String = when {
    kyu > 0 -> "${kyu}k"
    kyu == 0 -> "1d"
    else -> "${1 - kyu}d"
}

fun rankLongLabel(kyu: Int): String = when {
    kyu > 0 -> "$kyu kyu"
    kyu == 0 -> "1 dan"
    else -> "${1 - kyu} dan"
}

fun humanSlProfile(kyu: Int): String = "preaz_${rankLabel(kyu)}"

fun parseAiStyle(id: String?): AiStyle = when (id) {
    "full" -> AiStyle.Full
    "rank" -> AiStyle.Rank
    else -> AiStyle.Human
}

fun AiStyle.toStorageId(): String = when (this) {
    AiStyle.Full -> "full"
    AiStyle.Rank -> "rank"
    AiStyle.Human -> "human"
}

fun AiStyle.toSeatKind(): SeatKind = when (this) {
    AiStyle.Full -> SeatKind.Full
    AiStyle.Rank -> SeatKind.Rank
    AiStyle.Human -> SeatKind.HumanLike
}

fun SeatKind.toAiStyle(): AiStyle = when (this) {
    SeatKind.Full -> AiStyle.Full
    SeatKind.Rank -> AiStyle.Rank
    SeatKind.Human, SeatKind.HumanLike -> AiStyle.Human
}

fun SeatKind.toStorageId(): String = when (this) {
    SeatKind.Human -> "human"
    SeatKind.Rank -> "rank"
    SeatKind.HumanLike -> "humanlike"
    SeatKind.Full -> "full"
}

fun parseSeatKind(id: String?): SeatKind = when (id) {
    "rank" -> SeatKind.Rank
    "humanlike" -> SeatKind.HumanLike
    "full" -> SeatKind.Full
    else -> SeatKind.Human
}

fun PlayerSeat.toStorageKind(): String = kind.toStorageId()
