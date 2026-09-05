package com.acite.katahana.recents

import com.acite.katahana.domain.AiStyle
import com.acite.katahana.domain.GameConfig
import com.acite.katahana.domain.GameTree
import com.acite.katahana.domain.PlayMode
import com.acite.katahana.domain.Point
import com.acite.katahana.domain.Move
import com.acite.katahana.sgf.writeSgf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking

class RecentGamesIndexTest {

    @Test
    fun upsertMovesExistingToFrontAndReplaces() {
        val a = sample("a", "One")
        val b = sample("b", "Two")
        val first = RecentGamesIndex.upsert(emptyList(), a)
        val both = RecentGamesIndex.upsert(first, b)
        assertEquals(listOf("b", "a"), both.map { it.id })
        val updated = RecentGamesIndex.upsert(both, a.copy(title = "One*"))
        assertEquals(listOf("a", "b"), updated.map { it.id })
        assertEquals("One*", updated.first().title)
    }

    @Test
    fun capDropsOldest() {
        val games = (1..5).map { sample("id$it", "G$it") }
        var list = emptyList<RecentGame>()
        games.forEach { list = RecentGamesIndex.upsert(list, it, cap = 3) }
        assertEquals(listOf("id5", "id4", "id3"), list.map { it.id })
    }

    @Test
    fun removeDropsById() {
        val list = listOf(sample("a", "A"), sample("b", "B"))
        assertEquals(listOf("b"), RecentGamesIndex.remove(list, "a").map { it.id })
    }

    @Test
    fun encodeDecodeRoundTrip() {
        val list = listOf(sample("a", "Alpha", path = listOf(0, 1)))
        val decoded = RecentGamesIndex.decode(RecentGamesIndex.encode(list))
        assertEquals(list, decoded)
    }

    @Test
    fun encodeDecodeKeepsEvals() {
        val evals = listOf(
            PersistedEval(
                path = listOf(0),
                blackWinrate = 0.61,
                blackScoreLead = 3.5,
                visits = 400,
                toPlay = "W",
                pointsLost = 0.4,
            ),
            PersistedEval(
                path = listOf(0, 1),
                pointsLost = 12.0,
            ),
        )
        val list = listOf(sample("a", "Alpha", path = listOf(0, 1)).copy(evals = evals))
        val decoded = RecentGamesIndex.decode(RecentGamesIndex.encode(list))
        assertEquals(evals, decoded.single().evals)
    }

    @Test
    fun defaultTitleAndConfig() {
        val hvh = GameConfig(boardSize = 13, mode = PlayMode.HumanVsHuman)
        assertEquals("13×13 · Human vs Human", defaultRecentTitle(hvh))
        val hvai = GameConfig(
            boardSize = 19,
            mode = PlayMode.HumanVsAi,
            rankKyu = 5,
            aiStyle = AiStyle.Rank,
        )
        assertEquals("19×19 · vs 5k", defaultRecentTitle(hvai))
        val record = sample("x", "X").copy(
            boardSize = 9,
            mode = "hvai",
            rankKyu = 0,
            aiStyle = "full",
            humanPlaysBlack = false,
        )
        val config = record.toConfig()
        assertEquals(9, config.boardSize)
        assertEquals(PlayMode.HumanVsAi, config.mode)
        assertEquals(AiStyle.Full, config.aiStyle)
        assertEquals(false, config.humanPlaysBlack)
    }

    @Test
    fun relativeTimeLabels() {
        val now = 1_000_000L
        assertEquals("Just now", formatSavedAt(now - 10_000, now))
        assertEquals("5m ago", formatSavedAt(now - 5 * 60_000, now))
        assertEquals("3h ago", formatSavedAt(now - 3 * 3_600_000, now))
        assertEquals("2d ago", formatSavedAt(now - 2 * 86_400_000, now))
    }

    @Test
    fun repositoryOpenRestoresConfigAndPath() = runBlocking {
        val tree = GameTree(9)
        tree.play(Point(2, 2))
        tree.play(Point(3, 3))
        val config = GameConfig(
            boardSize = 9,
            mode = PlayMode.HumanVsAi,
            rankKyu = 3,
            humanPlaysBlack = false,
            aiStyle = AiStyle.Rank,
        )
        val record = RecentGame(
            id = "g1",
            title = "Night game",
            savedAt = 10,
            createdAt = 1,
            moveNumber = tree.current.moveNumber,
            boardSize = 9,
            komi = 7.5f,
            mode = config.toRecentMode(),
            rankKyu = config.rankKyu,
            humanPlaysBlack = config.humanPlaysBlack,
            aiStyle = config.toRecentAiStyle(),
            sgf = writeSgf(tree, config),
            currentPath = tree.childPath(),
        )
        val repo = RecentGamesRepository(MemoryTextStore())
        repo.upsert(record)
        val loaded = requireNotNull(repo.open("g1"))
        assertEquals("Night game", loaded.record.title)
        assertEquals(PlayMode.HumanVsAi, loaded.config.mode)
        assertEquals(3, loaded.config.rankKyu)
        assertEquals(false, loaded.config.humanPlaysBlack)
        assertEquals(Point(3, 3), (loaded.tree.current.move as Move.Place).point)
        assertEquals(2, loaded.tree.current.moveNumber)
    }

    private fun sample(id: String, title: String, path: List<Int> = emptyList()) = RecentGame(
        id = id,
        title = title,
        savedAt = 1,
        createdAt = 1,
        moveNumber = 2,
        boardSize = 19,
        komi = 7.5f,
        mode = "hvh",
        sgf = "(;FF[4]GM[1]SZ[19])",
        currentPath = path,
    )
}
