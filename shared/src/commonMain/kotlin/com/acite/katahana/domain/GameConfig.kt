package com.acite.katahana.domain

enum class PlayMode {
    HumanVsHuman,
    HumanVsAi,
}

enum class AiStyle {
    Rank,
    Full,
}

data class GameConfig(
    val boardSize: Int = 19,
    val komi: Float = 7.5f,
    val mode: PlayMode = PlayMode.HumanVsHuman,
    val rankKyu: Int = 5,
    val humanPlaysBlack: Boolean = true,
    val aiStyle: AiStyle = AiStyle.Rank,
) {
    init {
        require(boardSize == 9 || boardSize == 13 || boardSize == 19)
        require(rankKyu in -2..15)
    }
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
