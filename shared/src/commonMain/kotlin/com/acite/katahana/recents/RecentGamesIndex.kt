package com.acite.katahana.recents

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

const val RECENT_CAP = 40

@Serializable
internal data class RecentGamesFile(
    val games: List<RecentGame> = emptyList(),
)

internal val recentJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

object RecentGamesIndex {
    fun upsert(games: List<RecentGame>, incoming: RecentGame, cap: Int = RECENT_CAP): List<RecentGame> {
        val without = games.filter { it.id != incoming.id }
        return (listOf(incoming) + without).take(cap)
    }

    fun remove(games: List<RecentGame>, id: String): List<RecentGame> =
        games.filter { it.id != id }

    fun encode(games: List<RecentGame>): String =
        recentJson.encodeToString(RecentGamesFile.serializer(), RecentGamesFile(games))

    fun decode(text: String): List<RecentGame> =
        recentJson.decodeFromString(RecentGamesFile.serializer(), text).games
}
