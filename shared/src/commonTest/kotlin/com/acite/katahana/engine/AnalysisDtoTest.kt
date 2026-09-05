package com.acite.katahana.engine

import com.acite.katahana.domain.GameTree
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AnalysisDtoTest {

    @Test
    fun parsesRealEnginePayloadAndDropsUnknownFields() {
        val json = FakeAnalysisServer.reply(
            analysisJson.encodeToString(
                AnalysisQuery.serializer(),
                buildTestQuery("probe:n0:live:1"),
            ),
        )
        val response = analysisJson.decodeFromString(AnalysisResponse.serializer(), json)
        assertEquals("probe:n0:live:1", response.id)
        assertFalse(response.isDuringSearch)
        assertEquals(0, response.turnNumber)
        assertEquals("B", response.rootInfo?.currentPlayer)
        assertEquals(0.064430148, response.rootInfo?.winrate ?: 0.0, 1e-9)
        assertEquals(-0.945011317, response.rootInfo?.scoreLead ?: 0.0, 1e-9)
        assertTrue(response.moveInfos.size >= 3)
        assertEquals("E5", response.moveInfos[0].move)
        assertEquals(0, response.moveInfos[0].order)
        assertNull(response.error)
        assertEquals(emptyList(), response.ownership)
        assertEquals(listOf("E5", "G5", "F6", "D6"), response.moveInfos[0].pv)
    }

    @Test
    fun parsesOwnershipWhenRequested() {
        val query = buildLiveQuery("s", "n0", "1", GameTree(9), maxVisits = 8)
        val json = FakeAnalysisServer.reply(analysisJson.encodeToString(AnalysisQuery.serializer(), query))
        val response = analysisJson.decodeFromString(AnalysisResponse.serializer(), json)
        assertEquals(81, response.ownership.size)
        assertEquals(1.0, response.ownership.first(), 1e-6)
        assertEquals(-1.0, response.ownership.last(), 1e-6)
    }

    @Test
    fun parsesErrorFrame() {
        val json = """{"error":"Could not parse board location: ZZ","field":"moves","id":"probe:bad:1"}"""
        val response = analysisJson.decodeFromString(AnalysisResponse.serializer(), json)
        assertEquals("probe:bad:1", response.id)
        assertEquals("Could not parse board location: ZZ", response.error)
        assertEquals("moves", response.field)
    }

    @Test
    fun parsesTerminateEcho() {
        val json = """{"action":"terminate","id":"term:1","terminateId":"probe:n2:live:2"}"""
        val response = analysisJson.decodeFromString(AnalysisResponse.serializer(), json)
        assertEquals("terminate", response.action)
        assertEquals("term:1", response.id)
        assertNull(response.rootInfo)
    }

    @Test
    fun encodesLiveQueryWithOfficialFieldNames() {
        val encoded = analysisJson.encodeToString(
            AnalysisQuery.serializer(),
            AnalysisQuery(
                id = "s:n1:live:ab",
                komi = 7.5,
                boardXSize = 19,
                boardYSize = 19,
                moves = listOf(listOf("B", "Q16"), listOf("W", "D4")),
                analyzeTurns = listOf(2),
                maxVisits = 400,
                includeOwnership = false,
                includePolicy = false,
                reportDuringSearchEvery = 0.4,
            ),
        )
        assertTrue(encoded.contains("\"id\":\"s:n1:live:ab\""))
        assertTrue(encoded.contains("\"rules\":\"chinese\""))
        assertTrue(encoded.contains("\"boardXSize\":19"))
        assertTrue(encoded.contains("\"analyzeTurns\":[2]"))
        assertTrue(encoded.contains("\"includeOwnership\":false"))
        assertTrue(encoded.contains("\"includePolicy\":false"))
        assertTrue(encoded.contains("[\"B\",\"Q16\"]"))
        assertTrue(encoded.contains("\"overrideSettings\""))
        assertTrue(encoded.contains("\"reportAnalysisWinratesAs\":\"BLACK\""))
        assertFalse(encoded.contains("humanSLProfile"))
        assertFalse(encoded.contains("ignorePreRootHistory"))
        assertFalse(encoded.contains("ownershipMap"))
    }

    @Test
    fun parsesHumanPolicyWhenRequested() {
        val query = buildHumanQuery("s", "n0", "1", GameTree(9), rankKyu = 5)
        val json = FakeAnalysisServer.reply(analysisJson.encodeToString(AnalysisQuery.serializer(), query))
        val response = analysisJson.decodeFromString(AnalysisResponse.serializer(), json)
        assertEquals(82, response.humanPolicy.size)
        assertEquals(82, response.policy.size)
        assertTrue(response.humanPolicy[40] > 0.4)
    }

    @Test
    fun encodesTerminateWithTerminateId() {
        val encoded = analysisJson.encodeToString(
            TerminateQuery.serializer(),
            TerminateQuery(id = "term:1", terminateId = "s:n1:live:ab"),
        )
        assertTrue(encoded.contains("\"action\":\"terminate\""))
        assertTrue(encoded.contains("\"terminateId\":\"s:n1:live:ab\""))
    }
}
