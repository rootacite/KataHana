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

data class GameConfig(
    val boardSize: Int = 19,
    val komi: Float = 7.5f,
    val mode: PlayMode = PlayMode.HumanVsHuman,
    val rankKyu: Int = 5,
    val humanPlaysBlack: Boolean = true,
    val aiStyle: AiStyle = AiStyle.Human,
) : JavaSerializable {
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
