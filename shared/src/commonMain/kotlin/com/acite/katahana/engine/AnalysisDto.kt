package com.acite.katahana.engine

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * KataGo Analysis Engine JSON, verified against the local WS gateway
 * (KataGo 1.18.1, Python websockets/16, one text frame per object).
 */
val analysisJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
    isLenient = true
}

@Serializable
data class AnalysisQuery(
    val id: String,
    val rules: String = "chinese",
    val komi: Double,
    val boardXSize: Int,
    val boardYSize: Int,
    val moves: List<List<String>> = emptyList(),
    val analyzeTurns: List<Int>? = null,
    val maxVisits: Int? = null,
    val includeOwnership: Boolean = false,
    val includePolicy: Boolean = false,
    val reportDuringSearchEvery: Double? = null,
    val analysisPVLen: Int? = null,
    val overrideSettings: OverrideSettings = OverrideSettings(),
)

@Serializable
data class OverrideSettings(
    val reportAnalysisWinratesAs: String = "BLACK",
)

@Serializable
data class TerminateQuery(
    val id: String,
    val action: String = "terminate",
    val terminateId: String,
)

@Serializable
data class AnalysisResponse(
    val id: String? = null,
    val isDuringSearch: Boolean = false,
    val error: String? = null,
    val field: String? = null,
    val warning: String? = null,
    val action: String? = null,
    val version: String? = null,
    val turnNumber: Int? = null,
    val rootInfo: RootInfo? = null,
    val moveInfos: List<MoveInfo> = emptyList(),
    val policy: List<Double> = emptyList(),
    val ownership: List<Double> = emptyList(),
)

@Serializable
data class RootInfo(
    val winrate: Double = 0.0,
    val scoreLead: Double = 0.0,
    val visits: Int = 0,
    val currentPlayer: String? = null,
)

@Serializable
data class MoveInfo(
    val move: String = "pass",
    val order: Int = 0,
    val visits: Int = 0,
    val winrate: Double = 0.0,
    val scoreLead: Double = 0.0,
    val prior: Double? = null,
    val pv: List<String> = emptyList(),
)
