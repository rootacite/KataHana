package com.acite.katahana.recents

import com.acite.katahana.domain.AiStyle
import com.acite.katahana.domain.GameConfig
import com.acite.katahana.domain.PlayMode
import com.acite.katahana.domain.parseAiStyle
import com.acite.katahana.domain.rankLabel
import com.acite.katahana.domain.toStorageId
import kotlinx.serialization.Serializable

@Serializable
data class RecentGame(
    val id: String,
    val title: String,
    val savedAt: Long,
    val createdAt: Long,
    val moveNumber: Int,
    val boardSize: Int,
    val komi: Float,
    val mode: String,
    val rankKyu: Int = 5,
    val humanPlaysBlack: Boolean = true,
    val aiStyle: String = "rank",
    val sgf: String,
    val currentPath: List<Int> = emptyList(),
    val evals: List<PersistedEval> = emptyList(),
) {
    fun toConfig(): GameConfig = GameConfig(
        boardSize = if (boardSize == 9 || boardSize == 13 || boardSize == 19) boardSize else 19,
        komi = komi,
        mode = if (mode == "hvai") PlayMode.HumanVsAi else PlayMode.HumanVsHuman,
        rankKyu = rankKyu.coerceIn(-2, 15),
        humanPlaysBlack = humanPlaysBlack,
        aiStyle = parseAiStyle(aiStyle),
    )

    fun modeLine(): String {
        val match = when {
            mode != "hvai" -> "Human vs Human"
            aiStyle == "full" -> "vs Full"
            else -> "vs ${rankLabel(rankKyu)}"
        }
        return "${boardSize}×${boardSize}  ·  Move $moveNumber  ·  $match"
    }
}

@Serializable
data class PersistedEval(
    val path: List<Int>,
    val blackWinrate: Double? = null,
    val blackScoreLead: Double? = null,
    val visits: Int = 0,
    val toPlay: String = "B",
    val pointsLost: Double? = null,
)

fun GameConfig.toRecentMode(): String =
    if (mode == PlayMode.HumanVsAi) "hvai" else "hvh"

fun GameConfig.toRecentAiStyle(): String = aiStyle.toStorageId()

fun defaultRecentTitle(config: GameConfig): String = when (config.mode) {
    PlayMode.HumanVsHuman -> "${config.boardSize}×${config.boardSize} · Human vs Human"
    PlayMode.HumanVsAi -> {
        val ai = if (config.aiStyle == AiStyle.Full) "Full" else rankLabel(config.rankKyu)
        "${config.boardSize}×${config.boardSize} · vs $ai"
    }
}

fun formatSavedAt(thenMs: Long, nowMs: Long): String {
    val sec = ((nowMs - thenMs).coerceAtLeast(0L)) / 1000L
    return when {
        sec < 60 -> "Just now"
        sec < 3600 -> "${sec / 60}m ago"
        sec < 86_400 -> "${sec / 3600}h ago"
        sec < 86_400 * 7 -> "${sec / 86_400}d ago"
        else -> "${sec / (86_400 * 7)}w ago"
    }
}
