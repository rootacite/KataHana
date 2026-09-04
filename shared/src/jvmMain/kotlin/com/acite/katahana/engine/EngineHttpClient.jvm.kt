package com.acite.katahana.engine

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.WebSockets

internal actual fun createEngineHttpClient(): HttpClient = HttpClient(CIO) {
    install(WebSockets)
    engine {
        requestTimeout = 0
    }
}
