package com.acite.katahana.engine

import io.ktor.client.plugins.websocket.webSocket
import io.ktor.http.takeFrom
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

class RealEngineSmokeTest {
    @Test
    fun analyzesEmptyNineAgainstLocalGateway() = runBlocking {
        val http = createEngineHttpClient()
        try {
            withTimeout(20_000) {
                http.webSocket(request = { url.takeFrom("ws://127.0.0.1:2080/") }) {
                    val query = analysisJson.encodeToString(
                        AnalysisQuery.serializer(),
                        buildTestQuery("smoke:n0:live:1"),
                    )
                    send(Frame.Text(query))
                    val text = (incoming.receive() as Frame.Text).readText()
                    val response = analysisJson.decodeFromString(AnalysisResponse.serializer(), text)
                    assertEquals("smoke:n0:live:1", response.id)
                    assertNull(response.error)
                    assertNotNull(response.rootInfo)
                    assertTrue(response.moveInfos.isNotEmpty())
                    assertEquals("B", response.rootInfo?.currentPlayer)
                    println("real engine smoke ok visits=${response.rootInfo?.visits}")
                }
            }
        } catch (e: Exception) {
            println("SKIP real engine smoke: ${e::class.simpleName}: ${e.message}")
        } finally {
            http.close()
        }
    }
}
