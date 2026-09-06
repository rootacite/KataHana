package com.acite.katahana.engine

import com.acite.katahana.domain.GameTree
import com.acite.katahana.domain.Move
import com.acite.katahana.domain.Point
import com.acite.katahana.domain.StoneColor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QueryBuilderTest {

    @Test
    fun emptyBoardAnalyzesTurnZero() {
        val tree = GameTree(9)
        val query = buildLiveQuery("sess", tree.current.id, "1", tree, maxVisits = 8)
        assertEquals(emptyList(), query.moves)
        assertEquals(listOf(0), query.analyzeTurns)
        assertEquals(9, query.boardXSize)
        assertEquals(9, query.boardYSize)
        assertEquals("chinese", query.rules)
        assertEquals(7.5, query.komi, 1e-6)
        assertTrue(query.id.contains(":live:"))
        assertTrue(query.id.startsWith("sess:${tree.current.id}:live:"))
        assertEquals(true, query.includeOwnership)
        assertEquals(false, query.includePolicy)
        assertEquals(12, query.analysisPVLen)
        assertEquals("BLACK", query.overrideSettings.reportAnalysisWinratesAs)
    }

    @Test
    fun reviewQueryUsesPathMovesAndReviewPurpose() {
        val tree = GameTree(9)
        tree.play(Point(4, 4))
        val node = tree.current
        tree.play(Point(3, 3))
        val currentId = tree.current.id
        val moves = tree.pathMoves(node)
        val query = buildReviewQuery("sess", node.id, "ab", tree, moves, maxVisits = 400)
        assertTrue(query.id.contains(":review:"))
        assertTrue(query.id.startsWith("sess:${node.id}:review:"))
        assertEquals(true, query.includeOwnership)
        assertEquals(12, query.analysisPVLen)
        assertEquals(400, query.maxVisits)
        assertEquals(listOf(listOf("B", "E5")), query.moves)
        assertEquals(listOf(1), query.analyzeTurns)
        assertEquals(currentId, tree.current.id)
    }

    @Test
    fun rankQueryAsksPolicyAtOneVisit() {
        val tree = GameTree(9)
        val query = buildRankQuery("sess", tree.current.id, "1", tree)
        assertTrue(query.id.contains(":rank:"))
        assertEquals(true, query.includePolicy)
        assertEquals(true, query.includeOwnership)
        assertEquals(1, query.maxVisits)
        assertEquals(listOf(0), query.analyzeTurns)
        assertEquals(0.4, query.reportDuringSearchEvery)
        assertEquals(null, query.overrideSettings.humanSLProfile)
    }

    @Test
    fun humanQueryAsksHumanPolicyAtOneVisit() {
        val tree = GameTree(9)
        val query = buildHumanQuery("sess", tree.current.id, "1", tree, rankKyu = 5)
        assertTrue(query.id.contains(":human:"))
        assertEquals(true, query.includePolicy)
        assertEquals(true, query.includeOwnership)
        assertEquals(1, query.maxVisits)
        assertEquals("preaz_5k", query.overrideSettings.humanSLProfile)
        assertEquals(false, query.overrideSettings.ignorePreRootHistory)
        val encoded = analysisJson.encodeToString(AnalysisQuery.serializer(), query)
        assertTrue(encoded.contains("\"humanSLProfile\":\"preaz_5k\""))
        assertTrue(encoded.contains("\"ignorePreRootHistory\":false"))
        val live = analysisJson.encodeToString(
            AnalysisQuery.serializer(),
            buildLiveQuery("sess", tree.current.id, "1", tree, maxVisits = 8),
        )
        assertTrue(!live.contains("humanSLProfile"))
        val rank = analysisJson.encodeToString(
            AnalysisQuery.serializer(),
            buildRankQuery("sess", tree.current.id, "1", tree),
        )
        assertTrue(!rank.contains("humanSLProfile"))
        val gen = analysisJson.encodeToString(
            AnalysisQuery.serializer(),
            buildGenmoveQuery("sess", tree.current.id, "1", tree, maxVisits = 400),
        )
        assertTrue(!gen.contains("humanSLProfile"))
    }

    @Test
    fun forecastQueryAppendsOriginAndAsksFifteenPv() {
        val tree = GameTree(9)
        tree.play(Point(4, 4))
        val origin = Point(3, 3)
        val query = buildForecastQuery("sess", tree.current.id, "ab", tree, origin, maxVisits = 400)
        assertTrue(query.id.contains(":forecast:"))
        assertTrue(query.id.startsWith("sess:${tree.current.id}:forecast:"))
        assertEquals(
            listOf(listOf("B", "E5"), listOf("W", "D6")),
            query.moves,
        )
        assertEquals(listOf(2), query.analyzeTurns)
        assertEquals(15, query.analysisPVLen)
        assertEquals(400, query.maxVisits)
        assertEquals(true, query.includeOwnership)
        assertEquals(false, query.includePolicy)
        assertEquals(null, query.reportDuringSearchEvery)
    }

    @Test
    fun forecastOwnershipQuerySkipsPlyOneAndKeepsOwnership() {
        val tree = GameTree(9)
        val origin = Point(4, 4)
        val continuation = listOf(
            Move.Place(StoneColor.Black, origin),
            Move.Place(StoneColor.White, Point(3, 3)),
            Move.Place(StoneColor.Black, Point(5, 5)),
        )
        val query = buildForecastOwnershipQuery(
            "sess",
            tree.current.id,
            "cd",
            tree,
            continuation,
            maxVisits = 400,
        )
        assertTrue(query.id.contains(":forecast-own:"))
        assertEquals(listOf(2, 3), query.analyzeTurns)
        assertEquals(3, query.moves.size)
        assertEquals(true, query.includeOwnership)
        assertEquals(false, query.includePolicy)
        assertEquals(400, query.maxVisits)
        assertEquals(15, query.analysisPVLen)
        assertEquals(null, query.reportDuringSearchEvery)
    }

    @Test
    fun forecastOwnershipQueryEmptyWhenOnlyOrigin() {
        val tree = GameTree(9)
        val continuation = listOf(
            Move.Place(StoneColor.Black, Point(4, 4)),
        )
        val query = buildForecastOwnershipQuery("sess", tree.current.id, "ef", tree, continuation, 80)
        assertEquals(emptyList(), query.analyzeTurns)
        assertEquals(1, query.moves.size)
    }

    @Test
    fun genmoveQueryUsesPlayVisits() {
        val tree = GameTree(9)
        val query = buildGenmoveQuery("sess", tree.current.id, "1", tree, maxVisits = 400)
        assertTrue(query.id.contains(":genmove:"))
        assertEquals(400, query.maxVisits)
        assertEquals(false, query.includePolicy)
        assertEquals(true, query.includeOwnership)
        assertEquals(0.4, query.reportDuringSearchEvery)
    }

    @Test
    fun benchQueriesUseEmptyNineteenAndNoLiveReports() {
        val policy = buildBenchPolicyQuery("bench:policy:0:ab")
        assertEquals("bench:policy:0:ab", policy.id)
        assertEquals(19, policy.boardXSize)
        assertEquals(19, policy.boardYSize)
        assertEquals(emptyList(), policy.moves)
        assertEquals(listOf(0), policy.analyzeTurns)
        assertEquals(1, policy.maxVisits)
        assertEquals(true, policy.includePolicy)
        assertEquals(true, policy.includeOwnership)
        assertEquals(null, policy.reportDuringSearchEvery)
        assertEquals(null, policy.overrideSettings.humanSLProfile)

        val search = buildBenchSearchQuery("bench:search:cd")
        assertEquals(BENCH_PLAY_VISITS, search.maxVisits)
        assertEquals(false, search.includePolicy)
        assertEquals(true, search.includeOwnership)
        assertEquals(null, search.reportDuringSearchEvery)
        assertEquals(19, search.boardXSize)
        val stress = buildBenchSearchQuery("bench:search:2000:cd", BENCH_STRESS_VISITS)
        assertEquals(2_000, stress.maxVisits)
        assertEquals(false, stress.includePolicy)
        assertEquals(19, stress.boardXSize)

        val human = buildBenchHumanQuery("bench:human:ef")
        assertEquals(1, human.maxVisits)
        assertEquals(true, human.includePolicy)
        assertEquals("preaz_5k", human.overrideSettings.humanSLProfile)
        assertEquals(false, human.overrideSettings.ignorePreRootHistory)
        val encoded = analysisJson.encodeToString(AnalysisQuery.serializer(), human)
        assertTrue(encoded.contains("\"humanSLProfile\":\"preaz_5k\""))
        val policyJson = analysisJson.encodeToString(AnalysisQuery.serializer(), policy)
        assertTrue(!policyJson.contains("humanSLProfile"))
    }

    @Test
    fun gtpSkipsIAndRecordsPass() {
        val tree = GameTree(19)
        tree.play(Point(8, 0))
        tree.pass()
        val query = buildLiveQuery("s", tree.current.id, "n", tree, 400)
        assertEquals(
            listOf(listOf("B", "J19"), listOf("W", "pass")),
            query.moves,
        )
        assertEquals(listOf(2), query.analyzeTurns)
    }
}
