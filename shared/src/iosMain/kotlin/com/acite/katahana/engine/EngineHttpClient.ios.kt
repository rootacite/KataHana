package com.acite.katahana.engine

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.websocket.WebSockets

internal actual fun createEngineHttpClient(): HttpClient = HttpClient(Darwin) {
    install(WebSockets)
}
