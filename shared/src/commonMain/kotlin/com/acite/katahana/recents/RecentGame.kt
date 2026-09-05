package com.acite.katahana.recents

import com.acite.katahana.domain.GameConfig
import com.acite.katahana.domain.PlayMode
import com.acite.katahana.domain.PlayerSeat
import com.acite.katahana.domain.SeatKind
import com.acite.katahana.domain.parseAiStyle
import com.acite.katahana.domain.parseSeatKind
import com.acite.katahana.domain.rankLabel
import com.acite.katahana.domain.seatsFromLegacy
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
    val blackKind: String? = null,
    val whiteKind: String? = null,
    val blackRankKyu: Int = 5,
    val whiteRankKyu: Int = 5,
    val sgf: String,
    val currentPath: List<Int> = emptyList(),
    val evals: List<PersistedEval> = emptyList(),
) {
    fun toConfig(): GameConfig {
        val size = if (boardSize == 9 || boardSize == 13 || boardSize == 19) boardSize else 19
        val (black, white) = if (blackKind != null || whiteKind != null) {
            PlayerSeat(
                kind = parseSeatKind(blackKind),
                rankKyu = blackRankKyu.coerceIn(-2, 15),
            ) to PlayerSeat(
                kind = parseSeatKind(whiteKind),
                rankKyu = whiteRankKyu.coerceIn(-2, 15),
            )
        } else {
            seatsFromLegacy(
                mode = if (mode == "hvai") PlayMode.HumanVsAi else PlayMode.HumanVsHuman,
                humanPlaysBlack = humanPlaysBlack,
                aiStyle = parseAiStyle(aiStyle),
                rankKyu = rankKyu.coerceIn(-2, 15),
            )
        }
        return GameConfig(boardSize = size, komi = komi, black = black, white = white)
    }

    fun modeLine(): String {
        val config = toConfig()
        return "${boardSize}×${boardSize}  ·  Move $moveNumber  ·  ${matchLine(config)}"
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

fun matchLine(config: GameConfig): String =
    "${seatSummary(config.black)} vs ${seatSummary(config.white)}"

fun defaultRecentTitle(config: GameConfig): String =
    "${config.boardSize}×${config.boardSize} · ${matchLine(config)}"

fun seatName(seat: PlayerSeat): String = when (seat.kind) {
    SeatKind.Human -> "Human"
    SeatKind.Rank -> "Rank AI"
    SeatKind.HumanLike -> "Human-like"
    SeatKind.Full -> "KataGo"
}

fun seatRankChip(seat: PlayerSeat): String? = when (seat.kind) {
    SeatKind.Human -> null
    SeatKind.Rank, SeatKind.HumanLike -> rankLabel(seat.rankKyu)
    SeatKind.Full -> "9D+"
}

fun seatSummary(seat: PlayerSeat): String {
    val rank = seatRankChip(seat) ?: return seatName(seat)
    return "${seatName(seat)} $rank"
}

fun seatSgfName(seat: PlayerSeat): String = seatSummary(seat)

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
