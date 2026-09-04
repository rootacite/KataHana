package com.acite.katahana.engine

/**
 * In-process stand-in for the local WS gateway. Returns KataGo-shaped JSON
 * including extra fields the real 1.18.1 engine sent (those must be ignored).
 */
object FakeAnalysisServer {
    fun reply(requestJson: String): String {
        val query = analysisJson.decodeFromString(AnalysisQuery.serializer(), requestJson)
        val turn = query.analyzeTurns?.lastOrNull() ?: query.moves.size
        val toPlay = if (turn % 2 == 0) "B" else "W"
        val winrate = if (toPlay == "B") 0.064430148 else 0.365329923
        val lead = if (toPlay == "B") -0.945011317 else -0.848749292
        return """
            {
              "id": "${query.id}",
              "isDuringSearch": false,
              "turnNumber": $turn,
              "rootInfo": {
                "currentPlayer": "$toPlay",
                "rawLead": -1.35969257,
                "rawWinrate": 0.0601626337,
                "scoreLead": $lead,
                "utility": -0.87872291,
                "visits": ${query.maxVisits ?: 8},
                "weight": 17.253783937464846,
                "winrate": $winrate
              },
              "moveInfos": [
                {
                  "edgeVisits": 6,
                  "lcb": 0.05,
                  "move": "E5",
                  "order": 0,
                  "prior": 0.488737941,
                  "pv": ["E5", "G5"],
                  "scoreLead": -0.886509664,
                  "visits": 6,
                  "winrate": 0.0666426835
                },
                {
                  "move": "F5",
                  "order": 1,
                  "prior": 0.074267678,
                  "pv": ["F5"],
                  "scoreLead": -0.787197828,
                  "visits": 1,
                  "winrate": 0.0454937462
                },
                {
                  "move": "pass",
                  "order": 2,
                  "prior": 0.01,
                  "pv": ["pass"],
                  "scoreLead": -1.2,
                  "visits": 1,
                  "winrate": 0.02
                },
                {
                  "move": "D5",
                  "order": 3,
                  "prior": 0.074267678,
                  "pv": ["D5"],
                  "scoreLead": -0.787197828,
                  "visits": 1,
                  "winrate": 0.0454937462
                }
              ]
            }
        """.trimIndent()
    }
}
